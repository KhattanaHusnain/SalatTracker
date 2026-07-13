package com.salattracker.data.remote.api

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

// ── Quran.com API v4 response ─────────────────────────────────────────────────

data class QuranVerseResponse(val verse: QuranVerseDto)

data class QuranVerseDto(
    val verse_key: String,
    val translations: List<QuranTranslationDto> = emptyList(),
)

data class QuranTranslationDto(val text: String)

// ── Retrofit service ──────────────────────────────────────────────────────────

interface QuranApiService {
    /**
     * GET https://api.quran.com/api/v4/verses/by_key/2:238?translations=20
     * translations=20 is Saheeh International — a widely used, mainstream English translation.
     */
    @GET("verses/by_key/{verseKey}")
    suspend fun getVerse(
        @Path("verseKey") verseKey: String,
        @Query("translations") translationId: Int = 20,
    ): QuranVerseResponse
}
