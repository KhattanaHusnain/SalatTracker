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
