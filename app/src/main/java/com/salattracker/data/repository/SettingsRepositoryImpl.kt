package com.salattracker.data.repository

import com.salattracker.data.local.preferences.DataStoreSettingsRepository
import com.salattracker.domain.model.PrayerType
import com.salattracker.domain.repository.SettingsRepository
import com.salattracker.domain.repository.UserPrefs
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin delegation wrapper that satisfies the domain [SettingsRepository] interface
 * while the actual logic lives in [DataStoreSettingsRepository].
 *
 * This indirection means ViewModels only depend on the domain interface,
 * keeping them testable without a real DataStore instance.
 */
@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStoreSettingsRepository,
) : SettingsRepository {

    override fun observeSettings(): Flow<UserPrefs> = dataStore.observeSettings()

    override suspend fun getCurrentPrefs(): UserPrefs = dataStore.getCurrentPrefs()

    override suspend fun saveSettings(
        useDeviceLocation: Boolean,
        latitude: Double,
        longitude: Double,
        calculationMethodId: Int,
        reminderEnabled: Map<PrayerType, Boolean>,
        reminderOffsetMinutes: Map<PrayerType, Int>,
        use24HourFormat: Boolean,
    ) = dataStore.saveSettings(
        useDeviceLocation     = useDeviceLocation,
        latitude              = latitude,
        longitude             = longitude,
        calculationMethodId   = calculationMethodId,
        reminderEnabled       = reminderEnabled,
        reminderOffsetMinutes = reminderOffsetMinutes,
        use24HourFormat       = use24HourFormat,
    )
}