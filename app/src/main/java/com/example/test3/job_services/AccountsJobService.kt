package com.example.test3.job_services

import android.app.NotificationManager
import android.os.Bundle
import androidx.core.app.NotificationCompat
import com.example.test3.NotificationChannels
import com.example.test3.NotificationIDs
import com.example.test3.R
import com.example.test3.account_manager.CodeforcesAccountManager
import com.example.test3.account_manager.STATUS
import com.example.test3.account_view.CodeforcesAccountPanel
import com.example.test3.makePendingIntentOpenURL
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class AccountsJobService : CoroutineJobService() {

    override suspend fun makeJobs(): ArrayList<Job> {
        val jobs = arrayListOf<Job>()
        if(CodeforcesAccountPanel.getDataStore(this).getObserveContribution()){
            jobs.add(launch { codeforcesContribution() })
        }
        if(jobs.isEmpty()) JobServicesCenter.stopJobService(this, JobServiceIDs.accounts_parsers)
        return jobs
    }

    private suspend fun codeforcesContribution() {
        val accountManager = CodeforcesAccountManager(this)
        val info = accountManager.getSavedInfo() as CodeforcesAccountManager.CodeforcesUserInfo
        if(info.status != STATUS.OK) return

        val handle = info.handle
        val contribution = (accountManager.loadInfo(handle) as CodeforcesAccountManager.CodeforcesUserInfo).let {
            if(it.status != STATUS.OK) return
            it.contribution
        }

        if(info.contribution != contribution){
            accountManager.setSavedInfo(info.copy(contribution = contribution))

            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

            val oldShowedContribution: Int = if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M) info.contribution
            else {
                notificationManager.activeNotifications.find {
                    it.id == NotificationIDs.codeforces_contribution_changes
                }?.notification?.extras?.getInt("contribution", info.contribution) ?: info.contribution
            }

            fun signed(x: Int): String = if(x>0) "+$x" else "$x"

            val n = NotificationCompat.Builder(this, NotificationChannels.codeforces_contribution_changes).apply {
                setSubText(handle)
                setContentTitle("Contribution change: ${signed(oldShowedContribution)} → ${signed(contribution)}")
                setSmallIcon(R.drawable.ic_person)
                setNotificationSilent()
                setAutoCancel(true)
                setShowWhen(false)
                setContentIntent(makePendingIntentOpenURL(info.link(), this@AccountsJobService))
                extras = Bundle().apply {
                    putInt("contribution", oldShowedContribution)
                }
            }
            notificationManager.notify( NotificationIDs.codeforces_contribution_changes, n.build())
        }
    }

}