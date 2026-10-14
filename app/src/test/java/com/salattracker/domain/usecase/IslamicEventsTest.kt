package com.salattracker.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IslamicEventsTest {

    @Test
    fun `known Hijri dates map to their event`() {
        assertEquals(IslamicEvent.ISLAMIC_NEW_YEAR, HijriDate(1, 1, 1448).islamicEventOrNull())
        assertEquals(IslamicEvent.ASHURA, HijriDate(10, 1, 1448).islamicEventOrNull())
        assertEquals(IslamicEvent.MAWLID_AL_NABI, HijriDate(12, 3, 1448).islamicEventOrNull())
        assertEquals(IslamicEvent.ISRA_AND_MIRAJ, HijriDate(27, 7, 1448).islamicEventOrNull())
        assertEquals(IslamicEvent.RAMADAN_START, HijriDate(1, 9, 1448).islamicEventOrNull())
        assertEquals(IslamicEvent.LAYLAT_AL_QADR, HijriDate(27, 9, 1448).islamicEventOrNull())
        assertEquals(IslamicEvent.EID_AL_FITR, HijriDate(1, 10, 1448).islamicEventOrNull())
        assertEquals(IslamicEvent.ARAFAH_DAY, HijriDate(9, 12, 1448).islamicEventOrNull())
        assertEquals(IslamicEvent.EID_AL_ADHA, HijriDate(10, 12, 1448).islamicEventOrNull())
    }

    @Test
    fun `the year never matters, only month and day`() {
        assertEquals(IslamicEvent.EID_AL_FITR, HijriDate(1, 10, 1300).islamicEventOrNull())
        assertEquals(IslamicEvent.EID_AL_FITR, HijriDate(1, 10, 1500).islamicEventOrNull())
    }

    @Test
    fun `an ordinary day has no event`() {
        assertNull(HijriDate(15, 6, 1448).islamicEventOrNull())
        assertNull(HijriDate(2, 1, 1448).islamicEventOrNull())
    }
}
