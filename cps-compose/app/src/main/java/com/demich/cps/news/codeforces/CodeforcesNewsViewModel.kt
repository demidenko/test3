package com.demich.cps.news.codeforces

import android.content.Context
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.demich.cps.accounts.managers.CodeforcesUserInfo
import com.demich.cps.news.settings.settingsNews
import com.demich.cps.room.followListDao
import com.demich.cps.utils.LoadingStatus
import com.demich.cps.utils.asyncPair
import com.demich.cps.utils.codeforces.*
import com.demich.cps.utils.combine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import kotlin.math.max

class CodeforcesNewsViewModel: ViewModel() {

    private inner class DataLoader<T>(
        init: T,
        val getData: suspend (CodeforcesLocale) -> T?
    ) {
        private val dataFlow: MutableStateFlow<T> = MutableStateFlow(init)

        private var inactive = true
        fun getDataFlow(context: Context): StateFlow<T> {
            if (inactive) {
                inactive = false
                viewModelScope.launch {
                    launchLoadIfActive(locale = context.settingsNews.codeforcesLocale())
                }
            }
            return dataFlow.asStateFlow()
        }

        val loadingStatusState = MutableStateFlow(LoadingStatus.PENDING)

        fun launchLoadIfActive(locale: CodeforcesLocale) {
            if (inactive) return
            require(loadingStatusState.value != LoadingStatus.LOADING)
            viewModelScope.launch {
                loadingStatusState.value = LoadingStatus.LOADING
                val data = withContext(Dispatchers.IO) { getData(locale) }
                if(data == null) loadingStatusState.value = LoadingStatus.FAILED
                else {
                    dataFlow.value = data
                    loadingStatusState.value = LoadingStatus.PENDING
                }
            }
        }
    }


    private val reloadableTitles = listOf(
        CodeforcesTitle.MAIN,
        CodeforcesTitle.TOP,
        CodeforcesTitle.RECENT
    )

    fun flowOfLoadingStatus(): Flow<LoadingStatus> =
        listOf(
            mainBlogEntries.loadingStatusState,
            topBlogEntries.loadingStatusState,
            topComments.loadingStatusState,
            recentActions.loadingStatusState
        ).combine()

    fun flowOfLoadingStatus(title: CodeforcesTitle): Flow<LoadingStatus> {
        return when (title) {
            CodeforcesTitle.MAIN -> mainBlogEntries.loadingStatusState
            CodeforcesTitle.TOP -> {
                listOf(topBlogEntries.loadingStatusState, topComments.loadingStatusState)
                    .combine()
            }
            CodeforcesTitle.RECENT -> recentActions.loadingStatusState
            else -> flowOf(LoadingStatus.PENDING)
        }
    }

    private val mainBlogEntries = DataLoader(emptyList()) { loadBlogEntries(page = "/", locale = it) }
    fun flowOfMainBlogEntries(context: Context) = mainBlogEntries.getDataFlow(context)

    private val topBlogEntries = DataLoader(emptyList()) { loadBlogEntries(page = "/top", locale = it) }
    fun flowOfTopBlogEntries(context: Context) = topBlogEntries.getDataFlow(context)

    private val topComments = DataLoader(emptyList()) { loadComments(page = "/topComments?days=2", locale = it) }
    fun flowOfTopComments(context: Context) = topComments.getDataFlow(context)

    private val recentActions = DataLoader(Pair(emptyList(), emptyList())) { loadRecentActions(locale = it) }
    fun flowOfRecentActions(context: Context) = recentActions.getDataFlow(context)

    private fun reload(title: CodeforcesTitle, locale: CodeforcesLocale) {
        when(title) {
            CodeforcesTitle.MAIN -> mainBlogEntries.launchLoadIfActive(locale)
            CodeforcesTitle.TOP -> {
                topBlogEntries.launchLoadIfActive(locale)
                topComments.launchLoadIfActive(locale)
            }
            CodeforcesTitle.RECENT -> recentActions.launchLoadIfActive(locale)
            else -> return
        }
    }

