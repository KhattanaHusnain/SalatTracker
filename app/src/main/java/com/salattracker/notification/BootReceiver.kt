package com.salattracker.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.WorkManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** AlarmManager alarms don't survive a reboot, so re-schedule them once the device is back up. */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var workManager: WorkManager

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            PrayerReminderWorker.enqueueOneTime(workManager)
            GoalReminderWorker.enqueueOneTime(workManager)
        }
    }
}
