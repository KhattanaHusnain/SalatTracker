package com.salattracker.domain.model

enum class PrayerStatus {
    CONGREGATION,
    INDIVIDUAL,
    QAZA_PENDING,
    QAZA_COMPLETED,
    MENSTRUATION,
    NOT_RECORDED;

    fun label(): String = when (this) {
        CONGREGATION   -> "Congregation"
        INDIVIDUAL     -> "Individual"
        QAZA_PENDING   -> "Qaza Pending"
        QAZA_COMPLETED -> "Qaza Completed"
        MENSTRUATION   -> "Menstruation"
        NOT_RECORDED   -> "Not Recorded"
    }
}

enum class PrayerType {
    FAJR, DHUHR, ASR, MAGHRIB, ISHA;

    fun label(): String = name.lowercase().replaceFirstChar { it.uppercase() }
}

data class PrayerEntry(
    val id: Long = 0,
    val date: java.time.LocalDate,
    val prayerType: PrayerType,
    val status: PrayerStatus,
    val time: String = "",          // e.g. "05:24 AM" from AlAdhan cache
    val syncState: SyncState = SyncState.PENDING,
)

enum class SyncState { PENDING, SYNCED, FAILED }