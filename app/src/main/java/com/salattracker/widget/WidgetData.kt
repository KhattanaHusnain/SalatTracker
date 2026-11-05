package com.salattracker.widget

import android.content.Context
import android.content.res.Configuration
import com.salattracker.R
import com.salattracker.core.ui.labelRes
import com.salattracker.domain.model.Gender
import com.salattracker.domain.model.PrayerStatus
import com.salattracker.domain.model.PrayerType
import com.salattracker.domain.repository.PrayerRepository
import com.salattracker.domain.repository.SettingsRepository
import com.salattracker.domain.repository.UserPrefs
import com.salattracker.domain.usecase.activeHomePrayer
import com.salattracker.domain.usecase.homePrayerWindows
import com.salattracker.domain.usecase.prayerDayForHome
import com.salattracker.domain.usecase.toHijriOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Builds widget data independently of Compose and HomeViewModel. */
class WidgetSnapshotBuilder(
    private val context: Context,
    private val prayers: PrayerRepository,
    private val settings: SettingsRepository,
) {
    suspend fun build(now: LocalDateTime = LocalDateTime.now()): WidgetSnapshot = withContext(Dispatchers.IO) {
        val prefs = settings.getCurrentPrefs()
        val locale = Locale.forLanguageTag(prefs.language.code)
        val localized = localizedContext(context, locale)
        if (!prefs.onboardingCompleted) return@withContext emptySnapshot(now, locale, localized)

        val civilDate = now.toLocalDate()
        val todayTimes = prayers.getPrayerTimes(civilDate)
        val prayerDate = prayerDayForHome(now, todayTimes?.get(PrayerType.FAJR))
        val times = if (prayerDate == civilDate) todayTimes else prayers.getPrayerTimes(prayerDate)
        val tomorrowFajr = prayers.getPrayerTimes(prayerDate.plusDays(1))?.get(PrayerType.FAJR)
        val sunrise = prayers.getSunriseTime(prayerDate)
        if (times == null) return@withContext emptySnapshot(now, locale, localized)

        val windows = homePrayerWindows(prayerDate, times, sunrise, tomorrowFajr)
        val activeType = activeHomePrayer(windows, now)
        val entries = prayers.getPrayerEntriesForDate(prayerDate).associateBy { it.prayerType }
        val jumuah = prefs.jumuahEnabled && prefs.gender != Gender.FEMALE
        val starts = (windows.map { (type, window) -> type to window.first } +
            listOfNotNull(tomorrowFajr?.let { PrayerType.FAJR to prayerDate.plusDays(1).atTime(it) }))
            .filter { it.second.isAfter(now) }
        val next = starts.minByOrNull { it.second }
        val list = PrayerType.values().map { type ->
            WidgetPrayer(
                type = type,
                label = localized.getString(type.labelRes(prayerDate, jumuah, prefs.madhab)),
                time = times[type],
                status = entries[type]?.status ?: PrayerStatus.NOT_RECORDED,
                isActive = type == activeType,
                isNext = type == next?.first,
            )
        }
        val streaks = prayers.getStreakSnapshot()
        val topStreak = streaks.maxByOrNull { it.value.currentStreak }
        val active = list.firstOrNull { it.isActive }
        val deadline = active?.type?.let(windows::get)?.second
        val nextAt = next?.second
        val nextTransition = listOfNotNull(deadline, nextAt, civilDate.plusDays(1).atStartOfDay())
            .filter { it.isAfter(now) }.minOrNull() ?: civilDate.plusDays(1).atStartOfDay()
        WidgetSnapshot(
            configured = true,
            prayerDate = prayerDate,
            now = now.atZone(ZoneId.systemDefault()).toInstant(),
            locale = locale,
            use24Hour = prefs.use24HourFormat,
            gregorianDate = civilDate.format(DateTimeFormatter.ofPattern("EEE, d MMM", locale)),
            hijriDate = civilDate.toHijriOrNull(prefs.hijriAdjustmentDays)?.let { hijri ->
                localized.getString(R.string.hijri_date_full, hijri.day,
                    localized.resources.getStringArray(R.array.hijri_months)[hijri.month - 1], hijri.year)
            },
            location = prefs.locationLabel?.substringBefore(",")?.takeIf { it.isNotBlank() },
            prayers = list,
            activePrayer = active,
            activeDeadline = deadline?.atZone(ZoneId.systemDefault())?.toInstant(),
            nextPrayer = list.firstOrNull { it.type == next?.first },
            nextPrayerAt = nextAt?.atZone(ZoneId.systemDefault())?.toInstant(),
            streakPrayer = topStreak?.key?.let { localized.getString(it.labelRes(prayerDate, jumuah, prefs.madhab)) },
            streakDays = topStreak?.value?.currentStreak ?: 0,
            nextTransitionAt = nextTransition.atZone(ZoneId.systemDefault()).toInstant(),
        )
    }

    private fun emptySnapshot(now: LocalDateTime, locale: Locale, localized: Context) = WidgetSnapshot(
        configured = false, prayerDate = now.toLocalDate(), now = now.atZone(ZoneId.systemDefault()).toInstant(),
        locale = locale, use24Hour = false,
        gregorianDate = now.toLocalDate().format(DateTimeFormatter.ofPattern("EEE, d MMM", locale)),
        hijriDate = null, location = null, prayers = emptyList(), activePrayer = null,
        activeDeadline = null, nextPrayer = null, nextPrayerAt = null, streakPrayer = null,
        streakDays = 0, nextTransitionAt = now.toLocalDate().plusDays(1).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant(),
    )

    private fun localizedContext(base: Context, locale: Locale): Context {
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        return base.createConfigurationContext(config)
    }
}
