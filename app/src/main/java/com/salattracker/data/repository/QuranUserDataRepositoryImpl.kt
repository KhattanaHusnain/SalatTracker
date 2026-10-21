package com.salattracker.data.repository

import com.salattracker.data.local.dao.QuranBookmarkDao
import com.salattracker.data.local.dao.QuranContentPackDao
import com.salattracker.data.local.dao.QuranReadHistoryDao
import com.salattracker.data.local.dao.QuranSearchHistoryDao
import com.salattracker.data.local.entity.QuranBookmarkEntity
import com.salattracker.data.local.entity.QuranContentPackEntity
import com.salattracker.data.local.entity.QuranReadHistoryEntity
import com.salattracker.data.local.entity.QuranSearchHistoryEntity
import com.salattracker.domain.model.QuranBookmark
import com.salattracker.domain.model.QuranContentPack
import com.salattracker.domain.model.QuranReadHistoryEntry
import com.salattracker.domain.model.QuranSearchHistoryEntry
import com.salattracker.domain.repository.QuranUserDataRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuranUserDataRepositoryImpl @Inject constructor(
    private val bookmarkDao: QuranBookmarkDao,
    private val readHistoryDao: QuranReadHistoryDao,
    private val searchHistoryDao: QuranSearchHistoryDao,
    private val contentPackDao: QuranContentPackDao,
) : QuranUserDataRepository {

    override fun observeBookmarks(): Flow<List<QuranBookmark>> =
        bookmarkDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun addBookmark(bookmark: QuranBookmark): Long = bookmarkDao.insert(bookmark.toEntity().copy(id = 0))

    override suspend fun updateBookmark(bookmark: QuranBookmark) = bookmarkDao.update(bookmark.toEntity())

    override suspend fun deleteBookmark(id: Long) = bookmarkDao.delete(id)

    override fun observeRecentReads(limit: Int): Flow<List<QuranReadHistoryEntry>> =
        readHistoryDao.observeRecent(limit).map { list -> list.map { it.toDomain() } }

    override fun observeLastRead(): Flow<QuranReadHistoryEntry?> =
        readHistoryDao.observeLast().map { it?.toDomain() }

    override suspend fun recordRead(chapterNumber: Int, verseNumber: Int, juzNumber: Int?, pageNumber: Int?) {
        readHistoryDao.insert(
            QuranReadHistoryEntity(
                chapterNumber = chapterNumber,
                verseNumber = verseNumber,
                juzNumber = juzNumber,
                pageNumber = pageNumber,
            )
        )
    }

    override suspend fun clearReadHistory() = readHistoryDao.clear()

    override fun observeRecentSearches(limit: Int): Flow<List<QuranSearchHistoryEntry>> =
        searchHistoryDao.observeRecent(limit).map { list -> list.map { it.toDomain() } }

    override suspend fun recordSearch(query: String) {
        searchHistoryDao.insert(QuranSearchHistoryEntity(query = query))
    }

    override suspend fun clearSearchHistory() = searchHistoryDao.clear()

    override fun observeContentPacks(): Flow<List<QuranContentPack>> =
        contentPackDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeDownloadedContentPacks(): Flow<List<QuranContentPack>> =
        contentPackDao.observeDownloaded().map { list -> list.map { it.toDomain() } }

    override suspend fun getContentPack(packId: String): QuranContentPack? = contentPackDao.get(packId)?.toDomain()

    override suspend fun upsertContentPackCatalog(packs: List<QuranContentPack>) =
        contentPackDao.upsertAll(packs.map { it.toEntity() })

    override suspend fun markContentPackDownloaded(packId: String, localPath: String) =
        contentPackDao.markDownloaded(packId, localPath, System.currentTimeMillis())

    override suspend fun markContentPackRemoved(packId: String) = contentPackDao.markRemoved(packId)
}

private fun QuranBookmarkEntity.toDomain() = QuranBookmark(id, chapterNumber, fromVerse, toVerse, note, createdAt)
private fun QuranBookmark.toEntity() = QuranBookmarkEntity(id, chapterNumber, fromVerse, toVerse, note, createdAt)

private fun QuranReadHistoryEntity.toDomain() = QuranReadHistoryEntry(
    id, chapterNumber, verseNumber, juzNumber, pageNumber, readAt,
)

private fun QuranSearchHistoryEntity.toDomain() = QuranSearchHistoryEntry(id, query, searchedAt)

private fun QuranContentPackEntity.toDomain() = QuranContentPack(
    packId, type, language, title, author, sizeBytes, sourceUrl, version, isDownloaded, localPath, downloadedAt,
)
private fun QuranContentPack.toEntity() = QuranContentPackEntity(
    packId, type, language, title, author, sizeBytes, sourceUrl, version, isDownloaded, localPath, downloadedAt,
)
