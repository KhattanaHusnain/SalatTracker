package com.salattracker.domain.model

/** A bookmarked verse ([fromVerse] == [toVerse]) or verse range, with an optional personal note. */
data class QuranBookmark(
    val id: Long = 0,
    val chapterNumber: Int,
    val fromVerse: Int,
    val toVerse: Int,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
) {
    val isRange: Boolean get() = toVerse > fromVerse
}

/** One "the user read this verse" event. The most recent row is also "continue reading" — see
 * [com.salattracker.domain.repository.QuranUserDataRepository.observeLastRead]. */
data class QuranReadHistoryEntry(
    val id: Long = 0,
    val chapterNumber: Int,
    val verseNumber: Int,
    val juzNumber: Int? = null,
    val pageNumber: Int? = null,
    val readAt: Long = System.currentTimeMillis(),
)

data class QuranSearchHistoryEntry(
    val id: Long = 0,
    val query: String,
    val searchedAt: Long = System.currentTimeMillis(),
)

/**
 * A downloadable content pack's catalog metadata plus this device's download state. [isDownloaded]
 * and [localPath]/[downloadedAt] are the only fields this device changes locally — everything else
 * is catalog metadata, synced in from a remote manifest (the actual source/fetch mechanism is a
 * later phase; this repository only stores and serves whatever catalog it's given).
 */
data class QuranContentPack(
    val packId: String,
    val type: QuranContentPackType,
    val language: String,
    val title: String,
    val author: String? = null,
    val sizeBytes: Long,
    val sourceUrl: String,
    val version: Int,
    val isDownloaded: Boolean = false,
    val localPath: String? = null,
    val downloadedAt: Long? = null,
)
