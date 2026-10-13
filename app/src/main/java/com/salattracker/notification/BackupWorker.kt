package com.salattracker.notification

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import com.salattracker.domain.model.BackupResult
import com.salattracker.domain.repository.BackupRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Runs once a day whenever the device is online — "each day, or whenever connected"
 * is exactly what a 24h periodic request with a CONNECTED constraint gives: WorkManager
 * runs it as soon as that constraint is next satisfied, which is immediately if the
 * device is already online, or on reconnecting if it wasn't. A no-op until an account
 * is connected (see [BackupResult.Disabled]).
 */
@HiltWorker
class BackupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val backupRepository: BackupRepository,
    private val backupNotifier: BackupNotifier,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            when (val result = backupRepository.backupNow()) {
                is BackupResult.Disabled, is BackupResult.UpToDate, is BackupResult.Uploaded -> Result.success()
                is BackupResult.Blocked -> {
                    notifyOnce(result)
                    Result.success()
                }
                is BackupResult.Retryable -> Result.retry()
                is BackupResult.Failed -> Result.retry()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private suspend fun notifyOnce(blocked: BackupResult.Blocked) {
        // observeStatus() is a Flow of live DataStore reads; backupNow() just persisted
        // attentionNotified, so a plain current-value read is what's needed here, not a
        // live collector — first() on a cold Flow does exactly that.
        val alreadyNotified = backupRepository.observeStatus().first().attentionNotified
        if (!alreadyNotified) {
            backupNotifier.notifyAttention(blocked.attention)
            backupRepository.markAttentionNotified()
        }
    }

    companion object {
        private const val PERIODIC_WORK_NAME = "backup_worker_periodic"

        fun enqueuePeriodic(workManager: WorkManager) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = PeriodicWorkRequestBuilder<BackupWorker>(24, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, WorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
                .build()
            // UPDATE (not KEEP): lets a future change to this worker's constraints/interval
            // actually take effect for installs that already have the old request queued.
            workManager.enqueueUniquePeriodicWork(PERIODIC_WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
        }
    }
}
