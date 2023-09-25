package com.demich.cps.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.demich.cps.ui.bottomprogressbar.ProgressBarInfo
import com.demich.cps.utils.getCurrentTime
import com.demich.cps.utils.joinAllWithCounter
import com.demich.cps.utils.jsonCPS
import com.demich.datastore_itemized.ItemizedDataStore
import com.demich.datastore_itemized.dataStoreWrapper
import com.demich.datastore_itemized.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlin.time.Duration

abstract class CPSWorker(
    private val work: CPSWork,
    val parameters: WorkerParameters
): CoroutineWorker(work.context, parameters) {

    protected val context get() = work.context
    protected val workerStartTime by lazy { getCurrentTime() }

    final override suspend fun doWork(): Result {
        if (!work.isEnabled()) {
            work.stop()
            return Result.success()
        }

        val workersInfo = CPSWorkersDataStore(context)

        val result = withContext(Dispatchers.IO) {
            workersInfo.edit { prefs ->
                prefs.edit(lastExecutionTime) {
                    this[work.name] = workerStartTime
                }
                prefs.edit(lastResult) {
                    remove(work.name)
                }
                prefs.edit(lastDuration) {
                    remove(work.name)
                }
            }

            kotlin.runCatching { runWork() }
                .getOrElse { Result.failure() }
                .also { result ->
                    workersInfo.edit { prefs ->
                        prefs.edit(lastResult) {
                            val type = result.toType()
                            if (type == null) remove(work.name)
                            else this[work.name] = type
                        }
                        prefs.edit(lastDuration) {
                            this[work.name] = getCurrentTime() - workerStartTime
                        }
                    }
            }
        }

        return result
    }

    abstract suspend fun runWork(): Result

    protected suspend fun setProgressInfo(progressInfo: ProgressBarInfo) {
        if (progressInfo.total == 0) return
        setProgress(workDataOf(
            KEY_PROGRESS to arrayOf(progressInfo.current, progressInfo.total)
        ))
    }

    protected suspend inline fun<reified T> List<T>.forEachWithProgress(
        action: (T) -> Unit
    ) {
        var progressInfo = ProgressBarInfo(total = size)
        setProgressInfo(progressInfo)
        forEach {
            action(it)
            progressInfo++
            setProgressInfo(progressInfo)
        }
    }

    protected suspend fun List<suspend () -> Unit>.joinAllWithProgress() {
        if (isEmpty()) return
        joinAllWithCounter {
            setProgressInfo(ProgressBarInfo(current = it, total = size))
        }
    }


    enum class ResultTypes {
        SUCCESS, RETRY, FAILURE
    }

    private fun Result.toType(): ResultTypes? {
        return when (this) {
            is Result.Success -> ResultTypes.SUCCESS
            is Result.Retry -> ResultTypes.RETRY
            is Result.Failure -> ResultTypes.FAILURE
            else -> null
        }
    }
}

private const val KEY_PROGRESS = "cpsworker_progress"
fun WorkInfo.getProgressInfo(): ProgressBarInfo? {
    val arr = progress.getIntArray(KEY_PROGRESS)?.takeIf { it.size == 2 } ?: return null
    return ProgressBarInfo(
        current = arr[0],
        total = arr[1]
    )
}


class CPSWorkersDataStore(context: Context): ItemizedDataStore(context.workersDataStore) {
    companion object {
        private val Context.workersDataStore by dataStoreWrapper(name = "workers_info")
    }

    val lastExecutionTime = jsonCPS.itemMap<String, Instant>(name = "last_execution_time")

    val lastResult = jsonCPS.itemMap<String, CPSWorker.ResultTypes>(name = "last_result_type")

    val lastDuration = jsonCPS.itemMap<String, Duration>(name = "last_duration")
}
