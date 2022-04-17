package com.demich.cps.contests

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.demich.cps.AdditionalBottomBarBuilder
import com.demich.cps.Screen
import com.demich.cps.ui.CPSIconButton
import com.demich.cps.ui.CPSReloadingButton
import com.demich.cps.utils.LoadingStatus

@Composable
fun ContestsScreen(navController: NavController) {

}

fun contestsBottomBarBuilder(navController: NavController)
: AdditionalBottomBarBuilder = {
    CPSIconButton(icon = Icons.Default.Settings) {
        navController.navigate(Screen.ContestsSettings.route)
    }
    CPSReloadingButton(loadingStatus = LoadingStatus.PENDING) {

    }
}
