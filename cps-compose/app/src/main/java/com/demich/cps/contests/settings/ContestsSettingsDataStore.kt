package com.demich.cps.contests.settings

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.demich.cps.contests.Contest
import com.demich.cps.contests.loaders.ContestsLoaders
import com.demich.cps.utils.CListApi
import com.demich.cps.utils.CPSDataStore
import com.demich.cps.utils.ClistResource
import com.demich.cps.utils.DurationAsSecondsSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

val Context.settingsContests: ContestsSettingsDataStore
    get() = ContestsSettingsDataStore(this)

class ContestsSettingsDataStore(context: Context): CPSDataStore(context.contests_settings_dataStore) {

    companion object {
        private val Context.contests_settings_dataStore by preferencesDataStore("contests_settings")
    }

    val enabledPlatforms = itemEnumSet(name = "enabled_platforms", defaultValue = setOf(Contest.Platform.unknown))
    val lastReloadedPlatforms = itemEnumSet<Contest.Platform>(name = "last_reloaded_platforms", defaultValue = emptySet())
    val ignoredContests = itemJsonable<List<Pair<Contest.Platform, String>>>(name = "ignored_contests_list", defaultValue = emptyList())

    val clistApiAccess = itemJsonable(name = "clist_api_access", defaultValue = CListApi.ApiAccess("", ""))
    val clistAdditionalResources = itemJsonable<List<ClistResource>>(name = "clist_additional_resources", defaultValue = emptyList())
    val clistLastReloadedAdditionalResources = itemJsonable<Set<Int>>(name = "clist_additional_last_reloaded", defaultValue = emptySet())

    val contestsDateLimits = itemJsonable(name = "contests_date_limits", defaultValue = ContestDateLimits())

    val contestsLoadersPriorityLists = itemJsonable(name = "loading_priorities", defaultValue = defaultLoadingPriorities)

}

@Serializable
data class ContestDateLimits(
    @Serializable(with = DurationAsSecondsSerializer::class)
    val nowToStartTimeMaxDuration: Duration = 120.days,

    @Serializable(with = DurationAsSecondsSerializer::class)
    val endTimeToNowMaxDuration: Duration = 7.days,

    @Serializable(with = DurationAsSecondsSerializer::class)
    val maxContestDuration: Duration = 30.days,
) {
    fun createLimits(now: Instant) = Limits(
        maxStartTime = now + nowToStartTimeMaxDuration,
        minEndTime = now - endTimeToNowMaxDuration,
        maxDuration = maxContestDuration
    )
    data class Limits(
        val maxStartTime: Instant,
        val minEndTime: Instant,
        val maxDuration: Duration
    )
}

private val defaultLoadingPriorities by lazy {
    Contest.platforms.associateWith { platform ->
        when (platform) {
            Contest.Platform.codeforces -> listOf(
                ContestsLoaders.clist,
                ContestsLoaders.codeforces
            )
            Contest.Platform.dmoj -> listOf(
                ContestsLoaders.clist,
                ContestsLoaders.dmoj
            )
            else -> listOf(ContestsLoaders.clist)
        }
    }
}
