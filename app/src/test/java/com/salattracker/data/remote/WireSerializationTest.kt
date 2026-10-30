package com.salattracker.data.remote

import com.google.gson.Gson
import com.salattracker.data.remote.api.DriveAboutResponse
import com.salattracker.data.remote.api.DriveFileListResponse
import com.salattracker.data.remote.api.OverpassResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WireSerializationTest {
    private val gson = Gson()

    @Test
    fun `drive responses use stable Google API field names`() {
        val about = gson.fromJson(
            """{"user":{"emailAddress":"reader@example.com","displayName":"Reader"}}""",
            DriveAboutResponse::class.java,
        )
        val files = gson.fromJson(
            """{"files":[{"id":"file-1","size":"42","md5Checksum":"abc","appProperties":{"revisionId":"r1"}}]}""",
            DriveFileListResponse::class.java,
        )

        assertEquals("reader@example.com", about.user?.emailAddress)
        assertEquals("Reader", about.user?.displayName)
        assertEquals("file-1", files.files?.single()?.id)
        assertEquals("r1", files.files?.single()?.appProperties?.get("revisionId"))
        assertTrue(gson.toJson(files).contains("\"md5Checksum\""))
    }

    @Test
    fun `overpass responses retain nested geometry and tags`() {
        val response = gson.fromJson(
            """{"elements":[{"type":"way","id":7,"center":{"lat":24.1,"lon":39.2},"tags":{"name":"Masjid"}}]}""",
            OverpassResponse::class.java,
        )

        val element = response.elements?.single()
        assertEquals("way", element?.type)
        assertEquals(24.1, element?.center?.lat ?: 0.0, 0.0)
        assertEquals("Masjid", element?.tags?.get("name"))
    }

}
