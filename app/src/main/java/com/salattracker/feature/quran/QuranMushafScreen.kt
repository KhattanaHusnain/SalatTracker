package com.salattracker.feature.quran

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salattracker.R
import com.salattracker.core.theme.*
import com.salattracker.domain.model.MushafPage
import com.salattracker.domain.model.MushafLine
import com.salattracker.domain.model.VerseLocation

// ── Mushaf (print-accurate paginated) reader. Every word is rendered using that printed page's
// own QCF glyph font, bundled in the base APK at assets/quran/mushaf_fonts/v2/pN.ttf. This keeps
// the reader fully offline, unlike the regular verse-by-verse QuranReaderScreen's natural text
// reflow. ──

@Composable
fun QuranMushafScreen(
    uiState: QuranMushafUiState,
    onPreviousPage: () -> Unit = {},
    onNextPage: () -> Unit = {},
) {
    Column(modifier = Modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SalatPrimary)
            }
            uiState.errorRes != null -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(uiState.errorRes), color = SalatOnSurfaceVariant, fontSize = 14.sp, fontFamily = AppUiFontFamily)
            }
            uiState.page != null -> MushafPageContent(
                page = uiState.page,
                juzStartVerses = uiState.juzStartVerses,
                juzStartVerse = uiState.juzStartVerse,
                nextJuzStartVerse = uiState.nextJuzStartVerse,
                translationChunks = uiState.translationChunks,
                showTranslations = uiState.showTranslations,
                onPreviousPage = onPreviousPage,
                onNextPage = onNextPage,
            )
        }
    }
}

@Composable
private fun MushafPageContent(
    page: MushafPage,
    juzStartVerses: Set<VerseLocation>,
    juzStartVerse: VerseLocation?,
    nextJuzStartVerse: VerseLocation?,
    translationChunks: Map<Int, List<ReaderTranslationChunk>>,
    showTranslations: Boolean,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
) {
    val visibleLines = remember(page, juzStartVerse, nextJuzStartVerse) {
        page.lines.boundedBy(juzStartVerse, nextJuzStartVerse)
    }
    var dragDistance by remember(page.pageNumber) { mutableStateOf(0f) }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .border(1.dp, SalatOutlineVariant)
            .pointerInput(page.pageNumber) {
                detectHorizontalDragGestures(
                    onDragStart = { dragDistance = 0f },
                    onHorizontalDrag = { _, amount -> dragDistance += amount },
                    onDragEnd = {
                        when {
                            dragDistance <= -80f -> onNextPage()
                            dragDistance >= 80f -> onPreviousPage()
                        }
                    },
                )
            },
        contentPadding = PaddingValues(0.dp),
    ) {
        items(visibleLines, key = { it.lineNumber }) { line ->
            val isJuzStart = line.startsJuz(juzStartVerses)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SalatOutlineVariant),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isJuzStart) Color.Black else Color.Transparent)
                        .padding(vertical = 5.dp),
                ) {
                    MushafLineArabic(
                        line = ReaderLine(page.pageNumber, line.words, emptyList()),
                        textColor = if (isJuzStart) Color.White else SalatOnSurface,
                    )
                }
                val chunks = translationChunks[line.lineNumber].orEmpty()
                if (showTranslations && chunks.isNotEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(SalatOutlineVariant))
                    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp)) {
                        UrduTranslation(chunks.joinToString(" ") { it.text })
                    }
                }
            }
        }
    }
}

private fun MushafLine.startsJuz(juzStartVerses: Set<VerseLocation>): Boolean =
    words.any { word ->
        word.position == 1 && VerseLocation(word.chapterNumber, word.verseNumber) in juzStartVerses
    }

/** Drops preceding-juz lines and excludes following-juz lines when a printed page is shared. */
private fun List<MushafLine>.boundedBy(
    juzStartVerse: VerseLocation?,
    nextJuzStartVerse: VerseLocation?,
): List<MushafLine> {
    val firstLine = juzStartVerse?.let { boundary -> indexOfFirst { it.startsAt(boundary) } } ?: -1
    val afterStart = if (firstLine >= 0) drop(firstLine) else this
    val nextLine = nextJuzStartVerse?.let { boundary -> afterStart.indexOfFirst { it.startsAt(boundary) } } ?: -1
    return if (nextLine >= 0) afterStart.take(nextLine) else afterStart
}

private fun MushafLine.startsAt(location: VerseLocation): Boolean =
    words.any { word ->
        word.position == 1 && word.chapterNumber == location.chapterNumber && word.verseNumber == location.verseNumber
    }

// No app assets are available in a preview, so this only exercises the loading state — the
// actual glyph rendering needs a device/emulator run.
@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun QuranMushafScreenPreview() {
    QuranMushafScreen(
        uiState = QuranMushafUiState(isLoading = true, pageNumber = 1),
    )
}
