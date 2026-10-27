package com.salattracker.feature.qaza

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salattracker.R
import com.salattracker.domain.model.Gender
import com.salattracker.domain.model.PrayerStatus
import com.salattracker.domain.model.PrayerType
import com.salattracker.domain.model.QazaActivity
import com.salattracker.domain.model.QazaActivitySource
import com.salattracker.domain.repository.PrayerRepository
import com.salattracker.domain.repository.PrayerSlot
import com.salattracker.domain.repository.QazaPreferences
import com.salattracker.domain.repository.QazaPreferencesRepository
import com.salattracker.domain.repository.SettingsRepository
import com.salattracker.domain.usecase.PACE_WINDOW_DAYS
import com.salattracker.domain.usecase.QazaAllocationPreview
import com.salattracker.domain.usecase.QazaProgress
import com.salattracker.domain.usecase.QazaSlot
import com.salattracker.domain.usecase.allocateOlderQaza
import com.salattracker.domain.usecase.allocateQazaByDates
import com.salattracker.domain.usecase.allocateQazaCounts
import com.salattracker.domain.usecase.cappedQazaGoal
import com.salattracker.domain.usecase.pendingByPrayer
import com.salattracker.domain.usecase.pickQazaForSettlement
import com.salattracker.domain.usecase.qazaProgress
import com.salattracker.domain.usecase.toQazaSlot
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class QazaV10Page { LANDING, ADD_MISSED, LOG_SEVERAL, OLDER_MISSED, DAILY_GOALS }
enum class QazaAddMode { DATES, COUNT }

data class QazaAddForm(
    val mode: QazaAddMode,
    val from: LocalDate,
    val to: LocalDate,
    val prayers: Set<PrayerType> = PrayerType.values().toSet(),
    val counts: Map<PrayerType, Int> = PrayerType.values().associateWith { 0 },
    val reason: String? = null,
    val preview: QazaPreviewSummary? = null,
)

data class QazaPreviewSummary(val requested: Int, val added: Int, val skipped: Int)
data class QazaResultMessage(val added: Int, val skipped: Int, val estimated: Boolean)

data class QazaOlderForm(
    val years: Int = 5,
    val excusedDaysPerMonth: Int = 7,
    val preview: QazaPreviewSummary? = null,
)

data class QazaV10State(
    val isLoading: Boolean,
    val page: QazaV10Page,
    val progress: QazaProgress,
    val pending: List<QazaSlot>,
    val pendingByPrayer: Map<PrayerType, Int>,
    val activities: List<QazaActivity>,
    val preferences: QazaPreferences,
    val firstRecordedDate: LocalDate?,
    val gender: Gender?,
    val addForm: QazaAddForm,
    val logCounts: Map<PrayerType, Int>,
    val olderForm: QazaOlderForm,
    val isSaving: Boolean = false,
    val undoCount: Int? = null,
    val message: QazaResultMessage? = null,
    val errorRes: Int? = null,
) {
    val outstanding: Int get() = pending.size
    val madeUpToday: Int get() {
        val today = LocalDate.now()
        return activities.count {
            it.newStatus == PrayerStatus.QAZA_COMPLETED &&
                Instant.ofEpochMilli(it.recordedAt).atZone(ZoneId.systemDefault()).toLocalDate() == today
        }
    }
    val todayTarget: Int get() = cappedQazaGoal(preferences.dailyGoal, outstanding + madeUpToday)
}

