package com.salattracker.di

import android.content.Context
import androidx.room.Room
import com.salattracker.data.local.quran.ChapterDao
import com.salattracker.data.local.quran.MushafWordDao
import com.salattracker.data.local.quran.QuranCoreDatabase
import com.salattracker.data.local.quran.QuranVerseDao
import com.salattracker.data.local.quran.WordSegmentDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** DI for [QuranCoreDatabase] — a separate Room database from [com.salattracker.data.local.db.SalatDatabase],
 * since it holds bundled Quran content rather than user data. Downloadable content-pack databases
 * (translations/tafsirs/word-by-word) aren't provided here: there can be many of them, opened on
 * demand by [com.salattracker.data.repository.QuranContentRepositoryImpl] rather than injected as
 * a single instance — see [com.salattracker.data.local.quran.QuranPackDatabases]. */
@Module
@InstallIn(SingletonComponent::class)
object QuranDatabaseModule {

    @Provides
    @Singleton
    fun provideQuranCoreDatabase(@ApplicationContext context: Context): QuranCoreDatabase =
        Room.databaseBuilder(context, QuranCoreDatabase::class.java, "quran_core.db")
            // Bundled, re-derivable content (re-shipped with the next app update), not
            // user-generated data — unlike SalatDatabase, a destructive fallback here costs
            // nothing a user would notice, so real Migration objects aren't needed.
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideChapterDao(db: QuranCoreDatabase): ChapterDao         = db.chapterDao()
    @Provides fun provideQuranVerseDao(db: QuranCoreDatabase): QuranVerseDao   = db.verseDao()
    @Provides fun provideWordSegmentDao(db: QuranCoreDatabase): WordSegmentDao = db.wordSegmentDao()
    @Provides fun provideMushafWordDao(db: QuranCoreDatabase): MushafWordDao   = db.mushafWordDao()
}
