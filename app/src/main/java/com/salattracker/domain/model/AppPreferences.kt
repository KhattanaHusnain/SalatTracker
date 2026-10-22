package com.salattracker.domain.model

/**
 * App language chosen during onboarding / in Settings.
 *
 * Applied app-wide via [com.salattracker.core.locale.LocaleManager] (which drives RTL for
 * Arabic/Urdu too) — see its call sites in `MainActivity.onCreate` and `SettingsViewModel`.
 * [code] is the BCP-47 language tag passed to it.
 */
enum class AppLanguage(val code: String, val displayName: String) {
    ENGLISH("en", "English"),
    ARABIC("ar", "العربية"),
    URDU("ur", "اردو"),
    FRENCH("fr", "Français"),
}

/**
 * App theme mode chosen during onboarding / in Settings.
 *
 * Applied via [isDark] — `MainActivity` reads it (through `AppStartViewModel`) and passes the
 * result into `SalatTrackerTheme(darkTheme = ...)`.
 */
enum class ThemeMode { LIGHT, DARK, SYSTEM }

/** Resolves this [ThemeMode] against the device's current dark-theme state. [SYSTEM] defers to
 * [systemInDarkTheme] (typically `isSystemInDarkTheme()`); [LIGHT]/[DARK] ignore it. */
fun ThemeMode.isDark(systemInDarkTheme: Boolean): Boolean = when (this) {
    ThemeMode.LIGHT  -> false
    ThemeMode.DARK   -> true
    ThemeMode.SYSTEM -> systemInDarkTheme
}

/**
 * School of thought used to calculate Asr (and, for [JAFARI], Isha too), chosen at onboarding /
 * in Settings (or via the one-time prompt existing installs see after this preference was
 * introduced). Independent of which *calculation method* (angles) is used, which is a separate,
 * 23-option setting — see [com.salattracker.data.prayertimes.CalculationParametersFactory].
 *
 * [STANDARD] and [HANAFI] mirror `com.batoulapps.adhan2.Madhab` exactly — the library only
 * distinguishes those two schools for Asr timing (shadow-length ratio 1 vs 2).
 *
 * [JAFARI] has no `adhan2` equivalent at all: Shia (Ja'fari) practice doesn't compute Asr/Isha
 * from a shadow ratio or twilight angle, it combines them with the prayer before — Asr with
 * Dhuhr ("Zuhrain") and Isha with Maghrib ("Maghribain") — so both are simply set equal to the
 * time of the prayer they're combined with. This is handled entirely in
 * [com.salattracker.data.prayertimes.AdhanPrayerTimeCalculator], outside the library.
 *
 * [STANDARD] is the default so existing users' Asr time doesn't change when this preference is
 * introduced — it matches what every prayer time in the app has always meant until now.
 */
enum class Madhab {
    /** Shafi'i, Maliki and Hanbali: Asr begins when an object's shadow equals its own length. */
    STANDARD,

    /** Hanafi: Asr begins when an object's shadow is twice its own length (later than [STANDARD],
     * often by around an hour). */
    HANAFI,

    /** Ja'fari (Shia): Asr is prayed with Dhuhr ("Zuhrain") and Isha with Maghrib
     * ("Maghribain") — see the class doc for how this is calculated. */
    JAFARI,
}

/**
 * How Fajr/Isha are bounded at high latitudes, where the sun angles the calculation method uses
 * may never be reached in summer (or are reached for most of the night in winter).
 *
 * Mirrors `com.batoulapps.adhan2.HighLatitudeRule`, plus [AUTOMATIC] — this app's own default,
 * which defers to the library's `HighLatitudeRule.recommendedFor(coordinates)` (seventh-of-the-
 * night above 48° latitude, otherwise middle-of-the-night) rather than forcing one rule on every
 * location.
 */
enum class HighLatitudeRule {
    AUTOMATIC,
    MIDDLE_OF_THE_NIGHT,
    SEVENTH_OF_THE_NIGHT,
    TWILIGHT_ANGLE,
}

/**
 * Collected at onboarding's "About you" step. Purely a stored preference today — it does not
 * gate anything (e.g. [com.salattracker.domain.model.PrayerStatus.MENSTRUATION] stays available
 * to everyone in the status pickers, since hiding it for an existing install with gender still
 * null/absent would risk hiding a status some users already rely on). Null means "not asked yet"
 * (every install that existed before this preference, and any install that skips the step).
 */
enum class Gender { MALE, FEMALE }
