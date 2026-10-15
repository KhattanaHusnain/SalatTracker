package com.salattracker.domain.repository

import com.salattracker.domain.model.AppLanguage

/**
 * Applies the app's chosen language device-wide. An interface over
 * [com.salattracker.core.locale.LocaleManager]'s static object so ViewModels can be
 * unit-tested — calling the real `AppCompatDelegate`-backed object directly from a plain JVM
 * test would crash (no Android framework present).
 */
interface LocaleApplier {
    fun apply(language: AppLanguage)
}
