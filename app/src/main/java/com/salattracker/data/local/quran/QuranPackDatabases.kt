package com.salattracker.data.local.quran

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [TranslationTextEntity::class], version = 1, exportSchema = true)
abstract class QuranTranslationPackDatabase : RoomDatabase() {
    abstract fun translationTextDao(): TranslationTextDao
}

@Database(entities = [TafsirTextEntity::class], version = 1, exportSchema = true)
abstract class QuranTafsirPackDatabase : RoomDatabase() {
    abstract fun tafsirTextDao(): TafsirTextDao
}

@Database(entities = [WordByWordTranslationEntity::class], version = 1, exportSchema = true)
abstract class QuranWordByWordPackDatabase : RoomDatabase() {
    abstract fun wordByWordTranslationDao(): WordByWordTranslationDao
}

/**
 * Opens a single downloaded content-pack file by its on-disk path (see
 * [com.salattracker.domain.model.QuranContentPack.localPath]). Each pack file is its own
 * single-table SQLite database, so there's no shared instance to inject — the caller
 * ([com.salattracker.data.repository.QuranContentRepositoryImpl]) opens one per pack it needs
 * and is responsible for closing it when done.
 *
 * `fallbackToDestructiveMigration` is safe here in a way it wouldn't be for [QuranCoreDatabase]
 * or the main app database: a pack file holds nothing the user created — a schema mismatch just
 * means re-downloading it, not losing data.
 */
object QuranPackDatabases {
    fun openTranslation(context: Context, path: String): QuranTranslationPackDatabase =
        Room.databaseBuilder(context, QuranTranslationPackDatabase::class.java, path)
            .fallbackToDestructiveMigration()
            .build()

    fun openTafsir(context: Context, path: String): QuranTafsirPackDatabase =
        Room.databaseBuilder(context, QuranTafsirPackDatabase::class.java, path)
            .fallbackToDestructiveMigration()
            .build()

    fun openWordByWord(context: Context, path: String): QuranWordByWordPackDatabase =
        Room.databaseBuilder(context, QuranWordByWordPackDatabase::class.java, path)
            .fallbackToDestructiveMigration()
            .build()
}
