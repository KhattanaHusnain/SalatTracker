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
import com.salattracker.domain.repository.VerseRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/** Fills in any curated prayer verses not yet cached, so they're available offline once fetched. */
@HiltWorker
class VerseCacheWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val verseRepository: VerseRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            verseRepository.refreshCache()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val ONE_TIME_WORK_NAME = "verse_cache_worker"
        private const val PERIODIC_WORK_NAME = "verse_cache_worker_periodic"

        fun enqueueOneTime(workManager: WorkManager) {
            val request = OneTimeWorkRequestBuilder<VerseCacheWorker>().build()
            workManager.enqueueUniqueWork(ONE_TIME_WORK_NAME, ExistingWorkPolicy.KEEP, request)
        }

        fun enqueuePeriodic(workManager: WorkManager) {
            val request = PeriodicWorkRequestBuilder<VerseCacheWorker>(24, TimeUnit.HOURS).build()
            workManager.enqueueUniquePeriodicWork(PERIODIC_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
