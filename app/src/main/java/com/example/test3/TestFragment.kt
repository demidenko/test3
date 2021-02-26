package com.example.test3

import android.app.ActivityManager
import android.content.Context
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.text.bold
import androidx.core.text.color
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.fragment.app.Fragment
import com.example.test3.account_manager.HandleColor
import com.example.test3.account_manager.RatedAccountManager
import com.example.test3.job_services.JobServicesCenter
import com.example.test3.job_services.settingsJobServices
import com.example.test3.utils.SettingsDataStore
import com.example.test3.utils.getCurrentTimeSeconds
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.*
import java.util.concurrent.TimeUnit


class TestFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_test, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?){
        super.onViewCreated(view, savedInstanceState)
        val mainActivity = requireActivity() as MainActivity


        val stuff = view.findViewById<TextView>(R.id.stuff_textview)

        //show running jobs
        mainActivity.navigation_develop.findViewById<ImageButton>(R.id.navigation_dev_jobs).setOnClickListener {
            val currentTimeSeconds = getCurrentTimeSeconds()
            val jobservices =  JobServicesCenter.getRunningJobServices(mainActivity).joinToString(separator = "\n"){ info ->
                buildString {
                    append("Job ${info.id}: ")
                    appendLine(info.service.shortClassName.removeSuffix("JobService").removePrefix(".job_services."))
                    val startTimeSeconds = runBlocking {
                        TimeUnit.MILLISECONDS.toSeconds(mainActivity.settingsJobServices.getStartTimeMillis(info.id))
                    }
                    append(timeDifference(startTimeSeconds, currentTimeSeconds))
                }
            }
            val services = (mainActivity.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getRunningServices(Int.MAX_VALUE)
                .mapNotNull {
                    val s = it.service.className
                    val s2 = s.removePrefix("com.example.test3.")
                    if(s == s2) null
                    else s2
                }
                .joinToString(separator = "\n") { it }
            stuff.text = jobservices + "\n\n" + services
        }

        //colors
        mainActivity.navigation_develop.findViewById<ImageButton>(R.id.navigation_dev_colors).setOnClickListener{
            val table = view.findViewById<LinearLayout>(R.id.table_handle_colors)
            table.removeAllViews()

            fun addRow(row: ArrayList<CharSequence>) {
                val l = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
                row.forEach { s->
                    l.addView(
                        TextView(context).apply{ text = s },
                        LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT).apply { weight = 1f }
                    )
                }
                table.addView(l)
            }

            addRow(arrayListOf(
                "app",
                "Codeforces",
                "AtCoder",
                "Topcoder"
            ))

            for(handleColor in HandleColor.values()){
                val row = arrayListOf<CharSequence>()

                row.add(
                    SpannableStringBuilder().bold {
                        color(handleColor.getARGB(mainActivity.accountsFragment.codeforcesAccountManager, false)) { append(handleColor.name) }
                    }
                )

                arrayOf<RatedAccountManager>(
                    mainActivity.accountsFragment.codeforcesAccountManager,
                    mainActivity.accountsFragment.atcoderAccountManager,
                    mainActivity.accountsFragment.topcoderAccountManager,
                ).forEach { manager ->
                    val s = SpannableStringBuilder().bold {
                        try {
                            color(handleColor.getARGB(manager, true)) { append(handleColor.name) }
                        } catch (e: HandleColor.UnknownHandleColorException) {
                            append("")
                        }
                    }
                    row.add(s)
                }
                addRow(row)
            }
        }

    }
}


class SettingsDev(context: Context) : SettingsDataStore(context, "settings_develop") {
    private val KEY_DEV = booleanPreferencesKey("develop_enabled")

    fun getDevEnabled() = runBlocking { dataStore.data.first()[KEY_DEV] ?: false }
    fun setDevEnabled(flag: Boolean) {
        runBlocking { dataStore.edit { it[KEY_DEV] = flag } }
    }
}