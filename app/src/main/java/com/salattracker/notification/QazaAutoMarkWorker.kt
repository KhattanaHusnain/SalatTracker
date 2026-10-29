package com.salattracker.notification

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.salattracker.domain.model.PrayerStatus
import com.salattracker.domain.model.QazaActivitySource
import com.salattracker.domain.model.PrayerType
import com.salattracker.domain.repository.PrayerRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/**
 * Every [CHECK_INTERVAL_MINUTES] (WorkManager's minimum periodic interval), scans
 * today's prayers and every fully elapsed date since its last successful run, then auto-marks
 * any still-NOT_RECORDED prayer as QAZA_PENDING after its valid prayer window has ended (plus a
 * short logging buffer). This makes missed days visible after a device has been offline or
 * WorkManager has been delayed, without calling an on-time prayer missed.
 */
@HiltWorker
class QazaAutoMarkWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val prayerRepository: PrayerRepository,
    private val stateStore: QazaAutoMarkStateStore,
    private val clock: Clock,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val today = LocalDate.now(clock)
            for (date in overdueDatesToCatchUp(stateStore.lastProcessedDate(), today)) {
                markOverdue(date)
            }
            markOverdue(today)
            stateStore.setLastProcessedDate(today.minusDays(1))
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private suspend fun markOverdue(date: LocalDate) {
        val times = prayerRepository.getPrayerTimes(date) ?: return
        val entries = prayerRepository.getPrayerEntriesForDate(date)
        val now = LocalDateTime.now(clock)
        val nextDayTimes = prayerRepository.getPrayerTimes(date.plusDays(1))
        val sunrise = prayerRepository.getSunriseTime(date)

        PrayerType.values().forEach { type ->
            val deadline = autoMarkDeadline(type, date, times, sunrise, nextDayTimes)
                ?.plusMinutes(LOGGING_BUFFER_MINUTES)
                ?: return@forEach
            if (now.isBefore(deadline)) return@forEach

            val status = entries.find { it.prayerType == type }?.status ?: PrayerStatus.NOT_RECORDED
            if (status == PrayerStatus.NOT_RECORDED) {
                prayerRepository.upsertPrayerStatus(
                    date, type, PrayerStatus.QAZA_PENDING,
                    source = QazaActivitySource.AUTOMATIC,
                )
            }
        }
    }

    companion object {
        private const val LOGGING_BUFFER_MINUTES = 15L
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

/** Fully elapsed dates missed since [lastProcessedDate]. A fresh install keeps the prior
 * yesterday-only behaviour; subsequent runs fill every gap after an outage. */
internal fun overdueDatesToCatchUp(lastProcessedDate: LocalDate?, today: LocalDate): List<LocalDate> {
    val latestElapsedDate = today.minusDays(1)
    val firstDate = lastProcessedDate?.plusDays(1) ?: latestElapsedDate
    if (firstDate.isAfter(latestElapsedDate)) return emptyList()
    return generateSequence(firstDate) { date -> date.plusDays(1).takeIf { !it.isAfter(latestElapsedDate) } }.toList()
}

/** The first moment a prayer can be considered missed, before the small logging buffer. */
internal fun autoMarkDeadline(
    type: PrayerType,
    date: LocalDate,
    times: Map<PrayerType, java.time.LocalTime>,
    sunrise: java.time.LocalTime?,
    nextDayTimes: Map<PrayerType, java.time.LocalTime>?,
): LocalDateTime? = when (type) {
    PrayerType.FAJR -> sunrise?.let { LocalDateTime.of(date, it) }
    PrayerType.DHUHR -> times[PrayerType.ASR]
        ?.takeIf { it > (times[PrayerType.DHUHR] ?: it) }
        ?.let { LocalDateTime.of(date, it) }
        // Ja'fari combines Dhuhr and Asr at the same time; their shared window lasts to Maghrib.
        ?: times[PrayerType.MAGHRIB]?.let { LocalDateTime.of(date, it) }
    PrayerType.ASR -> times[PrayerType.MAGHRIB]?.let { LocalDateTime.of(date, it) }
    PrayerType.MAGHRIB -> times[PrayerType.ISHA]
        ?.takeIf { it > (times[PrayerType.MAGHRIB] ?: it) }
        ?.let { LocalDateTime.of(date, it) }
        // Ja'fari combines Maghrib and Isha; their shared window lasts to the next Fajr.
        ?: nextDayTimes?.get(PrayerType.FAJR)?.let { LocalDateTime.of(date.plusDays(1), it) }
    PrayerType.ISHA -> nextDayTimes?.get(PrayerType.FAJR)?.let { LocalDateTime.of(date.plusDays(1), it) }
}
