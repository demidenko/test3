package com.example.test3.news

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.Group
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.addRepeatingJob
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.test3.R
import com.example.test3.account_manager.CodeforcesAccountManager
import com.example.test3.account_manager.CodeforcesUserInfo
import com.example.test3.news.codeforces.CodeforcesBlogEntriesFragment
import com.example.test3.news.codeforces.CodeforcesNewsViewModel
import com.example.test3.news.codeforces.adapters.CodeforcesNewsItemsAdapter
import com.example.test3.room.CodeforcesUserBlog
import com.example.test3.room.getFollowDao
import com.example.test3.ui.CPSFragment
import com.example.test3.ui.settingsUI
import com.example.test3.utils.disable
import com.example.test3.utils.enable
import com.example.test3.utils.ignoreFirst
import com.example.test3.workers.CodeforcesNewsFollowWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ManageCodeforcesFollowListFragment: CPSFragment() {

    private val newsViewModel: CodeforcesNewsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_manage_cf_follow, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cpsTitle = "::news.codeforces.follow.list"
        setBottomPanelId(R.id.support_navigation_cf_follow, R.layout.navigation_cf_follow)

        setHasOptionsMenu(true)

        val followRecyclerView = view.findViewById<RecyclerView>(R.id.manage_cf_follow_users_list).apply {
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            setHasFixedSize(true)
        }

        val dataConnector = CodeforcesNewsFollowWorker.FollowDataConnector(requireContext())

        val followListAdapter = FollowListItemsAdapter(
            this,
            getFollowDao(requireContext()).flowOfAll().map { blogs ->
                blogs.sortedByDescending { it.id }
            },
            onShowBlog = { info ->
                mainActivity.cpsFragmentManager.pushBack(
                    CodeforcesBlogEntriesFragment().apply {
                        setHandle(info.handle)
                    }
                )
            },
            onRemove = { info -> newsViewModel.removeFromFollowList(info.handle, requireContext()) }
        )

        followRecyclerView.adapter = followListAdapter

        val buttonAdd = requireBottomPanel().findViewById<ImageButton>(R.id.navigation_cf_follow_add).apply {
            disable()
        }
        buttonAdd.setOnClickListener {
            buttonAdd.disable()
            lifecycleScope.launch {
                mainActivity.chooseUserID(CodeforcesAccountManager(requireContext()))?.let { userInfo ->
                    if(!newsViewModel.addToFollowList(userInfo, requireContext())){
                        mainActivity.showToast("User already in list")
                        return@let
                    }
                    followRecyclerView.scrollToPosition(0) //TODO not working
                }
                buttonAdd.enable()
            }
        }

        lifecycleScope.launch {
            followRecyclerView.isEnabled = false
            dataConnector.updateUsersInfo()
            followRecyclerView.isEnabled = true
            buttonAdd.enable()
        }

        viewLifecycleOwner.addRepeatingJob(Lifecycle.State.STARTED){
            mainActivity.settingsUI.getUseRealColorsFlow().ignoreFirst().collect { followListAdapter.refreshHandles() }
        }
    }

    class FollowListItemsAdapter(
        fragment: CPSFragment,
        dataFlow: Flow<List<CodeforcesUserBlog>>,
        val onShowBlog: ((CodeforcesUserInfo) -> Unit) = {},
        val onRemove: ((CodeforcesUserInfo) -> Unit) = {},
    ): CodeforcesNewsItemsAdapter<FollowListItemsAdapter.UserBlogInfoViewHolder,List<CodeforcesUserBlog>>(fragment, dataFlow) {

        private var items: Array<CodeforcesUserBlog> = emptyArray()
        override fun getItemCount(): Int = items.size

        class UserBlogInfoViewHolder(val view: ConstraintLayout) : RecyclerView.ViewHolder(view) {
            val title: TextView = view.findViewById(R.id.manage_cf_follow_users_list_item_title)
            val blogSize: TextView = view.findViewById(R.id.manage_cf_follow_users_list_item_blog_size)
            val blogInfo: Group = view.findViewById(R.id.manage_cf_follow_users_list_item_blog_info)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserBlogInfoViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.manage_cf_follow_list_item, parent, false) as ConstraintLayout
            return UserBlogInfoViewHolder(view)
        }

        override fun onBindViewHolder(holder: UserBlogInfoViewHolder, position: Int) {
            setHandle(holder, position)

            with(items[position].blogEntries) {
                holder.blogInfo.isVisible = this != null
                if(this != null) holder.blogSize.text = size.toString()
            }

            holder.view.setOnClickListener {
                val userInfo = items[holder.bindingAdapterPosition].userInfo
                PopupMenu(it.context, holder.title, Gravity.CENTER_HORIZONTAL).apply {
                    inflate(R.menu.popup_cf_follow_list_item)

                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        setForceShowIcon(true)
                    }

                    setOnMenuItemClickListener {
                        when(it.itemId){
                            R.id.cf_follow_list_item_open_blogs -> {
                                onShowBlog(userInfo)
                                true
                            }
                            R.id.cf_follow_list_item_delete -> {
                                onRemove(userInfo)
                                true
                            }
                            else -> false
                        }
                    }

                    show()
                }
            }
        }

        private fun setHandle(holder: UserBlogInfoViewHolder, position: Int) {
            with(items[position]){
                holder.title.text = codeforcesAccountManager.makeSpan(userInfo.copy(handle = handle))
            }
        }

        override fun refreshHandles(holder: UserBlogInfoViewHolder, position: Int) = setHandle(holder, position)

        override suspend fun applyData(data: List<CodeforcesUserBlog>): DiffUtil.DiffResult {
            val newItems = data.toTypedArray()
            return DiffUtil.calculateDiff(diffCallback(items, newItems)).also {
                items = newItems
            }
        }

        companion object {
            private fun diffCallback(old: Array<CodeforcesUserBlog>, new: Array<CodeforcesUserBlog>) =
                object : DiffUtil.Callback() {
                    override fun getOldListSize() = old.size
                    override fun getNewListSize() = new.size

                    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                        return old[oldItemPosition].id == new[newItemPosition].id
                    }

                    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                        return old[oldItemPosition] == new[newItemPosition]
                    }

                }
        }
    }
}