package com.salattracker.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.salattracker.domain.model.PrayerType
import com.salattracker.domain.repository.UserPrefs
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Schedules one exact alarm per prayer type via [AlarmManager]. [tomorrowTimes] is
 * used as a fallback whenever today's reminder instant has already passed, so a
 * reminder is always queued for the next occurrence instead of silently dropped
 * until something happens to reschedule it (app open, periodic worker tick, etc).
 */
class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(
        prefs: UserPrefs,
        todayTimes: Map<PrayerType, LocalTime>,
        tomorrowTimes: Map<PrayerType, LocalTime>,
    ) {
        PrayerType.values().forEach { type ->
            if (prefs.reminderEnabled[type] != true) {
                cancel(type)
                return@forEach
            }
            scheduleOne(type, todayTimes[type], tomorrowTimes[type], prefs.reminderOffsetMinutes[type] ?: 0)
        }
    }

    private fun scheduleOne(
        type: PrayerType,
        todayTime: LocalTime?,
        tomorrowTime: LocalTime?,
        offsetMinutes: Int,
    ) {
        val zoneId = ZoneId.systemDefault()
        val now = System.currentTimeMillis()

        val todayTrigger = todayTime?.let { triggerMillis(it, offsetMinutes, LocalDate.now(), zoneId) }
        val triggerAt = if (todayTrigger != null && todayTrigger > now) {
            todayTrigger
        } else {
            tomorrowTime?.let { triggerMillis(it, offsetMinutes, LocalDate.now().plusDays(1), zoneId) }
                ?: return
        }

        val pendingIntent = pendingIntentFor(type)
        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        } catch (_: SecurityException) {
            // No "schedule exact alarms" grant on this device (Android 12+) — fall
            // back to an inexact alarm rather than dropping the reminder entirely.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }
    }

    private fun triggerMillis(time: LocalTime, offsetMinutes: Int, date: LocalDate, zoneId: ZoneId): Long =
        time.plusMinutes(offsetMinutes.toLong())
            .atDate(date)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()

    fun cancelAll() {
        PrayerType.values().forEach { cancel(it) }
    }

    private fun cancel(type: PrayerType) {
        alarmManager.cancel(pendingIntentFor(type))
    }

    private fun pendingIntentFor(type: PrayerType): PendingIntent {
        val intent = Intent(context, PrayerReminderReceiver::class.java).apply {
            putExtra(EXTRA_PRAYER_TYPE, type.name)
        }
        return PendingIntent.getBroadcast(
            context,
            type.ordinal,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        const val EXTRA_PRAYER_TYPE = "prayer_type"
    }
}
