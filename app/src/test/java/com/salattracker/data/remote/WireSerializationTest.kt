package com.salattracker.data.remote

import com.google.gson.Gson
import com.salattracker.data.remote.api.DriveAboutResponse
import com.salattracker.data.remote.api.DriveFileListResponse
import com.salattracker.data.remote.api.OverpassResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Array
import java.nio.file.Files
import java.nio.file.Path

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

    @Test
    fun `Quran asset models retain bundled JSON field names`() {
        val chapter = parseQuranAsset(
            "ChapterAsset",
            """{"number":1,"nameArabic":"الفاتحة","nameTransliteration":"Al-Fatihah","nameTranslation":"The Opening","versesCount":7,"revelationPlace":"MECCAN","revelationOrder":5}""",
        )
        val verse = parseQuranAsset(
            "VerseAsset",
            """{"chapterNumber":1,"verseNumber":1,"textUthmani":"بسم الله","juzNumber":1,"hizbNumber":1,"rubNumber":1,"pageNumber":1,"hasSajda":false}""",
        )
        val translation = parseQuranAsset(
            "TranslationAsset",
            """{"chapterNumber":1,"verseNumber":1,"text":"In the name of Allah"}""",
        )
        val word = parseQuranAsset(
            "MushafWordAsset",
            """{"pageNumber":1,"chapterNumber":1,"verseNumber":1,"position":1,"lineNumber":1,"glyphCode":"ﭐ","isVerseEndMarker":false}""",
        )

        assertEquals("Al-Fatihah", chapter.field("nameTransliteration"))
        assertEquals(1, verse.field("pageNumber"))
        assertEquals("In the name of Allah", translation.field("text"))
        assertEquals("ﭐ", word.field("glyphCode"))
        assertTrue(gson.toJson(word).contains("\"isVerseEndMarker\""))
    }

    @Test
    fun `bundled Quran seed files deserialize with the production wire models`() {
        val assets = Path.of("src", "main", "assets", "quran")
        listOf(
            assets.resolve("chapters.json") to "ChapterAsset",
            assets.resolve("verses.json") to "VerseAsset",
            assets.resolve("translations/en_sahih.json") to "TranslationAsset",
            assets.resolve("translations/ur_jalandhry.json") to "TranslationAsset",
            assets.resolve("mushaf_words.json") to "MushafWordAsset",
        ).forEach { (path, className) ->
            assertTrue("Missing bundled fixture: $path", Files.isRegularFile(path))
            val modelClass = Class.forName("com.salattracker.notification.$className")
            val arrayClass = Array.newInstance(modelClass, 0).javaClass
            Files.newBufferedReader(path).use { reader ->
                val records = gson.fromJson<Any>(reader, arrayClass)
                assertTrue("Empty bundled fixture: $path", Array.getLength(records) > 0)
            }
        }
    }

    private fun parseQuranAsset(simpleName: String, json: String): Any =
        gson.fromJson(json, Class.forName("com.salattracker.notification.$simpleName"))

    private fun Any.field(name: String): Any? = javaClass.getDeclaredField(name).let { field ->
        field.isAccessible = true
        field.get(this)
    }
}
