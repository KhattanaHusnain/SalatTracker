package com.salattracker.data.repository

import com.salattracker.data.local.dao.MosqueDao
import com.salattracker.data.local.entity.MosqueEntity
import com.salattracker.data.remote.api.OverpassService
import com.salattracker.data.remote.api.buildNearbyMosquesQuery
import com.salattracker.data.remote.api.toNearbyMosques
import com.salattracker.domain.model.Mosque
import com.salattracker.domain.model.NearbyMosque
import com.salattracker.domain.repository.MosqueRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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

    override suspend fun findNearby(latitude: Double, longitude: Double): List<NearbyMosque> =
        overpassService.query(buildNearbyMosquesQuery(latitude, longitude)).toNearbyMosques(latitude, longitude)
}

private fun MosqueEntity.toDomain() = Mosque(id, name, latitude, longitude, radiusMeters)

private fun Mosque.toEntity() = MosqueEntity(id, name, latitude, longitude, radiusMeters)
