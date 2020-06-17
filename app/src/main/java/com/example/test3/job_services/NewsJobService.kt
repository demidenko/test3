package com.example.test3.job_services

import android.content.Context
import android.graphics.Color
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.test3.*
import kotlinx.coroutines.launch
import java.nio.charset.Charset

class NewsJobService : CoroutineJobService() {

    companion object{
        const val PREFERENCES_FILE_NAME = "news_parsers"

        //acmp
        const val ACMP_LAST_NEWS = "acmp_last_news"

        //project euler
        const val PROJECT_EULER_LAST_NEWS = "project_euler_last_news"
    }

    override suspend fun doJob() {
        launch { parseACMP() }
        launch { parseProjectEuler() }
    }

    private suspend fun parseACMP() {
        val s = readURLData("https://acmp.ru", Charset.forName("windows-1251")) ?: return

        val prefs = getSharedPreferences(PREFERENCES_FILE_NAME, Context.MODE_PRIVATE)

        val lastNewsID = prefs.getInt(ACMP_LAST_NEWS, 0)
        val news = mutableListOf<Pair<Int,String>>()
        var i = 0
        while (true) {
            i = s.indexOf("<a name=news_", i+1)
            if(i==-1) break

            val currentID = s.substring(s.indexOf("_",i)+1, s.indexOf(">",i)).toInt()
            if(lastNewsID!=-1 && currentID<=lastNewsID) break

            val title = fromHTML(s.substring(i,s.indexOf("<br><br>", i)))

            news.add(Pair(currentID,title))
        }

        if(news.isEmpty()) return

        if(lastNewsID != 0){
            val group = "acmp_news_group"
            val color = Color.argb(0xFF, 0, 0x66, 0)

            news.forEach { (id, title) ->
                val n = NotificationCompat.Builder(this, NotificationChannels.acmp_news).apply {
                    setSubText("acmp news")
                    setContentText(title)
                    setStyle(NotificationCompat.BigTextStyle())
                    setSmallIcon(R.drawable.ic_news)
                    setColor(color)
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
                setColor(color)
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
        val s = readURLData("https://projecteuler.net/news") ?: return

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
                    setColor(Color.parseColor("#6b4e3d"))
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