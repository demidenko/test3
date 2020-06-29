package com.example.test3

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.RemoteViews
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.Fragment
import com.example.test3.contest_watch.CodeforcesContestWatchService
import com.example.test3.job_services.JobServicesCenter
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
        val activity = requireActivity() as MainActivity


        val stuff = view.findViewById<TextView>(R.id.stuff_textview)
        val editText = view.findViewById<EditText>(R.id.text_editor)
        val prefs = activity.getSharedPreferences("test", Context.MODE_PRIVATE)
        editText.setText(prefs.getInt("contest_id", 0).toString())


        //monitor alpha
        view.findViewById<Button>(R.id.button_watcher).setOnClickListener { button -> button as Button

            val manager = activity.accountsFragment.codeforcesAccountManager
            val handle = manager.savedInfo.userID
            val contestID = editText.text.toString().toInt()
            activity.startForegroundService(
                Intent(activity, CodeforcesContestWatchService::class.java)
                    .setAction("start")
                    .putExtra("handle", handle)
                    .putExtra("contestID", contestID)
            )

            with(prefs.edit()){
                putInt("contest_id", contestID)
                apply()
            }
        }

        view.findViewById<Button>(R.id.button_watcher_stop).setOnClickListener { button -> button as Button
            activity.startService(
                Intent(activity, CodeforcesContestWatchService::class.java)
                    .setAction("stop")
            )
        }

        //show running jobs
        view.findViewById<Button>(R.id.button_running_jobs).setOnClickListener {
            stuff.text = JobServicesCenter.getRunningJobServices(activity).joinToString(separator = "\n"){ info ->
                "Job " + info.id + ": " + info.service.shortClassName.removeSuffix("JobService").removePrefix(".job_services.")
            }
        }

        //test notify
        val r = RemoteViews(activity.packageName, R.layout.cf_watcher_notification_small)
        view.findViewById<Button>(R.id.button_test_notify).setOnClickListener{ button -> button as Button
            val n = NotificationCompat.Builder(activity, "test").apply {
                setSmallIcon(R.drawable.ic_develop)
                setNotificationSilent()
                setShowWhen(false)
                setStyle(NotificationCompat.DecoratedCustomViewStyle())

                r.setTextViewText(R.id.cf_watcher_notification_phase, "${System.currentTimeMillis()}")

                if(System.currentTimeMillis()/10%2==0L) {
                    r.setChronometer(R.id.cf_watcher_notification_progress, SystemClock.elapsedRealtime(), null, false)
                    r.setTextViewText(R.id.cf_watcher_notification_progress, "test")

                }
                else
                    r.setChronometer(R.id.cf_watcher_notification_progress, SystemClock.elapsedRealtime() + TimeUnit.HOURS.toMillis(2), null, true)

                setCustomContentView(r)
            }

            NotificationManagerCompat.from(activity).notify(Int.MAX_VALUE, n.build())
        }
    }
}
