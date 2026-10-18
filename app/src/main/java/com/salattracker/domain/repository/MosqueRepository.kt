package com.salattracker.domain.repository

import com.salattracker.domain.model.GeofenceSync
import com.salattracker.domain.model.Mosque
import com.salattracker.domain.model.MosqueModeState
import com.salattracker.domain.model.NearbyMosque
import com.salattracker.domain.model.RingerMode
import kotlinx.coroutines.flow.Flow

interface MosqueRepository {
    fun observeMosques(): Flow<List<Mosque>>
    suspend fun getMosques(): List<Mosque>
    suspend fun addMosque(mosque: Mosque): Long
    suspend fun updateMosque(mosque: Mosque)
    suspend fun deleteMosque(id: Long)

    /** Mosques within [com.salattracker.domain.model.MosqueMode.NEARBY_RADIUS_METERS], nearest first. Throws on network failure. */
    suspend fun findNearby(latitude: Double, longitude: Double): List<NearbyMosque>
}

interface MosqueModeStore {
    fun observeState(): Flow<MosqueModeState>
    suspend fun getState(): MosqueModeState
    suspend fun setEnabled(enabled: Boolean)
    suspend fun setVisit(insideIds: Set<Long>, restoreTo: RingerMode?, activeMosqueName: String?)
}

interface MosqueGeofencer {
    suspend fun register(mosques: List<Mosque>): GeofenceSync
    suspend fun clear()
}

interface RingerController {
    val canVibrate: Boolean
    fun current(): RingerMode
    /** Returns false if the change was not applied. */
    fun set(mode: RingerMode): Boolean
}

interface MosqueModeNotifier {
    fun showVibrateOn(mosqueName: String)
    fun dismiss()
}

interface MosqueModeSyncScheduler {
    fun requestSync()
}
