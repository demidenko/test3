package com.example.test3.job_services

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.preference.PreferenceManager
import com.example.test3.*
import com.example.test3.utils.ACMPAPI
import com.example.test3.utils.ProjectEulerAPI
import com.example.test3.utils.fromHTML
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

class NewsJobService : CoroutineJobService() {

    companion object{
        const val PREFERENCES_FILE_NAME = "news_parsers"

        //acmp
        const val ACMP_LAST_NEWS = "acmp_last_news"

        //project euler
        const val PROJECT_EULER_LAST_NEWS = "project_euler_last_news"
    }

    override suspend fun doJob() {
        val jobs = arrayListOf<Job>()
        with(PreferenceManager.getDefaultSharedPreferences(this)){
            if(getBoolean(getString(R.string.news_project_euler_feed),false)) jobs.add(launch { parseProjectEuler() })
            if(getBoolean(getString(R.string.news_acmp_feed),false)) jobs.add(launch { parseACMP() })
        }
        jobs.joinAll()
    }

    private suspend fun parseACMP() {
        val s = ACMPAPI.getMainPage() ?: return

        val prefs = getSharedPreferences(PREFERENCES_FILE_NAME, Context.MODE_PRIVATE)

        val lastNewsID = prefs.getInt(ACMP_LAST_NEWS, 0)
        val news = mutableListOf<Pair<Int,String>>()
        var i = 0
        while (true) {
            i = s.indexOf("<a name=news_", i+1)
            if(i==-1) break

            val currentID = s.substring(s.indexOf("_",i)+1, s.indexOf(">",i)).toInt()
            if(lastNewsID!=-1 && currentID<=lastNewsID) break

            val title = fromHTML(s.substring(i, s.indexOf("<br><br>", i)))

            news.add(Pair(currentID,title))
        }

        if(news.isEmpty()) return

        if(lastNewsID != 0){
            val group = "acmp_news_group"

            news.forEach { (id, title) ->
                val n = NotificationCompat.Builder(this, NotificationChannels.acmp_news).apply {
                    setSubText("acmp news")
                    setContentText(title)
                    setStyle(NotificationCompat.BigTextStyle())
                    setSmallIcon(R.drawable.ic_news)
                    setColor(NotificationColors.acmp_main)
                    setShowWhen(true)
                    setAutoCancel(true)
                    setGroup(group)
                    setContentIntent(makePendingIntentOpenURL("https://acmp.ru", this@NewsJobService))
                }
                NotificationManagerCompat.from(this).notify( NotificationIDs.makeACMPNewsNotificationID(id), n.build())
            }

            val n = NotificationCompat.Builder(this, NotificationChannels.acmp_news).apply {
                setStyle(NotificationCompat.InboxStyle().setSummaryText("acmp news"))
                setSmallIcon(R.drawable.ic_news)
                setColor(NotificationColors.acmp_main)
                setAutoCancel(true)
                setGroup(group)
                setGroupSummary(true)
            }
            NotificationManagerCompat.from(this).notify( NotificationIDs.makeACMPNewsNotificationID(0), n.build())
        }

        val firstID = news.first().first
        if(firstID != lastNewsID) {
            with(prefs.edit()) {
                putInt(ACMP_LAST_NEWS, firstID)
                apply()
            }
        }
    }

    private suspend fun parseProjectEuler() {
        val s = ProjectEulerAPI.getNews() ?: return

        val prefs = getSharedPreferences(PREFERENCES_FILE_NAME, Context.MODE_PRIVATE)

        val lastNewsID = prefs.getString(PROJECT_EULER_LAST_NEWS, null) ?: ""

        val news = mutableListOf<Pair<String, String>>()
        var i = 0
        while (true) {
            i = s.indexOf("<div class=\"news\">", i+1)
            if(i == -1) break

            val currentID = s.substring(s.indexOf("<h4>",i)+4, s.indexOf("</h4>",i))
            if(currentID == lastNewsID) break

            val content = s.substring(s.indexOf("<div>",i)+5, s.indexOf("</div>",i))

            news.add(Pair(currentID,content))
        }

        if(news.isEmpty()) return

        if(lastNewsID!=""){
            news.forEach { (title, content) ->
                val n = NotificationCompat.Builder(this, NotificationChannels.project_euler_news).apply {
                    setSubText("Project Euler news")
                    setContentTitle(title)
                    setContentText(fromHTML(content))
                    setStyle(NotificationCompat.BigTextStyle())
                    setSmallIcon(R.drawable.ic_news)
                    setColor(NotificationColors.project_euler_main)
                    setShowWhen(true)
                    setAutoCancel(true)
                    setContentIntent(makePendingIntentOpenURL("https://projecteuler.net/news", this@NewsJobService))
                }
                NotificationManagerCompat.from(this).notify( NotificationIDs.makeProjectEulerNewsNotificationID(title), n.build())
            }
        }

        val firstID = news.first().first
        if(firstID != lastNewsID) {
            with(prefs.edit()) {
                putString(PROJECT_EULER_LAST_NEWS, firstID)
                apply()
            }
        }
    }

}