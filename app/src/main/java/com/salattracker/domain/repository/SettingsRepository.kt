package com.salattracker.domain.repository

import com.salattracker.domain.model.AppLanguage
import com.salattracker.domain.model.HighLatitudeRule
import com.salattracker.domain.model.Madhab
import com.salattracker.domain.model.PrayerType
import com.salattracker.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

/** Sentinel for [UserPrefs.reminderSoundUri]: the user explicitly chose the device's own
 * default notification sound, as opposed to never having touched the setting (null, which
 * means the app's own default — the bundled Azan — applies instead). */
const val DEVICE_DEFAULT_SOUND_URI = "device_default"

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

    /** Persist all settings in one atomic DataStore write. Still needed by backup restore,
     * which writes a whole snapshot at once; the Settings screen itself uses the granular
     * setters below instead (live-apply, one write per change). */
    suspend fun saveSettings(
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
    )

    /** One-shot read, used when prayer times need refreshing. */
    suspend fun getCurrentPrefs(): UserPrefs

    // ── Settings screen — granular, live-apply writes ──────────────────────────

    suspend fun setUseDeviceLocation(enabled: Boolean)

    /** Used by both the manual lat/lon fields and a successful device-location fix. */
    suspend fun setLocation(latitude: Double, longitude: Double)

    suspend fun setCalculationMethodId(id: Int)

    suspend fun setUse24HourFormat(enabled: Boolean)

    /** Also marks [UserPrefs.madhabChosen] true, so this is the single call both onboarding and
     * the one-time existing-user prompt need — there's no separate "mark chosen" setter to
     * forget. Bulk restore (which must set the flag independently of a real user choice) goes
     * through [saveSettings] instead. */
    suspend fun setMadhab(madhab: Madhab)

    suspend fun setHighLatitudeRule(rule: HighLatitudeRule)

    /** A correction to the displayed/alarmed prayer time itself — distinct from
     * [setReminderOffsetMinutes], which only shifts when the *notification* fires. */
    suspend fun setPrayerTimeAdjustMinutes(type: PrayerType, minutes: Int)

    suspend fun setJumuahEnabled(enabled: Boolean)

    /** 0 = no separate early reminder for Jumu'ah. */
    suspend fun setJumuahReminderLeadMinutes(minutes: Int)

    // ── Onboarding / app-preference granular writes ────────────────────────────

    suspend fun setLanguage(language: AppLanguage)

    suspend fun setThemeMode(themeMode: ThemeMode)

    /** Master toggle used by onboarding — enables/disables every per-prayer reminder at once. */
    suspend fun setAllRemindersEnabled(enabled: Boolean)

    /** Marks first-run onboarding as finished so it never shows again. */
    suspend fun setOnboardingCompleted(completed: Boolean)

    // ── Prayer Reminders screen — granular, live-apply writes ──────────────────

    suspend fun setReminderEnabled(type: PrayerType, enabled: Boolean)

    suspend fun setReminderOffsetMinutes(type: PrayerType, minutes: Int)

    suspend fun setReminderSnoozeMinutes(type: PrayerType, minutes: Int)

    suspend fun setReminderFullScreenAlert(type: PrayerType, enabled: Boolean)

    suspend fun setReminderVibrationEnabled(type: PrayerType, enabled: Boolean)

    /** Null = app default (Azan), [DEVICE_DEFAULT_SOUND_URI] = the device's own default sound
     * explicitly chosen, "" = Silent (the ringtone picker returns a null Uri for Silent,
     * re-encoded here so it stays distinguishable from "never touched"), else a content:// URI. */
    suspend fun setReminderSoundUri(type: PrayerType, uri: String?)
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
    val reminderSnoozeMinutes: Map<PrayerType, Int> = PrayerType.values()
        .associateWith { 10 },
    val reminderFullScreenAlert: Map<PrayerType, Boolean> = PrayerType.values()
        .associateWith { true },
    val reminderVibrationEnabled: Map<PrayerType, Boolean> = PrayerType.values()
        .associateWith { true },
    /** Null = app default (Azan). See setReminderSoundUri for the full encoding. */
    val reminderSoundUri: Map<PrayerType, String?> = PrayerType.values()
        .associateWith { null },
    val use24HourFormat: Boolean = false,
    // App preferences collected at onboarding.
    val language: AppLanguage = AppLanguage.ENGLISH,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val onboardingCompleted: Boolean = false,
    val madhab: Madhab = Madhab.STANDARD,
    val highLatitudeRule: HighLatitudeRule = HighLatitudeRule.AUTOMATIC,
    /** A correction (in minutes) applied to the calculated prayer time itself — not to be
     * confused with [reminderOffsetMinutes], which only shifts when the notification fires. */
    val prayerTimeAdjustMinutes: Map<PrayerType, Int> = PrayerType.values()
        .associateWith { 0 },
    /** False only for installs that existed before [madhab] was introduced and haven't yet seen
     * the one-time prompt (`MainActivity`'s `MadhabPromptDialog`) or been through onboarding. */
    val madhabChosen: Boolean = false,
    val jumuahEnabled: Boolean = true,
    val jumuahReminderLeadMinutes: Int = 0,
)