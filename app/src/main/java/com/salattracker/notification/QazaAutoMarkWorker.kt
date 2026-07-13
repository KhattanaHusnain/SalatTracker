package com.salattracker.notification

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.salattracker.domain.model.PrayerStatus
import com.salattracker.domain.model.PrayerType
import com.salattracker.domain.repository.PrayerRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/**
 * Every [CHECK_INTERVAL_MINUTES] (WorkManager's minimum periodic interval), scans
 * today's and yesterday's prayers and auto-marks any prayer still NOT_RECORDED as
 * QAZA_PENDING once [GRACE_PERIOD_MINUTES] have passed since its time — so a missed
 * prayer lands in the Qaza tracker on its own instead of staying silently blank.
 * Yesterday is included too, as a safety net for whatever the last tick before
 * midnight didn't get to (device asleep, Doze, etc).
 */
@HiltWorker
class QazaAutoMarkWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val prayerRepository: PrayerRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val today = LocalDate.now()
            markOverdue(today.minusDays(1))
            markOverdue(today)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private suspend fun markOverdue(date: LocalDate) {
        val times = prayerRepository.getCachedPrayerTimes(date) ?: return
        val entries = prayerRepository.getPrayerEntriesForDate(date)
        val now = LocalDateTime.now()

        PrayerType.values().forEach { type ->
            val prayerTime = times[type] ?: return@forEach
            val deadline = LocalDateTime.of(date, prayerTime).plusMinutes(GRACE_PERIOD_MINUTES)
            if (now.isBefore(deadline)) return@forEach

            val status = entries.find { it.prayerType == type }?.status ?: PrayerStatus.NOT_RECORDED
            if (status == PrayerStatus.NOT_RECORDED) {
                prayerRepository.upsertPrayerStatus(date, type, PrayerStatus.QAZA_PENDING)
            }
        }
    }

    companion object {
        private const val GRACE_PERIOD_MINUTES = 30L
        private const val CHECK_INTERVAL_MINUTES = 15L
        private const val PERIODIC_WORK_NAME = "qaza_auto_mark_worker_periodic"

        fun enqueuePeriodic(workManager: WorkManager) {
            val request = PeriodicWorkRequestBuilder<QazaAutoMarkWorker>(
                CHECK_INTERVAL_MINUTES, TimeUnit.MINUTES
            ).build()
            workManager.enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
