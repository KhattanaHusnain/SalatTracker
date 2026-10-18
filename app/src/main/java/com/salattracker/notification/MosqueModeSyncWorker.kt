package com.salattracker.notification

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.salattracker.domain.model.GeofenceSync
import com.salattracker.domain.usecase.MosqueModeController
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * Re-registers every saved mosque's geofence. Geofences are lost on reboot and when location is
 * switched off, so this runs at boot, on app launch, after a backup restore, and daily.
 */
@HiltWorker
class MosqueModeSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val controller: MosqueModeController,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val result = controller.sync()
        Log.i(TAG, "sync: $result")
        return if (result == GeofenceSync.FAILED) Result.retry() else Result.success()
    }

    companion object {
        private const val TAG = "MosqueMode"
        private const val ONE_TIME_WORK_NAME = "mosque_mode_sync"
        private const val PERIODIC_WORK_NAME = "mosque_mode_sync_periodic"

        fun enqueueOneTime(workManager: WorkManager) {
            val request = OneTimeWorkRequestBuilder<MosqueModeSyncWorker>().build()
            workManager.enqueueUniqueWork(ONE_TIME_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
        }

        fun enqueuePeriodic(workManager: WorkManager) {
            val request = PeriodicWorkRequestBuilder<MosqueModeSyncWorker>(24, TimeUnit.HOURS).build()
            workManager.enqueueUniquePeriodicWork(PERIODIC_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
