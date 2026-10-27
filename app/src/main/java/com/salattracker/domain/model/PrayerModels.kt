package com.salattracker.domain.model

import java.time.LocalDate

// ── Prayer type ───────────────────────────────────────────────────────────────

enum class PrayerType {
    FAJR, DHUHR, ASR, MAGHRIB, ISHA;
}

// ── Prayer status ─────────────────────────────────────────────────────────────

enum class PrayerStatus {
    CONGREGATION,
    INDIVIDUAL,
    QAZA_PENDING,
    QAZA_COMPLETED,
    MENSTRUATION,
    NOT_RECORDED;
}

// ── Domain model (no Room / Firestore imports) ────────────────────────────────

data class PrayerEntry(
    val id: Long,
    val userId: String,
    val date: LocalDate,
    val prayerType: PrayerType,
    val status: PrayerStatus,
    val updatedAt: Long,
    val loggedLate: Boolean = false,
    val qazaReason: String? = null,
)

enum class QazaActivitySource { QAZA, HOME, CALENDAR, BULK_UPDATE, AUTOMATIC, ONBOARDING, REMINDER, OTHER }

data class QazaActivity(
    val id: Long,
    val prayerDate: LocalDate,
    val prayerType: PrayerType,
    val previousStatus: PrayerStatus?,
    val newStatus: PrayerStatus,
    val reasonTag: String? = null,
    val source: QazaActivitySource = QazaActivitySource.OTHER,
    val recordedAt: Long,
    val batchId: String? = null,
)

// ── Streak ────────────────────────────────────────────────────────────────────

data class StreakInfo(
    val currentStreak: Int,
    val highestStreak: Int,
)

// ── Verse ─────────────────────────────────────────────────────────────────────

data class VerseInfo(
    val text: String,
    val citation: String,   // e.g. "Al-Baqarah 2:238"
    val arabic: String = "",
)
