package com.salattracker.data.local.quran

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.salattracker.domain.model.RevelationPlace

// ── Converters ────────────────────────────────────────────────────────────────

class QuranCoreConverters {
    @TypeConverter fun fromRevelationPlace(p: RevelationPlace): String = p.name
    @TypeConverter fun toRevelationPlace(s: String): RevelationPlace   = RevelationPlace.valueOf(s)
}

// ── Chapter (surah) ───────────────────────────────────────────────────────────

@Entity(tableName = "chapters")
data class ChapterEntity(
    @PrimaryKey
    @ColumnInfo(name = "number")               val number: Int,
    @ColumnInfo(name = "name_arabic")          val nameArabic: String,
    @ColumnInfo(name = "name_transliteration") val nameTransliteration: String,
    @ColumnInfo(name = "name_translation")     val nameTranslation: String,
    @ColumnInfo(name = "verses_count")         val versesCount: Int,
    @ColumnInfo(name = "revelation_place")     val revelationPlace: RevelationPlace,
    @ColumnInfo(name = "revelation_order")     val revelationOrder: Int,
)

// ── Verse — base Arabic text (Uthmani) + static per-verse facts ───────────────

@Entity(
    tableName   = "verses",
    primaryKeys = ["chapter_number", "verse_number"],
)
data class VerseEntity(
    @ColumnInfo(name = "chapter_number") val chapterNumber: Int,
    @ColumnInfo(name = "verse_number")   val verseNumber: Int,
    @ColumnInfo(name = "text_uthmani")   val textUthmani: String,
    @ColumnInfo(name = "juz_number")     val juzNumber: Int,
    @ColumnInfo(name = "hizb_number")    val hizbNumber: Int,
    @ColumnInfo(name = "rub_number")     val rubNumber: Int,
    @ColumnInfo(name = "page_number")    val pageNumber: Int,
    @ColumnInfo(name = "has_sajda", defaultValue = "0") val hasSajda: Boolean = false,
)

// ── Word segment — base Arabic word-by-word split, language-independent ───────

@Entity(
    tableName   = "word_segments",
    primaryKeys = ["chapter_number", "verse_number", "position"],
)
data class WordSegmentEntity(
    @ColumnInfo(name = "chapter_number") val chapterNumber: Int,
    @ColumnInfo(name = "verse_number")   val verseNumber: Int,
    @ColumnInfo(name = "position")       val position: Int,
    @ColumnInfo(name = "text_arabic")    val textArabic: String,
)

// ── Mushaf word — QCF glyph code + line placement for the print-accurate paginated reader ─────
// Bundled, not fetched: [orderIndex] (not [position], which restarts at 1 per verse) preserves
// the original "verse order, then word order" reading sequence so a page can be reconstructed
// exactly even on a line that mixes words from two verses — see MushafRepositoryImpl.

@Entity(
    tableName   = "mushaf_words",
    primaryKeys = ["page_number", "edition", "order_index"],
)
data class MushafWordEntity(
    @ColumnInfo(name = "page_number")    val pageNumber: Int,
    @ColumnInfo(name = "edition")        val edition: Int,
    @ColumnInfo(name = "order_index")    val orderIndex: Int,
    @ColumnInfo(name = "chapter_number") val chapterNumber: Int,
    @ColumnInfo(name = "verse_number")   val verseNumber: Int,
    @ColumnInfo(name = "position")       val position: Int,
    @ColumnInfo(name = "line_number")    val lineNumber: Int,
    @ColumnInfo(name = "glyph_code")     val glyphCode: String,
    @ColumnInfo(name = "is_verse_end")   val isVerseEndMarker: Boolean,
)
