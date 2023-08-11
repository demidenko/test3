package com.demich.cps.contests.loaders

import com.demich.cps.contests.database.Contest
import com.demich.cps.contests.loading.ContestsLoaders
import com.demich.cps.platforms.utils.AtCoderUtils
import com.demich.cps.platforms.api.AtCoderApi

class AtCoderContestsLoader: ContestsLoader(type = ContestsLoaders.atcoder_parse) {
    override suspend fun loadContests(platform: Contest.Platform): List<Contest> =
        AtCoderUtils.extractContests(source = AtCoderApi.getContestsPage())
}