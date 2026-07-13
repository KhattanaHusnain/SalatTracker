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
import com.salattracker.domain.repository.PrayerRepository
import com.salattracker.domain.repository.SettingsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/**
 * (Re)schedules the exact alarms for every enabled prayer reminder. Runs once
 * immediately whenever settings/prayer times might have changed, and on a 24h
 * periodic schedule so reminders keep working even if the app is never reopened.
 */
@HiltWorker
class PrayerReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val prayerRepository: PrayerRepository,
    private val settingsRepository: SettingsRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val prefs = settingsRepository.getCurrentPrefs()
            val today = LocalDate.now()
            val todayTimes = prayerRepository.getCachedPrayerTimes(today).orEmpty()
            val tomorrowTimes = prayerRepository.getCachedPrayerTimes(today.plusDays(1)).orEmpty()

            AlarmScheduler(applicationContext).schedule(prefs, todayTimes, tomorrowTimes)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val ONE_TIME_WORK_NAME = "prayer_reminder_worker"
        private const val PERIODIC_WORK_NAME = "prayer_reminder_worker_periodic"

        fun enqueueOneTime(workManager: WorkManager) {
            val request = OneTimeWorkRequestBuilder<PrayerReminderWorker>().build()
            workManager.enqueueUniqueWork(ONE_TIME_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
        }

        fun enqueuePeriodic(workManager: WorkManager) {
            val request = PeriodicWorkRequestBuilder<PrayerReminderWorker>(24, TimeUnit.HOURS).build()
            workManager.enqueueUniquePeriodicWork(PERIODIC_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
