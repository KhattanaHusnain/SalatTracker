package com.salattracker.notification

import androidx.work.WorkManager
import com.salattracker.domain.repository.MosqueModeSyncScheduler
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MosqueModeSyncSchedulerImpl @Inject constructor(
    private val workManager: WorkManager,
) : MosqueModeSyncScheduler {
    override fun requestSync() = MosqueModeSyncWorker.enqueueOneTime(workManager)
}
