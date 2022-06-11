package com.demich.cps.news.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.demich.cps.R
import com.demich.cps.accounts.managers.CodeforcesAccountManager
import com.demich.cps.news.codeforces.CodeforcesTitle
import com.demich.cps.ui.*
import com.demich.cps.utils.CPSDataStoreItem
import com.demich.cps.utils.codeforces.CodeforcesLocale
import com.demich.cps.utils.codeforces.CodeforcesUtils
import com.demich.cps.utils.context
import com.demich.cps.utils.rememberCollect
import com.demich.cps.workers.WorkersCenter
import com.demich.cps.workers.WorkersNames
import kotlinx.coroutines.launch


@Composable
fun NewsSettingsScreen() {
    SettingsColumn {
        CodeforcesDefaultTabSettingsItem()
        CodeforcesFollowSettingsItem()
        CodeforcesLostSettingsItem()
        CodeforcesRuEnabledSettingsItem()
    }
}

@Composable
private fun CodeforcesDefaultTabSettingsItem() {
    val context = context
    SettingsEnumItem(
        item = context.settingsNews.codeforcesDefaultTab,
        title = "Default tab",
        options = listOf(
            CodeforcesTitle.MAIN,
            CodeforcesTitle.TOP,
            CodeforcesTitle.RECENT
        )
    )
}

@Composable
private fun CodeforcesFollowSettingsItem() {
    val context = context
    SettingsSwitchItem(
        item = context.settingsNews.codeforcesFollowEnabled,
        title = "Follow",
        description = stringResource(id = R.string.news_settings_cf_follow_description)
    ) { checked ->
        if (checked) WorkersCenter.startCodeforcesNewsFollowWorker(context = context, restart = true)
        else WorkersCenter.stopWorker(context = context, workName = WorkersNames.codeforces_news_follow)
    }
}

@Composable
private fun CodeforcesLostSettingsItem() {
    val context = context
    val scope = rememberCoroutineScope()
    val settings = remember { context.settingsNews }
    val enabled by rememberCollect { settings.codeforcesLostEnabled.flow }
    SettingsItem {
        Column {
            SettingsSwitchItemContent(
                checked = enabled,
                title = "Lost recent blog entries",
                description = stringResource(id = R.string.news_settings_cf_lost_description),
                onCheckedChange = { checked ->
                    scope.launch { settings.codeforcesLostEnabled(newValue = checked) }
                }
            )
            AnimatedVisibility(visible = enabled) {
                CodeforcesLostAuthorSettingsItem(item = settings.codeforcesLostMinRatingTag)
            }
        }
    }
}

@Composable
private fun CodeforcesLostAuthorSettingsItem(
    item: CPSDataStoreItem<CodeforcesUtils.ColorTag>
) {
    val context = context
    val manager = remember { CodeforcesAccountManager(context) }
    val options = remember {
        listOf(
            CodeforcesUtils.ColorTag.BLACK to "Exists",
            CodeforcesUtils.ColorTag.GRAY to "Newbie",
            CodeforcesUtils.ColorTag.GREEN to "Pupil",
            CodeforcesUtils.ColorTag.CYAN to "Specialist",
            CodeforcesUtils.ColorTag.BLUE to "Expert",
            CodeforcesUtils.ColorTag.VIOLET to "Candidate Master",
            CodeforcesUtils.ColorTag.ORANGE to "Master",
            CodeforcesUtils.ColorTag.RED to "Grandmaster",
            CodeforcesUtils.ColorTag.LEGENDARY to "LGM"
        )
    }
    Box(modifier = Modifier.padding(top = 10.dp)) {
        SettingsEnumItemContent(
            item = item,
            title = "Author at least",
            options = options.map { it.first },
            optionToString = { tag ->
                manager.makeHandleSpan(
                    handle = options.first { it.first == tag }.second,
                    tag = tag
                )
            }
        )
    }
}

@Composable
private fun CodeforcesRuEnabledSettingsItem() {
    val context = context
    val scope = rememberCoroutineScope()

    val locale by rememberCollect { context.settingsNews.codeforcesLocale.flow }

    SettingsSwitchItem(
        title = "Russian content",
        checked = locale == CodeforcesLocale.RU,
        onCheckedChange = { checked ->
            scope.launch {
                context.settingsNews.codeforcesLocale(
                    newValue = if (checked) CodeforcesLocale.RU else CodeforcesLocale.EN
                )
            }
        }
    )
}