package com.salattracker.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.salattracker.R
import com.salattracker.core.ui.labelRes
import com.salattracker.domain.model.Gender
import com.salattracker.domain.model.Madhab
import com.salattracker.domain.model.PrayerStatus
import com.salattracker.domain.model.PrayerType
import com.salattracker.domain.repository.PrayerRepository
import com.salattracker.domain.repository.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Handles both the original "prayer time has arrived" alarm trigger (no action
 * string on the intent) and the notification's own action buttons — each button
 * re-targets this same receiver with a distinct action string.
 */
@AndroidEntryPoint
class PrayerReminderReceiver : BroadcastReceiver() {

    @Inject lateinit var prayerRepository: PrayerRepository
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var channels: PrayerReminderChannels

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.getBooleanExtra(AlarmScheduler.EXTRA_JUMUAH_LEAD, false)) {
            showJumuahLeadNotification(context)
            return
        }
        val typeName = intent.getStringExtra(AlarmScheduler.EXTRA_PRAYER_TYPE) ?: return
        val type = try {
            PrayerType.valueOf(typeName)
        } catch (e: IllegalArgumentException) {
            return
        }
        val date = intent.prayerDateOrToday()

        when (intent.action) {
            ACTION_MARK_CONGREGATION -> markAndDismiss(context, type, date, PrayerStatus.CONGREGATION)
            ACTION_MARK_INDIVIDUAL   -> markAndDismiss(context, type, date, PrayerStatus.INDIVIDUAL)
            ACTION_MARK_QAZA_PENDING -> markAndDismiss(context, type, date, PrayerStatus.QAZA_PENDING)
            ACTION_SNOOZE            -> snooze(context, type, date, intent.getIntExtra(EXTRA_SNOOZE_MINUTES, DEFAULT_SNOOZE_MINUTES))
            else                     -> showNotification(context, type, date)
        }
    }

    private fun markAndDismiss(context: Context, type: PrayerType, date: LocalDate, status: PrayerStatus) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                prayerRepository.upsertPrayerStatus(date, type, status)
            } finally {
                NotificationManagerCompat.from(context).cancel(type.ordinal)
                pendingResult.finish()
            }
        }
    }

    private fun snooze(context: Context, type: PrayerType, date: LocalDate, snoozeMinutes: Int) {
        NotificationManagerCompat.from(context).cancel(type.ordinal)
        scheduleSnooze(context, type, date, snoozeMinutes)
    }

    private fun showNotification(context: Context, type: PrayerType, date: LocalDate) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                val prefs = settingsRepository.getCurrentPrefs()
                val soundUri = prefs.reminderSoundUri[type]
                val vibrationEnabled = prefs.reminderVibrationEnabled[type] ?: true
                val fullScreenAlert = prefs.reminderFullScreenAlert[type] ?: true
                val snoozeMinutes = prefs.reminderSnoozeMinutes[type] ?: DEFAULT_SNOOZE_MINUTES
                // Women aren't obliged to attend Jumu'ah or pray in congregation — both suppressed
                // regardless of a possibly-stale stored toggle, matching AlarmScheduler/the UI.
                val isFemale = prefs.gender == Gender.FEMALE
                val label = context.getString(type.labelRes(date, prefs.jumuahEnabled && !isFemale, prefs.madhab))

                channels.ensure(type, soundUri, vibrationEnabled)
                val fullScreenPendingIntent = fullScreenPendingIntent(context, type, date)

                val builder = NotificationCompat.Builder(context, channels.channelId(type))
                    .setSmallIcon(R.drawable.ic_mosque)
                    .setContentTitle(context.getString(R.string.notification_prayer_title, label))
                    .setContentText(context.getString(R.string.notification_prayer_body, label))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setAutoCancel(true)
                    .setContentIntent(fullScreenPendingIntent)
                if (!isFemale) {
                    builder.addAction(action(context, type, date, ACTION_MARK_CONGREGATION, R.drawable.ic_groups, context.getString(R.string.status_congregation)))
                }
                builder
                    .addAction(action(context, type, date, ACTION_MARK_INDIVIDUAL, R.drawable.ic_person, context.getString(R.string.status_individual)))
                    .addAction(action(context, type, date, ACTION_MARK_QAZA_PENDING, R.drawable.ic_pending_actions, context.getString(R.string.status_qaza_pending)))
                    .addAction(action(context, type, date, ACTION_SNOOZE, R.drawable.ic_schedule, context.getString(R.string.reminder_snooze, snoozeMinutes), snoozeMinutes))
                // Android 14+ lets the user (or Play policy) revoke this capability. Requesting
                // it anyway is a no-op, so fall back to the normal high-priority notification.
                if (fullScreenAlert && canUseFullScreenIntent(context)) {
                    builder.setFullScreenIntent(fullScreenPendingIntent, true)
                }

                NotificationManagerCompat.from(context).notify(type.ordinal, builder.build())
            } finally {
                pendingResult.finish()
            }
        }
    }

    /** The optional early Jumu'ah heads-up — a plain, low-priority notification, distinct from
     * the actual Dhuhr/Jumu'ah alarm (no full-screen intent, no mark/snooze actions). */
    private fun showJumuahLeadNotification(context: Context) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                channels.ensure(PrayerType.DHUHR, null, true)
                val contentIntent = Intent(context, com.salattracker.feature.main.MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val pendingIntent = PendingIntent.getActivity(
                    context, JUMUAH_LEAD_NOTIFICATION_ID, contentIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                val builder = NotificationCompat.Builder(context, channels.channelId(PrayerType.DHUHR))
                    .setSmallIcon(R.drawable.ic_mosque)
                    .setContentTitle(context.getString(R.string.jumuah_lead_notif_title))
                    .setContentText(context.getString(R.string.jumuah_lead_notif_body))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                NotificationManagerCompat.from(context).notify(JUMUAH_LEAD_NOTIFICATION_ID, builder.build())
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun fullScreenPendingIntent(context: Context, type: PrayerType, date: LocalDate): PendingIntent {
        val intent = Intent(context, PrayerReminderActivity::class.java).apply {
            putExtra(AlarmScheduler.EXTRA_PRAYER_TYPE, type.name)
            putExtra(AlarmScheduler.EXTRA_PRAYER_DATE, date.toString())
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            FULLSCREEN_REQUEST_CODE_OFFSET + type.ordinal,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun action(
        context: Context,
        type: PrayerType,
        date: LocalDate,
        actionName: String,
        iconRes: Int,
        label: String,
        snoozeMinutes: Int? = null,
    ): NotificationCompat.Action {
        val intent = Intent(context, PrayerReminderReceiver::class.java).apply {
            action = actionName
            putExtra(AlarmScheduler.EXTRA_PRAYER_TYPE, type.name)
            putExtra(AlarmScheduler.EXTRA_PRAYER_DATE, date.toString())
            if (snoozeMinutes != null) putExtra(EXTRA_SNOOZE_MINUTES, snoozeMinutes)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            actionName.hashCode() xor type.ordinal,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Action.Builder(iconRes, label, pendingIntent).build()
    }

    companion object {
        const val ACTION_MARK_CONGREGATION = "com.salattracker.action.MARK_CONGREGATION"
        const val ACTION_MARK_INDIVIDUAL   = "com.salattracker.action.MARK_INDIVIDUAL"
        const val ACTION_MARK_QAZA_PENDING = "com.salattracker.action.MARK_QAZA_PENDING"
        const val ACTION_SNOOZE            = "com.salattracker.action.SNOOZE"
        const val EXTRA_SNOOZE_MINUTES     = "snooze_minutes"
        const val DEFAULT_SNOOZE_MINUTES   = 10

        private const val SNOOZE_REQUEST_CODE_OFFSET = 1000
        private const val FULLSCREEN_REQUEST_CODE_OFFSET = 2000
        private const val JUMUAH_LEAD_NOTIFICATION_ID = 3000

        /** Shared by the notification's Snooze action and the full-screen reminder Activity. */
        fun scheduleSnooze(context: Context, type: PrayerType, date: LocalDate, snoozeMinutes: Int) {
            val alarmManager = context.getSystemService(AlarmManager::class.java)
            val triggerAt = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(snoozeMinutes.toLong())
            val intent = Intent(context, PrayerReminderReceiver::class.java).apply {
                putExtra(AlarmScheduler.EXTRA_PRAYER_TYPE, type.name)
                putExtra(AlarmScheduler.EXTRA_PRAYER_DATE, date.toString())
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                SNOOZE_REQUEST_CODE_OFFSET + type.ordinal,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            } catch (_: SecurityException) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
        }

        /** Cancels a deferred snooze when the prayer is recorded elsewhere in the app. */
        fun cancelSnooze(context: Context, type: PrayerType) {
            val intent = Intent(context, PrayerReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                SNOOZE_REQUEST_CODE_OFFSET + type.ordinal,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
            ) ?: return
            context.getSystemService(AlarmManager::class.java).cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}

internal fun Intent.prayerDateOrToday(): LocalDate =
    parsePrayerDate(getStringExtra(AlarmScheduler.EXTRA_PRAYER_DATE), LocalDate.now())

internal fun parsePrayerDate(value: String?, fallback: LocalDate): LocalDate =
    value?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: fallback

internal fun canUseFullScreenIntent(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
        context.getSystemService(android.app.NotificationManager::class.java).canUseFullScreenIntent()

