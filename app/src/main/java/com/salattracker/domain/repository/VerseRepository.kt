package com.salattracker.domain.repository

import com.salattracker.domain.model.VerseInfo

interface VerseRepository {
    /** Reads from the locally-seeded Quran data — instant, fully offline, no cache or network needed. */
    suspend fun getRandomVerse(): VerseInfo
}
