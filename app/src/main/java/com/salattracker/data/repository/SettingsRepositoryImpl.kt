package com.salattracker.data.repository

import com.salattracker.data.local.preferences.DataStoreSettingsRepository
import com.salattracker.domain.model.AppLanguage
import com.salattracker.domain.model.HighLatitudeRule
import com.salattracker.domain.model.Madhab
import com.salattracker.domain.model.PrayerType
import com.salattracker.domain.model.ThemeMode
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
        language: AppLanguage,
        themeMode: ThemeMode,
        madhab: Madhab,
        highLatitudeRule: HighLatitudeRule,
        prayerTimeAdjustMinutes: Map<PrayerType, Int>,
        madhabChosen: Boolean,
        jumuahEnabled: Boolean,
        jumuahReminderLeadMinutes: Int,
    ) = dataStore.saveSettings(
        useDeviceLocation         = useDeviceLocation,
        latitude                  = latitude,
        longitude                 = longitude,
        calculationMethodId       = calculationMethodId,
        reminderEnabled           = reminderEnabled,
        reminderOffsetMinutes     = reminderOffsetMinutes,
        use24HourFormat           = use24HourFormat,
        language                  = language,
        themeMode                 = themeMode,
        madhab                    = madhab,
        highLatitudeRule          = highLatitudeRule,
        prayerTimeAdjustMinutes   = prayerTimeAdjustMinutes,
        madhabChosen              = madhabChosen,
        jumuahEnabled             = jumuahEnabled,
        jumuahReminderLeadMinutes = jumuahReminderLeadMinutes,
    )

    override suspend fun setUseDeviceLocation(enabled: Boolean) = dataStore.setUseDeviceLocation(enabled)

    override suspend fun setLocation(latitude: Double, longitude: Double) = dataStore.setLocation(latitude, longitude)

    override suspend fun setCalculationMethodId(id: Int) = dataStore.setCalculationMethodId(id)

    override suspend fun setUse24HourFormat(enabled: Boolean) = dataStore.setUse24HourFormat(enabled)

    override suspend fun setLanguage(language: AppLanguage) = dataStore.setLanguage(language)

    override suspend fun setThemeMode(themeMode: ThemeMode) = dataStore.setThemeMode(themeMode)

    override suspend fun setAllRemindersEnabled(enabled: Boolean) = dataStore.setAllRemindersEnabled(enabled)

    override suspend fun setOnboardingCompleted(completed: Boolean) = dataStore.setOnboardingCompleted(completed)

    override suspend fun setReminderEnabled(type: PrayerType, enabled: Boolean) =
        dataStore.setReminderEnabled(type, enabled)

    override suspend fun setReminderOffsetMinutes(type: PrayerType, minutes: Int) =
        dataStore.setReminderOffsetMinutes(type, minutes)

    override suspend fun setReminderSnoozeMinutes(type: PrayerType, minutes: Int) =
        dataStore.setReminderSnoozeMinutes(type, minutes)

    override suspend fun setReminderFullScreenAlert(type: PrayerType, enabled: Boolean) =
        dataStore.setReminderFullScreenAlert(type, enabled)

    override suspend fun setReminderVibrationEnabled(type: PrayerType, enabled: Boolean) =
        dataStore.setReminderVibrationEnabled(type, enabled)

    override suspend fun setReminderSoundUri(type: PrayerType, uri: String?) =
        dataStore.setReminderSoundUri(type, uri)

    override suspend fun setMadhab(madhab: Madhab) = dataStore.setMadhab(madhab)

    override suspend fun setHighLatitudeRule(rule: HighLatitudeRule) = dataStore.setHighLatitudeRule(rule)

    override suspend fun setPrayerTimeAdjustMinutes(type: PrayerType, minutes: Int) =
        dataStore.setPrayerTimeAdjustMinutes(type, minutes)

    override suspend fun setJumuahEnabled(enabled: Boolean) = dataStore.setJumuahEnabled(enabled)

    override suspend fun setJumuahReminderLeadMinutes(minutes: Int) =
        dataStore.setJumuahReminderLeadMinutes(minutes)
}