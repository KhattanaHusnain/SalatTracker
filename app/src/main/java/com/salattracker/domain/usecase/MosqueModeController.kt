package com.salattracker.domain.usecase

import com.salattracker.domain.model.GeofenceSync
import com.salattracker.domain.model.RingerMode
import com.salattracker.domain.repository.MosqueGeofencer
import com.salattracker.domain.repository.MosqueModeNotifier
import com.salattracker.domain.repository.MosqueModeStore
import com.salattracker.domain.repository.MosqueRepository
import com.salattracker.domain.repository.RingerController
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * All Mosque Mode rules live here; receivers, workers and ViewModels only call in. A single
 * mutex keeps geofence broadcasts and UI actions from interleaving.
 */
@Singleton
class MosqueModeController @Inject constructor(
    private val mosques: MosqueRepository,
    private val store: MosqueModeStore,
    private val geofencer: MosqueGeofencer,
    private val ringer: RingerController,
    private val notifier: MosqueModeNotifier,
) {
    private val mutex = Mutex()

    suspend fun onDwell(ids: Collection<Long>) = mutex.withLock {
        val state = store.getState()
        if (!state.enabled) return@withLock
        val known = mosques.getMosques().associateBy { it.id }
        val entered = ids.filter { it in known }
        if (entered.isEmpty()) return@withLock

        val wasOutside = state.insideIds.isEmpty()
        val inside = state.insideIds + entered
        if (!wasOutside) {
            store.setVisit(inside, state.restoreTo, state.activeMosqueName)
            return@withLock
        }

        val name = known.getValue(entered.first()).name
        if (ringer.current() != RingerMode.NORMAL || !ringer.canVibrate) {
            store.setVisit(inside, null, null)
            return@withLock
        }
        // State first: if we crash after changing the ringer, the exit can still restore it.
        store.setVisit(inside, RingerMode.NORMAL, name)
        if (ringer.set(RingerMode.VIBRATE)) {
            notifier.showVibrateOn(name)
        } else {
            store.setVisit(inside, null, null)
        }
    }

    suspend fun onExit(ids: Collection<Long>) = mutex.withLock {
        val state = store.getState()
        if (state.insideIds.isEmpty()) return@withLock
        val remaining = state.insideIds - ids.toSet()
        if (remaining == state.insideIds) return@withLock
        if (remaining.isNotEmpty()) {
            store.setVisit(remaining, state.restoreTo, state.activeMosqueName)
        } else {
            endVisit(state.restoreTo)
        }
    }

    /** Restores sound but keeps the phone marked "inside", so this visit won't vibrate again. */
    suspend fun restoreSound() = mutex.withLock {
        val state = store.getState()
        val target = state.restoreTo
        if (target != null && ringer.current() == RingerMode.VIBRATE) ringer.set(target)
        notifier.dismiss()
        store.setVisit(state.insideIds, null, null)
    }

    suspend fun setEnabled(on: Boolean): GeofenceSync {
        store.setEnabled(on)
        return sync()
    }

    suspend fun sync(): GeofenceSync = mutex.withLock {
        val state = store.getState()
        val all = mosques.getMosques()

        if (state.insideIds.isNotEmpty()) {
            val stillSaved = state.insideIds.filter { id -> all.any { it.id == id } }.toSet()
            when {
                !state.enabled || stillSaved.isEmpty() -> endVisit(state.restoreTo)
                stillSaved != state.insideIds ->
                    store.setVisit(stillSaved, state.restoreTo, state.activeMosqueName)
            }
        }

        if (!state.enabled || all.isEmpty() || !ringer.canVibrate) {
            geofencer.clear()
            return@withLock GeofenceSync.OK
        }
        geofencer.register(all)
    }

    private suspend fun endVisit(restoreTo: RingerMode?) {
        if (restoreTo != null && ringer.current() == RingerMode.VIBRATE) ringer.set(restoreTo)
        notifier.dismiss()
        store.setVisit(emptySet(), null, null)
    }
}
