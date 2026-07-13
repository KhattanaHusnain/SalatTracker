package com.salattracker.domain.model

/**
 * App language chosen during onboarding / in Settings.
 *
 * NOTE: the selection is persisted but not yet applied — actual localization
 * (per-locale strings, RTL, locale switching) is deferred follow-up work.
 * [code] is the BCP-47 language tag to use when that work is picked up.
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
 * NOTE: the selection is persisted but not yet applied — the app is still
 * light-only. Wiring a dark [androidx.compose.material3.ColorScheme] is deferred.
 */
enum class ThemeMode { LIGHT, DARK, SYSTEM }
