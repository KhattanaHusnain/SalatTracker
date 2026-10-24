package com.salattracker.notification

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** Persists the latest fully elapsed date examined by [QazaAutoMarkWorker]. */
interface QazaAutoMarkStateStore {
    suspend fun lastProcessedDate(): LocalDate?
    suspend fun setLastProcessedDate(date: LocalDate)
}

@Singleton
class DataStoreQazaAutoMarkStateStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : QazaAutoMarkStateStore {

    override suspend fun lastProcessedDate(): LocalDate? = dataStore.data.first()[LAST_PROCESSED_DATE]
        ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

    override suspend fun setLastProcessedDate(date: LocalDate) {
        dataStore.edit { it[LAST_PROCESSED_DATE] = date.toString() }
    }

    private companion object {
        val LAST_PROCESSED_DATE = stringPreferencesKey("qaza_auto_mark_last_processed_date")
    }
}
