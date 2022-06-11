package com.demich.cps.workers

import android.content.Context
import androidx.work.*
import com.demich.cps.news.settings.settingsNews
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

object WorkersNames {
    const val accounts_parsers = "accounts"
    const val news_parsers = "news_feeds"
    const val codeforces_news_lost_recent = "cf_lost"
    const val codeforces_news_follow = "cf_follow"
    const val codeforces_contest_watcher = "cf_contest_watcher"
    const val codeforces_contest_watch_launcher = "cf_contest_watch_launcher"
    const val codeforces_upsolving_suggestions = "cf_upsolving_suggestions"
    const val project_euler_recent_problems = "pe_recent"
}

private val Context.workManager get() = WorkManager.getInstance(this)

object WorkersCenter {

    private const val commonTag = "cps"
    //fun getWorksLiveData(context: Context) = context.workManager.getWorkInfosByTagLiveData(commonTag)
    //fun getWorkInfo(context: Context, name: String) = context.workManager.getWorkInfosForUniqueWork(name)

    private inline fun<reified T: CoroutineWorker> makeAndEnqueueWork(
        context: Context,
        name: String,
        restart: Boolean,
        repeatInterval: Duration,
        flex: Duration = repeatInterval,
        batteryNotLow: Boolean = false
    ) {
        val request = PeriodicWorkRequestBuilder<T>(
            repeatInterval = repeatInterval.inWholeMilliseconds,
            repeatIntervalTimeUnit = TimeUnit.MILLISECONDS,
            flexTimeInterval = flex.inWholeMilliseconds,
            flexTimeIntervalUnit = TimeUnit.MILLISECONDS
        ).apply {
            addTag(commonTag)
            setConstraints(
                Constraints(
                    requiredNetworkType = NetworkType.CONNECTED,
                    requiresBatteryNotLow = batteryNotLow,
                    requiresCharging = false
                )
            )
            setBackoffCriteria(
                BackoffPolicy.LINEAR,
                PeriodicWorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
        }.build()

        context.workManager.enqueueUniquePeriodicWork(
            name,
            if (restart) ExistingPeriodicWorkPolicy.REPLACE else ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun stopWorker(context: Context, workName: String) = context.workManager.cancelUniqueWork(workName)

    suspend fun startWorkers(context: Context) {
        //startAccountsWorker(context, false)
        //startNewsWorker(context, false)

        with(context.settingsNews) {
            if (codeforcesFollowEnabled()) startCodeforcesNewsFollowWorker(context, false)
        }

        //if (CodeforcesNewsLostRecentWorker.isEnabled(context)) startCodeforcesNewsLostRecentWorker(context, false)
        //if (ProjectEulerRecentProblemsWorker.isEnabled(context)) startProjectEulerRecentProblemsWorker(context, false)

        /*with(CodeforcesAccountManager(context).getSettings()) {
            if (contestWatchEnabled()) startCodeforcesContestWatchLauncherWorker(context, false)
            if (upsolvingSuggestionsEnabled()) startCodeforcesUpsolveSuggestionsWorker(context, false)
        }*/
    }

    /*fun startCodeforcesNewsLostRecentWorker(context: Context, restart: Boolean = true) {
        makeAndEnqueueWork<CodeforcesNewsLostRecentWorker>(
            context,
            WorkersNames.codeforces_news_lost_recent,
            restart,
            1.hours
        )
    }*/

    fun startCodeforcesNewsFollowWorker(context: Context, restart: Boolean) {
        makeAndEnqueueWork<CodeforcesNewsFollowWorker>(
            context = context,
            name = WorkersNames.codeforces_news_follow,
            restart = restart,
            repeatInterval = 6.hours,
            //flex = 3.hours,
            batteryNotLow = true
        )
    }

    /*fun startCodeforcesContestWatchLauncherWorker(context: Context, restart: Boolean = true) {
        makeAndEnqueueWork<CodeforcesContestWatchLauncherWorker>(
            context,
            WorkersNames.codeforces_contest_watch_launcher,
            restart,
            45.minutes
        )
    }*/

    /*fun startCodeforcesUpsolveSuggestionsWorker(context: Context, restart: Boolean = true) {
        makeAndEnqueueWork<CodeforcesUpsolvingSuggestionsWorker>(
            context,
            WorkersNames.codeforces_upsolving_suggestions,
            restart,
            12.hours,
            batteryNotLow = true
        )
    }*/

    /*fun startAccountsWorker(context: Context, restart: Boolean = true) {
        makeAndEnqueueWork<AccountsWorker>(
            context,
            WorkersNames.accounts_parsers,
            restart,
            15.minutes
        )
    }*/

    /*fun startNewsWorker(context: Context, restart: Boolean = true) {
        makeAndEnqueueWork<NewsWorker>(
            context,
            WorkersNames.news_parsers,
            restart,
            6.hours,
            batteryNotLow = true
        )
    }*/

    /*fun startProjectEulerRecentProblemsWorker(context: Context, restart: Boolean = true) {
        makeAndEnqueueWork<ProjectEulerRecentProblemsWorker>(
            context,
            WorkersNames.project_euler_recent_problems,
            restart,
            1.hours
        )
    }*/

    /*fun startCodeforcesContestWatcher(context: Context, request: OneTimeWorkRequest.Builder) {
        getWorkManager(context).enqueueUniqueWork(
            WorkersNames.codeforces_contest_watcher,
            ExistingWorkPolicy.REPLACE,
            request.addTag(commonTag).build()
        )
    }*/
}