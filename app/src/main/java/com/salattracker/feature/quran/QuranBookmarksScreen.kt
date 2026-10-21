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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salattracker.R
import com.salattracker.core.theme.*
import com.salattracker.domain.model.QuranBookmark

// ── Quran bookmarks list ──────────────────────────────────────────────────────

@Composable
fun QuranBookmarksScreen(
    uiState: QuranBookmarksUiState,
    onBookmarkClick: (Int) -> Unit,
    onDeleteClick: (Long) -> Unit,
) {
    if (!uiState.isLoading && uiState.bookmarks.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.quran_no_bookmarks),
                color = SalatOnSurfaceVariant, fontSize = 14.sp, fontFamily = AppUiFontFamily, textAlign = TextAlign.Center,
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
    ) {
        items(uiState.bookmarks, key = { it.bookmark.id }) { item ->
            BookmarkRow(item = item, onClick = { onBookmarkClick(item.bookmark.chapterNumber) }, onDeleteClick = { onDeleteClick(item.bookmark.id) })
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun BookmarkRow(item: QuranBookmarkUi, onClick: () -> Unit, onDeleteClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SalatSurfaceContainerLow)
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(item.chapterName, color = SalatOnSurface, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, fontFamily = AppTitleFontFamily)
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.quran_bookmark_verse_label, item.bookmark.fromVerse),
                color = SalatOnSurfaceVariant, fontSize = 12.sp, fontFamily = AppUiFontFamily,
            )
            if (!item.bookmark.note.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(item.bookmark.note, color = SalatOnSurfaceVariant, fontSize = 12.sp, fontFamily = AppUiFontFamily, maxLines = 2)
            }
        }
        IconButton(onClick = onDeleteClick) {
            Icon(painterResource(R.drawable.ic_delete), contentDescription = stringResource(R.string.quran_remove_bookmark), tint = SalatOnSurfaceVariant)
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun QuranBookmarksScreenPreview() {
    QuranBookmarksScreen(
        uiState = QuranBookmarksUiState(
            isLoading = false,
            bookmarks = listOf(
                QuranBookmarkUi(QuranBookmark(id = 1, chapterNumber = 2, fromVerse = 255, toVerse = 255, note = "Ayat al-Kursi"), chapterName = "Al-Baqarah"),
                QuranBookmarkUi(QuranBookmark(id = 2, chapterNumber = 36, fromVerse = 1, toVerse = 1), chapterName = "Ya-Sin"),
            ),
        ),
        onBookmarkClick = {},
        onDeleteClick = {},
    )
}
