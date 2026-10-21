package com.salattracker.feature.quran

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salattracker.domain.model.MUSHAF_TOTAL_PAGES
import com.salattracker.domain.model.MushafPage
import com.salattracker.domain.repository.MushafRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val FIRST_MUSHAF_PAGE = 1

data class QuranMushafUiState(
    val isLoading: Boolean = true,
    val pageNumber: Int = FIRST_MUSHAF_PAGE,
    val page: MushafPage? = null,
    val errorMessage: String? = null,
)

@HiltViewModel
class QuranMushafViewModel @Inject constructor(
    private val mushafRepository: MushafRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        QuranMushafUiState(pageNumber = checkNotNull(savedStateHandle["pageNumber"]))
    )
    val uiState: StateFlow<QuranMushafUiState> = _uiState.asStateFlow()

    val hasPreviousPage: Boolean get() = _uiState.value.pageNumber > FIRST_MUSHAF_PAGE
    val hasNextPage: Boolean get() = _uiState.value.pageNumber < MUSHAF_TOTAL_PAGES

    init {
        loadPage(_uiState.value.pageNumber)
    }

    fun onNextPage() {
        val next = _uiState.value.pageNumber + 1
        if (next <= MUSHAF_TOTAL_PAGES) loadPage(next)
    }

    fun onPreviousPage() {
        val previous = _uiState.value.pageNumber - 1
        if (previous >= FIRST_MUSHAF_PAGE) loadPage(previous)
    }

    private fun loadPage(pageNumber: Int) {
        _uiState.update { it.copy(isLoading = true, pageNumber = pageNumber, errorMessage = null) }
        viewModelScope.launch {
            runCatching { mushafRepository.getPage(pageNumber) }.fold(
                onSuccess = { page -> _uiState.update { it.copy(isLoading = false, page = page) } },
                onFailure = { e -> _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Couldn't load this page") } },
            )
        }
    }
}
