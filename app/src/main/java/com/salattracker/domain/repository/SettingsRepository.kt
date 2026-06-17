package com.salattracker.domain.repository

import com.salattracker.domain.model.PrayerType
import kotlinx.coroutines.flow.Flow

/**
 * Domain-layer contract for user preferences.
 * Implemented in the data layer via DataStore.
 *
 * File type: Interface — no Android/DataStore imports here.
 * The implementation (DataStoreSettingsRepository) lives in
 * data/local/preferences/ and is bound via Hilt in a RepositoryModule.
 */
interface SettingsRepository {

    /**
     * Reactive stream of current user preferences.
     * Emits immediately with current values, then on every change.
     */
    fun observeSettings(): Flow<UserPrefs>

    /** Persist all settings in one atomic DataStore write. */
    suspend fun saveSettings(
        useDeviceLocation: Boolean,
        latitude: Double,
        longitude: Double,
        calculationMethodId: Int,
        reminderEnabled: Map<PrayerType, Boolean>,
        reminderOffsetMinutes: Map<PrayerType, Int>,
        use24HourFormat: Boolean,
    )

    /** One-shot read, used when prayer times need refreshing. */
    suspend fun getCurrentPrefs(): UserPrefs
}

/**
 * Snapshot of all user-configurable preferences.
 * Kept as a plain data class so it can be unit-tested without Android deps.
 */
data class UserPrefs(
    val useDeviceLocation: Boolean = true,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timezone: String = "UTC",
    val calculationMethodId: Int = 3,           // 3 = MWL (AlAdhan default)
    val reminderEnabled: Map<PrayerType, Boolean> = PrayerType.values()
        .associateWith { true },
    val reminderOffsetMinutes: Map<PrayerType, Int> = PrayerType.values()
        .associateWith { 0 },
    val use24HourFormat: Boolean = false,
    val lastSyncAt: Long = 0L,                  // epoch ms, used by SyncWorker
)