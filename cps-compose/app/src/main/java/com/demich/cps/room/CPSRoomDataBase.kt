package com.demich.cps.room

import android.content.Context
import androidx.room.*
import com.demich.cps.contests.Contest

@Database(
    entities = [
        Contest::class,
        CodeforcesUserBlog::class,
        CodeforcesLostBlogEntry::class,
    ],
    version = 1
)
@TypeConverters(
    IntsListConverter::class,
    CodeforcesUserInfoConverter::class,
    InstantSecondsConverter::class
)
abstract class RoomSingleton: RoomDatabase() {
    abstract fun lostBlogEntriesDao(): LostBlogEntriesDao
    abstract fun followListDao(): FollowListDao
    abstract fun contestsListDao(): ContestsListDao

    companion object {
        private var instance: RoomSingleton? = null
        fun getInstance(context: Context): RoomSingleton {
            return instance
                ?: Room.databaseBuilder(context, RoomSingleton::class.java, "CPSdb")
                    .build()
                    .also { instance = it }
        }
    }
}

