package com.salattracker.data.local.quran

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

// ── Content-pack entities ──────────────────────────────────────────────────────
// Each downloaded pack is its OWN single-table SQLite file (one translation = one file, one
// tafsir = one file, ...), opened dynamically by path — see QuranPackDatabases.kt. A pack file
// never contains more than its own table, so each gets its own @Database class rather than one
// shared schema every pack would have to match.

@Entity(
    tableName   = "translation_texts",
    primaryKeys = ["chapter_number", "verse_number"],
)
data class TranslationTextEntity(
    @ColumnInfo(name = "chapter_number") val chapterNumber: Int,
    @ColumnInfo(name = "verse_number")   val verseNumber: Int,
    @ColumnInfo(name = "text")           val text: String,
)

@Entity(tableName = "tafsir_texts")
data class TafsirTextEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")             val id: Long = 0,
    @ColumnInfo(name = "chapter_number") val chapterNumber: Int,
    @ColumnInfo(name = "from_verse")     val fromVerse: Int,
    @ColumnInfo(name = "to_verse")       val toVerse: Int,
    @ColumnInfo(name = "text")           val text: String,
)

@Entity(
    tableName   = "word_by_word_translations",
    primaryKeys = ["chapter_number", "verse_number", "position"],
)
data class WordByWordTranslationEntity(
    @ColumnInfo(name = "chapter_number") val chapterNumber: Int,
    @ColumnInfo(name = "verse_number")   val verseNumber: Int,
    @ColumnInfo(name = "position")       val position: Int,
    @ColumnInfo(name = "text")           val text: String,
)
