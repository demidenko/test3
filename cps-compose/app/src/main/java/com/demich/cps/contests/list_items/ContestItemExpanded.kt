package com.demich.cps.contests.list_items

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demich.cps.contests.ContestPlatformIcon
import com.demich.cps.contests.database.Contest
import com.demich.cps.contests.dateRange
import com.demich.cps.contests.isVirtual
import com.demich.cps.ui.CPSDefaults
import com.demich.cps.ui.CPSDropdownMenuButton
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.dialogs.CPSDeleteDialog
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.DangerType
import com.demich.cps.utils.context
import com.demich.cps.utils.openUrlInBrowser

@Composable
internal fun ContestExpandedItemContent(
    contest: Contest,
    collisionType: DangerType,
    onDeleteRequest: () -> Unit
) {
    val data = dataByCurrentTime(contest)
    ContestPlatform(
        platform = contest.platform,
        platformName = contest.platformName()
    )
    ContestTitle(
        contest = contest,
        phase = data.phase,
    )
    ContestItemDatesAndMenuButton(
        contest = contest,
        collisionType = if (data.phase == Contest.Phase.BEFORE) collisionType else DangerType.SAFE,
        onDeleteRequest = onDeleteRequest
    )
    ContestCounter(
        phase = data.phase,
        counter = data.counter
    )
}

@Composable
private fun ContestPlatform(
    platform: Contest.Platform,
    platformName: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        ContestPlatformIcon(
            platform = platform,
            size = 18.sp,
            color = cpsColors.contentAdditional
        )
        Text(
            text = platformName,
            style = CPSDefaults.MonospaceTextStyle.copy(
                fontSize = 13.sp,
                color = cpsColors.contentAdditional
            ),
            maxLines = 1,
            modifier = Modifier.padding(start = 5.dp)
        )
    }
}

@Composable
private fun ContestTitle(
    contest: Contest,
    phase: Contest.Phase,
) {
    ContestTitleExpanded(
        title = contest.title,
        phase = phase,
        isVirtual = contest.isVirtual,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ContestItemDatesAndMenuButton(
    contest: Contest,
    collisionType: DangerType,
    onDeleteRequest: () -> Unit
) {
    ContestItemDatesAndMenuButton(
        dateRange = contest.dateRange(),
        contestLink = contest.link,
        collisionType = collisionType,
        modifier = Modifier.fillMaxWidth(),
        onDeleteRequest = onDeleteRequest
    )
}

@Composable
private fun ContestCounter(
    phase: Contest.Phase,
    counter: String
) {
    Text(
        text = when (phase) {
            Contest.Phase.BEFORE -> "starts in $counter"
            Contest.Phase.RUNNING -> "ends in $counter"
            Contest.Phase.FINISHED -> "finished"
        },
        style = CPSDefaults.MonospaceTextStyle.copy(
            fontSize = 15.sp,
            color = cpsColors.contentAdditional
        )
    )
}

@Composable
private fun ContestItemDatesAndMenuButton(
    dateRange: String,
    collisionType: DangerType,
    contestLink: String?,
    modifier: Modifier = Modifier,
    onDeleteRequest: () -> Unit
) {
    Box(modifier = modifier) {
        ProvideTextStyle(CPSDefaults.MonospaceTextStyle.copy(
            fontSize = 15.sp,
            color = cpsColors.contentAdditional
        )) {
            AttentionText(
                text = dateRange,
                collisionType = collisionType,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        ContestItemMenuButton(
            contestLink = contestLink,
            modifier = Modifier.align(Alignment.CenterEnd),
            onDeleteRequest = onDeleteRequest
        )
    }
}

@Composable
private fun ContestItemMenuButton(
    contestLink: String?,
    modifier: Modifier = Modifier,
    onDeleteRequest: () -> Unit
) {
    val context = context
    var showDeleteDialog by remember { mutableStateOf(false) }
    CPSDropdownMenuButton(
        icon = CPSIcons.More,
        color = cpsColors.contentAdditional,
        iconSize = 22.dp,
        modifier = modifier
    ) {
        if (contestLink != null) {
            CPSDropdownMenuItem(title = "Open in browser", icon = CPSIcons.OpenInBrowser) {
                context.openUrlInBrowser(contestLink)
            }
        }
        CPSDropdownMenuItem(title = "Delete", icon = CPSIcons.Delete) {
            showDeleteDialog = true
        }
    }
    if (showDeleteDialog) {
        CPSDeleteDialog(
            title = "Delete contest from list?",
            onDismissRequest = { showDeleteDialog = false },
            onConfirmRequest = onDeleteRequest
        )
    }
}