package com.demich.cps.contests.monitors

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demich.cps.platforms.api.CodeforcesContestPhase
import com.demich.cps.platforms.api.CodeforcesContestType
import com.demich.cps.platforms.api.CodeforcesParticipationType
import com.demich.cps.ui.CPSDefaults
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.ContentWithCPSDropdownMenu
import com.demich.cps.ui.IconSp
import com.demich.cps.ui.theme.CPSTheme
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.currentTimeAsState
import com.demich.cps.utils.rememberWith
import com.demich.cps.utils.toHHMMSS
import com.demich.cps.utils.toMMSS
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds


@Composable
fun CodeforcesMonitorWidget(
    contestData: CodeforcesMonitorData,
    requestFailed: Boolean,
    modifier: Modifier = Modifier,
    onOpenInBrowser: () -> Unit,
    onStop: () -> Unit
) {
    ContentWithCPSDropdownMenu(
        content = {
            CodeforcesMonitor(
                contestData = contestData,
                requestFailed = requestFailed,
                modifier = modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(color = cpsColors.backgroundAdditional)
                    .padding(
                        start = 4.dp,
                        end = 7.dp,
                        top = 4.dp,
                        bottom = 3.dp
                    )
            )
        }
    ) {
        CPSDropdownMenuItem(title = "Browse", icon = CPSIcons.OpenInBrowser, onClick = onOpenInBrowser)
        CPSDropdownMenuItem(title = "Close", icon = CPSIcons.Close, onClick = onStop)
    }
}



