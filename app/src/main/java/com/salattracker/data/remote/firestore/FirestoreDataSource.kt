package com.salattracker.data.remote.firestore

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.salattracker.data.remote.dto.*
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * All Firestore reads/writes live here.
 * Schema is keyed by user email under users/{email}/.
 */
@Singleton
class FirestoreDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
) {
    // ── Path helpers ──────────────────────────────────────────────────────────

    private fun userDoc(email: String)    = firestore.collection("users").document(email)
    private fun prayersCol(email: String) = userDoc(email).collection("prayers")
    private fun prayerDoc(email: String, docId: String) = prayersCol(email).document(docId)
    private fun streakDoc(email: String)  = userDoc(email).collection("streaks").document("summary")
    private fun qazaDoc(email: String)    = userDoc(email).collection("qaza").document("balance")

    // ── Profile ───────────────────────────────────────────────────────────────

    suspend fun upsertProfile(email: String, dto: UserProfileDto) {
        userDoc(email).set(dto, SetOptions.merge()).await()
    }

    // ── Prayer entries ────────────────────────────────────────────────────────

    suspend fun upsertPrayerEntry(email: String, dto: PrayerEntryDto) {
        prayerDoc(email, dto.documentId)
            .set(dto, SetOptions.merge())
            .await()
    }

    suspend fun batchUpsertPrayerEntries(email: String, entries: List<PrayerEntryDto>) {
        if (entries.isEmpty()) return
        // Firestore batches max 500 ops; chunk to be safe
        entries.chunked(400).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { dto ->
                batch.set(prayerDoc(email, dto.documentId), dto, SetOptions.merge())
            }
            batch.commit().await()
        }
    }

    /**
     * Fetch all prayer entries updated after [sinceMs].
     * Used on login to pull changes made on other devices.
     */
    suspend fun fetchEntriesUpdatedSince(email: String, sinceMs: Long): List<PrayerEntryDto> {
        val snapshot = prayersCol(email)
            .whereGreaterThan("updatedAt", sinceMs)
            .get()
            .await()
        return snapshot.documents.mapNotNull { it.toObject(PrayerEntryDto::class.java) }
    }

    /** Fetch ALL prayer entries for a user (used on first login / fresh install). */
    suspend fun fetchAllEntries(email: String): List<PrayerEntryDto> {
        val snapshot = prayersCol(email).get().await()
        return snapshot.documents.mapNotNull { it.toObject(PrayerEntryDto::class.java) }
    }

    // ── Streak ────────────────────────────────────────────────────────────────

    suspend fun upsertStreak(email: String, dto: StreakDto) {
        streakDoc(email).set(dto, SetOptions.merge()).await()
    }

    suspend fun fetchStreak(email: String): StreakDto? =
        streakDoc(email).get().await().toObject(StreakDto::class.java)

    // ── Qaza balance ──────────────────────────────────────────────────────────

    suspend fun upsertQazaBalance(email: String, dto: QazaBalanceDto) {
        qazaDoc(email).set(dto, SetOptions.merge()).await()
    }

    suspend fun fetchQazaBalance(email: String): QazaBalanceDto? =
        qazaDoc(email).get().await().toObject(QazaBalanceDto::class.java)
}