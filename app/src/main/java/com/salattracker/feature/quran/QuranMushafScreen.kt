package com.salattracker.feature.quran

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salattracker.R
import com.salattracker.core.theme.*
import com.salattracker.domain.model.MushafPage

// ── Mushaf (print-accurate paginated) reader. Every word is rendered using that printed page's
// own QCF glyph font, bundled as an asset (assets/quran/mushaf_fonts/v2/pN.ttf) — see
// QuranSeedWorker — so line breaks match a real Mushaf exactly and the whole mode works with no
// connection at all, unlike the regular verse-by-verse QuranReaderScreen's natural text reflow. ──

@Composable
fun QuranMushafScreen(
    uiState: QuranMushafUiState,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SalatPrimary)
            }
            uiState.errorMessage != null -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(uiState.errorMessage, color = SalatOnSurfaceVariant, fontSize = 14.sp, fontFamily = InterFamily)
            }
            uiState.page != null -> MushafPageContent(page = uiState.page)
        }
    }
}

// A QCF page font bakes its own per-line kashida justification into the glyph outlines
// themselves — every line on a page is already shaped to the same fixed design width at the
// font's natural size, that width just doesn't match this device's screen. So rather than
// wrapping/scrolling an overflowing line (which breaks the print-accurate line breaks that are
// the whole point of this mode), every line is measured once at a reference size and the whole
// page is rendered at one computed font size that makes the widest line exactly fill the screen.
private val REFERENCE_FONT_SIZE = 40.sp
private const val PAGE_WIDTH_FRACTION = 0.94f

@Composable
private fun MushafPageContent(page: MushafPage) {
    val assets = LocalContext.current.assets
    val fontFamily = remember(page.pageNumber) {
        FontFamily(Font("quran/mushaf_fonts/v2/p${page.pageNumber}.ttf", assets))
    }

    val lineTexts = remember(page) {
        page.lines.map { line -> line.lineNumber to line.words.joinToString(" ") { it.glyphCode } }
    }
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val availableWidthPx = with(density) { (maxWidth * PAGE_WIDTH_FRACTION).toPx() }
        val fittedFontSize = remember(lineTexts, fontFamily, availableWidthPx) {
            val style = TextStyle(fontFamily = fontFamily, fontSize = REFERENCE_FONT_SIZE, textDirection = TextDirection.Rtl)
            val widestLinePx = lineTexts
                .maxOfOrNull { (_, text) -> textMeasurer.measure(text = text, style = style).size.width }
                ?.takeIf { it > 0 }
                ?: return@remember REFERENCE_FONT_SIZE
            REFERENCE_FONT_SIZE * (availableWidthPx / widestLinePx)
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .border(1.dp, SalatOutlineVariant),
            contentPadding = PaddingValues(0.dp),
        ) {
            itemsIndexed(lineTexts, key = { _, (lineNumber, _) -> lineNumber }) { _, (_, text) ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SalatOutlineVariant)
                        .padding(vertical = 5.dp),
                ) {
                    Text(
                        text = text,
                        fontFamily = fontFamily,
                        fontSize = fittedFontSize,
                        style = TextStyle(textDirection = TextDirection.Rtl, textAlign = TextAlign.Center),
                        color = SalatOnSurface,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
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
