package com.salattracker.feature.mosque

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salattracker.core.util.runCatchingCancellable
import com.salattracker.domain.model.GeofenceSync
import com.salattracker.domain.model.LocationAccess
import com.salattracker.domain.model.Mosque
import com.salattracker.domain.model.MosqueMode
import com.salattracker.domain.model.NearbyMosque
import com.salattracker.domain.repository.LocationProvider
import com.salattracker.domain.repository.MosqueModeStore
import com.salattracker.domain.repository.MosqueRepository
import com.salattracker.domain.repository.RingerController
import com.salattracker.domain.usecase.MosqueModeController
import com.salattracker.domain.usecase.distanceMeters
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class MosqueMessage { LOCATION_UNAVAILABLE, LIMIT_REACHED, SAVE_FAILED, SYNC_FAILED }

/** The add/edit sheet's working copy. [id] is null while adding a new mosque. */
data class MosqueDraft(
    val id: Long? = null,
    val name: String = "",
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Int = MosqueMode.DEFAULT_RADIUS_METERS,
    val nameMissing: Boolean = false,
)

data class NearbyItem(val mosque: NearbyMosque, val alreadyAdded: Boolean)

enum class NearbyFailure { LOCATION_UNAVAILABLE, SEARCH_FAILED }

data class NearbyUiState(
    val isLoading: Boolean = true,
    val results: List<NearbyItem> = emptyList(),
    val failure: NearbyFailure? = null,
)

data class MosqueModeUiState(
    val enabled: Boolean = false,
    val canVibrate: Boolean = true,
    val mosques: List<Mosque> = emptyList(),
    /** Set while this app has the phone on vibrate for a visit. */
    val vibrateActiveAt: String? = null,
    val access: LocationAccess = LocationAccess.UNKNOWN,
    val locationOff: Boolean = false,
    val isLocating: Boolean = false,
    val draft: MosqueDraft? = null,
    val nearby: NearbyUiState? = null,
    val showDeleteConfirm: Boolean = false,
    val message: MosqueMessage? = null,
) {
    val atLimit: Boolean get() = mosques.size >= MosqueMode.MAX_MOSQUES
}

