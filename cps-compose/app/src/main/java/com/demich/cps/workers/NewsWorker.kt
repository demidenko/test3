package com.demich.cps.workers

import android.content.Context
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkerParameters
import com.demich.cps.*
import com.demich.cps.news.settings.NewsSettingsDataStore
import com.demich.cps.news.settings.NewsSettingsDataStore.NewsFeed.atcoder_news
import com.demich.cps.news.settings.NewsSettingsDataStore.NewsFeed.project_euler_news
import com.demich.cps.utils.AtCoderApi
import com.demich.cps.utils.ProjectEulerApi
import com.demich.datastore_itemized.edit
import kotlinx.datetime.Instant
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import kotlin.time.Duration.Companion.hours

class NewsWorker(
    context: Context,
    parameters: WorkerParameters
): CPSWorker(
    work = getWork(context),
    parameters = parameters
) {
    companion object {
        fun getWork(context: Context) = object : CPSWork(name = "news", context = context) {
            override suspend fun isEnabled() = NewsSettingsDataStore(context).enabledNewsFeeds().isNotEmpty()
            override val requestBuilder: PeriodicWorkRequest.Builder
                get() = CPSPeriodicWorkRequestBuilder<NewsWorker>(
                    repeatInterval = 6.hours,
                    batteryNotLow = true
                )
        }
    }

    val settings by lazy { NewsSettingsDataStore(context) }
    override suspend fun runWork(): Result {
        val jobs = buildList {
            settings.enabledNewsFeeds().let { enabled ->
                if (atcoder_news in enabled) add(::atcoderNews)
                if (project_euler_news in enabled) add(::projectEulerNews)
            }
        }

        jobs.joinAllWithProgress()

        return Result.success()
    }

    private suspend fun atcoderNews() {
        data class Post(
            val title: String,
            val time: Instant,
            override val id: String
        ): PostEntry

        getPosts(
            newsFeed = atcoder_news,
            elements = Jsoup.parse(AtCoderApi.getMainPage()).select("div.panel.panel-default"),
            extractPost = { panel ->
                val header = panel.expectFirst("div.panel-heading")
                val titleElement = header.expectFirst("h3.panel-title")
                val timeElement = header.expectFirst("span.tooltip-unix")
                val id = titleElement.expectFirst("a").attr("href").removePrefix("/posts/")
                Post(
                    title = titleElement.text(),
                    time = Instant.fromEpochSeconds(timeElement.attr("title").toLong()),
                    id = id
                )
            }
        ) {
            notificationBuildAndNotify(
                context = context,
                channel = NotificationChannels.atcoder.news,
                notificationId = NotificationIds.makeAtCoderNewsId(it.id.toInt())
            ) {
                setSubText("atcoder news")
                setBigContent(it.title.trim())
                setSmallIcon(R.drawable.ic_news)
                setWhen(it.time)
                attachUrl(url = AtCoderApi.urls.post(it.id.toInt()), context = context)
                //setColor
                setAutoCancel(true)
            }
        }
    }

    private suspend fun projectEulerNews() {
        data class Post(
            val title: String,
            val descriptionHtml: String,
            override val id: String
        ): PostEntry

        getPosts(
            newsFeed = project_euler_news,
            elements = Jsoup.parse(ProjectEulerApi.getRSSPage()).select("item"),
            extractPost = { item ->
                val idFull = item.expectFirst("guid").text()
                val id = idFull.removePrefix("news_id_")
                if (id != idFull) {
                    Post(
                        title = item.expectFirst("title").text(),
                        descriptionHtml = item.expectFirst("description").html(),
                        id = id
                    )
                } else {
                    null
                }
            }
        ) {
            notificationBuildAndNotify(
                context = context,
                channel = NotificationChannels.project_euler.news,
                notificationId = NotificationIds.makeProjectEulerNewsId(it.id.toInt())
            ) {
                setSubText("Project Euler news")
                setContentTitle(it.title)
                setBigContent(
                    Jsoup.parse(it.descriptionHtml).text()
                        .replace("\n", "")
                        .replace("<p>", "")
                        .replace("</p>", "\n\n")
                )
                setSmallIcon(R.drawable.ic_news)
                setColor(context.getColor(R.color.project_euler_main))
                setShowWhen(false)
                setAutoCancel(true)
                attachUrl(url = ProjectEulerApi.urls.news, context = context)
            }
        }
    }

    private suspend fun<T: PostEntry> getPosts(
        newsFeed: NewsSettingsDataStore.NewsFeed,
        elements: Elements,
        extractPost: (Element) -> T?,
        onNewPost: (T) -> Unit
    ) {
        val lastId = settings.newsFeedsLastIds()[newsFeed]

        val newEntries = buildList {
            for (element in elements) {
                val post = extractPost(element) ?: continue
                if (post.id == lastId) break
                add(post)
            }
        }

        if (newEntries.isEmpty()) return

        if (lastId != null) {
            newEntries.forEach(onNewPost)
        }

        settings.newsFeedsLastIds.edit {
            this[newsFeed] = newEntries.first().id
        }
    }

    private interface PostEntry {
        val id: String
    }
}