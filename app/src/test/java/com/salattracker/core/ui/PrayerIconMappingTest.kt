package com.salattracker.core.ui

import com.salattracker.R
import com.salattracker.domain.model.PrayerType
import org.junit.Assert.assertEquals
import org.junit.Test

class PrayerIconMappingTest {

    @Test
    fun `every prayer uses its standard home icon`() {
        val expected = mapOf(
            PrayerType.FAJR to R.drawable.home_prayer_fajr,
            PrayerType.DHUHR to R.drawable.home_prayer_dhuhr,
            PrayerType.ASR to R.drawable.home_prayer_asr,
            PrayerType.MAGHRIB to R.drawable.home_prayer_maghrib,
            PrayerType.ISHA to R.drawable.home_prayer_isha,
        )

        expected.forEach { (prayer, drawable) ->
            assertEquals(prayer.name, drawable, prayer.iconRes)
        }
    }
}
