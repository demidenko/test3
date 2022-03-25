package com.demich.cps

import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.PeopleAlt
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import com.demich.cps.ui.*
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.context
import com.demich.cps.utils.rememberCollect
import kotlinx.coroutines.launch

@Composable
fun CPSTopBar(
    navController: NavHostController,
    currentBackStackEntry: NavBackStackEntry?
) {
    val currentScreen = currentBackStackEntry?.destination?.getScreen()

    var showUIPanel by rememberSaveable { mutableStateOf(false) }
    var showAbout by rememberSaveable { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    TopAppBar(
        backgroundColor = cpsColors.background,
        elevation = 0.dp,
        contentPadding = PaddingValues(start = 10.dp),
        modifier = Modifier.height(56.dp)
    ) {
        Box(modifier = Modifier
            .weight(1f)
            .fillMaxHeight()) {
            CPSTitle(screen = currentScreen, modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.CenterStart))
            androidx.compose.animation.AnimatedVisibility(
                visible = showUIPanel,
                enter = expandHorizontally(expandFrom = Alignment.Start),
                exit = shrinkHorizontally(shrinkTowards = Alignment.Start),
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                UIPanel(modifier = Modifier.fillMaxWidth()) { showUIPanel = false }
            }
        }

        IconButton(
            onClick = { showMenu = true },
            content = { Icon(Icons.Default.MoreVert, null) },
        )

        CPSDropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
        ) {
            CPSDropdownMenuItem(title = "UI", icon = Icons.Filled.SettingsApplications) {
                showUIPanel = true
            }
            CPSDropdownMenuItem(title = "About", icon = Icons.Outlined.Info) {
                showAbout = true
            }
            if (currentScreen == Screen.News) {
                Divider(color = cpsColors.dividerColor)
                CPSDropdownMenuItem(title = "Settings", icon = Icons.Default.Settings) {
                    navController.navigate(Screen.NewsSettings.route)
                }
                CPSDropdownMenuItem(title = "Follow List", icon = Icons.Rounded.PeopleAlt) {
                    //TODO Open FollowList
                }
            }
        }
    }

    if (showAbout) CPSAboutDialog(onDismissRequest = { showAbout = false })
}


@Composable
private fun CPSTitle(
    screen: Screen?,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 16.sp
) = Column(modifier = modifier) {
    MonospacedText(
        text = "Competitive Programming && Solving",
        color = cpsColors.textColor,
        fontSize = fontSize,
        maxLines = 1
    )
    MonospacedText(
        text = screen?.subtitle ?: "",
        color = cpsColors.textColorAdditional,
        fontSize = fontSize,
        maxLines = 1
    )
}

@Composable
private fun UIPanel(
    modifier: Modifier = Modifier,
    onClosePanel: () -> Unit
) = Row(modifier = modifier.background(cpsColors.background)) {
    val scope = rememberCoroutineScope()

    val settingsUI = context.settingsUI
    val useOriginalColors by rememberCollect { settingsUI.useOriginalColors.flow }
    val coloredStatusBar by rememberCollect { settingsUI.coloredStatusBar.flow }
    val darkLightMode by rememberCollect { settingsUI.darkLightMode.flow }

    CPSIconButton(icon = Icons.Default.Close, onClick = onClosePanel)
    Row(
        modifier = Modifier.weight(1f),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CPSIconButton(icon = Icons.Default.ColorLens, onState = useOriginalColors) {
            scope.launch {
                settingsUI.useOriginalColors(!useOriginalColors)
            }
        }
        CPSIconButton(icon = Icons.Default.WebAsset, onState = coloredStatusBar) {
            scope.launch {
                settingsUI.coloredStatusBar(!coloredStatusBar)
            }
        }
        /*
        TODO:
        1) disable buttons iff recorded list is empty
        2) disable bar iff none enabled
        3) on enable click and if none enabled -> open menu
        4) if someone checked -> enable bar (because of 2)
         */
        StatusBarAccountsButton(
            enabled = coloredStatusBar
        )
        DarkLightModeButton(
            mode = darkLightMode,
            isSystemInDarkMode = isSystemInDarkTheme()
        ) { mode ->
            scope.launch {
                settingsUI.darkLightMode(mode)
            }
        }
    }
}

@Composable
private fun DarkLightModeButton(
    mode: DarkLightMode,
    isSystemInDarkMode: Boolean,
    onModeChanged: (DarkLightMode) -> Unit
) {
    CPSIconButton(
        icon = when (mode) {
            DarkLightMode.SYSTEM -> Icons.Default.BrightnessAuto
            else -> Icons.Default.BrightnessMedium
        },
        onClick = {
            onModeChanged(
                when (mode) {
                    DarkLightMode.SYSTEM -> if (isSystemInDarkMode) DarkLightMode.LIGHT else DarkLightMode.DARK
                    DarkLightMode.DARK -> DarkLightMode.LIGHT
                    DarkLightMode.LIGHT -> DarkLightMode.DARK
                }
            )
        }
    )
}

@Composable
private fun CPSAboutDialog(onDismissRequest: () -> Unit) {
    val scope = rememberCoroutineScope()

    val settingsDev = context.settingsDev
    val devModeEnabled by settingsDev.devModeEnabled.collectAsState()
    var showDevModeLine by remember { mutableStateOf(devModeEnabled) }

    showDevModeLine = true //TODO enable by click pattern

    CPSDialog(
        horizontalAlignment = Alignment.Start,
        onDismissRequest = onDismissRequest
    ) {
        MonospacedText("   Competitive")
        MonospacedText("   Programming")
        MonospacedText("&& Solving")
        MonospacedText("{")
        MonospacedText("   version = ${BuildConfig.VERSION_NAME}")
        if (showDevModeLine) {
            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MonospacedText("   dev_mode = $devModeEnabled", Modifier.weight(1f))
                CPSCheckBox(checked = devModeEnabled) { checked ->
                    scope.launch { settingsDev.devModeEnabled(checked) }
                }
            }
        }
        MonospacedText("}")
    }
}
