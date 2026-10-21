package com.salattracker.feature.quran

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salattracker.R
import com.salattracker.core.theme.*
import com.salattracker.core.ui.StatTile
import com.salattracker.domain.model.Chapter
import com.salattracker.domain.model.RevelationPlace

// ── Quran chapter info — structural facts only, no narrative description yet (see the
// ViewModel's doc comment for why). ──

@Composable
fun QuranChapterInfoScreen(uiState: QuranChapterInfoUiState, onReadClick: (Int) -> Unit) {
    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = SalatPrimary)
        }
        return
    }

    val chapter = uiState.chapter ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = 32.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(chapter.nameArabic, color = SalatOnSurface, fontSize = 32.sp, fontWeight = FontWeight.SemiBold, fontFamily = InterFamily)
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${chapter.nameTransliteration} · ${chapter.nameTranslation}",
                color = SalatOnSurfaceVariant, fontSize = 14.sp, fontFamily = InterFamily,
            )
        }
        Spacer(Modifier.height(20.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(
                label = stringResource(R.string.quran_info_revelation_place),
                value = stringResource(if (chapter.revelationPlace == RevelationPlace.MECCAN) R.string.quran_revelation_place_meccan else R.string.quran_revelation_place_medinan),
                modifier = Modifier.weight(1f),
            )
            StatTile(
                label = stringResource(R.string.quran_info_revelation_order),
                value = stringResource(R.string.quran_info_revelation_order_value, chapter.revelationOrder),
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(
                label = stringResource(R.string.quran_info_verse_count),
                value = chapter.versesCount.toString(),
                modifier = Modifier.weight(1f),
            )
            StatTile(
                label = stringResource(R.string.quran_info_juz),
                value = if (uiState.firstJuz == uiState.lastJuz) uiState.firstJuz.toString() else "${uiState.firstJuz}–${uiState.lastJuz}",
                modifier = Modifier.weight(1f),
            )
            StatTile(
                label = stringResource(R.string.quran_info_page),
                value = if (uiState.firstPage == uiState.lastPage) uiState.firstPage.toString() else "${uiState.firstPage}–${uiState.lastPage}",
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { onReadClick(chapter.number) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SalatPrimary, contentColor = SalatOnPrimary),
        ) {
            Text(stringResource(R.string.quran_info_start_reading), fontFamily = InterFamily, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun QuranChapterInfoScreenPreview() {
    QuranChapterInfoScreen(
        uiState = QuranChapterInfoUiState(
            isLoading = false,
            chapter = Chapter(
                number = 2, nameArabic = "البقرة",
                nameTransliteration = "Al-Baqarah", nameTranslation = "The Cow",
                versesCount = 286, revelationPlace = RevelationPlace.MEDINAN, revelationOrder = 87,
            ),
            firstJuz = 1, lastJuz = 3, firstPage = 2, lastPage = 49,
        ),
        onReadClick = {},
    )
}
