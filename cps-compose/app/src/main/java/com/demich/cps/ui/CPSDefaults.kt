package com.demich.cps.ui

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object CPSDefaults {
    const val buttonOnOffDurationMillis: Int = 800

    val bottomBarHeight = 56.dp //as BottomNavigationHeight

    val scrollBarWidth = 5.dp

    val MonospaceTextStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        letterSpacing = 0.sp
    )
}