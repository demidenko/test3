package com.example.test3.contest_watch

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.work.*
import com.example.test3.*
import com.example.test3.R
import com.example.test3.utils.CodeforcesURLFactory
import com.example.test3.workers.WorkersCenter
import com.example.test3.workers.WorkersNames
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CodeforcesContestWatchWorker(val context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    companion object {
        private const val keyHandle = "handle"
        private const val keyContestID = "contestID"

        fun startWorker(context: Context, handle: String, contestID: Int) {
            WorkersCenter.getWorkInfo(context, WorkersNames.codeforces_contest_watcher).get().getOrNull(0)?.run {
                if(state == WorkInfo.State.RUNNING){
                    if(progress.getString(keyHandle) == handle && progress.getInt(keyContestID,-1) == contestID) return
                }
            }

            val request =  OneTimeWorkRequestBuilder<CodeforcesContestWatchWorker>()
                .setInputData(workDataOf(
                    keyHandle to handle,
                    keyContestID to contestID
                ))

            WorkersCenter.startCodeforcesContestWatcher(context, request)
        }
    }

    override suspend fun doWork(): Result {
        val handle = inputData.getString(keyHandle) ?: return Result.failure()
        val contestID = inputData.getInt(keyContestID, -1)

        setProgress(workDataOf(
            keyHandle to handle,
            keyContestID to contestID
        ))

        val notification = createNotificationBuilder(handle, contestID)

        setForeground(ForegroundInfo(NotificationIDs.codeforces_contest_watcher, notification.build()))

        withContext(Dispatchers.Default){
            CodeforcesContestWatcher(
                handle,
                contestID,
            ).apply {
                addCodeforcesContestWatchListener(
                    CodeforcesContestWatcherTableNotification(
                        context,
                        handle,
                        notification
                    )
                )
            }.start()
        }

        return Result.success()
    }




    private fun createNotificationBuilder(handle: String, contestID: Int): NotificationCompat.Builder {
        val notification = notificationBuilder(context, NotificationChannels.codeforces_contest_watcher).apply {
            setSmallIcon(R.drawable.ic_contest)
            setSubText(handle)
            setShowWhen(false)
            setNotificationSilent()
            setStyle(NotificationCompat.DecoratedCustomViewStyle())
        }

        //TODO call CodeforcesContestWatchLauncherWorker.stopWatcher
        notification.addAction(NotificationCompat.Action(
            createIcon(R.drawable.ic_delete_item),
            "Close",
            WorkManager.getInstance(context).createCancelPendingIntent(id)
        ))

        notification.addAction(NotificationCompat.Action(
            createIcon(R.drawable.ic_open_in_browser),
            "Browse",
            makePendingIntentOpenURL(CodeforcesURLFactory.contest(contestID),context)
        ))

        return notification
    }

    private fun createIcon(@DrawableRes resid: Int): IconCompat? {
        return if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O){
            IconCompat.createWithResource(context, resid)
        } else null
    }
}