package com.salattracker.core.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import com.salattracker.R
import com.salattracker.domain.model.GoalCategory
import com.salattracker.domain.model.GoalFrequency
import com.salattracker.domain.model.PrayerStatus
import com.salattracker.domain.model.PrayerType
import com.salattracker.domain.usecase.CalendarViewType
import com.salattracker.domain.usecase.HijriDate
import com.salattracker.domain.usecase.IslamicEvent
import java.time.DayOfWeek

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

/** Reuses Calendar's weekday strings rather than java.time's Locale-based day names, which read
 * the system locale in a way Compose can't observe for recomposition on locale change. */
@get:StringRes
val DayOfWeek.shortLabelRes: Int get() = when (this) {
    DayOfWeek.SUNDAY    -> R.string.weekday_sun
    DayOfWeek.MONDAY    -> R.string.weekday_mon
    DayOfWeek.TUESDAY   -> R.string.weekday_tue
    DayOfWeek.WEDNESDAY -> R.string.weekday_wed
    DayOfWeek.THURSDAY  -> R.string.weekday_thu
    DayOfWeek.FRIDAY    -> R.string.weekday_fri
    DayOfWeek.SATURDAY  -> R.string.weekday_sat
}

@Composable fun DayOfWeek.shortLabel(): String = stringResource(shortLabelRes)

@get:StringRes
val CalendarViewType.labelRes: Int get() = when (this) {
    CalendarViewType.MONTH -> R.string.view_month
    CalendarViewType.WEEK  -> R.string.view_week
    CalendarViewType.YEAR  -> R.string.view_year
}

@Composable fun CalendarViewType.label(): String = stringResource(labelRes)

@get:StringRes
val IslamicEvent.labelRes: Int get() = when (this) {
    IslamicEvent.ISLAMIC_NEW_YEAR -> R.string.islamic_event_new_year
    IslamicEvent.ASHURA           -> R.string.islamic_event_ashura
    IslamicEvent.MAWLID_AL_NABI   -> R.string.islamic_event_mawlid
    IslamicEvent.ISRA_AND_MIRAJ   -> R.string.islamic_event_isra_miraj
    IslamicEvent.RAMADAN_START    -> R.string.islamic_event_ramadan_start
    IslamicEvent.LAYLAT_AL_QADR   -> R.string.islamic_event_laylat_al_qadr
    IslamicEvent.EID_AL_FITR      -> R.string.islamic_event_eid_al_fitr
    IslamicEvent.ARAFAH_DAY       -> R.string.islamic_event_arafah_day
    IslamicEvent.EID_AL_ADHA      -> R.string.islamic_event_eid_al_adha
}

@Composable fun IslamicEvent.label(): String = stringResource(labelRes)

// ── Hijri date formatting ──────────────────────────────────────────────────────
// Pure day/month/year -> display text lives here rather than in the domain layer, since it
// needs locale-aware string resources (month names, AH suffix) the same way every other
// enum-to-label mapping above does.

/** "22 Rajab 1445 AH" — a single Hijri date, e.g. Calendar's selected-day panel. */
@Composable
fun HijriDate.fullLabel(): String {
    val months = stringArrayResource(R.array.hijri_months)
    return stringResource(R.string.hijri_date_full, day, months[month - 1], year)
}

/** "Rabiʻ II – Jumada I 1448 AH" (or "Rabiʻ II 1448 AH" when [from] and [to] land in the same
 * Hijri month) — the subtitle under a Month/Month-grid header, spanning the Hijri months
 * touched by one Gregorian month. */
@Composable
fun hijriMonthRangeLabel(from: HijriDate, to: HijriDate): String {
    val months = stringArrayResource(R.array.hijri_months)
    val startName = months[from.month - 1]
    val endName = months[to.month - 1]
    return when {
        from.month == to.month && from.year == to.year -> stringResource(R.string.hijri_single_month_year, startName, to.year)
        from.year == to.year                            -> stringResource(R.string.hijri_two_months_same_year, startName, endName, to.year)
        else                                              -> stringResource(R.string.hijri_two_months_diff_year, startName, from.year, endName, to.year)
    }
}

/** "1447 – 1448 AH" (or "1448 AH" when [startYear] and [endYear] are the same) — the Year
 * view's header subtitle. */
@Composable
fun hijriYearRangeLabel(startYear: Int, endYear: Int): String =
    if (startYear == endYear) stringResource(R.string.hijri_single_year, startYear)
    else stringResource(R.string.hijri_two_years, startYear, endYear)

/** Goal units are stored as their English keys (so existing goals and backups keep working);
 * this maps a known key to its localized label and shows anything else as-is. */
@Composable
fun goalUnitLabel(unit: String): String = when (unit) {
    "Time(s)" -> stringResource(R.string.unit_times)
    "Minutes" -> stringResource(R.string.unit_minutes)
    "Pages"   -> stringResource(R.string.unit_pages)
    else      -> unit
}
