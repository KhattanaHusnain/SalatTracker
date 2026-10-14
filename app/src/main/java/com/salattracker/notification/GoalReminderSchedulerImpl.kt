package com.salattracker.notification

import android.content.Context
import androidx.work.WorkManager
import com.salattracker.domain.repository.GoalReminderScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoalReminderSchedulerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val workManager: WorkManager,
) : GoalReminderScheduler {

    override fun cancel(goalId: Long) = GoalAlarmScheduler(context).cancelFor(goalId)

    override fun rescheduleAll() = GoalReminderWorker.enqueueOneTime(workManager)
}
