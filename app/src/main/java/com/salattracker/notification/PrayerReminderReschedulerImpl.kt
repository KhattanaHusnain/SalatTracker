package com.salattracker.notification

import androidx.work.WorkManager
import com.salattracker.domain.repository.PrayerReminderRescheduler
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrayerReminderReschedulerImpl @Inject constructor(
    private val workManager: WorkManager,
) : PrayerReminderRescheduler {

    override fun rescheduleAll() = PrayerReminderWorker.enqueueOneTime(workManager)
}
