package com.salattracker.domain.repository

import com.salattracker.domain.model.VerseInfo

interface VerseRepository {

    /** Reads only from the local cache — instant, no network wait. Never empty after the first successful refresh. */
    suspend fun getRandomVerse(): VerseInfo

    /** Fetches any curated verses not yet cached. Safe to call repeatedly — skips ones already cached. */
    suspend fun refreshCache()
}
