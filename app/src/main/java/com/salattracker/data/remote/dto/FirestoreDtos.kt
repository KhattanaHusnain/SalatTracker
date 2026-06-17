package com.salattracker.data.remote.dto

import com.salattracker.data.local.entity.PrayerEntryEntity
import com.salattracker.data.local.entity.SyncState
import com.salattracker.domain.model.PrayerStatus
import com.salattracker.domain.model.PrayerType
import java.time.LocalDate

// ── Firestore document structure ──────────────────────────────────────────────
//
// users/{userEmail}/
//   profile                      → UserProfileDto
//   prayers/{yyyy-MM-dd_PRAYER}  → PrayerEntryDto
//   streaks/summary              → StreakDto
//   qaza/balance                 → QazaBalanceDto
//
// Keyed by email (not UID) so data is human-readable and survives auth provider
// changes. UID is stored inside the profile document for reference.

data class UserProfileDto(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

data class PrayerEntryDto(
    val userId: String = "",           // email
    val date: String = "",             // "yyyy-MM-dd"
    val prayerType: String = "",       // PrayerType.name
    val status: String = "",           // PrayerStatus.name
    val updatedAt: Long = System.currentTimeMillis(),
) {
    /** Firestore document ID: "2024-01-22_FAJR" */
    val documentId: String get() = "${date}_${prayerType}"

    fun toEntity(syncState: SyncState = SyncState.SYNCED) = PrayerEntryEntity(
        userId     = userId,
        date       = LocalDate.parse(date),
        prayerType = PrayerType.valueOf(prayerType),
        status     = PrayerStatus.valueOf(status),
        updatedAt  = updatedAt,
        syncState  = syncState,
    )
}

fun PrayerEntryEntity.toDto() = PrayerEntryDto(
    userId     = userId,
    date       = date.toString(),
    prayerType = prayerType.name,
    status     = status.name,
    updatedAt  = updatedAt,
)

data class StreakDto(
    val currentStreak: Int = 0,
    val highestStreak: Int = 0,
    val updatedAt: Long = System.currentTimeMillis(),
)

data class QazaBalanceDto(
    val fajr: Int    = 0,
    val dhuhr: Int   = 0,
    val asr: Int     = 0,
    val maghrib: Int = 0,
    val isha: Int    = 0,
) {
    fun toMap(): Map<PrayerType, Int> = mapOf(
        PrayerType.FAJR    to fajr,
        PrayerType.DHUHR   to dhuhr,
        PrayerType.ASR     to asr,
        PrayerType.MAGHRIB to maghrib,
        PrayerType.ISHA    to isha,
    )
}