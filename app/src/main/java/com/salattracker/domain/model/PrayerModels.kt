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
)

// ── Streak ────────────────────────────────────────────────────────────────────

data class StreakInfo(
    val currentStreak: Int,
    val highestStreak: Int,
)