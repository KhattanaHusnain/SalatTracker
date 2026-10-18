package com.salattracker.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoDistanceTest {

    @Test
    fun `the same point is zero meters away`() {
        assertEquals(0.0, distanceMeters(24.4672, 39.6111, 24.4672, 39.6111), 0.001)
    }

    @Test
    fun `one degree of longitude on the equator is about 111 km`() {
        assertEquals(111_195.0, distanceMeters(0.0, 0.0, 0.0, 1.0), 100.0)
    }

    @Test
    fun `Makkah to Madinah is about 339 km`() {
        val meters = distanceMeters(21.4225, 39.8262, 24.4672, 39.6111)
        assertTrue("was $meters", meters in 337_000.0..341_000.0)
    }

    @Test
    fun `distance is symmetric`() {
        assertEquals(
            distanceMeters(24.0, 39.0, 24.01, 39.02),
            distanceMeters(24.01, 39.02, 24.0, 39.0),
            0.001,
        )
    }
}
