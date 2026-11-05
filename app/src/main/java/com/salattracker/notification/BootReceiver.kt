package com.salattracker.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.WorkManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.salattracker.widget.WidgetUpdateScheduler

/** Restores reminders after system events that can invalidate or shift their next occurrence. */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var workManager: WorkManager

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                PrayerReminderWorker.enqueueOneTime(workManager)
                GoalReminderWorker.enqueueOneTime(workManager)
                MosqueModeSyncWorker.enqueueOneTime(workManager)
                WidgetUpdateScheduler.enqueue(context)
            }
            Intent.ACTION_TIMEZONE_CHANGED -> {
                // Rebuild absolute alarm instants for the new zone even if location access is
                // unavailable in the background. The location worker refreshes an opted-in fix
                // when Android permits it, and schedules again if the city has changed.
                PrayerReminderWorker.enqueueOneTime(workManager)
                DeviceLocationRefreshWorker.enqueue(workManager)
                WidgetUpdateScheduler.enqueue(context)
            }
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_DATE_CHANGED -> WidgetUpdateScheduler.enqueue(context)
        }
    }
}
