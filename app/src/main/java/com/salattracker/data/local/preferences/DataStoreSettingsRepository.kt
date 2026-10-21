package com.salattracker.data.local.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.salattracker.domain.model.AppLanguage
import com.salattracker.domain.model.HighLatitudeRule
import com.salattracker.domain.model.Madhab
import com.salattracker.domain.model.PrayerType
import com.salattracker.domain.model.ThemeMode
import com.salattracker.domain.repository.SettingsRepository
import com.salattracker.domain.repository.UserPrefs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataStoreSettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    // ── Keys ──────────────────────────────────────────────────────────────────

    private object Keys {
        val USE_DEVICE_LOCATION   = booleanPreferencesKey("use_device_location")
        val LATITUDE              = doublePreferencesKey("latitude")
        val LONGITUDE             = doublePreferencesKey("longitude")
        val TIMEZONE              = stringPreferencesKey("timezone")
        val CALCULATION_METHOD_ID = intPreferencesKey("calculation_method_id")
        val USE_24_HOUR           = booleanPreferencesKey("use_24_hour")
        val LANGUAGE              = stringPreferencesKey("language")
        val THEME_MODE            = stringPreferencesKey("theme_mode")
        val ONBOARDING_COMPLETED  = booleanPreferencesKey("onboarding_completed")
        val MADHAB                = stringPreferencesKey("madhab")
        val HIGH_LATITUDE_RULE    = stringPreferencesKey("high_latitude_rule")
        val MADHAB_CHOSEN         = booleanPreferencesKey("madhab_chosen")
        val JUMUAH_ENABLED        = booleanPreferencesKey("jumuah_enabled")
        val JUMUAH_REMINDER_LEAD  = intPreferencesKey("jumuah_reminder_lead_minutes")

        // Per-prayer reminder toggle: "reminder_enabled_FAJR" etc.
        fun reminderEnabled(type: PrayerType) =
            booleanPreferencesKey("reminder_enabled_${type.name}")

        // Per-prayer offset: "reminder_offset_FAJR" etc. (notification lead/lag — see
        // UserPrefs.reminderOffsetMinutes vs prayerTimeAdjustMinutes).
        fun reminderOffset(type: PrayerType) =
            intPreferencesKey("reminder_offset_${type.name}")

        // Per-prayer correction to the calculated time itself: "prayer_time_adjust_FAJR" etc.
        fun prayerTimeAdjust(type: PrayerType) =
            intPreferencesKey("prayer_time_adjust_${type.name}")

        fun reminderSnooze(type: PrayerType) =
            intPreferencesKey("reminder_snooze_${type.name}")

        fun reminderFullScreen(type: PrayerType) =
            booleanPreferencesKey("reminder_fullscreen_${type.name}")

        fun reminderVibration(type: PrayerType) =
            booleanPreferencesKey("reminder_vibration_${type.name}")

        fun reminderSound(type: PrayerType) =
            stringPreferencesKey("reminder_sound_${type.name}")
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    override fun observeSettings(): Flow<UserPrefs> =
        dataStore.data
            .catch { e ->
                if (e is IOException) emit(emptyPreferences()) else throw e
            }
            .map { prefs -> prefs.toUserPrefs() }

    override suspend fun getCurrentPrefs(): UserPrefs =
        dataStore.data.first().toUserPrefs()

    private fun Preferences.toUserPrefs() = UserPrefs(
        useDeviceLocation    = this[Keys.USE_DEVICE_LOCATION]   ?: true,
        latitude             = this[Keys.LATITUDE]               ?: 0.0,
        longitude            = this[Keys.LONGITUDE]              ?: 0.0,
        timezone             = this[Keys.TIMEZONE]               ?: "UTC",
        calculationMethodId  = this[Keys.CALCULATION_METHOD_ID] ?: 3,
        reminderEnabled      = PrayerType.values().associateWith { type ->
            this[Keys.reminderEnabled(type)] ?: true
        },
        reminderOffsetMinutes = PrayerType.values().associateWith { type ->
            this[Keys.reminderOffset(type)] ?: 0
        },
        reminderSnoozeMinutes = PrayerType.values().associateWith { type ->
            this[Keys.reminderSnooze(type)] ?: 10
        },
        reminderFullScreenAlert = PrayerType.values().associateWith { type ->
            this[Keys.reminderFullScreen(type)] ?: true
        },
        reminderVibrationEnabled = PrayerType.values().associateWith { type ->
            this[Keys.reminderVibration(type)] ?: true
        },
        reminderSoundUri      = PrayerType.values().associateWith { type ->
            this[Keys.reminderSound(type)]
        },
        use24HourFormat      = this[Keys.USE_24_HOUR]            ?: false,
        language             = this[Keys.LANGUAGE]?.let { runCatching { AppLanguage.valueOf(it) }.getOrNull() }
            ?: AppLanguage.ENGLISH,
        themeMode            = this[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
            ?: ThemeMode.SYSTEM,
        onboardingCompleted  = this[Keys.ONBOARDING_COMPLETED]   ?: false,
        madhab               = this[Keys.MADHAB]?.let { runCatching { Madhab.valueOf(it) }.getOrNull() }
            ?: Madhab.STANDARD,
        highLatitudeRule     = this[Keys.HIGH_LATITUDE_RULE]?.let { runCatching { HighLatitudeRule.valueOf(it) }.getOrNull() }
            ?: HighLatitudeRule.AUTOMATIC,
        prayerTimeAdjustMinutes = PrayerType.values().associateWith { type ->
            this[Keys.prayerTimeAdjust(type)] ?: 0
        },
        madhabChosen         = this[Keys.MADHAB_CHOSEN]          ?: false,
        jumuahEnabled        = this[Keys.JUMUAH_ENABLED]         ?: true,
        jumuahReminderLeadMinutes = this[Keys.JUMUAH_REMINDER_LEAD] ?: 0,
    )

    // ── Write ─────────────────────────────────────────────────────────────────

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
    ) {
        dataStore.edit { prefs ->
            prefs[Keys.USE_DEVICE_LOCATION]   = useDeviceLocation
            prefs[Keys.LATITUDE]              = latitude
            prefs[Keys.LONGITUDE]             = longitude
            prefs[Keys.CALCULATION_METHOD_ID] = calculationMethodId
            prefs[Keys.USE_24_HOUR]           = use24HourFormat
            prefs[Keys.LANGUAGE]              = language.name
            prefs[Keys.THEME_MODE]            = themeMode.name
            prefs[Keys.MADHAB]                = madhab.name
            prefs[Keys.HIGH_LATITUDE_RULE]    = highLatitudeRule.name
            prefs[Keys.MADHAB_CHOSEN]         = madhabChosen
            prefs[Keys.JUMUAH_ENABLED]        = jumuahEnabled
            prefs[Keys.JUMUAH_REMINDER_LEAD]  = jumuahReminderLeadMinutes
            PrayerType.values().forEach { type ->
                prefs[Keys.reminderEnabled(type)]  = reminderEnabled[type] ?: true
                prefs[Keys.reminderOffset(type)]   = reminderOffsetMinutes[type] ?: 0
                prefs[Keys.prayerTimeAdjust(type)] = prayerTimeAdjustMinutes[type] ?: 0
            }
        }
    }

    override suspend fun setUseDeviceLocation(enabled: Boolean) {
        dataStore.edit { it[Keys.USE_DEVICE_LOCATION] = enabled }
    }

    override suspend fun setLocation(latitude: Double, longitude: Double) {
        dataStore.edit { prefs ->
            prefs[Keys.LATITUDE] = latitude
            prefs[Keys.LONGITUDE] = longitude
        }
    }

    override suspend fun setCalculationMethodId(id: Int) {
        dataStore.edit { it[Keys.CALCULATION_METHOD_ID] = id }
    }

    override suspend fun setUse24HourFormat(enabled: Boolean) {
        dataStore.edit { it[Keys.USE_24_HOUR] = enabled }
    }

    override suspend fun setLanguage(language: AppLanguage) {
        dataStore.edit { it[Keys.LANGUAGE] = language.name }
    }

    override suspend fun setThemeMode(themeMode: ThemeMode) {
        dataStore.edit { it[Keys.THEME_MODE] = themeMode.name }
    }

    override suspend fun setAllRemindersEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            PrayerType.values().forEach { type -> prefs[Keys.reminderEnabled(type)] = enabled }
        }
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }

    override suspend fun setReminderEnabled(type: PrayerType, enabled: Boolean) {
        dataStore.edit { it[Keys.reminderEnabled(type)] = enabled }
    }

    override suspend fun setReminderOffsetMinutes(type: PrayerType, minutes: Int) {
        dataStore.edit { it[Keys.reminderOffset(type)] = minutes }
    }

    override suspend fun setReminderSnoozeMinutes(type: PrayerType, minutes: Int) {
        dataStore.edit { it[Keys.reminderSnooze(type)] = minutes }
    }

    override suspend fun setReminderFullScreenAlert(type: PrayerType, enabled: Boolean) {
        dataStore.edit { it[Keys.reminderFullScreen(type)] = enabled }
    }

    override suspend fun setReminderVibrationEnabled(type: PrayerType, enabled: Boolean) {
        dataStore.edit { it[Keys.reminderVibration(type)] = enabled }
    }

    override suspend fun setReminderSoundUri(type: PrayerType, uri: String?) {
        dataStore.edit { prefs ->
            if (uri == null) prefs.remove(Keys.reminderSound(type)) else prefs[Keys.reminderSound(type)] = uri
        }
    }

    override suspend fun setMadhab(madhab: Madhab) {
        dataStore.edit { prefs ->
            prefs[Keys.MADHAB] = madhab.name
            prefs[Keys.MADHAB_CHOSEN] = true
        }
    }

    override suspend fun setHighLatitudeRule(rule: HighLatitudeRule) {
        dataStore.edit { it[Keys.HIGH_LATITUDE_RULE] = rule.name }
    }

    override suspend fun setPrayerTimeAdjustMinutes(type: PrayerType, minutes: Int) {
        dataStore.edit { it[Keys.prayerTimeAdjust(type)] = minutes }
    }

    override suspend fun setJumuahEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.JUMUAH_ENABLED] = enabled }
    }

    override suspend fun setJumuahReminderLeadMinutes(minutes: Int) {
        dataStore.edit { it[Keys.JUMUAH_REMINDER_LEAD] = minutes }
    }
}