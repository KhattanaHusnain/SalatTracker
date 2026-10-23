package com.salattracker.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.salattracker.domain.model.Gender
import com.salattracker.domain.model.Madhab
import com.salattracker.domain.model.PrayerType
import com.salattracker.domain.repository.UserPrefs
import java.time.DayOfWeek
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
            // Ja'fari combines Dhuhr+Asr and Maghrib+Isha at the same calculated instant.
            // Keep the first prayer of each pair as the single reminder; otherwise both alarms
            // fire together and the second full-screen intent replaces the first.
            if (prefs.madhab == Madhab.JAFARI && type in setOf(PrayerType.ASR, PrayerType.ISHA)) {
                cancel(type)
                return@forEach
            }
            if (prefs.reminderEnabled[type] != true) {
                cancel(type)
                return@forEach
            }
            scheduleOne(type, todayTimes[type], tomorrowTimes[type], prefs.reminderOffsetMinutes[type] ?: 0)
        }

        // Women aren't obliged to attend Jumu'ah — suppressed regardless of the stored toggle,
        // which may be stale from before gender was set (or simply never touched).
        val leadMinutes = if (prefs.jumuahEnabled && prefs.gender != Gender.FEMALE) prefs.jumuahReminderLeadMinutes else 0
        scheduleJumuahLead(leadMinutes, todayTimes[PrayerType.DHUHR], tomorrowTimes[PrayerType.DHUHR])
    }

    /** Separate from [scheduleOne]: it isn't tied to a single [PrayerType] alarm slot, and only
     * fires on the next Friday Dhuhr, [leadMinutes] early. [leadMinutes] <= 0 means "off". */
    private fun scheduleJumuahLead(leadMinutes: Int, todayDhuhr: LocalTime?, tomorrowDhuhr: LocalTime?) {
        if (leadMinutes <= 0) {
            cancelJumuahLead()
            return
        }
        val zoneId = ZoneId.systemDefault()
        val now = System.currentTimeMillis()
        val today = LocalDate.now()

        val todayTrigger = if (today.dayOfWeek == DayOfWeek.FRIDAY) {
            todayDhuhr?.let { triggerMillis(it, -leadMinutes, today, zoneId) }
        } else null

        val triggerAt = if (todayTrigger != null && todayTrigger > now) {
            todayTrigger
        } else {
            val tomorrow = today.plusDays(1)
            if (tomorrow.dayOfWeek == DayOfWeek.FRIDAY) {
                tomorrowDhuhr?.let { triggerMillis(it, -leadMinutes, tomorrow, zoneId) }
            } else null
        }

        if (triggerAt == null) {
            cancelJumuahLead()
            return
        }

        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, jumuahLeadPendingIntent())
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, jumuahLeadPendingIntent())
        }
    }

    private fun cancelJumuahLead() {
        alarmManager.cancel(jumuahLeadPendingIntent())
    }

    private fun jumuahLeadPendingIntent(): PendingIntent {
        val intent = Intent(context, PrayerReminderReceiver::class.java).apply {
            putExtra(EXTRA_PRAYER_TYPE, PrayerType.DHUHR.name)
            putExtra(EXTRA_JUMUAH_LEAD, true)
        }
        return PendingIntent.getBroadcast(
            context,
            JUMUAH_LEAD_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
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
        val scheduledDate = if (todayTrigger != null && todayTrigger > now) {
            LocalDate.now()
        } else {
            if (tomorrowTime == null) return
            LocalDate.now().plusDays(1)
        }

        val scheduledTime = if (scheduledDate == LocalDate.now()) todayTime else tomorrowTime
        val triggerAt = triggerMillis(scheduledTime ?: return, offsetMinutes, scheduledDate, zoneId)
        val pendingIntent = pendingIntentFor(type, scheduledDate)
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
        cancelJumuahLead()
    }

    private fun cancel(type: PrayerType) {
        alarmManager.cancel(pendingIntentFor(type))
    }

    private fun pendingIntentFor(type: PrayerType, date: LocalDate = LocalDate.now()): PendingIntent {
        val intent = Intent(context, PrayerReminderReceiver::class.java).apply {
            putExtra(EXTRA_PRAYER_TYPE, type.name)
            putExtra(EXTRA_PRAYER_DATE, date.toString())
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
        const val EXTRA_PRAYER_DATE = "prayer_date"
        const val EXTRA_JUMUAH_LEAD = "jumuah_lead"
        private const val JUMUAH_LEAD_REQUEST_CODE = 100
    }
}
