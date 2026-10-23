package com.salattracker.feature.quran

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salattracker.R
import com.salattracker.core.theme.*
import com.salattracker.domain.model.Chapter
import com.salattracker.domain.model.QuranReadHistoryEntry
import com.salattracker.domain.model.RevelationPlace
import java.util.Locale

// ── Quran chapter list — the tab's landing screen ────────────────────────────

@Composable
fun QuranChapterListScreen(
    uiState: QuranChapterListUiState,
    onChapterClick: (Int) -> Unit,
    onJuzClick: (juzNumber: Int, pageNumber: Int) -> Unit,
    onTabSelected: (QuranBrowseTab) -> Unit,
    onSearchDialogDismiss: () -> Unit,
) {
    if (uiState.showSearchDialog) {
        QuranNameSearchDialog(
            chapters = uiState.chapters,
            juzEntries = uiState.juzEntries,
            onChapterClick = onChapterClick,
            onJuzClick = onJuzClick,
            onDismiss = onSearchDialogDismiss,
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        BrowseTabRow(selected = uiState.selectedTab, onTabSelected = onTabSelected)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        ) {
            if (uiState.selectedTab == QuranBrowseTab.SURAH) {
                items(uiState.chapters, key = { "surah-${it.number}" }) { chapter ->
                    ChapterRow(chapter = chapter, onClick = { onChapterClick(chapter.number) })
                    Spacer(Modifier.height(8.dp))
                }
            } else {
                items(uiState.juzEntries, key = { "juz-${it.juzNumber}" }) { juz ->
                    JuzRow(juz = juz, onClick = { onJuzClick(juz.juzNumber, juz.pageNumber) })
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun BrowseTabRow(selected: QuranBrowseTab, onTabSelected: (QuranBrowseTab) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BrowseTabChip(
            label = stringResource(R.string.quran_tab_juz),
            isSelected = selected == QuranBrowseTab.JUZ,
            onClick = { onTabSelected(QuranBrowseTab.JUZ) },
            modifier = Modifier.weight(1f),
        )
        BrowseTabChip(
            label = stringResource(R.string.quran_tab_surah),
            isSelected = selected == QuranBrowseTab.SURAH,
            onClick = { onTabSelected(QuranBrowseTab.SURAH) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun QuranNameSearchDialog(
    chapters: List<Chapter>,
    juzEntries: List<QuranJuzUi>,
    onChapterClick: (Int) -> Unit,
    onJuzClick: (Int, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val matchingChapters = remember(query, chapters) {
        if (query.isBlank()) emptyList() else chapters.filter { it.matchesQuranName(query) }
    }
    val matchingJuz = remember(query, juzEntries) {
        if (query.isBlank()) emptyList() else juzEntries.filter { it.matchesQuranTitle(query) }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SalatSurfaceContainerLowest,
        title = {
            Text(
                stringResource(R.string.quran_search_title),
                color = SalatOnSurface, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, fontFamily = AppTitleFontFamily,
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = {
                        Text(
                            stringResource(R.string.quran_search_hint),
                            fontFamily = AppUiFontFamily,
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SalatPrimary,
                        unfocusedBorderColor = SalatOutlineVariant,
                        focusedContainerColor = SalatSurfaceContainerLow,
                        unfocusedContainerColor = SalatSurfaceContainerLow,
                    ),
                    shape = RoundedCornerShape(8.dp),
                )
                Spacer(Modifier.height(12.dp))
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp)) {
                    if (query.isNotBlank() && matchingChapters.isEmpty() && matchingJuz.isEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.quran_search_no_results),
                                color = SalatOnSurfaceVariant,
                                fontSize = 13.sp,
                                fontFamily = AppUiFontFamily,
                                modifier = Modifier.padding(vertical = 12.dp),
                            )
                        }
                    }
                    if (matchingChapters.isNotEmpty()) {
                        item { SearchSectionLabel(stringResource(R.string.quran_tab_surah)) }
                        items(matchingChapters, key = { "search-surah-${it.number}" }) { chapter ->
                            QuranSearchResult(
                                primary = chapter.nameTransliteration,
                                secondary = chapter.nameTranslation,
                                arabic = chapter.nameArabic,
                                onClick = { onChapterClick(chapter.number); onDismiss() },
                            )
                        }
                    }
                    if (matchingJuz.isNotEmpty()) {
                        item { SearchSectionLabel(stringResource(R.string.quran_tab_juz)) }
                        items(matchingJuz, key = { "search-juz-${it.juzNumber}" }) { juz ->
                            QuranSearchResult(
                                primary = juz.title.transliteration,
                                secondary = stringResource(R.string.quran_juz_label, juz.juzNumber),
                                arabic = juz.title.arabic,
                                onClick = { onJuzClick(juz.juzNumber, juz.pageNumber); onDismiss() },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel), color = SalatOnSurfaceVariant, fontFamily = AppUiFontFamily) }
        },
    )
}

@Composable
private fun SearchSectionLabel(label: String) {
    Text(
        text = label,
        color = SalatPrimary,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        fontFamily = AppUiFontFamily,
        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
    )
}

@Composable
private fun QuranSearchResult(
    primary: String,
    secondary: String,
    arabic: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(primary, color = SalatOnSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFamily = AppTitleFontFamily)
            Text(secondary, color = SalatOnSurfaceVariant,  = 12.sp, fontFamily = AppUiFontFamily)
        }
        Text(arabic, color = SalatOnSurface, fontSize = 18.sp, fontFamily = AmiriQuranFamily, textAlign = TextAlign.End)
    }
}

private fun Chapter.matchesQuranName(query: String): Boolean = listOf(
    nameArabic,
    nameTransliteration,
    nameTranslation,
).any { it.normalizedQuranSearch().contains(query.normalizedQuranSearch()) }

private fun QuranJuzUi.matchesQuranTitle(query: String): Boolean = listOf(
    title.arabic,
    title.transliteration,
).any { it.normalizedQuranSearch().contains(query.normalizedQuranSearch()) }

private fun String.normalizedQuranSearch(): String = trim()
    .lowercase(Locale.ROOT)
    .replace(Regex("[\\u064B-\\u065F\\u0670]"), "")
    .replace('أ', 'ا')
    .replace('إ', 'ا')
    .replace('آ', 'ا')
    .replace('ى', 'ي')

@Composable
private fun BrowseTabChip(label: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick  = onClick,
        modifier = modifier,
        shape    = RoundedCornerShape(10.dp),
        color    = if (isSelected) SalatPrimary else SalatSurfaceContainerLow,
        border   = BorderStroke(1.dp, if (isSelected) SalatPrimary else SalatOutlineVariant),
    ) {
        Text(
            text       = label,
            modifier   = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            textAlign  = TextAlign.Center,
            color      = if (isSelected) SalatOnPrimary else SalatOnSurfaceVariant,
            fontSize   = 13.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = AppUiFontFamily,
        )
    }
}

@Composable
private fun JuzRow(juz: QuranJuzUi, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SalatSurfaceContainerLow)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(CircleShape).background(SalatPrimaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(juz.juzNumber.toString(), color = SalatOnPrimaryContainer, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = AppUiFontFamily)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = juz.title.transliteration,
                color = SalatOnSurface, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, fontFamily = AppTitleFontFamily,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${juz.pageCount} pages · ${juz.ayahCount} ayahs",
                color = SalatOnSurfaceVariant,  = 12.sp, fontFamily = AppUiFontFamily,
            )
        }
        Text(
            text = juz.title.arabic,
            color = SalatOnSurface, fontSize = 20.sp, fontFamily = AmiriQuranFamily, textAlign = TextAlign.End,
        )
    }
}


