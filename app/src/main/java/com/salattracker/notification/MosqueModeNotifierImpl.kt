package com.salattracker.notification

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.salattracker.R
import com.salattracker.domain.repository.MosqueModeNotifier
import com.salattracker.feature.main.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val NOTIFICATION_ID = 1
// A tag keeps this clear of the id-ranges used by prayer, goal and backup notifications.
private const val NOTIFICATION_TAG = "mosque_mode"
private const val CONTENT_REQUEST_CODE = 8101
private const val RESTORE_REQUEST_CODE = 8102

@Singleton
class MosqueModeNotifierImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : MosqueModeNotifier {

    @SuppressLint("MissingPermission")
    override fun showVibrateOn(mosqueName: String) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_OPEN_ROUTE, MainActivity.ROUTE_MOSQUE_MODE)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPending = PendingIntent.getActivity(
            context, CONTENT_REQUEST_CODE, contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val restoreIntent = Intent(context, MosqueModeReceiver::class.java)
            .setAction(MosqueModeReceiver.ACTION_RESTORE_SOUND)
        val restorePending = PendingIntent.getBroadcast(
            context, RESTORE_REQUEST_CODE, restoreIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, context.getString(R.string.notification_channel_mosque_id))
            .setSmallIcon(R.drawable.ic_mosque)
            .setContentTitle(context.getString(R.string.mosque_notif_title))
            .setContentText(context.getString(R.string.mosque_notif_body, mosqueName))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(contentPending)
            .addAction(0, context.getString(R.string.mosque_notif_restore), restorePending)
            .build()

        manager.notify(NOTIFICATION_TAG, NOTIFICATION_ID, notification)
    }

    override fun dismiss() {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_TAG, NOTIFICATION_ID)
    }
}
