package com.salattracker.feature.quran

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salattracker.domain.model.QuranReadHistoryEntry
import com.salattracker.domain.repository.QuranContentRepository
import com.salattracker.domain.repository.QuranUserDataRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class QuranReadHistoryUi(val entry: QuranReadHistoryEntry, val chapterName: String)

data class QuranReadHistoryUiState(
    val isLoading: Boolean = true,
    val entries: List<QuranReadHistoryUi> = emptyList(),
)

@HiltViewModel
class QuranReadHistoryViewModel @Inject constructor(
    private val userDataRepository: QuranUserDataRepository,
    private val contentRepository: QuranContentRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuranReadHistoryUiState())
    val uiState: StateFlow<QuranReadHistoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userDataRepository.observeRecentReads(limit = 100).collect { entries ->
                val items = entries.map { entry ->
                    val chapterName = contentRepository.getChapter(entry.chapterNumber)?.nameTransliteration.orEmpty()
                    QuranReadHistoryUi(entry, chapterName)
                }
                _uiState.update { it.copy(isLoading = false, entries = items) }
            }
        }
    }

    fun onClearHistory() {
        viewModelScope.launch { userDataRepository.clearReadHistory() }
    }
}
