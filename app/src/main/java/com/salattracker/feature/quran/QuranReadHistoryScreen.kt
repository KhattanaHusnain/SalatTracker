package com.salattracker.feature.quran

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.salattracker.core.ui.LocalUse24HourFormat
import com.salattracker.core.ui.formatAppTime
import com.salattracker.core.theme.*
import com.salattracker.domain.model.QuranReadHistoryEntry
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// ── Quran read history ─────────────────────────────────────────────────────────

@Composable
fun QuranReadHistoryScreen(
    uiState: QuranReadHistoryUiState,
    onEntryClick: (Int) -> Unit,
    onClearHistory: () -> Unit,
) {
    if (!uiState.isLoading && uiState.entries.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.quran_no_read_history),
                color = SalatOnSurfaceVariant, fontSize = 14.sp, fontFamily = AppUiFontFamily, textAlign = TextAlign.Center,
            )
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (uiState.entries.isNotEmpty()) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onClearHistory) {
                    Text(stringResource(R.string.quran_clear_history), color = SalatPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = AppUiFontFamily)
                }
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
        ) {
            items(uiState.entries, key = { it.entry.id }) { item ->
                ReadHistoryRow(item = item, use24HourFormat = LocalUse24HourFormat.current, onClick = { onEntryClick(item.entry.chapterNumber) })
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ReadHistoryRow(item: QuranReadHistoryUi, use24HourFormat: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SalatSurfaceContainerLow)
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(item.chapterName, color = SalatOnSurface, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, fontFamily = AppTitleFontFamily)
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.quran_bookmark_verse_label, item.entry.verseNumber),
                color = SalatOnSurfaceVariant, fontSize = 12.sp, fontFamily = AppUiFontFamily,
            )
        }
        Text(formatReadAt(item.entry.readAt, use24HourFormat), color = SalatOnSurfaceVariant,  = 12.sp, fontFamily = AppUiFontFamily)
    }
}

private fun formatReadAt(epochMillis: Long, use24HourFormat: Boolean): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).let {
        "${it.toLocalDate().format(DateTimeFormatter.ofPattern(\"d MMM\"))}, ${it.toLocalTime().formatAppTime(use24HourFormat)}"
    }

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun QuranReadHistoryScreenPreview() {
    QuranReadHistoryScreen(
        uiState = QuranReadHistoryUiState(
            isLoading = false,
            entries = listOf(
                QuranReadHistoryUi(QuranReadHistoryEntry(id = 1, chapterNumber = 2, verseNumber = 1), chapterName = "Al-Baqarah"),
                QuranReadHistoryUi(QuranReadHistoryEntry(id = 2, chapterNumber = 1, verseNumber = 1), chapterName = "Al-Fatihah"),
            ),
        ),
        onEntryClick = {},
        onClearHistory = {},
    )
}
