package com.example.test3.account_manager

import android.content.Context
import com.example.test3.utils.ACMPAPI
import com.example.test3.utils.jsonCPS
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

class ACMPAccountManager(context: Context): AccountManager(context) {

    @Serializable
    data class ACMPUserInfo(
        override var status: STATUS,
        var id: String,
        var userName: String = "",
        var rating: Int = 0,
        var solvedTasks: Int = 0,
        var rank: Int = 0
    ): UserInfo() {
        override val userID: String
            get() = id

        override fun makeInfoOKString(): String {
            return "$userName [$solvedTasks / $rating]"
        }

        override fun link(): String = "https://acmp.ru/index.asp?main=user&id=$id"
    }



    override val PREFERENCES_FILE_NAME: String
        get() = preferences_file_name

    companion object {
        const val preferences_file_name = "acmp"
    }

    override fun emptyInfo() = ACMPUserInfo(STATUS.NOT_FOUND, "")

    override suspend fun downloadInfo(data: String): ACMPUserInfo {
        val res = ACMPUserInfo(STATUS.FAILED, data)
        val s = ACMPAPI.getUser(data) ?: return res
        if(!s.contains("index.asp?main=status&id_mem=$data")) return res.apply { status = STATUS.NOT_FOUND }
        var i = s.indexOf("<title>")
        if(i!=-1){
            res.userName = s.substring(s.indexOf('>',i)+1, s.indexOf("</title>"))
        }
        i = s.indexOf("Решенные задачи")
        if(i!=-1){
            i = s.indexOf('(', i)
            res.solvedTasks = s.substring(i+1, s.indexOf(')',i)).toInt()
        }
        i = s.indexOf("<b class=btext>Рейтинг:")
        if(i!=-1){
            i = s.indexOf(':', i)
            res.rating = s.substring(i+2, s.indexOf('/', i)-1).toInt()
            i = s.lastIndexOf("<b class=btext>Место:", i)
            i = s.indexOf(':', i)
            res.rank = s.substring(i+2, s.indexOf('/', i)-1).toInt()
        }
        res.status = STATUS.OK
        return res
    }

    override fun decodeFromString(str: String) = jsonCPS.decodeFromString<ACMPUserInfo>(str)

    override fun encodeToString(info: UserInfo) = jsonCPS.encodeToString(info as ACMPUserInfo)

    override suspend fun loadSuggestions(str: String): List<AccountSuggestion>? {
        if(str.toIntOrNull()!=null) return null
        val s = ACMPAPI.getUserSearch(str) ?: return null
        val res = ArrayList<AccountSuggestion>()
        var k = s.indexOf("<table cellspacing=1 cellpadding=2 align=center class=main>")
        while(true){
            k = s.indexOf("<tr class=white>", k+1)
            if(k==-1) break
            var i = s.indexOf("<td>", k+1)
            i = s.indexOf('>',i+4)
            val userid = s.substring(s.lastIndexOf("id=",i)+3, i)
            val username = s.substring(i+1, s.indexOf("</a",i))
            i = s.indexOf("<td align=right>", i)
            i = s.indexOf("</a></td>", i)
            val tasks = s.substring(s.lastIndexOf('>',i)+1, i)
            res += AccountSuggestion(username, tasks, userid)
        }
        return res
    }
}