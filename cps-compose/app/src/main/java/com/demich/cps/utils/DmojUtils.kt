package com.demich.cps.utils

import io.ktor.client.request.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*

object DmojApi {
    private val client = cpsHttpClient(json = jsonCPS) { }

    suspend fun getUser(handle: String): DmojUserResult {
        val json = client.getAs<JsonObject>(urlString = "${urls.main}/api/v2/user/$handle")
        val obj = json["data"]!!.jsonObject["object"]!!
        return jsonCPS.decodeFromJsonElement(obj)
    }

    suspend fun getUserPage(handle: String): String {
        return client.getText(urlString = "${urls.main}/user/$handle")
    }

    suspend fun getSuggestions(str: String): List<DmojSuggestion> {
        val json = client.getAs<JsonObject>(urlString = "${urls.main}/widgets/select2/user_search") {
            parameter("_type", "query")
            parameter("term", str)
            parameter("q", str)
        }
        return jsonCPS.decodeFromJsonElement(json["results"]!!)
    }

    suspend fun getContests(): List<DmojContest> {
        val json = client.getAs<JsonObject>(urlString = "${urls.main}/api/v2/contests")
        val obj = json["data"]!!.jsonObject["objects"]!!
        return jsonCPS.decodeFromJsonElement(obj)
    }

    suspend fun getRatingChanges(handle: String): List<DmojRatingChange> {
        val s = getUserPage(handle = handle)
        val i = s.indexOf("var rating_history = [")
        if (i == -1) return emptyList()
        val j = s.indexOf("];", i)
        val str = s.substring(s.indexOf('[', i), j+1)
        return jsonCPS.decodeFromString(str)
    }

    object urls {
        const val main = "https://dmoj.ca"
        fun user(username: String) = "$main/user/$username"
    }
}

@Serializable
data class DmojUserResult(
    val id: Int,
    val username: String,
    val rating: Int?
)

@Serializable
data class DmojSuggestion(
    val text: String,
    val id: String
)

@Serializable
data class DmojRatingChange(
    val label: String,
    val rating: Int,
    val ranking: Int,
    val link: String,
    val timestamp: Double
)

@Serializable
data class DmojContest(
    val key: String,
    val name: String,
    val start_time: String,
    val end_time: String
)