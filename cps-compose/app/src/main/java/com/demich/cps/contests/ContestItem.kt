package com.demich.cps.contests

import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demich.cps.R
import com.demich.cps.ui.MonospacedText
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.format
import com.demich.cps.utils.getCurrentTime
import com.demich.cps.utils.timeDifference
import com.demich.cps.utils.toHHMMSS
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

@Composable
fun ContestItem(
    contest: Contest,
    currentTimeMillis: Long, //Instant is not Stable
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        ContestItemHeader(contest = contest)
        ContestItemFooter(
            contest = contest,
            currentTimeMillis = currentTimeMillis,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ContestItemHeader(contest: Contest) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ContestPlatformIcon(
            platform = contest.platform,
            modifier = Modifier.padding(end = 4.dp),
            size = 18.sp
        )
        Text(
            text = contest.title,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ContestItemFooter(
    contest: Contest,
    currentTimeMillis: Long,
    modifier: Modifier = Modifier
) {
    val currentTime = Instant.fromEpochMilliseconds(currentTimeMillis)

    val date: String
    val counter: String
    when (contest.getPhase(currentTime)) {
        Contest.Phase.RUNNING -> {
            date = "ends " + contest.endTime.contestDate()
            counter = "left " + contestTimeDifference(currentTime, contest.endTime)
        }
        Contest.Phase.BEFORE -> {
            date = contest.dateRange()
            counter = "in " + contestTimeDifference(currentTime, contest.startTime)
        }
        Contest.Phase.FINISHED -> {
            date = contest.startTime.contestDate() + " " + contest.endTime.contestDate()
            counter = ""
        }
    }

    ContestItemFooter(
        date = date,
        counter = counter,
        modifier = modifier
    )
}

@Composable
private fun ContestItemFooter(
    date: String,
    counter: String,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        MonospacedText(
            text = date,
            fontSize = 15.sp,
            color = cpsColors.textColorAdditional,
            modifier = Modifier.align(Alignment.CenterStart)
        )
        MonospacedText(
            text = counter,
            fontSize = 15.sp,
            color = cpsColors.textColorAdditional,
            modifier = Modifier.align(Alignment.CenterEnd)
        )
    }
}

private fun contestTimeDifference(fromTime: Instant, toTime: Instant): String {
    val t: Duration = toTime - fromTime
    if(t < 24.hours * 2) return t.toHHMMSS()
    return timeDifference(fromTime, toTime)
}

@Composable
fun collectCurrentTime(): State<Instant> {
    //TODO: not collect in background
    return remember {
        flow {
            while (currentCoroutineContext().isActive) {
                val currentTime = getCurrentTime()
                emit(currentTime)
                println(currentTime)
                kotlinx.coroutines.delay(1000 - currentTime.toEpochMilliseconds() % 1000)
            }
        }
    }.collectAsState(initial = getCurrentTime())
}

@Composable
fun ContestPlatformIcon(
    platform: Contest.Platform,
    modifier: Modifier = Modifier,
    size: TextUnit
) {
    val iconId = when (platform) {
        Contest.Platform.codeforces -> R.drawable.ic_logo_codeforces
        Contest.Platform.atcoder -> R.drawable.ic_logo_atcoder
        Contest.Platform.topcoder -> R.drawable.ic_logo_topcoder
        Contest.Platform.codechef -> R.drawable.ic_logo_codechef
        Contest.Platform.google -> R.drawable.ic_logo_google
        Contest.Platform.dmoj -> R.drawable.ic_logo_dmoj
        else -> null
    }

    Icon(
        painter = iconId?.let { painterResource(it) } ?: rememberVectorPainter(Icons.Default.EmojiEvents),
        modifier = modifier.size(with(LocalDensity.current) { size.toDp() }),
        tint = cpsColors.textColorAdditional,
        contentDescription = null
    )
}

private fun Instant.contestDate() = format("dd.MM E HH:mm")

private fun Contest.dateRange(): String {
    val start = startTime.contestDate()
    val end = if (duration < 1.days) endTime.format("HH:mm") else "..."
    return "$start-$end"
}