package com.salattracker.data.remote

import com.salattracker.data.remote.api.OverpassCenter
import com.salattracker.data.remote.api.OverpassElement
import com.salattracker.data.remote.api.OverpassResponse
import com.salattracker.data.remote.api.buildNearbyMosquesQuery
import com.salattracker.data.remote.api.toNearbyMosques
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OverpassMappingTest {

    private val originLat = 24.0
    private val originLon = 39.0

    @Test
    fun `query rounds the coordinates to three decimals and uses a dot separator`() {
        val query = buildNearbyMosquesQuery(24.46721, 39.61119)

        assertTrue(query, query.contains("around:2000,24.467,39.611"))
        assertTrue(query, query.contains("\"religion\"=\"muslim\""))
        assertTrue(query, query.endsWith("out center tags;"))
    }

    @Test
    fun `a node uses its own coordinates and a way uses its centre`() {
        val response = OverpassResponse(
            listOf(
                OverpassElement(type = "node", lat = 24.001, lon = 39.0, tags = mapOf("name" to "Node Mosque")),
                OverpassElement(type = "way", center = OverpassCenter(24.002, 39.0), tags = mapOf("name" to "Way Mosque")),
            )
        )

        val result = response.toNearbyMosques(originLat, originLon)

        assertEquals(listOf("Node Mosque", "Way Mosque"), result.map { it.name })
        assertEquals(24.002, result[1].latitude, 0.0)
    }

    @Test
    fun `name falls back to the English name and then to null`() {
        val response = OverpassResponse(
            listOf(
                OverpassElement(lat = 24.001, lon = 39.0, tags = mapOf("name:en" to "English Only")),
                OverpassElement(lat = 24.002, lon = 39.0, tags = mapOf("name" to "  ")),
                OverpassElement(lat = 24.003, lon = 39.0, tags = null),
            )
        )

        val result = response.toNearbyMosques(originLat, originLon)

        assertEquals("English Only", result[0].name)
        assertNull(result[1].name)
        assertNull(result[2].name)
    }

    @Test
    fun `elements without coordinates are dropped`() {
        val response = OverpassResponse(
            listOf(
                OverpassElement(type = "relation", tags = mapOf("name" to "No Geometry")),
                OverpassElement(lat = 24.001, lon = 39.0, tags = mapOf("name" to "Has Geometry")),
            )
        )

        assertEquals(listOf("Has Geometry"), response.toNearbyMosques(originLat, originLon).map { it.name })
    }

    @Test
    fun `results are sorted nearest first with rounded distances`() {
        val response = OverpassResponse(
            listOf(
                OverpassElement(lat = 24.010, lon = 39.0, tags = mapOf("name" to "Far")),
                OverpassElement(lat = 24.001, lon = 39.0, tags = mapOf("name" to "Near")),
            )
        )

        val result = response.toNearbyMosques(originLat, originLon)

        assertEquals(listOf("Near", "Far"), result.map { it.name })
        assertEquals(111, result[0].distanceMeters)
    }

    @Test
    fun `a mosque mapped as both a node and a building appears once`() {
        val response = OverpassResponse(
            listOf(
                OverpassElement(type = "node", lat = 24.0010, lon = 39.0, tags = mapOf("name" to "Masjid Quba")),
                OverpassElement(type = "way", center = OverpassCenter(24.00102, 39.0), tags = mapOf("name" to "masjid quba")),
            )
        )

        assertEquals(1, response.toNearbyMosques(originLat, originLon).size)
    }

    @Test
    fun `two different mosques at the same spot are both kept`() {
        val response = OverpassResponse(
            listOf(
                OverpassElement(lat = 24.0010, lon = 39.0, tags = mapOf("name" to "Masjid A")),
                OverpassElement(lat = 24.0010, lon = 39.0, tags = mapOf("name" to "Masjid B")),
            )
        )

        assertEquals(2, response.toNearbyMosques(originLat, originLon).size)
    }

    @Test
    fun `an empty or missing element list gives no results`() {
        assertTrue(OverpassResponse(null).toNearbyMosques(originLat, originLon).isEmpty())
        assertTrue(OverpassResponse(emptyList()).toNearbyMosques(originLat, originLon).isEmpty())
    }
}
