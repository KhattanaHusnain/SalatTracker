package com.salattracker.domain.usecase

import com.salattracker.domain.model.PrayerType
import com.salattracker.domain.model.StreakInfo
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

private const val MINUTES_PER_DAY = 24 * 60

/** Date-aware display windows: Fajr ends at sunrise, Isha at the following Fajr. */
fun homePrayerWindows(date: LocalDate, times: Map<PrayerType, LocalTime>?, sunrise: LocalTime?,
    tomorrowFajr: LocalTime?): Map<PrayerType, Pair<LocalDateTime, LocalDateTime>> {
    if (times == null) return emptyMap()
    return PrayerType.values().mapNotNull { type ->
        val start = times[type]?.let(date::atTime) ?: return@mapNotNull null
        val end = when (type) {
            PrayerType.FAJR -> sunrise?.let(date::atTime)
            PrayerType.ISHA -> tomorrowFajr?.let { date.plusDays(1).atTime(it) }
            else -> times[PrayerType.values()[type.ordinal + 1]]?.let(date::atTime)
        } ?: return@mapNotNull null
        if (!end.isAfter(start)) null else type to (start to end)
    }.toMap()
}

fun activeHomePrayer(windows: Map<PrayerType, Pair<LocalDateTime, LocalDateTime>>, now: LocalDateTime): PrayerType? =
    windows.entries.firstOrNull { (_, window) -> !now.isBefore(window.first) && now.isBefore(window.second) }?.key

/** The next prayer Home's hero card counts down to. [isTomorrow] is true once every one of
 * today's prayers has passed and this points at tomorrow's Fajr instead. */
data class NextPrayer(
    val type: PrayerType,
    val time: LocalTime,
    val isTomorrow: Boolean,
)

/** Home's prayer day begins at Fajr rather than midnight. Before today's Fajr, last night's
 * Isha stays on the timeline so it can still be logged without opening Calendar. */
fun prayerDayForHome(now: LocalDateTime, todayFajr: LocalTime?): LocalDate =
    if (todayFajr != null && now.toLocalTime().isBefore(todayFajr)) now.toLocalDate().minusDays(1)
    else now.toLocalDate()

/** The first of today's prayers still ahead of [now], or tomorrow's Fajr (via [tomorrowFajr])
 * once today's are all behind it. Returns null only when even that isn't known yet (prayer
 * times haven't been cached for tomorrow). */
fun nextPrayerOf(
    todayTimes: Map<PrayerType, LocalTime>?,
    tomorrowFajr: LocalTime?,
    now: LocalTime,
): NextPrayer? {
    todayTimes?.let { times ->
        PrayerType.values().forEach { type ->
            val time = times[type] ?: return@forEach
            if (time.isAfter(now)) return NextPrayer(type, time, isTomorrow = false)
        }
    }
    return tomorrowFajr?.let { NextPrayer(PrayerType.FAJR, it, isTomorrow = true) }
}

/** Minutes from [now] until [next], wrapping past midnight when [NextPrayer.isTomorrow]. */
fun minutesUntil(next: NextPrayer, now: LocalTime): Long {
    val nowMinutes = now.hour * 60 + now.minute
    val nextMinutes = next.time.hour * 60 + next.time.minute
    return if (next.isTomorrow) {
        ((MINUTES_PER_DAY - nowMinutes) + nextMinutes).toLong()
    } else {
        (nextMinutes - nowMinutes).toLong().coerceAtLeast(0L)
    }
}

/** The prayer currently "in progress" — the last of today's prayers whose time has already
 * passed. Null before Fajr, or when today's times aren't known yet. */
fun currentPrayerOf(todayTimes: Map<PrayerType, LocalTime>?, now: LocalTime): PrayerType? {
    val times = todayTimes ?: return null
    var current: PrayerType? = null
    PrayerType.values().forEach { type ->
        val time = times[type] ?: return@forEach
        if (!time.isAfter(now)) current = type
    }
    return current
}

/** Home's single "perfect day" streak: the smallest of the five independent per-prayer streaks
 * (a missing prayer counts as 0), since every prayer has to still be unbroken for the day itself
 * to count. No new calculation — this is a read of the same per-prayer streaks Calendar and
 * Stats already show, kept in sync by [com.salattracker.domain.repository.PrayerRepository.observeStreaks].
 * It can read higher than truly warranted if a day's Menstruation status was only recorded for
 * some of that day's five prayers rather than all of them, since that status is neutral rather
 * than streak-breaking per prayer. */
fun perfectDayStreak(streaks: Map<PrayerType, StreakInfo>): Int =
    PrayerType.values().minOfOrNull { streaks[it]?.currentStreak ?: 0 } ?: 0