@Composable
private fun CodeforcesMonitor(
    contestData: CodeforcesMonitorData,
    requestFailed: Boolean,
    modifier: Modifier
) {
    Column(modifier) {
        StandingsRow(
            contestData = contestData,
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
        )
        Footer(
            contestData = contestData,
            requestFailed = requestFailed,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun Footer(
    contestData: CodeforcesMonitorData,
    requestFailed: Boolean,
    modifier: Modifier = Modifier
) {
    ProvideTextStyle(CPSDefaults.MonospaceTextStyle.copy(
        fontSize = 15.sp,
        color = cpsColors.contentAdditional
    )) {
        Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
            Rank(
                contestantRank = contestData.contestantRank,
                modifier = Modifier.weight(1f)
            )
            if (requestFailed) {
                IconSp(
                    imageVector = CPSIcons.Error,
                    size = 14.sp,
                    color = cpsColors.error,
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
            PhaseTitle(
                contestPhase = contestData.contestPhase
            )
        }
    }
}

@Composable
private fun PhaseTitle(
    contestPhase: CodeforcesMonitorData.ContestPhase,
    modifier: Modifier = Modifier
) {
    when (contestPhase) {
        is CodeforcesMonitorData.ContestPhase.Coding -> {
            val currentTime by currentTimeAsState(period = 1.seconds)
            PhaseTitle(
                phase = contestPhase.phase,
                modifier = modifier,
                info = (contestPhase.endTime - currentTime).coerceAtLeast(Duration.ZERO).let {
                    if (it < 1.hours) it.toMMSS() else it.toHHMMSS()
                }
            )
        }
        is CodeforcesMonitorData.ContestPhase.SystemTesting -> {
            PhaseTitle(
                phase = contestPhase.phase,
                modifier = modifier,
                info = contestPhase.percentage?.let { "$it%" } ?: ""
            )
        }
        else -> {
            PhaseTitle(phase = contestPhase.phase, modifier = modifier)
        }
    }
}

@Composable
private fun PhaseTitle(
    phase: CodeforcesContestPhase,
    modifier: Modifier = Modifier,
    info: String = ""
) {
    val title = when (phase) {
        CodeforcesContestPhase.CODING -> "left"
        else -> phase.title.lowercase()
    }
    Text(
        text = if (info.isEmpty()) title else "$title $info",
        modifier = modifier
    )
}


@Composable
private fun StandingsRow(
    contestData: CodeforcesMonitorData,
    modifier: Modifier = Modifier
) {
    val textStyle = rememberWith(contestData.problems.size) {
        //TODO: horizontal scroll??
        TextStyle.Default.copy(
            fontSize = when {
                this < 9 -> 16.sp
                this < 10 -> 15.sp
                else -> 14.sp
            }
        )
    }

    ProvideTextStyle(value = textStyle) {
        if (contestData.problems.isNotEmpty()) {
            Row(modifier = modifier) {
                contestData.problems.forEach {
                    ProblemColumn(
                        problemName = it.first,
                        problemResult = it.second,
                        contestType = contestData.contestInfo.type,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ProblemColumn(
    problemName: String,
    problemResult: CodeforcesMonitorData.ProblemResult,
    contestType: CodeforcesContestType,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = problemName)
        ProblemResultCell(
            problemResult = problemResult,
            contestType = contestType
        )
    }
}

@Composable
private fun ProblemResultCell(
    problemResult: CodeforcesMonitorData.ProblemResult,
    contestType: CodeforcesContestType,
    modifier: Modifier = Modifier
) {
    when (problemResult) {
        is CodeforcesMonitorData.ProblemResult.FailedSystemTest -> {
            ProblemResultCell(
                text = CodeforcesMonitorData.ProblemResult.failedSystemTestSymbol,
                color = cpsColors.error,
                modifier = modifier
            )
        }
        is CodeforcesMonitorData.ProblemResult.Pending -> {
            ProblemResultCell(
                text = "?",
                color = cpsColors.contentAdditional,
                modifier = modifier
            )
        }
        is CodeforcesMonitorData.ProblemResult.Empty -> {
            Text(
                text = "",
                modifier = modifier
            )
        }
        is CodeforcesMonitorData.ProblemResult.Points -> {
            ProblemResultCell(
                text = if (contestType == CodeforcesContestType.ICPC) "+" else {
                    problemResult.pointsToNiceString()
                },
                color = if (problemResult.isFinal) cpsColors.success else cpsColors.content,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun ProblemResultCell(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        color = color,
        fontWeight = FontWeight.Bold,
        modifier = modifier
    )
}

@Composable
private fun Rank(
    contestantRank: CodeforcesMonitorData.ContestRank,
    modifier: Modifier
) {
    Text(
        text = with(contestantRank) {
            val rankText = when {
                rank <= 0 -> ""
                participationType == CodeforcesParticipationType.CONTESTANT -> "$rank"
                else -> "*$rank"
            }
            "rank: $rankText"
        },
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
private fun TestProblemColumns() {
    CPSTheme {
        Row(modifier = Modifier.fillMaxWidth()) {
            ProblemColumn(
                problemName = "A",
                problemResult = CodeforcesMonitorData.ProblemResult.Points(1.0, true),
                contestType = CodeforcesContestType.ICPC,
                modifier = Modifier.weight(1f)
            )
            ProblemColumn(
                problemName = "A1",
                problemResult = CodeforcesMonitorData.ProblemResult.Points(500.0, false),
                contestType = CodeforcesContestType.CF,
                modifier = Modifier.weight(1f)
            )
            ProblemColumn(
                problemName = "A2",
                problemResult = CodeforcesMonitorData.ProblemResult.Points(500.0, true),
                contestType = CodeforcesContestType.CF,
                modifier = Modifier.weight(1f)
            )
            ProblemColumn(
                problemName = "F",
                problemResult = CodeforcesMonitorData.ProblemResult.FailedSystemTest,
                contestType = CodeforcesContestType.CF,
                modifier = Modifier.weight(1f)
            )
            ProblemColumn(
                problemName = "P",
                problemResult = CodeforcesMonitorData.ProblemResult.Pending,
                contestType = CodeforcesContestType.CF,
                modifier = Modifier.weight(1f)
            )
            ProblemColumn(
                problemName = "E",
                problemResult = CodeforcesMonitorData.ProblemResult.Empty,
                contestType = CodeforcesContestType.CF,
                modifier = Modifier.weight(1f)
            )
        }
    }
}