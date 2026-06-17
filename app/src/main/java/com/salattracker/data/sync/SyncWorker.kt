package com.salattracker.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.salattracker.data.local.dao.SyncQueueDao
import com.salattracker.data.local.preferences.DataStoreSettingsRepository
import com.salattracker.data.remote.dto.PrayerEntryDto
import com.salattracker.data.remote.firestore.FirestoreDataSource
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Background sync worker.
 *
 * Flow:
 *   1. Fetch up to 50 pending ops from sync_queue table
 *   2. Batch-write them to Firestore under users/{email}/
 *   3. On success → delete processed items from queue
 *   4. On failure → increment retry_count; WorkManager handles exponential back-off
 *   5. Purge items that have exceeded MAX_RETRIES
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncQueueDao: SyncQueueDao,
    private val firestoreDs: FirestoreDataSource,
    private val settingsRepo: DataStoreSettingsRepository,
    private val auth: FirebaseAuth,
    private val gson: Gson,
) : CoroutineWorker(context, params) {

    companion object {
        private const val MAX_RETRIES    = 5
        private const val BATCH_SIZE     = 50
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val email = auth.currentUser?.email ?: return@withContext Result.failure()

        try {
            val batch = syncQueueDao.fetchBatch(BATCH_SIZE)
            if (batch.isEmpty()) return@withContext Result.success()

            // Group by operation type
            val upserts = batch.filter { it.opType == "UPSERT" }

            if (upserts.isNotEmpty()) {
                val dtos = upserts.map { item ->
                    gson.fromJson(item.payloadJson, PrayerEntryDto::class.java)
                }
                firestoreDs.batchUpsertPrayerEntries(email, dtos)
                syncQueueDao.deleteProcessed(upserts.map { it.id })
            }

            // Update last sync timestamp
            settingsRepo.updateLastSyncAt(System.currentTimeMillis())

            // Clean up exhausted items
            syncQueueDao.purgeExhausted(MAX_RETRIES)

            Result.success()
        } catch (e: Exception) {
            // Increment retry counts for items still in queue
            val remaining = syncQueueDao.fetchBatch(BATCH_SIZE)
            if (remaining.isNotEmpty()) {
                syncQueueDao.incrementRetry(remaining.map { it.id })
            }

            // Retry with exponential back-off if not exhausted
            if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
        }
    }
}