package com.salattracker.data.repository

import com.salattracker.data.local.dao.MosqueDao
import com.salattracker.data.local.entity.MosqueEntity
import com.salattracker.data.remote.api.OverpassService
import com.salattracker.data.remote.api.buildNearbyMosquesQuery
import com.salattracker.data.remote.api.toNearbyMosques
import com.salattracker.domain.model.Mosque
import com.salattracker.domain.model.NearbyMosque
import com.salattracker.domain.repository.MosqueRepository
import android.util.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MosqueRepositoryImpl @Inject constructor(
    private val mosqueDao: MosqueDao,
    private val overpassService: OverpassService,
) : MosqueRepository {

    override fun observeMosques(): Flow<List<Mosque>> = mosqueDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getMosques(): List<Mosque> = mosqueDao.getAll().map { it.toDomain() }

    override suspend fun addMosque(mosque: Mosque): Long = mosqueDao.insert(mosque.toEntity().copy(id = 0))

    override suspend fun updateMosque(mosque: Mosque) = mosqueDao.update(mosque.toEntity())

    override suspend fun deleteMosque(id: Long) = mosqueDao.delete(id)

    override suspend fun findNearby(latitude: Double, longitude: Double): List<NearbyMosque> {
        val query = buildNearbyMosquesQuery(latitude, longitude)
        Log.d(TAG, "findNearby: query=$query")
        val response = queryWithRetry(query)
        val elementCount = response.elements?.size ?: -1
        val results = response.toNearbyMosques(latitude, longitude)
        Log.i(TAG, "findNearby: rawElements=$elementCount mapped=${results.size}")
        return results
    }

    // Overpass's own public instance is a shared, often-saturated resource; a 502/503/504 "busy,
    // try again" is routine rather than exceptional, and Overpass's own error page recommends
    // exactly this: wait, then retry with exponential backoff.
    private suspend fun queryWithRetry(query: String) = run {
        var lastError: Exception? = null
        for (attempt in 0..RETRY_DELAYS_MS.size) {
            try {
                return@run overpassService.query(query)
            } catch (e: HttpException) {
                if (e.code() !in RETRYABLE_HTTP_CODES || attempt == RETRY_DELAYS_MS.size) {
                    Log.e(TAG, "findNearby: request failed (HTTP ${e.code()})", e)
                    throw e
                }
                lastError = e
            } catch (e: Exception) {
                Log.e(TAG, "findNearby: request failed", e)
                throw e
            }
            val delayMs = RETRY_DELAYS_MS[attempt]
            Log.w(TAG, "findNearby: HTTP ${(lastError as? HttpException)?.code()}, retrying in ${delayMs}ms")
            delay(delayMs)
        }
        @Suppress("UNREACHABLE_CODE") throw lastError!!
    }

    private companion object {
        const val TAG = "MosqueMode"
        val RETRYABLE_HTTP_CODES = setOf(502, 503, 504)
        val RETRY_DELAYS_MS = longArrayOf(1_500, 3_000)
    }
}

private fun MosqueEntity.toDomain() = Mosque(id, name, latitude, longitude, radiusMeters)

private fun Mosque.toEntity() = MosqueEntity(id, name, latitude, longitude, radiusMeters)
