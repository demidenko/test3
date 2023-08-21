package com.demich.cps.ui

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.snap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demich.cps.ui.dialogs.CPSDialogSelectEnum
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.clickableNoRipple
import com.demich.cps.utils.context
import com.demich.cps.utils.rememberCollect
import com.demich.cps.workers.CPSWork
import com.demich.datastore_itemized.DataStoreItem
import kotlinx.coroutines.launch

@Composable
fun SettingsColumn(
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
        modifier = Modifier
            .padding(all = 10.dp)
            .verticalScroll(rememberScrollState())
            .fillMaxWidth()
    )
}

@Composable
fun SettingsSectionHeader(
    title: String,
    painter: Painter,
    modifier: Modifier = Modifier,
    color: Color = cpsColors.accent
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconSp(
            painter = painter,
            size = 18.sp,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp)
        )
        Text(
            text = title,
            fontSize = 14.sp,
            color = color,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun SettingsItem(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .background(cpsColors.backgroundAdditional, RoundedCornerShape(4.dp))
            .fillMaxWidth()
            .padding(all = 10.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        content()
    }
}

@Composable
fun SettingsItemContent(
    title: String,
    description: String = "",
    trailerContent: @Composable () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.heightIn(min = 48.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (description.isNotBlank()) {
                Text(
                    text = description,
                    color = cpsColors.contentAdditional,
                    fontSize = 14.sp
                )
            }
        }
        trailerContent()
    }
}

@Composable
fun SettingsItem(
    modifier: Modifier = Modifier,
    title: String,
    description: String = "",
    trailerContent: @Composable () -> Unit
) {
    SettingsItem(modifier = modifier) {
        SettingsItemContent(
            title = title,
            description = description,
            trailerContent = trailerContent
        )
    }
}

@Composable
fun SettingsSwitchItemContent(
    checked: Boolean,
    title: String,
    description: String = "",
    onCheckedChange: (Boolean) -> Unit
) {
    SettingsItemContent(
        title = title,
        description = description
    ) {
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.padding(start = 5.dp),
            colors = SwitchDefaults.colors(
                checkedThumbColor = cpsColors.accent
            )
        )
    }
}

@Composable
fun SettingsSwitchItem(
    checked: Boolean,
    title: String,
    description: String = "",
    onCheckedChange: (Boolean) -> Unit
) {
    SettingsItem {
        SettingsSwitchItemContent(
            checked = checked,
            title = title,
            description = description,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun SettingsSwitchItem(
    item: DataStoreItem<Boolean>,
    title: String,
    description: String = "",
    onCheckedChange: (Boolean) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val checked by rememberCollect { item.flow }
    SettingsSwitchItem(
        checked = checked,
        title = title,
        description = description
    ) {
        scope.launch {
            item(it)
            onCheckedChange(it)
        }
    }
}

@Composable
fun SettingsSwitchItemWithWork(
    item: DataStoreItem<Boolean>,
    title: String,
    description: String = "",
    workGetter: (Context) -> CPSWork,
    stopWorkOnUnchecked: Boolean = true
) {
    val context = context
    SettingsSwitchItem(
        item = item,
        title = title,
        description = description
    ) { checked ->
        with(workGetter(context)) {
            if (checked) startImmediate()
            else if (stopWorkOnUnchecked) stop()
        }
    }
}

@Composable
fun<T: Enum<T>> SettingsEnumItemContent(
    item: DataStoreItem<T>,
    title: String,
    description: String = "",
    optionToString: @Composable (T) -> AnnotatedString = { AnnotatedString(it.name) },
    options: List<T>
) {
    val scope = rememberCoroutineScope()
    val selectedOption by rememberCollect { item.flow }

    var showChangeDialog by rememberSaveable { mutableStateOf(false) }

    SettingsItemContent(
        title = title,
        description = description
    ) {
        TextButton(onClick = { showChangeDialog = true }) {
            Text(
                text = optionToString(selectedOption),
                fontSize = 18.sp,
                color = cpsColors.accent
            )
        }
    }

    if (showChangeDialog) {
        CPSDialogSelectEnum(
            title = title,
            options = options,
            selectedOption = selectedOption,
            optionTitle = { Text(text = optionToString(it)) },
            onDismissRequest = { showChangeDialog = false },
            onSelectOption = {
                scope.launch { item(newValue = it) }
            }
        )
    }
}

@Composable
fun<T: Enum<T>> SettingsEnumItem(
    item: DataStoreItem<T>,
    title: String,
    description: String = "",
    optionToString: @Composable (T) -> AnnotatedString = { AnnotatedString(it.name) },
    options: List<T>
) {
    SettingsItem {
        SettingsEnumItemContent(
            item = item,
            title = title,
            description = description,
            optionToString = optionToString,
            options = options
        )
    }
}


@Composable
fun<T> SettingsItemWithInfo(
    modifier: Modifier = Modifier,
    item: DataStoreItem<T>,
    title: String,
    infoContent: @Composable (T) -> Unit
) {
    SettingsItem(modifier = modifier) {
        val value by rememberCollect { item.flow }
        Column {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            infoContent(value)
        }
    }
}

@Composable
fun SettingsSubtitle(
    text: String
) {
    Text(
        text = text,
        fontSize = 15.sp,
        color = cpsColors.contentAdditional,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
fun<T: Enum<T>> SettingsSubtitleOfEnabled(
    enabled: Set<T>,
    allSize: Int? = null,
    name: (T) -> String = { it.name }
) {
    if (enabled.isEmpty()) SettingsSubtitle("none selected")
    else if (enabled.size == allSize) SettingsSubtitle("all selected")
    else WordsWithCounterOnOverflow(words = enabled.sortedBy { it.ordinal }.map(name), fontSize = 15.sp)
}

@Composable
fun ExpandableSettingsItem(
    title: String,
    collapsedContent: @Composable () -> Unit,
    expandedContent: @Composable () -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    SettingsItem(
        modifier = Modifier.clickableNoRipple(enabled = !expanded) { expanded = true }
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(animationSpec = snap()),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = CPSIcons.CollapseUp,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickableNoRipple { expanded = !expanded }
                )
                AnimatedContent(
                    targetState = expanded,
                    transitionSpec = { fadeIn() togetherWith fadeOut(snap()) },
                    modifier = Modifier.fillMaxWidth(),
                    label = "settings item content"
                ) { itemExpanded ->
                    if (itemExpanded) {
                        Box(
                            content = { expandedContent() },
                            modifier = Modifier
                                .padding(horizontal = 10.dp)
                                .padding(top = 8.dp)
                        )
                    } else {
                        collapsedContent()
                    }
                }
            }
        }
    }
}