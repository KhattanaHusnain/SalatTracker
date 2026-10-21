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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalTextStyle
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salattracker.R
import com.salattracker.core.theme.*
import com.salattracker.domain.model.Chapter
import com.salattracker.domain.model.QuranReadHistoryEntry
import com.salattracker.domain.model.RevelationPlace

// ── Quran chapter list — the tab's landing screen ────────────────────────────

@Composable
fun QuranChapterListScreen(
    uiState: QuranChapterListUiState,
    onChapterClick: (Int) -> Unit,
    onContinueReadingClick: (chapterNumber: Int, verseNumber: Int) -> Unit,
    onJuzClick: (juzNumber: Int, pageNumber: Int) -> Unit,
    onTabSelected: (QuranBrowseTab) -> Unit,
    onJumpDialogDismiss: () -> Unit,
    onJumpTypeSelected: (QuranJumpType) -> Unit,
    onJumpConfirm: (String) -> Unit,
) {
    val continueReading = uiState.continueReading?.let { last ->
        uiState.chapters.find { it.number == last.chapterNumber }?.let { it to last.verseNumber }
    }

    if (uiState.showJumpDialog) {
        QuranJumpDialog(
            jumpType = uiState.jumpType,
            isError = uiState.jumpError,
            onTypeSelected = onJumpTypeSelected,
            onConfirm = onJumpConfirm,
            onDismiss = onJumpDialogDismiss,
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        BrowseTabRow(selected = uiState.selectedTab, onTabSelected = onTabSelected)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        ) {
            if (uiState.selectedTab == QuranBrowseTab.SURAH) {
                if (continueReading != null) {
                    val (chapter, verseNumber) = continueReading
                    item {
                        ContinueReadingCard(
                            chapter = chapter,
                            verseNumber = verseNumber,
                            onClick = { onContinueReadingClick(chapter.number, verseNumber) },
                        )
                        Spacer(Modifier.height(16.dp))
                    }
                }
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
            label = stringResource(R.string.quran_tab_surah),
            isSelected = selected == QuranBrowseTab.SURAH,
            onClick = { onTabSelected(QuranBrowseTab.SURAH) },
            modifier = Modifier.weight(1f),
        )
        BrowseTabChip(
            label = stringResource(R.string.quran_tab_juz),
            isSelected = selected == QuranBrowseTab.JUZ,
            onClick = { onTabSelected(QuranBrowseTab.JUZ) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun QuranJumpDialog(
    jumpType: QuranJumpType,
    isError: Boolean,
    onTypeSelected: (QuranJumpType) -> Unit,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var input by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SalatSurfaceContainerLowest,
        title = {
            Text(
                stringResource(R.string.quran_jump_title),
                color = SalatOnSurface, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, fontFamily = MontserratFamily,
            )
        },
        text = {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BrowseTabChip(
                        label = stringResource(R.string.quran_jump_page),
                        isSelected = jumpType == QuranJumpType.PAGE,
                        onClick = { onTypeSelected(QuranJumpType.PAGE) },
                        modifier = Modifier.weight(1f),
                    )
                    BrowseTabChip(
                        label = stringResource(R.string.quran_jump_hizb),
                        isSelected = jumpType == QuranJumpType.HIZB,
                        onClick = { onTypeSelected(QuranJumpType.HIZB) },
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it.filter { ch -> ch.isDigit() } },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = isError,
                    label = {
                        Text(
                            stringResource(if (jumpType == QuranJumpType.PAGE) R.string.quran_jump_page_label else R.string.quran_jump_hizb_label),
                            fontFamily = InterFamily,
                        )
                    },
                    supportingText = if (isError) {
                        { Text(stringResource(R.string.quran_jump_error), fontFamily = InterFamily) }
                    } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = LocalTextStyle.current.copy(fontFamily = InterFamily, fontSize = 14.sp, color = SalatOnSurface),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SalatPrimary,
                        unfocusedBorderColor = SalatOutlineVariant,
                        focusedContainerColor = SalatSurfaceContainerLow,
                        unfocusedContainerColor = SalatSurfaceContainerLow,
                    ),
                    shape = RoundedCornerShape(8.dp),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(input) },
                colors = ButtonDefaults.buttonColors(containerColor = SalatPrimary, contentColor = SalatOnPrimary),
                shape = RoundedCornerShape(12.dp),
            ) { Text(stringResource(R.string.quran_jump_go), fontFamily = InterFamily, fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel), color = SalatOnSurfaceVariant, fontFamily = InterFamily) }
        },
    )
}

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
            fontFamily = InterFamily,
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
            Text(juz.juzNumber.toString(), color = SalatPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = InterFamily)
        }
        Column {
            Text(
                text = stringResource(R.string.quran_juz_label, juz.juzNumber),
                color = SalatOnSurface, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, fontFamily = MontserratFamily,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = juz.chapterNames.joinToString(" · "),
                color = SalatOnSurfaceVariant, fontSize = 12.sp, fontFamily = InterFamily,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(
                    R.string.quran_juz_starts_at,
                    juz.chapterNames.firstOrNull().orEmpty(),
                    juz.verseNumber,
                ),
                color = SalatOnSurfaceVariant, fontSize = 11.sp, fontFamily = InterFamily,
            )
        }
    }
}

@Composable
private fun ContinueReadingCard(chapter: Chapter, verseNumber: Int, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SalatPrimaryContainer)
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Column {
            Text(
                text = stringResource(R.string.quran_continue_reading),
                color = SalatPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = InterFamily,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.quran_continue_reading_subtitle, chapter.nameTransliteration, verseNumber),
                color = SalatOnSurface, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, fontFamily = MontserratFamily,
            )
        }
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
            Text(chapter.number.toString(), color = SalatPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = InterFamily)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(chapter.nameTransliteration, color = SalatOnSurface, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, fontFamily = MontserratFamily)
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${chapter.nameTranslation} · ${chapter.versesCount} " + stringResource(R.string.quran_verses_suffix),
                color = SalatOnSurfaceVariant, fontSize = 12.sp, fontFamily = InterFamily,
            )
        }
        Text(
            text = chapter.nameArabic,
            color = SalatOnSurface, fontSize = 20.sp, fontFamily = InterFamily, textAlign = TextAlign.End,
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
        onContinueReadingClick = { _, _ -> },
        onJuzClick = { _, _ -> },
        onTabSelected = {},
        onJumpDialogDismiss = {},
        onJumpTypeSelected = {},
        onJumpConfirm = {},
    )
}
