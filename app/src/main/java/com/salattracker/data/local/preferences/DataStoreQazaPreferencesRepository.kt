package com.salattracker.data.local.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import com.salattracker.domain.repository.QazaPreferences
import com.salattracker.domain.repository.QazaPreferencesRepository
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

@Singleton
class DataStoreQazaPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : QazaPreferencesRepository {
    private object Keys {
        val DAILY_GOAL = intPreferencesKey("qaza_v10_daily_goal")
        val OLDEST_FIRST = booleanPreferencesKey("qaza_v10_oldest_first")
        val SOUND = booleanPreferencesKey("qaza_v10_sound")
        val HIDE_BADGE = booleanPreferencesKey("qaza_v10_hide_badge")
        val REDUCED_MOTION = booleanPreferencesKey("qaza_v10_reduced_motion")
    }

    override fun observe(): Flow<QazaPreferences> = dataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { it.toQazaPreferences() }

    override suspend fun get(): QazaPreferences = dataStore.data.first().toQazaPreferences()

    override suspend fun save(preferences: QazaPreferences) {
        dataStore.edit { values ->
            values[Keys.DAILY_GOAL] = preferences.dailyGoal.coerceIn(5, 15).let { (it / 5) * 5 }
            values[Keys.OLDEST_FIRST] = preferences.oldestFirst
            values[Keys.SOUND] = preferences.soundEnabled
            values[Keys.HIDE_BADGE] = preferences.hideNavigationBadge
            values[Keys.REDUCED_MOTION] = preferences.reducedMotion
        }
    }

    private fun Preferences.toQazaPreferences() = QazaPreferences(
        dailyGoal = (this[Keys.DAILY_GOAL] ?: 5).takeIf { it in setOf(5, 10, 15) } ?: 5,
        oldestFirst = this[Keys.OLDEST_FIRST] ?: true,
        soundEnabled = this[Keys.SOUND] ?: true,
        hideNavigationBadge = this[Keys.HIDE_BADGE] ?: false,
        reducedMotion = this[Keys.REDUCED_MOTION] ?: false,
    )
}
