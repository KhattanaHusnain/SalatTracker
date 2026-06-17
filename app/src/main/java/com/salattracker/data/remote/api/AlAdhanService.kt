package com.salattracker.data.remote.api

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

// ── AlAdhan API response ──────────────────────────────────────────────────────

data class AlAdhanResponse(val code: Int = 0, val data: AlAdhanData? = null)
data class AlAdhanData(val timings: Map<String, String> = emptyMap())

// ── Retrofit service ──────────────────────────────────────────────────────────

interface AlAdhanService {
    /**
     * GET https://api.aladhan.com/v1/timings/2024-01-22?latitude=24.46&longitude=39.61&method=3
     */
    @GET("v1/timings/{date}")
    suspend fun getTimings(
        @Path("date")      date: String,        // "yyyy-MM-dd"
        @Query("latitude") latitude: Double,
        @Query("longitude")longitude: Double,
        @Query("method")   method: Int,
    ): AlAdhanResponse
}