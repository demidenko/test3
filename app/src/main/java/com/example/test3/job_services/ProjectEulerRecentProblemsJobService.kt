package com.example.test3.job_services

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.example.test3.*
import com.example.test3.news.SettingsNewsFragment
import com.example.test3.utils.ProjectEulerAPI
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.*

class ProjectEulerRecentProblemsJobService: CoroutineJobService() {

    companion object {
        private const val PREFERENCES_FILE_NAME = "project_euler"
        private const val LAST_RECENT_PROBLEM_ID = "last_recent_problem_id"

        suspend fun isEnabled(context: Context): Boolean =
            SettingsNewsFragment.getSettings(context).getNewsFeedEnabled(SettingsNewsFragment.NewsFeed.PROJECT_EULER_RECENT)
    }

    override suspend fun makeJobs(): List<Job> {
        if (isEnabled(this)) return listOf( launch { parseRecentProblems() } )
        else{
            JobServicesCenter.stopJobService(this, JobServiceIDs.project_euler_recent_problems)
            return emptyList()
        }
    }

    private suspend fun parseRecentProblems() {
        val s = ProjectEulerAPI.getRecentProblemsPage() ?: return

        val prefs = getSharedPreferences(PREFERENCES_FILE_NAME, Context.MODE_PRIVATE)
        val lastViewedProblemID = prefs.getInt(LAST_RECENT_PROBLEM_ID, 0)

        val newProblems = mutableListOf<Pair<Int,String>>()

        var i = 0
        while(true){
            i = s.indexOf("<td class=\"id_column\">", i+1)
            if(i==-1) break

            val problemID = s.substring(s.indexOf('>',i)+1, s.indexOf('<',i+1)).toInt()
            if(problemID == lastViewedProblemID) break

            i = s.indexOf("</a",i)
            val problemName = s.substring(s.lastIndexOf('>',i)+1, i)

            newProblems.add(Pair(problemID, problemName))
        }

        if(newProblems.isEmpty()) return

        if(lastViewedProblemID != 0){
            newProblems.forEach { (id, name) ->
                val n = notificationBuilder(this, NotificationChannels.project_euler_problems).apply {
                    setSubText("Project Euler • New problem published!")
                    setContentTitle("Problem $id")
                    setBigContent(name)
                    setSmallIcon(R.drawable.ic_projecteuler_logo)
                    setColor(NotificationColors.project_euler_main)
                    setShowWhen(true)
                    setAutoCancel(true)
                    setContentIntent(makePendingIntentOpenURL("https://projecteuler.net/problem=$id", this@ProjectEulerRecentProblemsJobService))
                }
                NotificationManagerCompat.from(this).notify(NotificationIDs.makeProjectEulerRecentProblemNotificationID(id), n.build())
            }
        }

        val firstID = newProblems.first().first
        if(firstID != lastViewedProblemID) {
            with(prefs.edit()) {
                putInt(LAST_RECENT_PROBLEM_ID, firstID)
                apply()
            }
        }
    }

}