package com.salattracker.domain.repository

import com.salattracker.domain.model.QuranBookmark
import com.salattracker.domain.model.QuranContentPack
import com.salattracker.domain.model.QuranReadHistoryEntry
import com.salattracker.domain.model.QuranSearchHistoryEntry
import kotlinx.coroutines.flow.Flow

/**
 * Per-device Quran user data: bookmarks, reading history (which also answers "where did I leave
 * off"), search history, and which downloadable content packs are on this device. The Quran text
 * itself, and the translations/tafsirs those packs unlock, are not here — see the content
 * repository added alongside the reading UI in a later phase.
 */
interface QuranUserDataRepository {
    // Bookmarks
    fun observeBookmarks(): Flow<List<QuranBookmark>>
    suspend fun addBookmark(bookmark: QuranBookmark): Long
    suspend fun updateBookmark(bookmark: QuranBookmark)
    suspend fun deleteBookmark(id: Long)

    // Read history / "continue reading"
    fun observeRecentReads(limit: Int = 50): Flow<List<QuranReadHistoryEntry>>

    /** The single most recent read, i.e. where "Continue reading" resumes. Null once no read has ever been recorded. */
    fun observeLastRead(): Flow<QuranReadHistoryEntry?>
    suspend fun recordRead(chapterNumber: Int, verseNumber: Int)
    suspend fun clearReadHistory()

    // Search history
    fun observeRecentSearches(limit: Int = 20): Flow<List<QuranSearchHistoryEntry>>
    suspend fun recordSearch(query: String)
    suspend fun clearSearchHistory()

    // Downloaded content-pack registry
    fun observeContentPacks(): Flow<List<QuranContentPack>>
    fun observeDownloadedContentPacks(): Flow<List<QuranContentPack>>
    suspend fun getContentPack(packId: String): QuranContentPack?
    suspend fun upsertContentPackCatalog(packs: List<QuranContentPack>)
    suspend fun markContentPackDownloaded(packId: String, localPath: String)
    suspend fun markContentPackRemoved(packId: String)
}
