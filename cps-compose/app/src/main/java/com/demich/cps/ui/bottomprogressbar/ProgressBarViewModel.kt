package com.demich.cps.ui.bottomprogressbar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch


@Immutable
data class ProgressBarInfo(
    val total: Int,
    val current: Int = 0,
    val title: String = ""
) {
    val fraction: Float get() = current.toFloat() / total

    operator fun inc(): ProgressBarInfo = copy(current = current + 1)
}

class ProgressBarViewModel: ViewModel() {
    val progressBars = mutableStateListOf<String>()

    private val states = mutableMapOf<String, MutableStateFlow<ProgressBarInfo>>()

    @Composable
    fun collectProgress(id: String) = states[id]!!.collectAsState()

    fun doJob(
        id: String,
        block: suspend CoroutineScope.(MutableStateFlow<ProgressBarInfo>) -> Unit
    ) {
        viewModelScope.launch {
            require(id !in states) { "progress bar with id=$id is already started" }
            val progressStateFlow = states.getOrPut(id) { MutableStateFlow(ProgressBarInfo(total = 0)) }
            progressBars.add(id)
            block(progressStateFlow)
            progressBars.remove(id)
            states.remove(id)
        }
    }
}