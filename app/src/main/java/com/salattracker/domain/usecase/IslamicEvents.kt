package com.salattracker.domain.usecase

/**
 * Major Islamic calendar observances, keyed by their fixed Hijri month/day — the same
 * calculated (Umm al-Qura) conversion [toHijriOrNull] already uses, not local moon-sighting
 * announcements, so a date here can land a day off from what's observed in some countries.
 * Mawlid al-Nabi and Laylat al-Qadr in particular are given on the most widely used date;
 * some traditions mark them differently.
 */
enum class IslamicEvent {
    ISLAMIC_NEW_YEAR,
    ASHURA,
    MAWLID_AL_NABI,
    ISRA_AND_MIRAJ,
    RAMADAN_START,
    LAYLAT_AL_QADR,
    EID_AL_FITR,
    ARAFAH_DAY,
    EID_AL_ADHA,
}

private data class HijriMonthDay(val month: Int, val day: Int)

private val ISLAMIC_EVENTS_BY_HIJRI_DATE: Map<HijriMonthDay, IslamicEvent> = mapOf(
    HijriMonthDay(1, 1)   to IslamicEvent.ISLAMIC_NEW_YEAR,
    HijriMonthDay(1, 10)  to IslamicEvent.ASHURA,
    HijriMonthDay(3, 12)  to IslamicEvent.MAWLID_AL_NABI,
    HijriMonthDay(7, 27)  to IslamicEvent.ISRA_AND_MIRAJ,
    HijriMonthDay(9, 1)   to IslamicEvent.RAMADAN_START,
    HijriMonthDay(9, 27)  to IslamicEvent.LAYLAT_AL_QADR,
    HijriMonthDay(10, 1)  to IslamicEvent.EID_AL_FITR,
    HijriMonthDay(12, 9)  to IslamicEvent.ARAFAH_DAY,
    HijriMonthDay(12, 10) to IslamicEvent.EID_AL_ADHA,
)

/** The Islamic event (if any) that falls on [this] Hijri date. */
fun HijriDate.islamicEventOrNull(): IslamicEvent? = ISLAMIC_EVENTS_BY_HIJRI_DATE[HijriMonthDay(month, day)]