@Composable
private fun ChapterRow(chapter: Chapter, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SalatSurfaceContainerLow)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(CircleShape).background(SalatPrimaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(chapter.number.toString(), color = SalatOnPrimaryContainer, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = AppUiFontFamily)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(chapter.nameTransliteration, color = SalatOnSurface, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, fontFamily = AppTitleFontFamily)
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${chapter.nameTranslation} · ${chapter.versesCount} " + stringResource(R.string.quran_verses_suffix),
                color = SalatOnSurfaceVariant, fontSize = 12.sp, fontFamily = AppUiFontFamily,
            )
        }
        Text(
            text = chapter.nameArabic,
            color = SalatOnSurface, fontSize = 20.sp, fontFamily = AmiriQuranFamily, textAlign = TextAlign.End,
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun QuranChapterListScreenPreview() {
    val sample = (1..10).map { n ->
        Chapter(
            number = n, nameArabic = "الفاتحة",
            nameTransliteration = "Al-Fatihah $n", nameTranslation = "The Opener",
            versesCount = 7, revelationPlace = RevelationPlace.MECCAN, revelationOrder = 5,
        )
    }
    QuranChapterListScreen(
        uiState = QuranChapterListUiState(
            isLoading = false,
            chapters = sample,
            continueReading = QuranReadHistoryEntry(chapterNumber = 2, verseNumber = 14),
        ),
        onChapterClick = {},
        onJuzClick = { _, _ -> },
        onTabSelected = {},
        onSearchDialogDismiss = {},
    )
}
