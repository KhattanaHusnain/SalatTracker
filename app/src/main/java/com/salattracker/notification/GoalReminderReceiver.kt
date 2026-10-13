package com.salattracker.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.salattracker.R
import com.salattracker.domain.repository.GoalRepository
import com.salattracker.feature.main.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Handles both the original "goal reminder time arrived" alarm trigger (no action
 * string on the intent) and the notification's own action buttons.
 */
@AndroidEntryPoint
class GoalReminderReceiver : BroadcastReceiver() {

    @Inject lateinit var goalRepository: GoalRepository

    override fun onReceive(context: Context, intent: Intent) {
        val goalId = intent.getLongExtra(EXTRA_GOAL_ID, -1L)
        if (goalId == -1L) return
        val goalName = intent.getStringExtra(EXTRA_GOAL_NAME) ?: ""

        when (intent.action) {
            ACTION_CHECK_IN -> checkInAndDismiss(context, goalId)
            ACTION_SNOOZE   -> snooze(context, goalId, goalName)
            else            -> showNotification(context, goalId, goalName)
        }
    }

    private fun checkInAndDismiss(context: Context, goalId: Long) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                goalRepository.checkIn(goalId)
            } finally {
                NotificationManagerCompat.from(context).cancel(notificationIdFor(goalId))
                pendingResult.finish()
            }
        }
    }

    private fun snooze(context: Context, goalId: Long, goalName: String) {
        NotificationManagerCompat.from(context).cancel(notificationIdFor(goalId))
        scheduleSnooze(context, goalId, goalName)
    }

    private fun showNotification(context: Context, goalId: Long, goalName: String) {
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_OPEN_ROUTE, MainActivity.ROUTE_GOALS)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context, notificationIdFor(goalId), contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, context.getString(R.string.notification_channel_goal_id))
            .setSmallIcon(R.drawable.ic_auto_awesome)
            .setContentTitle(context.getString(R.string.notification_goal_title, goalName))
            .setContentText(context.getString(R.string.notification_goal_body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(action(context, goalId, goalName, ACTION_CHECK_IN, R.drawable.ic_add, "Check-in"))
            .addAction(action(context, goalId, goalName, ACTION_SNOOZE, R.drawable.ic_schedule, "Snooze 10m"))
            .build()

        NotificationManagerCompat.from(context).notify(notificationIdFor(goalId), notification)
    }

    private fun action(
        context: Context,
        goalId: Long,
        goalName: String,
        actionName: String,
        iconRes: Int,
        label: String,
    ): NotificationCompat.Action {
        val intent = Intent(context, GoalReminderReceiver::class.java).apply {
            action = actionName
            putExtra(EXTRA_GOAL_ID, goalId)
            putExtra(EXTRA_GOAL_NAME, goalName)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            actionName.hashCode() xor goalId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Action.Builder(iconRes, label, pendingIntent).build()
    }

    companion object {
        const val ACTION_CHECK_IN = "com.salattracker.action.GOAL_CHECK_IN"
        const val ACTION_SNOOZE   = "com.salattracker.action.GOAL_SNOOZE"
        const val EXTRA_GOAL_ID   = "goal_id"
        const val EXTRA_GOAL_NAME = "goal_name"

        private const val SNOOZE_MINUTES = 10L
        private const val SNOOZE_REQUEST_CODE_OFFSET = 6000

        fun notificationIdFor(goalId: Long): Int = (6000 + goalId).toInt()

        /** Shared by the notification's Snooze action and the reminder scheduling worker. */
        fun scheduleSnooze(context: Context, goalId: Long, goalName: String) {
            val alarmManager = context.getSystemService(AlarmManager::class.java)
            val triggerAt = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(SNOOZE_MINUTES)
            val intent = Intent(context, GoalReminderReceiver::class.java).apply {
                putExtra(EXTRA_GOAL_ID, goalId)
                putExtra(EXTRA_GOAL_NAME, goalName)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                SNOOZE_REQUEST_CODE_OFFSET + goalId.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            } catch (_: SecurityException) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
        }
    }
}
