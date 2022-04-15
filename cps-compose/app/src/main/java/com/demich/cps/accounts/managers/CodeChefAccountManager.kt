package com.demich.cps.accounts.managers

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.preferencesDataStore
import com.demich.cps.accounts.SmallAccountPanelTwoLines
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.CodeChefAPI
import io.ktor.client.features.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import org.jsoup.Jsoup

@Serializable
data class CodeChefUserInfo(
    override val status: STATUS,
    val handle: String,
    val rating: Int = NOT_RATED
): UserInfo() {
    override val userId: String
        get() = handle

    override fun link() = CodeChefAPI.URLFactory.user(handle)
}

class CodeChefAccountManager(context: Context):
    RatedAccountManager<CodeChefUserInfo>(context, AccountManagers.codechef),
    AccountSuggestionsProvider
{
    companion object {
        private val Context.account_codechef_dataStore by preferencesDataStore(AccountManagers.codechef.name)

        private const val star = '★'
    }

    override val urlHomePage get() = CodeChefAPI.URLFactory.main

    override fun isValidForSearch(char: Char) = isValidForUserId(char)
    override fun isValidForUserId(char: Char) = when(char) {
        in 'a'..'z', in 'A'..'Z', in '0'..'9', in "._" -> true
        else -> false
    }

    override fun emptyInfo(): CodeChefUserInfo = CodeChefUserInfo(STATUS.NOT_FOUND, "")

    override suspend fun downloadInfo(data: String, flags: Int): CodeChefUserInfo {
        try {
            Jsoup.parse(CodeChefAPI.getUserPage(handle = data)).run {
                val rating = selectFirst("div.rating-ranks")
                    ?.select("a")
                    ?.takeIf { !it.all { it.text() == "Inactive" } }
                    ?.let { selectFirst("div.rating-header > div.rating-number")?.text()?.toInt() }
                    ?: NOT_RATED
                val userName = selectFirst("section.user-details")?.selectFirst("span.m-username--link")
                return CodeChefUserInfo(
                    status = STATUS.OK,
                    handle = userName?.text() ?: data,
                    rating = rating
                )
            }
        } catch (e: Throwable) {
            if (e is RedirectResponseException && e.response.status == HttpStatusCode.fromValue(302)) {
                return CodeChefUserInfo(status = STATUS.NOT_FOUND, handle = data)
            }
            return CodeChefUserInfo(status = STATUS.FAILED, handle = data)
        }
    }

    override suspend fun loadSuggestions(str: String): List<AccountSuggestion>? {
        try {
            return CodeChefAPI.getSuggestions(str).list.map {
                AccountSuggestion(
                    title = it.username,
                    info = it.rating.toString(),
                    userId = it.username
                )
            }
        } catch (e: Throwable) {
            return null
        }
    }

    override suspend fun loadRatingHistory(info: CodeChefUserInfo): List<RatingChange>? {
        TODO("not yet")
    }

    override fun getRating(userInfo: CodeChefUserInfo): Int = userInfo.rating

    override val ratingsUpperBounds = arrayOf(
        HandleColor.GRAY to 1400,
        HandleColor.GREEN to 1600,
        HandleColor.BLUE to 1800,
        HandleColor.VIOLET to 2000,
        HandleColor.YELLOW to 2200,
        HandleColor.ORANGE to 2500
    )

    override fun originalColor(handleColor: HandleColor): Color =
        when (handleColor) {
            HandleColor.GRAY -> Color(0xFF666666)
            HandleColor.GREEN -> Color(0xFF1E7D22)
            HandleColor.BLUE -> Color(0xFF3366CC)
            HandleColor.VIOLET -> Color(0xFF684273)
            HandleColor.YELLOW -> Color(255, 191, 0)
            HandleColor.ORANGE -> Color(255, 127, 0)
            HandleColor.RED -> Color(208,1,27)
            else -> throw HandleColor.UnknownHandleColorException(handleColor, this)
        }

    override val rankedHandleColorsList = HandleColor.rankedCodeChef

    private fun getRatingStarNumber(rating: Int): Int {
        val index = ratingsUpperBounds.indexOfFirst { rating < it.second }
        return if (index == -1) ratingsUpperBounds.size + 1 else index + 1
    }

    @Composable
    override fun makeHandleSpan(userInfo: CodeChefUserInfo): AnnotatedString {
        return buildAnnotatedString {
            if (userInfo.status == STATUS.OK && userInfo.rating != NOT_RATED) {
                withStyle(SpanStyle(color = colorFor(rating = userInfo.rating))) {
                    append("${getRatingStarNumber(userInfo.rating)}$star")
                }
            }
            append(' ')
            append(userInfo.handle)
        }
    }

    @Composable
    override fun Panel(userInfo: CodeChefUserInfo) {
        SmallAccountPanelTwoLines(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (userInfo.status == STATUS.OK && userInfo.rating != NOT_RATED) {
                        Box(
                            modifier = Modifier
                                .padding(all = 2.dp)
                                .padding(end = 8.dp)
                                .background(color = colorFor(rating = userInfo.rating))
                                .padding(horizontal = 4.dp)
                        ) {
                            Text(
                                text = "${getRatingStarNumber(userInfo.rating)}$star",
                                color = cpsColors.background,
                                fontSize = 20.sp
                            )
                        }
                    }
                    Text(
                        text = userInfo.handle,
                        fontSize = 30.sp
                    )
                }
            },
            additionalTitle = {
                if (userInfo.status == STATUS.OK) {
                    Text(
                        text = if (userInfo.rating == NOT_RATED) "[not rated]" else userInfo.rating.toString(),
                        fontSize = 25.sp
                    )
                }
            }
        )
    }

    @Composable
    override fun makeOKInfoSpan(userInfo: CodeChefUserInfo): AnnotatedString =
        buildAnnotatedString {
            require(userInfo.status == STATUS.OK)
            append(makeHandleSpan(userInfo.copy(
                handle = userInfo.handle
                        + " "
                        + (userInfo.rating.takeIf { it != NOT_RATED }?.toString() ?: "[not rated]")
            )))
        }

    override fun getDataStore() = accountDataStore(context.account_codechef_dataStore, emptyInfo())
}