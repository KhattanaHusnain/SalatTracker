package com.salattracker.domain.repository

import com.salattracker.domain.model.VerseInfo

interface VerseRepository {
    /** The date-stable, locally-seeded verse of the day, or null until Quran content is ready. */
    suspend fun getVerseOfDay(): VerseInfo?
}
