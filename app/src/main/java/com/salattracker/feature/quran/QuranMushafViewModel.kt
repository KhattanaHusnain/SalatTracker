package com.salattracker.feature.quran

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salattracker.R
import com.salattracker.domain.model.MUSHAF_TOTAL_PAGES
import com.salattracker.domain.model.MushafPage
import com.salattracker.domain.repository.QuranContentRepository
import com.salattracker.domain.repository.MushafRepository
import com.salattracker.domain.repository.QuranUserDataRepository
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
    val firstPageNumber: Int = FIRST_MUSHAF_PAGE,
    val lastPageNumber: Int = MUSHAF_TOTAL_PAGES,
    val page: MushafPage? = null,
    val errorRes: Int? = null,
)

@HiltViewModel
class QuranMushafViewModel @Inject constructor(
    private val mushafRepository: MushafRepository,
    private val contentRepository: QuranContentRepository,
    private val userDataRepository: QuranUserDataRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val selectedJuzNumber: Int? = savedStateHandle.get<Int>("juzNumber")
        ?.takeIf { it in 1..TOTAL_JUZ }

    private val _uiState = MutableStateFlow(
        QuranMushafUiState(pageNumber = checkNotNull(savedStateHandle["pageNumber"]))
    )
    val uiState: StateFlow<QuranMushafUiState> = _uiState.asStateFlow()

    val hasPreviousPage: Boolean get() = _uiState.value.pageNumber > _uiState.value.firstPageNumber
    val hasNextPage: Boolean get() = _uiState.value.pageNumber < _uiState.value.lastPageNumber

    init {
        val requestedPage = _uiState.value.pageNumber
        if (selectedJuzNumber == null) {
            loadPage(requestedPage)
        } else {
            loadJuz(selectedJuzNumber, requestedPage)
        }
    }

    fun onNextPage() {
        val next = _uiState.value.pageNumber + 1
        if (next <= _uiState.value.lastPageNumber) loadPage(next)
    }

    fun onPreviousPage() {
        val previous = _uiState.value.pageNumber - 1
        if (previous >= _uiState.value.firstPageNumber) loadPage(previous)
    }

    private fun loadJuz(juzNumber: Int, requestedPage: Int) {
        viewModelScope.launch {
            val firstLocation = contentRepository.firstVerseOfJuz(juzNumber)
                ?: return@launch loadPage(requestedPage)
            val firstPage = contentRepository.getVerse(firstLocation.chapterNumber, firstLocation.verseNumber)
                ?.pageNumber
                ?: return@launch loadPage(requestedPage)
            val page = requestedPage.coerceIn(firstPage, MUSHAF_TOTAL_PAGES)
            _uiState.update {
                it.copy(pageNumber = page, firstPageNumber = FIRST_MUSHAF_PAGE, lastPageNumber = MUSHAF_TOTAL_PAGES)
            }
            loadPage(page)
        }
    }

    private fun loadPage(pageNumber: Int) {
        _uiState.update { it.copy(isLoading = it.page == null, pageNumber = pageNumber, errorRes = null) }
        viewModelScope.launch {
            runCatching { mushafRepository.getPage(pageNumber) }.fold(
                onSuccess = { page ->
                    _uiState.update { it.copy(isLoading = false, page = page) }
                    userDataRepository.recordRead(
                        chapterNumber = page.firstVerse.chapterNumber,
                        verseNumber = page.firstVerse.verseNumber,
                        juzNumber = selectedJuzNumber,
                        pageNumber = page.pageNumber,
                    )
                },
                onFailure = { _ -> _uiState.update { it.copy(isLoading = false, errorRes = R.string.error_load_generic) } },
            )
        }
    }

    private companion object {
        const val TOTAL_JUZ = 30
    }
}
