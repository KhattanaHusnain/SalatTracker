package com.salattracker.data.remote

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.gson.Gson
import com.salattracker.data.remote.api.DriveAboutResponse
import com.salattracker.data.remote.api.DriveFileListResponse
import com.salattracker.data.remote.api.OverpassResponse
import com.salattracker.notification.QuranSeedAssetReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Run against the shrunk, isolated target with:
 * `gradlew -PsalatTestBuildType=optimized connectedOptimizedAndroidTest`
 *
 * The optimized target uses a separate application ID, so this suite cannot modify an installed
 * production build's data, notification channels, or scheduled work.
 */
@RunWith(AndroidJUnit4::class)
class OptimizedWireSerializationTest {
    private val gson = Gson()

    @Test
    fun driveAndOverpassFixturesDeserializeThroughOptimizedModels() {
        val about = gson.fromJson(
            """{"user":{"emailAddress":"reader@example.com","displayName":"Reader"}}""",
            DriveAboutResponse::class.java,
        )
        val files = gson.fromJson(
            """{"files":[{"id":"file-1","size":"42","md5Checksum":"abc","appProperties":{"revisionId":"r1"}}]}""",
            DriveFileListResponse::class.java,
        )
        val overpass = gson.fromJson(
            """{"elements":[{"type":"way","id":7,"center":{"lat":24.1,"lon":39.2},"tags":{"name":"Masjid"}}]}""",
            OverpassResponse::class.java,
        )

        assertEquals("reader@example.com", about.user?.emailAddress)
        assertEquals("file-1", files.files?.single()?.id)
        assertEquals("r1", files.files?.single()?.appProperties?.get("revisionId"))
        assertEquals("Masjid", overpass.elements?.single()?.tags?.get("name"))
    }

    @Test
    fun packagedQuranFixturesDeserializeThroughOptimizedSeedReader() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val reader = QuranSeedAssetReader(context)

        val chapters = reader.chapters()
        val verses = reader.verses()
        val english = reader.englishTranslation()
        val urdu = reader.urduTranslation()
        val words = reader.mushafWords()

        assertEquals(114, chapters.size)
        assertEquals(6_236, verses.size)
        assertEquals(6_236, english.size)
        assertEquals(6_236, urdu.size)
        assertFalse(words.isEmpty())
        assertEquals(1, words.first().pageNumber)
    }
}
