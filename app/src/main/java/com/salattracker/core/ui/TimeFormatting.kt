package com.salattracker.core.ui

import androidx.compose.runtime.staticCompositionLocalOf
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** App-wide display preference, provided by [com.salattracker.feature.main.MainActivity]. */
val LocalUse24HourFormat = staticCompositionLocalOf { false }

/** App-wide Hijri date correction, provided by MainActivity from the saved preference. */
val LocalHijriAdjustmentDays = staticCompositionLocalOf { 0 }

/** Formats a clock time consistently across the app. */
fun LocalTime.formatAppTime(use24HourFormat: Boolean, locale: Locale = Locale.getDefault()): String =
    format(DateTimeFormatter.ofPattern(if (use24HourFormat) "HH:mm" else "hh:mm a", locale))
