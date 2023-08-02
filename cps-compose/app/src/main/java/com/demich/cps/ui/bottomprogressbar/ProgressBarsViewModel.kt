package com.demich.cps.ui.bottomprogressbar

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.demich.cps.utils.sharedViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds


@Immutable
data class ProgressBarInfo(
    val total: Int,
    val current: Int = 0,
    val title: String = ""
) {
    val fraction: Float get() = current.toFloat() / total

    operator fun inc(): ProgressBarInfo = copy(current = current + 1)
}

@Composable
fun progressBarsViewModel(): ProgressBarsViewModel = sharedViewModel()

class ProgressBarsViewModel: ViewModel() {
    val progressBars = mutableStateListOf<String>()

    private val states = mutableMapOf<String, MutableState<ProgressBarInfo>>()

    @Composable
    fun collectProgress(id: String) = states.getValue(id)

    fun doJob(
        id: String,
        coroutineScope: CoroutineScope = viewModelScope,
        block: suspend CoroutineScope.(MutableState<ProgressBarInfo>) -> Unit
    ) {
        coroutineScope.launch {
            require(id !in states) { "progress bar with id=$id is already started" }
            val progressStateFlow = states.getOrPut(id) { mutableStateOf(ProgressBarInfo(total = 0)) }
            progressBars.add(id)
            block(progressStateFlow)
            if (progressStateFlow.value.total > 0) delay(1.seconds)
            progressBars.remove(id)
            states.remove(id)
        }
    }

    val clistImportIsRunning: Boolean get() = clistImportId in progressBars

    companion object {
        const val clistImportId = "clist_import"
    }
}