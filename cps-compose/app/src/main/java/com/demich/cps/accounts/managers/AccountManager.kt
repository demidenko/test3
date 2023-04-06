package com.demich.cps.accounts.managers

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import com.demich.cps.*
import com.demich.cps.accounts.userinfo.UserInfo
import com.demich.cps.accounts.userinfo.UserSuggestion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

enum class AccountManagers {
    codeforces,
    atcoder,
    codechef,
    dmoj,
    acmp,
    timus,
    clist
}

val Context.allAccountManagers: List<AccountManager<out UserInfo>>
    get() = listOf(
        CodeforcesAccountManager(this),
        AtCoderAccountManager(this),
        CodeChefAccountManager(this),
        DmojAccountManager(this),
        ACMPAccountManager(this),
        TimusAccountManager(this)
    )


abstract class AccountManager<U: UserInfo>(val context: Context, val type: AccountManagers) {

    abstract val userIdTitle: String
    abstract val urlHomePage: String

    protected abstract fun getDataStore(): AccountDataStore<U>
    fun flowOfInfo(): Flow<U?> = getDataStore().userInfo.flow.map { info ->
        info.takeIf { !it.isEmpty() }
    }
    fun flowOfInfoWithManager(): Flow<UserInfoWithManager<U>?> = flowOfInfo().map { info ->
        if (info != null) UserInfoWithManager(info, this) else null
    }

    abstract fun emptyInfo(): U

    protected abstract suspend fun downloadInfo(data: String): U
    suspend fun loadInfo(data: String): U {
        if (data.isBlank()) return emptyInfo()
        return withContext(Dispatchers.IO) {
            downloadInfo(data)
        }
    }

    open fun isValidForUserId(char: Char): Boolean = true

    suspend fun getSavedInfo(): U? = flowOfInfo().first()

    suspend fun setSavedInfo(info: U) {
        val oldUserId = getSavedInfo()?.userId ?: ""
        getDataStore().userInfo(info)
        if (info.userId != oldUserId && this is AccountSettingsProvider) getSettings().resetRelatedItems()
    }

    suspend fun deleteUserInfo() {
        getDataStore().userInfo(emptyInfo())
    }

    @Composable
    abstract fun makeOKInfoSpan(userInfo: U): AnnotatedString

    @Composable
    open fun PanelContent(userInfo: U) {}

    @Composable
    open fun ExpandedContent(
        userInfo: U,
        setBottomBarContent: (AdditionalBottomBarBuilder) -> Unit,
        modifier: Modifier = Modifier
     ) = Box(modifier) {
        PanelContent(userInfo)
    }
}

data class UserInfoWithManager<U: UserInfo>(
    val userInfo: U,
    val manager: AccountManager<U>
) {
    init {
        require(!userInfo.isEmpty())
    }

    val type: AccountManagers get() = manager.type
}

interface UserSuggestionsProvider {
    suspend fun loadSuggestions(str: String): List<UserSuggestion>
    fun isValidForSearch(char: Char): Boolean = true
}

