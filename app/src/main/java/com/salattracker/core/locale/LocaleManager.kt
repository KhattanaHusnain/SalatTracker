package com.salattracker.core.locale

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.salattracker.domain.model.AppLanguage

/**
 * Applies the app's chosen language via the AndroidX per-app locale API.
 *
 * [AppCompatDelegate.setApplicationLocales] recreates the activity in the new
 * locale, drives RTL (Arabic/Urdu) automatically, and — with the
 * `autoStoreLocales` service declared in the manifest — persists and restores
 * the selection across process restarts. Safe to call from any thread.
 */
object LocaleManager {

    fun apply(language: AppLanguage) {
        val current = AppCompatDelegate.getApplicationLocales()
        val desired = LocaleListCompat.forLanguageTags(language.code)
        // Avoid a redundant activity recreation when the locale is already active.
        if (current != desired) {
            AppCompatDelegate.setApplicationLocales(desired)
        }
    }
}
