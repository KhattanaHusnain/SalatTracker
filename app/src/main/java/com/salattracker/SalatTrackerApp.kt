package com.salattracker

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class.
 * - Annotated with @HiltAndroidApp to trigger Hilt's code generation.
 * - Creates notification channels on first run (required on Android 8+).
 *
 * File type: Application subclass — declare in AndroidManifest.xml as
 *   android:name=".SalatTrackerApp"
 */
@HiltAndroidApp
class SalatTrackerApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            // Prayer reminder channel
            manager.createNotificationChannel(
                NotificationChannel(
                    getString(R.string.notification_channel_prayer_id),
                    getString(R.string.notification_channel_prayer_name),
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = getString(R.string.notification_channel_prayer_desc)
                }
            )

            // Background sync channel
            manager.createNotificationChannel(
                NotificationChannel(
                    getString(R.string.notification_channel_sync_id),
                    getString(R.string.notification_channel_sync_name),
                    NotificationManager.IMPORTANCE_LOW,
                )
            )
        }
    }
}