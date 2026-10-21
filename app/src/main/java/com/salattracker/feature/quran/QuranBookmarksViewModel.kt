package com.salattracker.feature.quran

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salattracker.domain.model.QuranBookmark
import com.salattracker.domain.repository.QuranContentRepository
import com.salattracker.domain.repository.QuranUserDataRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class QuranBookmarkUi(val bookmark: QuranBookmark, val chapterName: String)

data class QuranBookmarksUiState(
    val isLoading: Boolean = true,
    val bookmarks: List<QuranBookmarkUi> = emptyList(),
)

@HiltViewModel
class QuranBookmarksViewModel @Inject constructor(
    private val userDataRepository: QuranUserDataRepository,
    private val contentRepository: QuranContentRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuranBookmarksUiState())
    val uiState: StateFlow<QuranBookmarksUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userDataRepository.observeBookmarks().collect { bookmarks ->
                val items = bookmarks.map { bookmark ->
                    val chapterName = contentRepository.getChapter(bookmark.chapterNumber)?.nameTransliteration.orEmpty()
                    QuranBookmarkUi(bookmark, chapterName)
                }
                _uiState.update { it.copy(isLoading = false, bookmarks = items) }
            }
        }
    }

    fun onDelete(id: Long) {
        viewModelScope.launch { userDataRepository.deleteBookmark(id) }
    }
}
