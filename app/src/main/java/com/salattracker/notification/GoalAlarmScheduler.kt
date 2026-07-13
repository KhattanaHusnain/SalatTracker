package com.salattracker.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.salattracker.domain.model.SpiritualGoal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** Schedules one exact daily-repeating alarm per active goal that has a reminder set. */
class GoalAlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(AlarmManager::class.java) as AlarmManager

    fun scheduleAll(goals: List<SpiritualGoal>) {
        goals.forEach { goal ->
            val reminderTime = goal.reminderTime
            if (!goal.reminderEnabled || reminderTime == null) {
                cancel(goal.id)
                return@forEach
            }

            val zoneId = ZoneId.systemDefault()
            val now = System.currentTimeMillis()
            val todayTrigger = LocalDateTime.of(LocalDate.now(), reminderTime).atZone(zoneId).toInstant().toEpochMilli()
            val triggerAt = if (todayTrigger > now) {
                todayTrigger
            } else {
                LocalDateTime.of(LocalDate.now().plusDays(1), reminderTime).atZone(zoneId).toInstant().toEpochMilli()
            }

            val pendingIntent = pendingIntentFor(goal.id, goal.name)
            try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            } catch (_: SecurityException) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
        }
    }

    /** Public so a goal's alarm can be cancelled immediately on deletion, not just on the next reschedule pass. */
    fun cancelFor(goalId: Long) = cancel(goalId)

    private fun cancel(goalId: Long) {
        alarmManager.cancel(pendingIntentFor(goalId, ""))
    }

    private fun pendingIntentFor(goalId: Long, goalName: String): PendingIntent {
        val intent = Intent(context, GoalReminderReceiver::class.java).apply {
            putExtra(GoalReminderReceiver.EXTRA_GOAL_ID, goalId)
            putExtra(GoalReminderReceiver.EXTRA_GOAL_NAME, goalName)
        }
        return PendingIntent.getBroadcast(
            context,
            GOAL_REQUEST_CODE_OFFSET + goalId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        private const val GOAL_REQUEST_CODE_OFFSET = 5000
    }
}
