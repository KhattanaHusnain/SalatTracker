package com.salattracker.core.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.salattracker.R
import com.salattracker.domain.model.GoalCategory
import com.salattracker.domain.model.GoalFrequency
import com.salattracker.domain.model.PrayerStatus
import com.salattracker.domain.model.PrayerType

/**
 * Locale-aware display labels for domain enums. Centralised here so the name→string-resource
 * mapping lives in one place instead of being duplicated (in English) across every screen.
 *
 * The `*Res` properties are plain (non-composable) string-resource ids, usable from composables
 * (via [stringResource]) and from non-composable code (via `context.getString(res)` — needed
 * e.g. inside a `joinToString` lambda). The `label()` helpers are the composable convenience form.
 */

@get:StringRes
val PrayerType.nameRes: Int get() = when (this) {
    PrayerType.FAJR    -> R.string.prayer_fajr
    PrayerType.DHUHR   -> R.string.prayer_dhuhr
    PrayerType.ASR     -> R.string.prayer_asr
    PrayerType.MAGHRIB -> R.string.prayer_maghrib
    PrayerType.ISHA    -> R.string.prayer_isha
}

@get:StringRes
val PrayerType.shortNameRes: Int get() = when (this) {
    PrayerType.MAGHRIB -> R.string.prayer_maghrib_short
    else               -> nameRes
}

@get:StringRes
val PrayerStatus.labelRes: Int get() = when (this) {
    PrayerStatus.CONGREGATION   -> R.string.status_congregation
    PrayerStatus.INDIVIDUAL     -> R.string.status_individual
    PrayerStatus.QAZA_PENDING   -> R.string.status_qaza_pending
    PrayerStatus.QAZA_COMPLETED -> R.string.status_qaza_completed
    PrayerStatus.MENSTRUATION   -> R.string.status_menstruation
    PrayerStatus.NOT_RECORDED   -> R.string.status_not_recorded
}

@get:StringRes
val GoalCategory.labelRes: Int get() = when (this) {
    GoalCategory.SALAT   -> R.string.category_salat
    GoalCategory.QURAN   -> R.string.category_quran
    GoalCategory.FASTING -> R.string.category_fasting
    GoalCategory.CHARITY -> R.string.category_charity
    GoalCategory.DHIKR   -> R.string.category_dhikr
}

@get:StringRes
val GoalFrequency.labelRes: Int get() = when (this) {
    GoalFrequency.DAILY   -> R.string.frequency_daily
    GoalFrequency.WEEKLY  -> R.string.frequency_weekly
    GoalFrequency.MONTHLY -> R.string.frequency_monthly
}

@Composable fun PrayerType.label(): String = stringResource(nameRes)
@Composable fun PrayerType.shortLabel(): String = stringResource(shortNameRes)
@Composable fun PrayerStatus.label(): String = stringResource(labelRes)
@Composable fun GoalCategory.label(): String = stringResource(labelRes)
@Composable fun GoalFrequency.label(): String = stringResource(labelRes)
