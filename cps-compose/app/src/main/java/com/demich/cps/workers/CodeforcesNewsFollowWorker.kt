package com.demich.cps.workers

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.demich.cps.R
import com.demich.cps.news.settings.settingsNews
import com.demich.cps.notifications.notificationChannels
import com.demich.cps.notifications.setProgress
import com.demich.cps.room.followListDao
import kotlin.time.Duration.Companion.hours


class CodeforcesNewsFollowWorker(
    context: Context,
    parameters: WorkerParameters
): CPSWorker(
    work = getWork(context),
    parameters = parameters
) {
    companion object {
        fun getWork(context: Context) = object : CPSWork(name = "cf_follow", context = context) {
            override suspend fun isEnabled() = context.settingsNews.codeforcesFollowEnabled()
            override val requestBuilder get() =
                CPSPeriodicWorkRequestBuilder<CodeforcesNewsFollowWorker>(
                    repeatInterval = 6.hours,
                    //flex = 3.hours,
                    batteryNotLow = true
                )

        }
    }

    override suspend fun runWork(): Result {
        progressNotificationBuilder().build { notification, id ->
            setForeground(ForegroundInfo(id, notification))
        }

        val dao = context.followListDao
        val notificationManagerCompat = NotificationManagerCompat.from(context)

        val savedHandles = dao.getHandles().shuffled()

        var done = 0
        savedHandles.forEachWithProgress { handle ->
            if (dao.getAndReloadBlogEntries(handle) == null) return Result.retry()
            ++done
            progressNotificationBuilder().apply {
                builder.setProgress(total = savedHandles.size, current = done)
            }.notifyBy(notificationManagerCompat)
        }

        return Result.success()
    }

    private fun progressNotificationBuilder() =
        notificationChannels.codeforces.follow_progress.builder(context) {
            setContentTitle("Codeforces follow update...")
            setSmallIcon(R.drawable.ic_logo_codeforces)
            setSilent(true)
            setShowWhen(false)
        }
}
