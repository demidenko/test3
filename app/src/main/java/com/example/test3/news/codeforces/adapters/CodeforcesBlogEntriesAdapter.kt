package com.example.test3.news.codeforces.adapters

import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.Group
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.test3.*
import com.example.test3.account_manager.CodeforcesAccountManager
import com.example.test3.account_manager.CodeforcesUserInfo
import com.example.test3.utils.*
import com.example.test3.workers.CodeforcesNewsFollowWorker
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


class CodeforcesBlogEntriesAdapter(
    fragment: Fragment,
    dataFlow: Flow<List<CodeforcesBlogEntry>>,
    private val viewedBlogEntriesIdsFlow: Flow<Set<Int>>?,
    val clearNewEntriesOnDataChange: Boolean = true
): CodeforcesNewsItemsTimedAdapter<CodeforcesBlogEntriesAdapter.CodeforcesBlogEntryViewHolder, List<CodeforcesBlogEntry>>(
    fragment, dataFlow
) {

    private var items: Array<CodeforcesBlogEntry> = emptyArray()
    override fun getItemCount() = items.size

    fun getBlogIDs() = items.map { it.id }

    private val newEntries = MutableSetLiveSize<Int>()
    fun getNewEntriesSizeFlow() = newEntries.sizeStateFlow

    override suspend fun applyData(data: List<CodeforcesBlogEntry>): DiffUtil.DiffResult {
        val oldItems = items
        val oldNewEntries = newEntries.values()
        items = data.toTypedArray()
        manageNewEntries()
        return DiffUtil.calculateDiff(diffCallback(oldItems, items, oldNewEntries, newEntries.values()))
    }

    private suspend fun manageNewEntries() {
        val savedBlogEntries = viewedBlogEntriesIdsFlow?.first() ?: return
        val currentBlogEntries = getBlogIDs()
        if(clearNewEntriesOnDataChange) newEntries.clear()
        else {
            newEntries.removeAll(newEntries.values().filter { it !in currentBlogEntries })
        }
        val newBlogEntries = currentBlogEntries.filter { it !in savedBlogEntries }
        newEntries.addAll(newBlogEntries)
    }



    class CodeforcesBlogEntryViewHolder(val view: ConstraintLayout) : RecyclerView.ViewHolder(view), TimeDepends {
        val title: TextView = view.findViewById(R.id.news_item_title)
        val author: TextView = view.findViewById(R.id.news_item_author)
        private val time: TextView = view.findViewById(R.id.news_item_time)
        private val rating: TextView = view.findViewById(R.id.news_item_rating)
        private val commentsCount: TextView = view.findViewById(R.id.news_item_comments_count)
        private val comments: Group = view.findViewById(R.id.news_item_comments)
        private val newEntryIndicator: View = view.findViewById(R.id.news_item_dot_new)

        fun setAuthor(handle: String, colorTag: CodeforcesUtils.ColorTag, manager: CodeforcesAccountManager) {
            author.text = manager.makeSpan(handle, colorTag)
        }

        fun setNewEntryIndicator(isNew: Boolean) {
            newEntryIndicator.isVisible = isNew
        }

        fun setRating(rating: Int) {
            this.rating.apply {
                if(rating == 0) isGone = true
                else {
                    isVisible = true
                    text = signedToString(rating)
                    setTextColor(getColorFromResource(context,
                        if (rating > 0) R.color.blog_rating_positive
                        else R.color.blog_rating_negative
                    ))
                }
            }
        }

        fun setComments(commentsCount: Int) {
            this.commentsCount.text = commentsCount.toString()
            comments.isGone = commentsCount == 0
        }

        override var startTimeSeconds: Long = 0
        override fun refreshTime(currentTimeSeconds: Long) {
            time.text = timeDifference(startTimeSeconds, currentTimeSeconds)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CodeforcesBlogEntryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.cf_news_page_item, parent, false) as ConstraintLayout
        return CodeforcesBlogEntryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CodeforcesBlogEntryViewHolder, position: Int, payloads: MutableList<Any>) {
        payloads.forEach {
            if(it is List<*>) {
                val blogEntry = items[position]
                it.forEach { obj ->
                    when(obj) {
                        is NEW_ENTRY -> holder.setNewEntryIndicator(blogEntry.id in newEntries)
                        is UPDATE_RATING -> holder.setRating(blogEntry.rating)
                        is UPDATE_COMMENTS -> holder.setComments(blogEntry.commentsCount)
                    }
                }
            }
        }
        super.onBindViewHolder(holder, position, payloads)
    }

    override fun onBindViewHolder(holder: CodeforcesBlogEntryViewHolder, position: Int) {
        with(holder){
            val blogEntry = items[position]

            val blogId = blogEntry.id
            view.setOnClickListener {
                if(blogId in newEntries){
                    newEntries.remove(blogId)
                    notifyItemChanged(bindingAdapterPosition, listOf(NEW_ENTRY))
                }
                it.context.startActivity(makeIntentOpenUrl(CodeforcesURLFactory.blog(blogId)))
            }

            view.isLongClickable = true
            view.setOnLongClickListener {
                val mainActivity = it.context!! as MainActivity
                runBlocking { CodeforcesNewsFollowWorker.isEnabled(mainActivity) }.apply {
                    if(this) addToFollowListWithSnackBar(this@with, mainActivity)
                }
            }

            title.text = blogEntry.title

            setAuthor(blogEntry.authorHandle, blogEntry.authorColorTag, codeforcesAccountManager)
            setNewEntryIndicator(blogId in newEntries)
            setComments(blogEntry.commentsCount)
            setRating(blogEntry.rating)

            startTimeSeconds = blogEntry.creationTimeSeconds
            refreshTime(getCurrentTimeSeconds())
        }
    }

    override fun refreshHandles(holder: CodeforcesBlogEntryViewHolder, position: Int) {
        val blogEntry = items[position]
        holder.setAuthor(blogEntry.authorHandle, blogEntry.authorColorTag, codeforcesAccountManager)
    }

    companion object {
        private object NEW_ENTRY
        private object UPDATE_RATING
        private object UPDATE_COMMENTS

        private fun diffCallback(
            old: Array<CodeforcesBlogEntry>,
            new: Array<CodeforcesBlogEntry>,
            oldNewEntries: Set<Int>,
            newNewEntries: Set<Int>,
        ) =
            object : DiffUtil.Callback() {
                override fun getOldListSize() = old.size
                override fun getNewListSize() = new.size

                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    return old[oldItemPosition].id == new[newItemPosition].id
                }

                override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    return old[oldItemPosition] == new[newItemPosition]
                }

                override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): List<Any>? {
                    val oldBlogEntry = old[oldItemPosition]
                    val newBlogEntry = new[newItemPosition]
                    if(oldBlogEntry.title != newBlogEntry.title) return null
                    if(oldBlogEntry.authorHandle != newBlogEntry.authorHandle) return null
                    if(oldBlogEntry.authorColorTag != newBlogEntry.authorColorTag) return null
                    val id = newBlogEntry.id
                    val res = mutableListOf<Any>()
                    if(oldBlogEntry.rating != newBlogEntry.rating) res.add(UPDATE_RATING)
                    if(oldBlogEntry.commentsCount != newBlogEntry.commentsCount) res.add(UPDATE_COMMENTS)
                    if(id in oldNewEntries != id in newNewEntries) res.add(NEW_ENTRY)
                    return res.takeIf { it.isNotEmpty() }
                }

            }

        private fun addToFollowListWithSnackBar(holder: CodeforcesBlogEntryViewHolder, mainActivity: MainActivity){
            mainActivity.newsFragment.lifecycleScope.launch {
                val connector = CodeforcesNewsFollowWorker.FollowDataConnector(mainActivity)
                val handle = holder.author.text
                val userInfo = CodeforcesAccountManager(mainActivity).loadInfo(handle.toString()) as CodeforcesUserInfo
                when(connector.add(userInfo)){
                    true -> {
                        Snackbar.make(holder.view, SpannableStringBuilder("You now followed ").append(handle), Snackbar.LENGTH_LONG).apply {
                            setAction("Manage"){
                                mainActivity.newsFragment.showCodeforcesFollowListManager()
                            }
                        }
                    }
                    false -> {
                        Snackbar.make(holder.view, SpannableStringBuilder("You already followed ").append(handle), Snackbar.LENGTH_LONG)
                    }
                }.setAnchorView(mainActivity.navigation).show()
            }
        }
    }
}
