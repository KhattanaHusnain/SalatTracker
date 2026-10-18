package com.salattracker.data.mosque

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.salattracker.domain.model.MosqueModeState
import com.salattracker.domain.model.RingerMode
import com.salattracker.domain.repository.MosqueModeStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Own file, so it never shares state with "salat_user_prefs" or the unqualified DataStore binding. */
private val Context.mosqueModeDataStore: DataStore<Preferences> by preferencesDataStore(name = "mosque_mode")

@Singleton
class MosqueModeStoreImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : MosqueModeStore {

    private object Keys {
        val ENABLED = booleanPreferencesKey("enabled")
        val INSIDE_IDS = stringPreferencesKey("inside_ids")
        val RESTORE_TO = stringPreferencesKey("restore_to")
        val ACTIVE_NAME = stringPreferencesKey("active_name")
    }

    override fun observeState(): Flow<MosqueModeState> = context.mosqueModeDataStore.data.map { it.toState() }

    override suspend fun getState(): MosqueModeState = context.mosqueModeDataStore.data.first().toState()

    override suspend fun setEnabled(enabled: Boolean) {
        context.mosqueModeDataStore.edit { it[Keys.ENABLED] = enabled }
    }

    override suspend fun setVisit(insideIds: Set<Long>, restoreTo: RingerMode?, activeMosqueName: String?) {
        context.mosqueModeDataStore.edit { prefs ->
            prefs[Keys.INSIDE_IDS] = insideIds.joinToString(",")
            if (restoreTo != null) prefs[Keys.RESTORE_TO] = restoreTo.name else prefs.remove(Keys.RESTORE_TO)
            if (activeMosqueName != null) prefs[Keys.ACTIVE_NAME] = activeMosqueName else prefs.remove(Keys.ACTIVE_NAME)
        }
    }

    private fun Preferences.toState() = MosqueModeState(
        enabled = this[Keys.ENABLED] ?: false,
        insideIds = this[Keys.INSIDE_IDS].orEmpty().split(',').mapNotNull { it.toLongOrNull() }.toSet(),
        restoreTo = this[Keys.RESTORE_TO]?.let { runCatching { RingerMode.valueOf(it) }.getOrNull() },
        activeMosqueName = this[Keys.ACTIVE_NAME],
    )
}
