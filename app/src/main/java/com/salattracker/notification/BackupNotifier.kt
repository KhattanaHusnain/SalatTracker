package com.salattracker.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.salattracker.R
import com.salattracker.domain.model.BackupAttention
import com.salattracker.feature.main.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val BACKUP_NOTIFICATION_ID = 7000

/** Posts at most one low-priority notification per backup "needs attention" episode. */
@Singleton
class BackupNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun notifyAttention(attention: BackupAttention) {
        if (attention == BackupAttention.NONE) return

        val (titleRes, bodyRes) = when (attention) {
            BackupAttention.SIGN_IN_REQUIRED -> R.string.backup_notif_sign_in_title to R.string.backup_notif_sign_in_body
            BackupAttention.REMOTE_CHANGED -> R.string.backup_notif_remote_changed_title to R.string.backup_notif_remote_changed_body
            BackupAttention.LOCAL_DATA_MISSING -> R.string.backup_notif_local_missing_title to R.string.backup_notif_local_missing_body
            BackupAttention.STORAGE_FULL -> R.string.backup_notif_storage_full_title to R.string.backup_notif_storage_full_body
            BackupAttention.NONE -> return
        }

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_OPEN_ROUTE, MainActivity.ROUTE_BACKUP)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, BACKUP_NOTIFICATION_ID, contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, context.getString(R.string.notification_channel_backup_id))
            .setSmallIcon(R.drawable.ic_cloud_upload)
            .setContentTitle(context.getString(titleRes))
            .setContentText(context.getString(bodyRes))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(BACKUP_NOTIFICATION_ID, notification)
    }
}
