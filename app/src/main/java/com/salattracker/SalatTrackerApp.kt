package com.salattracker

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import com.salattracker.notification.BackupWorker
import com.salattracker.notification.GoalReminderWorker
import com.salattracker.notification.PrayerReminderWorker
import com.salattracker.notification.QazaAutoMarkWorker
import com.salattracker.notification.VerseCacheWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class SalatTrackerApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var workManager: WorkManager

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        PrayerReminderWorker.enqueueOneTime(workManager)
        PrayerReminderWorker.enqueuePeriodic(workManager)
        QazaAutoMarkWorker.enqueuePeriodic(workManager)
        VerseCacheWorker.enqueueOneTime(workManager)
        VerseCacheWorker.enqueuePeriodic(workManager)
        GoalReminderWorker.enqueueOneTime(workManager)
        GoalReminderWorker.enqueuePeriodic(workManager)
        BackupWorker.enqueuePeriodic(workManager)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            manager.createNotificationChannel(
                NotificationChannel(
                    getString(R.string.notification_channel_prayer_id),
                    getString(R.string.notification_channel_prayer_name),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description =
                        getString(R.string.notification_channel_prayer_desc)
                }
            )

            manager.createNotificationChannel(
                NotificationChannel(
                    getString(R.string.notification_channel_goal_id),
                    getString(R.string.notification_channel_goal_name),
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description =
                        getString(R.string.notification_channel_goal_desc)
                }
            )

            manager.createNotificationChannel(
                NotificationChannel(
                    getString(R.string.notification_channel_backup_id),
                    getString(R.string.notification_channel_backup_name),
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description =
                        getString(R.string.notification_channel_backup_desc)
                }
            )
        }
    }
}
