package com.demich.cps.contests.loaders

import com.demich.cps.contests.Contest
import com.demich.cps.contests.settings.ContestDateConstraints
import com.demich.cps.contests.settings.ContestsSettingsDataStore
import com.demich.cps.utils.getCurrentTime
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

suspend fun getContests(
    setup: Map<Contest.Platform, List<ContestsLoaders>>,
    settings: ContestsSettingsDataStore,
    contestsReceiver: ContestsReceiver
) {
    val loaders = settings.createLoaders(
        loaderTypes = setup.flatMapTo(mutableSetOf()) { it.value }
    ).associateBy { it.type }

    val dateConstraints = settings.contestsDateConstraints().makeFor(currentTime = getCurrentTime())
    val memorizer = MultipleLoadersMemorizer(setup, dateConstraints)

    coroutineScope {
        setup.forEach { (platform, priorities) ->
            launch {
                loadUntilSuccess(
                    platform = platform,
                    priorities = priorities,
                    loaders = loaders,
                    memorizer = memorizer,
                    dateConstraints = dateConstraints,
                    contestsReceiver = contestsReceiver
                )
            }
        }
    }
}

private suspend fun loadUntilSuccess(
    platform: Contest.Platform,
    priorities: List<ContestsLoaders>,
    loaders: Map<ContestsLoaders, ContestsLoader>,
    memorizer: MultipleLoadersMemorizer,
    dateConstraints: ContestDateConstraints.Current,
    contestsReceiver: ContestsReceiver
) {
    require(priorities.isNotEmpty())
    contestsReceiver.startLoading(platform)
    for (loaderType in priorities) {
        val loader = loaders.getValue(loaderType)
        val result: Result<List<Contest>> = if (loader is ContestsLoaderMultiple) {
            memorizer.get(loader = loader).map { contests ->
                contests.filter { it.platform == platform }
            }
        } else {
            runCatching {
                loader.getContests(platform = platform, dateConstraints = dateConstraints)
            }
        }
        result.onSuccess { contests ->
            contestsReceiver.finishSuccess(platform, contests)
            return
        }.onFailure {
            contestsReceiver.consumeError(platform, loaderType, it)
        }
    }
    contestsReceiver.finishFailed(platform)
}

private class MultipleLoadersMemorizer(
    private val setup: Map<Contest.Platform, List<ContestsLoaders>>,
    private val dateConstraints: ContestDateConstraints.Current
) {
    private val mutex = Mutex()
    private val results = mutableMapOf<ContestsLoaders, Deferred<Result<List<Contest>>>>()
    suspend fun get(loader: ContestsLoaderMultiple): Result<List<Contest>> {
        return coroutineScope {
            mutex.withLock {
                results.getOrPut(loader.type) {
                    async { runLoader(loader) }
                }
            }
        }.await()
    }

    private suspend fun runLoader(loader: ContestsLoaderMultiple): Result<List<Contest>> {
        val platforms = setup.mapNotNull { (platform, loaderTypes) ->
            if (loader.type in loaderTypes) platform else null
        }
        return kotlin.runCatching {
            loader.getContests(
                platforms = platforms,
                dateConstraints = dateConstraints
            )
        }
    }
}

private suspend fun ContestsSettingsDataStore.createLoaders(
    loaderTypes: Set<ContestsLoaders>
): List<ContestsLoader> = loaderTypes.map { loaderType ->
    when (loaderType) {
        ContestsLoaders.clist -> ClistContestsLoader(
            apiAccess = clistApiAccess(),
            includeResourceIds = { clistAdditionalResources().map { it.id } }
        )
        ContestsLoaders.codeforces -> CodeforcesContestsLoader()
        ContestsLoaders.dmoj -> DmojContestsLoader()
    }
}