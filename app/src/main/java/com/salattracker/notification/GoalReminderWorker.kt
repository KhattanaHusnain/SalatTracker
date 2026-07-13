package com.salattracker.notification

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.salattracker.domain.repository.GoalRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * (Re)schedules the exact alarms for every active goal's reminder. Runs once
 * immediately whenever a goal is created (new input invalidates any previously
 * queued run), and on a 24h periodic schedule so reminders keep working even if
 * the app is never reopened.
 */
@HiltWorker
class GoalReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val goalRepository: GoalRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val goals = goalRepository.getAllActiveGoals()
            GoalAlarmScheduler(applicationContext).scheduleAll(goals)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val ONE_TIME_WORK_NAME = "goal_reminder_worker"
        private const val PERIODIC_WORK_NAME = "goal_reminder_worker_periodic"

        fun enqueueOneTime(workManager: WorkManager) {
            val request = OneTimeWorkRequestBuilder<GoalReminderWorker>().build()
            workManager.enqueueUniqueWork(ONE_TIME_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
        }

        fun enqueuePeriodic(workManager: WorkManager) {
            val request = PeriodicWorkRequestBuilder<GoalReminderWorker>(24, TimeUnit.HOURS).build()
            workManager.enqueueUniquePeriodicWork(PERIODIC_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
