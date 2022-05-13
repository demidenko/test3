package com.demich.cps.news.codeforces

import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.demich.cps.news.settings.settingsNews
import com.demich.cps.utils.LoadingStatus
import com.demich.cps.utils.codeforces.*
import com.demich.cps.utils.combine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
                    launchLoad(locale = context.settingsNews.codeforcesLocale())
                }
            }
            return dataFlow.asStateFlow()
        }

        val loadingStatusState = mutableStateOf(LoadingStatus.PENDING)

        fun launchLoad(locale: CodeforcesLocale) {
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

    fun combinedLoadingStatusState() = derivedStateOf {
        reloadableTitles.map { pageLoadingStatusState(it).value }.combine()
    }

    fun pageLoadingStatusState(title: CodeforcesTitle): State<LoadingStatus> {
        return when (title) {
            CodeforcesTitle.MAIN -> mainBlogEntries.loadingStatusState
            CodeforcesTitle.TOP -> topBlogEntries.loadingStatusState
            CodeforcesTitle.RECENT -> recentActions.loadingStatusState
            else -> throw IllegalArgumentException("$title is not reloadable")
        }
    }

    private val mainBlogEntries = DataLoader(emptyList()) { loadBlogEntries(page = "/", locale = it) }
    fun flowOfMainBlogEntries(context: Context) = mainBlogEntries.getDataFlow(context)

    private val topBlogEntries = DataLoader(emptyList()) { loadBlogEntries(page = "/top", locale = it) }
    fun flowOfTopBlogEntries(context: Context) = topBlogEntries.getDataFlow(context)

    private val recentActions = DataLoader(Pair(emptyList(), emptyList())) { loadRecentActions(locale = it) }
    fun flowOfRecentActions(context: Context) = recentActions.getDataFlow(context)

    private fun reload(title: CodeforcesTitle, locale: CodeforcesLocale) {
        when(title) {
            CodeforcesTitle.MAIN -> mainBlogEntries.launchLoad(locale)
            CodeforcesTitle.TOP -> {
                topBlogEntries.launchLoad(locale)
                //topComments.load(locale)
            }
            CodeforcesTitle.RECENT -> recentActions.launchLoad(locale)
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
        return Pair(
            first = CodeforcesUtils.extractRecentBlogEntries(s),
            second = CodeforcesUtils.extractComments(s)
        )
    }
}