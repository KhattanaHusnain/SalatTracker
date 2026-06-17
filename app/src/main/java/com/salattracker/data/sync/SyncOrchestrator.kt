package com.salattracker.data.sync

import androidx.work.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central coordinator for scheduling WorkManager sync jobs.
 * Called from [PrayerRepositoryImpl] after any local write,
 * and from [LoginViewModel] on successful sign-in.
 */
@Singleton
class SyncOrchestrator @Inject constructor(
    private val workManager: WorkManager,
) {
    companion object {
        const val PERIODIC_SYNC_TAG = "periodic_prayer_sync"
        const val ONE_TIME_SYNC_TAG = "onetime_prayer_sync"
    }

    /** Schedule a one-time sync that runs as soon as there's a network connection. */
    fun triggerOneTimeSync() {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(networkConstraints())
            .addTag(ONE_TIME_SYNC_TAG)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                java.util.concurrent.TimeUnit.MILLISECONDS,
            )
            .build()

        workManager.enqueueUniqueWork(
            ONE_TIME_SYNC_TAG,
            ExistingWorkPolicy.KEEP,   // don't replace an already-queued sync
            request,
        )
    }

    /** Schedule a repeating background sync every 15 minutes (WorkManager minimum). */
    fun schedulePeriodicSync() {
        val request = PeriodicWorkRequestBuilder<SyncWorker>(
            15, java.util.concurrent.TimeUnit.MINUTES,
        )
            .setConstraints(networkConstraints())
            .addTag(PERIODIC_SYNC_TAG)
            .build()

        workManager.enqueueUniquePeriodicWork(
            PERIODIC_SYNC_TAG,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    /** Cancel all pending syncs (e.g. on sign-out). */
    fun cancelAll() {
        workManager.cancelAllWorkByTag(ONE_TIME_SYNC_TAG)
        workManager.cancelAllWorkByTag(PERIODIC_SYNC_TAG)
    }

    private fun networkConstraints() = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()
}