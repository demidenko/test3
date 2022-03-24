package com.demich.cps.ui

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.demich.cps.accounts.allAccountManagers
import com.demich.cps.accounts.managers.*
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.context
import com.demich.cps.utils.rememberCollect
import com.google.accompanist.systemuicontroller.SystemUiController
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

@Composable
fun CPSStatusBar(
    systemUiController: SystemUiController
) {
    val context = context
    val coloredStatusBar by rememberCollect { context.settingsUI.coloredStatusBar.flow }

    val bestOrder by rememberCollect { makeFlowOfBestOrder(context) }

    val color = bestOrder?.run { manager.colorFor(handleColor) }
    ColorizeStatusBar(
        systemUiController = systemUiController,
        coloredStatusBar = coloredStatusBar && color != null,
        color = color ?: cpsColors.background
    )
}


private data class RatedOrder(
    val order: Double,
    val handleColor: HandleColor,
    val manager: RatedAccountManager<*>
)

private fun<U: UserInfo> RatedAccountManager<U>.getOrder(userInfo: U): RatedOrder? {
    if (userInfo.status != STATUS.OK) return null
    val rating = getRating(userInfo)
    if(rating == NOT_RATED) return null
    val handleColor = getHandleColor(rating)
    if(handleColor == HandleColor.RED) return RatedOrder(order = 1e9, handleColor = handleColor, manager = this)
    val i = rankedHandleColorsList.indexOfFirst { handleColor == it }
    val j = rankedHandleColorsList.indexOfLast { handleColor == it }
    val pos = ratingsUpperBounds.indexOfFirst { it.first == handleColor }
    require(i != -1 && j >= i && pos != -1)
    val lower = if(pos > 0) ratingsUpperBounds[pos-1].second else 0
    val upper = ratingsUpperBounds[pos].second
    val blockLength = (upper - lower).toDouble() / (j - i + 1)
    return RatedOrder(
        order = i + (rating - lower) / blockLength,
        handleColor = handleColor,
        manager = this
    )
}


private fun<U: UserInfo> RatedAccountManager<U>.flowOfRatedOrder(): Flow<RatedOrder?> =
    flowOfInfo().map { getOrder(it) }

private fun makeFlowOfBestOrder(context: Context): Flow<RatedOrder?> =
    combine(
        flow = combine(flows = context.allAccountManagers
            .filterIsInstance<RatedAccountManager<*>>()
            .map { it.flowOfRatedOrder() }
        ) { it },
        flow2 = context.settingsUI.statusBarDisabledManagers.flow
    ) { orders, disabledManagers ->
        orders.filterNotNull()
            .filter { it.manager.managerName !in disabledManagers }
            .maxByOrNull { it.order }
    }

@Composable
private fun ColorizeStatusBar(
    systemUiController: SystemUiController,
    coloredStatusBar: Boolean,
    color: Color,
    offColor: Color = cpsColors.background
) {
    /*
        Important:
        with statusbar=off switching dark/light mode MUST be as fast as everywhere else
    */
    val koef by animateFloatAsState(
        targetValue = if (coloredStatusBar) 1f else 0f,
        animationSpec = tween(buttonOnOffDurationMillis)
    )
    val statusBarColor by animateColorAsState(
        targetValue = color,
        animationSpec = tween(buttonOnOffDurationMillis)
    )
    val mixedColor by remember(offColor) {
        fun point(l: Float, r: Float): Float = (r - l) * koef + l
        derivedStateOf {
            val r = point(offColor.red, statusBarColor.red)
            val g = point(offColor.green, statusBarColor.green)
            val b = point(offColor.blue, statusBarColor.blue)
            Color(r, g, b)
        }
    }
    systemUiController.setStatusBarColor(
        color = mixedColor,
        darkIcons = MaterialTheme.colors.isLight
    )
}

