package com.salattracker.data.local.quran

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * The only Quran content bundled in the APK: base Arabic (Uthmani) text for all 114 chapters,
 * chapter metadata, the static juz/hizb/rub/page facts every verse carries, and the Mushaf
 * page/line/glyph data the paginated reader needs (QCF V2, all 604 pages — see
 * [MushafWordEntity] and QuranSeedWorker). Everything else — translations, tafsirs, word-by-word
 * glosses, recitation audio — is a downloadable content pack, opened on demand from its own
 * database file (see [QuranTranslationPackDatabase] and friends), not part of this one.
 *
 * Seeded from bundled assets once those assets exist (see the content-sourcing phase); until
 * then this opens as an empty database, which is harmless since nothing reads from it yet.
 */
@Database(
    entities = [
        ChapterEntity::class,
        VerseEntity::class,
        WordSegmentEntity::class,
        MushafWordEntity::class,
    ],
    version  = 2,
    exportSchema = true,
)
@TypeConverters(QuranCoreConverters::class)
abstract class QuranCoreDatabase : RoomDatabase() {
    abstract fun chapterDao(): ChapterDao
    abstract fun verseDao(): QuranVerseDao
    abstract fun wordSegmentDao(): WordSegmentDao
    abstract fun mushafWordDao(): MushafWordDao
}
