package com.example.test3

import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Context
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.FileNotFoundException
import java.io.PrintWriter
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.HashSet
import kotlin.coroutines.CoroutineContext


@JsonClass(generateAdapter = true)
data class BlogInfo(
    val id: Int,
    val title: String,
    val author: String,
    val authorColorTag: String,
    val creationTime: Long
){
    companion object{
        val jsonAdapter: JsonAdapter<BlogInfo> = Moshi.Builder().build().adapter(BlogInfo::class.java)
    }
}


class CodeforcesNewsLostRecentJobService : JobService(), CoroutineScope{
    override val coroutineContext: CoroutineContext = Job() + Dispatchers.Main

    override fun onStopJob(params: JobParameters?): Boolean {
        return false
    }

    override fun onStartJob(params: JobParameters?): Boolean {
        launch {
            job()
            jobFinished(params, false)
        }
        return true
    }

    private val highRated = arrayListOf("user-orange", "user-red", "user-legendary")
    suspend fun job(){
        val recentBlogs = CodeforcesNewsItemsRecentAdapter.parsePage(readURLData("https://codeforces.com/recent-actions?locale=ru") ?: return)
        if(recentBlogs.isEmpty()) return

        val currentTime = System.currentTimeMillis()

        val suspects = getBlogs(CF_LOST_SUSPECTS)
            .filter {
                currentTime - it.creationTime <= TimeUnit.DAYS.toMillis(1)
            }.toHashSet()

        val newSuspects = mutableListOf<BlogInfo>()
        recentBlogs.forEach { blog ->
            val blogID = blog.blogID.toInt()
            if(blog.authorColorTag in highRated && suspects.find { it.id == blogID } == null){
                val creationTime = CodeforcesUtils.getBlogCreationTimeMillis(blog.blogID)
                if(currentTime - creationTime <= TimeUnit.DAYS.toMillis(1)){
                    newSuspects.add(BlogInfo(
                        id = blogID,
                        creationTime = creationTime,
                        title = blog.title,
                        author = blog.author,
                        authorColorTag = blog.authorColorTag
                    ))
                }
            }
        }

        suspects.addAll(newSuspects)
        println("suspects = $suspects")

        val recentBlogIDs = recentBlogs.mapTo(HashSet()){ it.blogID.toInt() }
        val lost = getBlogs(CF_LOST)
            .filter {
                currentTime - it.creationTime <= TimeUnit.DAYS.toMillis(7)
            }.toHashSet()

        saveBlogs(CF_LOST_SUSPECTS,
            suspects
            .filter {
                if(it.id !in recentBlogIDs){
                    makeSimpleNotification(
                        this,
                        NotificationIDs.test,
                        "lost detected",
                        it.title,
                        false
                    )
                    lost.add(it)
                    false
                }else{
                    true
                }
            }
        )

        saveBlogs(CF_LOST, lost)

    }

    private val CF_LOST_SUSPECTS = "cf_lost_suspects.txt"
    private val CF_LOST = "cf_lost.txt"

    private fun getBlogs(file_name: String): List<BlogInfo> {
        try {
            val res = mutableListOf<BlogInfo>()
            val sc = Scanner(openFileInput(file_name))
            while(sc.hasNextLine()){
                val str = sc.nextLine()
                res.add(BlogInfo.jsonAdapter.fromJson(str)!!)
            }
            return res
        }catch (e: FileNotFoundException){
            return emptyList()
        }
    }

    private fun saveBlogs(file_name: String, blogs: Collection<BlogInfo>){
        println("save $file_name: $blogs")
        val out = PrintWriter(openFileOutput(file_name, Context.MODE_PRIVATE))
        blogs.forEach {
            val str = BlogInfo.jsonAdapter.toJson(it)
            out.println(str)
        }
        out.flush()
        out.close()
    }

}