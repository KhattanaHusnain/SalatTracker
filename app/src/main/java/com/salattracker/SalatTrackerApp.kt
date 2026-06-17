package com.salattracker

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class SalatTrackerApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
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
                    getString(R.string.notification_channel_sync_id),
                    getString(R.string.notification_channel_sync_name),
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }
    }
}