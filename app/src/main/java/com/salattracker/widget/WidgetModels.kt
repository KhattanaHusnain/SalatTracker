package com.salattracker.widget

import com.salattracker.domain.model.PrayerStatus
import com.salattracker.domain.model.PrayerType
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale

enum class WidgetStyle { MONOCHROME, THEMED }
enum class WidgetContent { ALL_FIVE, CURRENT_NEXT }
enum class WidgetSize { SMALL, MEDIUM, LARGE, LOCK_SCREEN }
enum class WidgetContrast { AUTO, LIGHT, DARK }
enum class WidgetWallpaper { HOME, LOCK }

/** The fixed visual identity exposed by one entry in Android's widget picker. */
enum class WidgetVariant(
    val style: WidgetStyle,
    val content: WidgetContent,
    val size: WidgetSize,
    val labelRes: Int,
) {
    SMALL_MONO_ALL(WidgetStyle.MONOCHROME, WidgetContent.ALL_FIVE, WidgetSize.SMALL, com.salattracker.R.string.widget_small_mono_all),
    SMALL_MONO_FOCUS(WidgetStyle.MONOCHROME, WidgetContent.CURRENT_NEXT, WidgetSize.SMALL, com.salattracker.R.string.widget_small_mono_focus),
    SMALL_THEME_ALL(WidgetStyle.THEMED, WidgetContent.ALL_FIVE, WidgetSize.SMALL, com.salattracker.R.string.widget_small_theme_all),
    SMALL_THEME_FOCUS(WidgetStyle.THEMED, WidgetContent.CURRENT_NEXT, WidgetSize.SMALL, com.salattracker.R.string.widget_small_theme_focus),
    MEDIUM_MONO_ALL(WidgetStyle.MONOCHROME, WidgetContent.ALL_FIVE, WidgetSize.MEDIUM, com.salattracker.R.string.widget_medium_mono_all),
    MEDIUM_MONO_FOCUS(WidgetStyle.MONOCHROME, WidgetContent.CURRENT_NEXT, WidgetSize.MEDIUM, com.salattracker.R.string.widget_medium_mono_focus),
    MEDIUM_THEME_ALL(WidgetStyle.THEMED, WidgetContent.ALL_FIVE, WidgetSize.MEDIUM, com.salattracker.R.string.widget_medium_theme_all),
    MEDIUM_THEME_FOCUS(WidgetStyle.THEMED, WidgetContent.CURRENT_NEXT, WidgetSize.MEDIUM, com.salattracker.R.string.widget_medium_theme_focus),
    LARGE_MONO_ALL(WidgetStyle.MONOCHROME, WidgetContent.ALL_FIVE, WidgetSize.LARGE, com.salattracker.R.string.widget_large_mono_all),
    LARGE_MONO_FOCUS(WidgetStyle.MONOCHROME, WidgetContent.CURRENT_NEXT, WidgetSize.LARGE, com.salattracker.R.string.widget_large_mono_focus),
    LARGE_THEME_ALL(WidgetStyle.THEMED, WidgetContent.ALL_FIVE, WidgetSize.LARGE, com.salattracker.R.string.widget_large_theme_all),
    LARGE_THEME_FOCUS(WidgetStyle.THEMED, WidgetContent.CURRENT_NEXT, WidgetSize.LARGE, com.salattracker.R.string.widget_large_theme_focus),
    LOCK_MONO_ALL(WidgetStyle.MONOCHROME, WidgetContent.ALL_FIVE, WidgetSize.LOCK_SCREEN, com.salattracker.R.string.widget_lock_mono_all),
    LOCK_MONO_FOCUS(WidgetStyle.MONOCHROME, WidgetContent.CURRENT_NEXT, WidgetSize.LOCK_SCREEN, com.salattracker.R.string.widget_lock_mono_focus),
    LOCK_THEME_ALL(WidgetStyle.THEMED, WidgetContent.ALL_FIVE, WidgetSize.LOCK_SCREEN, com.salattracker.R.string.widget_lock_theme_all),
    LOCK_THEME_FOCUS(WidgetStyle.THEMED, WidgetContent.CURRENT_NEXT, WidgetSize.LOCK_SCREEN, com.salattracker.R.string.widget_lock_theme_focus),
}

data class WidgetPrayer(
    val type: PrayerType,
    val label: String,
    val time: LocalTime?,
    val status: PrayerStatus,
    val isActive: Boolean,
    val isNext: Boolean,
)

data class WidgetSnapshot(
    val configured: Boolean,
    val prayerDate: LocalDate,
    val now: Instant,
    val locale: Locale,
    val use24Hour: Boolean,
    val gregorianDate: String,
    val hijriDate: String?,
    val location: String?,
    val prayers: List<WidgetPrayer>,
    val activePrayer: WidgetPrayer?,
    val activeDeadline: Instant?,
    val nextPrayer: WidgetPrayer?,
    val nextPrayerAt: Instant?,
    val streakPrayer: String?,
    val streakDays: Int,
    val nextTransitionAt: Instant,
)