@HiltViewModel
class MosqueModeViewModel @Inject constructor(
    private val mosqueRepository: MosqueRepository,
    private val store: MosqueModeStore,
    private val controller: MosqueModeController,
    private val locationProvider: LocationProvider,
    ringer: RingerController,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MosqueModeUiState(canVibrate = ringer.canVibrate))
    val uiState: StateFlow<MosqueModeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(mosqueRepository.observeMosques(), store.observeState()) { mosques, state ->
                _uiState.update {
                    it.copy(
                        enabled = state.enabled,
                        mosques = mosques,
                        vibrateActiveAt = state.activeMosqueName.takeIf { _ -> state.restoreTo != null },
                    )
                }
            }.collect {}
        }
    }

    // ── Master switch & permission state ──────────────────────────────────────

    fun onEnabledChanged(on: Boolean) {
        viewModelScope.launch { applySync(controller.setEnabled(on)) }
    }

    fun onAccessChanged(access: LocationAccess) {
        val previous = _uiState.value.access
        _uiState.update { it.copy(access = access) }
        if (previous != LocationAccess.FULL && access == LocationAccess.FULL && _uiState.value.enabled) {
            viewModelScope.launch { applySync(controller.sync()) }
        }
    }

    fun onRestoreSound() {
        viewModelScope.launch { controller.restoreSound() }
    }

    // ── Add from current location ─────────────────────────────────────────────

    fun onAddCurrentLocation() {
        if (_uiState.value.atLimit) return showMessage(MosqueMessage.LIMIT_REACHED)
        if (_uiState.value.isLocating) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLocating = true) }
            val fix = locationProvider.getCurrentLocation(highAccuracy = true)
            _uiState.update { state ->
                when (fix) {
                    null -> state.copy(isLocating = false, message = MosqueMessage.LOCATION_UNAVAILABLE)
                    else -> state.copy(isLocating = false, draft = MosqueDraft(latitude = fix.first, longitude = fix.second))
                }
            }
        }
    }

    // ── Nearby search ─────────────────────────────────────────────────────────

    fun onFindNearby() {
        if (_uiState.value.atLimit) return showMessage(MosqueMessage.LIMIT_REACHED)
        searchNearby()
    }

    fun onNearbyRetry() = searchNearby()

    private fun searchNearby() {
        viewModelScope.launch {
            _uiState.update { it.copy(nearby = NearbyUiState(isLoading = true)) }
            val fix = locationProvider.getCurrentLocation()
            if (fix == null) {
                _uiState.update { it.copy(nearby = NearbyUiState(isLoading = false, failure = NearbyFailure.LOCATION_UNAVAILABLE)) }
                return@launch
            }
            val saved = _uiState.value.mosques
            runCatchingCancellable { mosqueRepository.findNearby(fix.first, fix.second) }
                .onSuccess { found ->
                    val items = found.map { m ->
                        NearbyItem(m, saved.any { distanceMeters(it.latitude, it.longitude, m.latitude, m.longitude) < MosqueMode.ALREADY_ADDED_METERS })
                    }
                    _uiState.update { it.copy(nearby = NearbyUiState(isLoading = false, results = items)) }
                }
                .onFailure { _uiState.update { it.copy(nearby = NearbyUiState(isLoading = false, failure = NearbyFailure.SEARCH_FAILED)) } }
        }
    }

    fun onNearbyDismiss() = _uiState.update { it.copy(nearby = null) }

    fun onNearbySelected(item: NearbyItem) {
        if (item.alreadyAdded) return
        _uiState.update {
            it.copy(
                nearby = null,
                draft = MosqueDraft(
                    name = item.mosque.name.orEmpty(),
                    latitude = item.mosque.latitude,
                    longitude = item.mosque.longitude,
                ),
            )
        }
    }

    // ── Add / edit sheet ──────────────────────────────────────────────────────

    fun onMosqueClicked(mosque: Mosque) {
        _uiState.update {
            it.copy(draft = MosqueDraft(mosque.id, mosque.name, mosque.latitude, mosque.longitude, mosque.radiusMeters))
        }
    }

    fun onDraftNameChanged(name: String) = _uiState.update { state ->
        state.copy(draft = state.draft?.copy(name = name, nameMissing = false))
    }

    fun onDraftRadiusChanged(radius: Int) = _uiState.update { state ->
        state.copy(draft = state.draft?.copy(radiusMeters = radius))
    }

    fun onDraftDismiss() = _uiState.update { it.copy(draft = null, showDeleteConfirm = false) }

    fun onSaveMosque() {
        val draft = _uiState.value.draft ?: return
        val name = draft.name.trim()
        if (name.isEmpty()) {
            _uiState.update { it.copy(draft = draft.copy(nameMissing = true)) }
            return
        }
        viewModelScope.launch {
            val mosque = Mosque(draft.id ?: 0, name, draft.latitude, draft.longitude, draft.radiusMeters)
            runCatchingCancellable {
                if (draft.id == null) mosqueRepository.addMosque(mosque) else mosqueRepository.updateMosque(mosque)
            }.onSuccess {
                _uiState.update { it.copy(draft = null) }
                syncIfEnabled()
            }.onFailure { showMessage(MosqueMessage.SAVE_FAILED) }
        }
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    fun onDeleteClick() = _uiState.update { it.copy(showDeleteConfirm = true) }

    fun onDeleteDismiss() = _uiState.update { it.copy(showDeleteConfirm = false) }

    fun onConfirmDelete() {
        val id = _uiState.value.draft?.id ?: return
        viewModelScope.launch {
            runCatchingCancellable { mosqueRepository.deleteMosque(id) }
                .onSuccess {
                    _uiState.update { it.copy(draft = null, showDeleteConfirm = false) }
                    syncIfEnabled()
                }
                .onFailure { showMessage(MosqueMessage.SAVE_FAILED) }
        }
    }

    fun onMessageDismiss() = _uiState.update { it.copy(message = null) }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private suspend fun syncIfEnabled() {
        if (_uiState.value.enabled) applySync(controller.sync())
    }

    private fun applySync(result: GeofenceSync) {
        _uiState.update {
            it.copy(
                locationOff = result == GeofenceSync.LOCATION_OFF,
                message = if (result == GeofenceSync.FAILED) MosqueMessage.SYNC_FAILED else it.message,
            )
        }
    }

    private fun showMessage(message: MosqueMessage) = _uiState.update { it.copy(message = message) }
}
