package com.demich.cps.accounts

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.demich.cps.accounts.managers.*
import com.demich.cps.ui.bottomprogressbar.ProgressBarInfo
import com.demich.cps.ui.bottomprogressbar.ProgressBarsViewModel
import com.demich.cps.utils.CListUtils
import com.demich.cps.utils.LoadingStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch


class AccountsViewModel: ViewModel() {
    private val loadingStatuses = mutableMapOf<AccountManagers, MutableStateFlow<LoadingStatus>>()

    private fun mutableLoadingStatusFor(manager: AccountManager<out UserInfo>): MutableStateFlow<LoadingStatus> =
        loadingStatuses.getOrPut(manager.type) { MutableStateFlow(LoadingStatus.PENDING) }

    fun flowOfLoadingStatus(manager: AccountManager<out UserInfo>): Flow<LoadingStatus> =
        mutableLoadingStatusFor(manager)


    fun<U: UserInfo> reload(manager: AccountManager<U>) {
        viewModelScope.launch {
            val savedInfo = manager.getSavedInfo()
            if (savedInfo.isEmpty()) return@launch

            val loadingStatusState = mutableLoadingStatusFor(manager)
            if (loadingStatusState.value == LoadingStatus.LOADING) return@launch

            loadingStatusState.value = LoadingStatus.LOADING
            val info = manager.loadInfo(savedInfo.userId, 1)

            if (info.status == STATUS.FAILED) {
                loadingStatusState.value = LoadingStatus.FAILED
            } else {
                loadingStatusState.value = LoadingStatus.PENDING
                manager.setSavedInfo(info)
            }
        }
    }

    fun<U: UserInfo> delete(manager: AccountManager<U>) {
        viewModelScope.launch {
            val loadingStatusState = mutableLoadingStatusFor(manager)
            require(loadingStatusState.value != LoadingStatus.LOADING)
            loadingStatusState.value = LoadingStatus.PENDING
            manager.setSavedInfo(manager.emptyInfo())
        }
    }

    fun runClistImport(
        cListUserInfo: CListUserInfo,
        progressBarsViewModel: ProgressBarsViewModel,
        context: Context
    ) {
        progressBarsViewModel.doJob(id = ProgressBarsViewModel.clistImportId, coroutineScope = viewModelScope) { progress ->
            val supported = cListUserInfo.accounts.mapNotNull { (resource, userData) ->
                CListUtils.getManager(resource, userData.first, userData.second)
            }
            progress.value = ProgressBarInfo(title = "clist import", total = supported.size)
            val managers = context.allAccountManagers
            supported.map { (type, userId) ->
                val manager = managers.first { it.type == type }
                val loadingStatusState = mutableLoadingStatusFor(manager)
                launch {
                    //TODO: what if already loading??
                    loadingStatusState.value = LoadingStatus.LOADING
                    loadAndSave(manager, userId)
                    loadingStatusState.value = LoadingStatus.PENDING
                    progress.value++
                }
            }.joinAll()
        }
    }

    private suspend fun<U: UserInfo> loadAndSave(manager: AccountManager<U>, userId: String) {
        manager.setSavedInfo(manager.loadInfo(userId))
    }
}