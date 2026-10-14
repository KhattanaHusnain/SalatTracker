package com.salattracker.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.salattracker.domain.model.SpiritualGoal
import com.salattracker.domain.usecase.nextReminderAt
import java.time.LocalDateTime
import java.time.ZoneId

/** Schedules one exact alarm per goal, for its next due reminder (see [nextReminderAt]). */
class GoalAlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(AlarmManager::class.java) as AlarmManager

    fun scheduleAll(goals: List<SpiritualGoal>) {
        val zoneId = ZoneId.systemDefault()
        val now = LocalDateTime.now()
        goals.forEach { goal ->
            // Null when the reminder is off, the goal is archived, or it has no further due day
            // (wrong weekday all week, or past its end date) — nothing to schedule then.
            val triggerAt = nextReminderAt(goal, now)?.atZone(zoneId)?.toInstant()?.toEpochMilli()
            if (triggerAt == null) {
                cancel(goal.id)
                return@forEach
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