    fun reload(title: CodeforcesTitle, context: Context) {
        viewModelScope.launch {
            val locale = context.settingsNews.codeforcesLocale()
            reload(title = title, locale = locale)
        }
    }

    fun reloadAll(context: Context) {
        viewModelScope.launch {
            val locale = context.settingsNews.codeforcesLocale()
            reloadableTitles.forEach { reload(title = it, locale = locale) }
        }
    }

    private suspend fun loadBlogEntries(page: String, locale: CodeforcesLocale): List<CodeforcesBlogEntry>? {
        val s = CodeforcesApi.getPageSource(urlString = CodeforcesApi.urls.main + page, locale = locale) ?: return null
        return CodeforcesUtils.extractBlogEntries(s)
    }

    private suspend fun loadComments(page: String, locale: CodeforcesLocale): List<CodeforcesRecentAction>? {
        val s = CodeforcesApi.getPageSource(urlString = CodeforcesApi.urls.main + page, locale = locale) ?: return null
        return CodeforcesUtils.extractComments(s)
    }

    private suspend fun loadRecentActions(locale: CodeforcesLocale): Pair<List<CodeforcesBlogEntry>,List<CodeforcesRecentAction>>? {
        val s = CodeforcesApi.getPageSource(urlString = CodeforcesApi.urls.main + "/recent-actions", locale = locale) ?: return null
        val comments = CodeforcesUtils.extractComments(s)
        //blog entry with low rating disappeared from blogEntries but has comments, need to merge
        val blogEntries = CodeforcesUtils.extractRecentBlogEntries(s).toMutableList()
        val commentsGrouped = blogEntries.associate { it.id to mutableListOf<CodeforcesRecentAction>() }.toMutableMap()
        var index = 0
        val usedIds = mutableSetOf<Int>()
        for (comment in comments) {
            val id = comment.blogEntry!!.id
            commentsGrouped.getOrPut(id) {
                blogEntries.add(
                    index = index,
                    element = comment.blogEntry.copy(rating = -1) //mark low rated
                )
                mutableListOf()
            }.add(comment)
            if (id !in usedIds) {
                usedIds.add(id)
                val curIndex = blogEntries.indexOfFirst { it.id == id }
                index = max(index, curIndex + 1)
            }
        }
        return Pair(blogEntries, comments)
    }

    var followLoadingStatus by mutableStateOf(LoadingStatus.PENDING)
    fun addToFollowList(userInfo: CodeforcesUserInfo, context: Context) {
        viewModelScope.launch {
            context.followListDao.addNewUser(
                userInfo = userInfo,
                context = context
            )
        }
    }

    fun addToFollowList(handle: String, context: Context) {
        viewModelScope.launch {
            context.followListDao.addNewUser(
                handle = handle,
                context = context
            )
        }
    }

    fun updateFollowUsersInfo(context: Context) {
        //TODO: call while already updating??
        viewModelScope.launch {
            followLoadingStatus = LoadingStatus.LOADING
            context.followListDao.updateUsersInfo(context)
            followLoadingStatus = LoadingStatus.PENDING
        }
    }

    var blogLoadingStatus by mutableStateOf(LoadingStatus.PENDING)
    var blogEntriesState = mutableStateOf(emptyList<CodeforcesBlogEntry>())
    fun loadBlog(handle: String, context: Context) {
        require(blogLoadingStatus != LoadingStatus.LOADING)
        blogEntriesState.value = emptyList()
        viewModelScope.launch {
            blogLoadingStatus = LoadingStatus.LOADING
            val (result, colorTag) = asyncPair(
                { context.followListDao.getAndReloadBlogEntries(handle, context) },
                { CodeforcesUtils.getRealColorTag(handle) }
            )
            if (result == null) {
                blogLoadingStatus = LoadingStatus.FAILED
            } else {
                blogLoadingStatus = LoadingStatus.PENDING
                blogEntriesState.value = result.map {
                    it.copy(
                        title = Jsoup.parse(it.title).text(),
                        authorColorTag = colorTag
                    )
                }
            }
        }
    }

}