@HiltViewModel
class QazaV10ViewModel @Inject constructor(
    private val prayers: PrayerRepository,
    private val preferences: QazaPreferencesRepository,
    private val settings: SettingsRepository,
    private val clock: Clock,
) : ViewModel() {
    private val today get() = LocalDate.now(clock)
    private val _state = MutableStateFlow(
        QazaV10State(
            isLoading = true,
            page = QazaV10Page.LANDING,
            progress = qazaProgress(0, 0, emptyList()),
            pending = emptyList(),
            pendingByPrayer = PrayerType.values().associateWith { 0 },
            activities = emptyList(),
            preferences = QazaPreferences(),
            firstRecordedDate = null,
            gender = null,
            addForm = QazaAddForm(QazaAddMode.DATES, today.minusDays(1), today.minusDays(1)),
            logCounts = PrayerType.values().associateWith { 0 },
            olderForm = QazaOlderForm(),
        )
    )
    val state: StateFlow<QazaV10State> = _state.asStateFlow()
    private var undoSlots: List<QazaSlot>? = null
    private var undoJob: Job? = null
    private var pendingAddAllocation: QazaAllocationPreview? = null
    private var pendingOlderAllocation: QazaAllocationPreview? = null

    init {
        observe()
        viewModelScope.launch {
            _state.update { it.copy(firstRecordedDate = prayers.getFirstRecordedPrayerDate() ?: today) }
        }
    }

    private fun observe() = viewModelScope.launch {
        combine(
            prayers.observePendingQazaEntries(),
            prayers.observeQazaMadeUpCount(),
            prayers.observeQazaMadeUpSince(today.minusDays((PACE_WINDOW_DAYS - 1).toLong())),
            prayers.observeQazaActivities(),
            preferences.observe(),
            settings.observeSettings(),
        ) { values ->
            @Suppress("UNCHECKED_CAST")
            Observed(
                pending = values[0] as List<com.salattracker.domain.model.PrayerEntry>,
                madeUp = values[1] as Int,
                madeUpSince = values[2] as List<com.salattracker.domain.model.PrayerEntry>,
                activities = values[3] as List<QazaActivity>,
                preferences = values[4] as QazaPreferences,
                gender = (values[5] as com.salattracker.domain.repository.UserPrefs).gender,
            )
        }.collect { observed ->
            val pending = observed.pending.map { it.toQazaSlot() }
            _state.update {
                it.copy(
                    isLoading = false,
                    pending = pending,
                    pendingByPrayer = pendingByPrayer(pending),
                    progress = qazaProgress(pending.size, observed.madeUp, observed.madeUpSince.map { row -> row.updatedAt }, today),
                    activities = observed.activities,
                    preferences = observed.preferences,
                    gender = observed.gender,
                )
            }
        }
    }

    fun open(page: QazaV10Page) = _state.update { it.copy(page = page, message = null) }
    fun back() = _state.update { it.copy(page = QazaV10Page.LANDING) }
    fun clearMessage() = _state.update { it.copy(message = null) }
    fun clearError() = _state.update { it.copy(errorRes = null) }

    fun setDailyGoal(value: Int) = updatePreferences { it.copy(dailyGoal = value.takeIf { goal -> goal in setOf(5, 10, 15) } ?: 5) }
    fun setOldestFirst(value: Boolean) = updatePreferences { it.copy(oldestFirst = value) }
    fun setSound(value: Boolean) = updatePreferences { it.copy(soundEnabled = value) }
    fun setHideBadge(value: Boolean) = updatePreferences { it.copy(hideNavigationBadge = value) }
    fun setReducedMotion(value: Boolean) = updatePreferences { it.copy(reducedMotion = value) }

    private fun updatePreferences(transform: (QazaPreferences) -> QazaPreferences) {
        val next = transform(_state.value.preferences)
        _state.update { it.copy(preferences = next) }
        viewModelScope.launch { preferences.save(next) }
    }

    fun setAddMode(mode: QazaAddMode) = _state.update { it.copy(addForm = it.addForm.copy(mode = mode, preview = null)) }
    fun setAddDates(from: LocalDate, to: LocalDate) = _state.update {
        it.copy(addForm = it.addForm.copy(from = minOf(from, to), to = maxOf(from, to), preview = null))
    }
    fun togglePrayer(prayer: PrayerType) = _state.update {
        val selected = it.addForm.prayers.toMutableSet().apply { if (!add(prayer)) remove(prayer) }
        it.copy(addForm = it.addForm.copy(prayers = selected, preview = null))
    }
    fun setAddCount(prayer: PrayerType, count: Int) = _state.update {
        it.copy(addForm = it.addForm.copy(counts = it.addForm.counts + (prayer to count.coerceIn(0, 100_000)), preview = null))
    }
    fun setReason(reason: String?) = _state.update { it.copy(addForm = it.addForm.copy(reason = reason, preview = null)) }

    fun previewAdd() = viewModelScope.launch {
        val form = _state.value.addForm
        val first = _state.value.firstRecordedDate ?: today
        val preview = if (form.mode == QazaAddMode.COUNT) {
            allocateQazaCounts(form.counts, first, emptySet())
        } else {
            val dates = generateSequence(form.from) { d -> d.plusDays(1).takeIf { !it.isAfter(form.to) } }.toList()
            val occupied = dates.flatMap { date -> prayers.getPrayerEntriesForDate(date).map { PrayerSlot(it.date, it.prayerType) } }.toSet()
            allocateQazaByDates(dates, form.prayers, occupied)
        }
        pendingAddAllocation = preview
        _state.update { it.copy(addForm = it.addForm.copy(preview = preview.toSummary())) }
    }

    fun confirmAdd() {
        val form = _state.value.addForm
        saveMissed(pendingAddAllocation ?: return, form.reason, estimated = false)
    }

    fun setOlderYears(value: Int) = _state.update { it.copy(olderForm = it.olderForm.copy(years = value.coerceIn(1, 30), preview = null)) }
    fun setExcusedDays(value: Int) = _state.update { it.copy(olderForm = it.olderForm.copy(excusedDaysPerMonth = value.coerceIn(0, 15), preview = null)) }
    fun previewOlder() {
        val current = _state.value
        val excused = if (current.gender == Gender.FEMALE) current.olderForm.excusedDaysPerMonth else 0
        val preview = allocateOlderQaza(current.olderForm.years, current.firstRecordedDate ?: today, emptySet(), excused)
        pendingOlderAllocation = preview
        _state.update { it.copy(olderForm = it.olderForm.copy(preview = preview.toSummary())) }
    }
    fun confirmOlder() {
        val preview = pendingOlderAllocation ?: return
        saveMissed(preview, "OLDER_ESTIMATE", estimated = true)
    }

    private fun saveMissed(preview: QazaAllocationPreview, reason: String?, estimated: Boolean) {
        if (preview.slots.isEmpty()) return
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            runCatching {
                prayers.bulkUpsertPrayerStatus(
                    preview.slots.map { it.date to it.prayerType },
                    PrayerStatus.QAZA_PENDING,
                    qazaReason = reason,
                    source = QazaActivitySource.QAZA,
                )
            }.onSuccess {
                _state.update {
                    it.copy(
                        isSaving = false,
                        page = QazaV10Page.LANDING,
                        firstRecordedDate = preview.slots.minOfOrNull { slot -> slot.date } ?: it.firstRecordedDate,
                        message = QazaResultMessage(preview.added, preview.skipped, estimated),
                        addForm = it.addForm.copy(preview = null),
                        olderForm = it.olderForm.copy(preview = null),
                    )
                }
            }.onFailure { _state.update { it.copy(isSaving = false, errorRes = R.string.error_save_generic) } }
        }
    }

    fun setLogCount(prayer: PrayerType, count: Int) = _state.update {
        it.copy(logCounts = it.logCounts + (prayer to count.coerceIn(0, it.pendingByPrayer[prayer] ?: 0)))
    }

    fun confirmLog() {
        val current = _state.value
        val selected = pickQazaForSettlement(current.pending, current.logCounts, current.preferences.oldestFirst)
        makeUp(selected, returnToLanding = true)
    }

    fun makeUp(prayer: PrayerType) {
        val current = _state.value
        val selected = pickQazaForSettlement(current.pending, mapOf(prayer to 1), current.preferences.oldestFirst)
        makeUp(selected)
    }

    private fun makeUp(slots: List<QazaSlot>, returnToLanding: Boolean = false) {
        if (slots.isEmpty()) return
        viewModelScope.launch {
            runCatching {
                prayers.bulkUpsertPrayerStatus(
                    slots.map { it.date to it.prayerType },
                    PrayerStatus.QAZA_COMPLETED,
                    source = QazaActivitySource.QAZA,
                )
            }.onSuccess {
                undoSlots = slots
                undoJob?.cancel()
                _state.update {
                    it.copy(
                        page = if (returnToLanding) QazaV10Page.LANDING else it.page,
                        logCounts = PrayerType.values().associateWith { 0 },
                        undoCount = slots.size,
                    )
                }
                undoJob = viewModelScope.launch { delay(10_000); undoSlots = null; _state.update { it.copy(undoCount = null) } }
            }.onFailure { _state.update { it.copy(errorRes = R.string.error_save_generic) } }
        }
    }

    fun undoLast() {
        val slots = undoSlots ?: return
        undoSlots = null
        undoJob?.cancel()
        _state.update { it.copy(undoCount = null) }
        viewModelScope.launch {
            prayers.bulkUpsertPrayerStatus(
                slots.map { it.date to it.prayerType },
                PrayerStatus.QAZA_PENDING,
                source = QazaActivitySource.QAZA,
            )
        }
    }

    fun undoActivity(id: Long) = viewModelScope.launch { prayers.undoQazaActivity(id) }
}

private data class Observed(
    val pending: List<com.salattracker.domain.model.PrayerEntry>,
    val madeUp: Int,
    val madeUpSince: List<com.salattracker.domain.model.PrayerEntry>,
    val activities: List<QazaActivity>,
    val preferences: QazaPreferences,
    val gender: Gender?,
)

private fun QazaAllocationPreview.toSummary() = QazaPreviewSummary(requested, added, skipped)
