package com.salattracker.feature.quran

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salattracker.core.theme.*
import com.salattracker.R
import com.salattracker.domain.model.Chapter
import com.salattracker.domain.model.RevelationPlace
import com.salattracker.domain.model.Verse

// ── Quran reader — continuous Mushaf-style flow: Bismillah centred above the surah, the first
// printed line set off with an inverted (dark) banner, every real Mushaf line rendered in
// print-accurate QCF glyphs (the same bundled per-page fonts as Mushaf mode — see
// MushafRepository) and scaled to fit its row. This keeps a printed line intact instead of
// allowing a trailing word from an ayah to wrap onto a separate screen line. Bookmark/tafsir
// actions are hidden for now, not removed — QuranReaderViewModel still exposes them underneath.

// Fixed Quran content, not app-UI chrome, so not an Android string resource that varies with the
// app's own interface locale — Bismillah is a single universal line shown before every surah
// except Al-Fatihah (where it IS verse 1 itself) and At-Tawbah (surah 9, which has none, by
// long-standing scholarly convention). The bundled QCF glyph data doesn't carry it as a word at
// all — each surah's own verse-1 QCF words start directly at that surah's real first word — so
// it's rendered with a regular Arabic-capable font instead of print-accurate QCF glyphs.
private const val BISMILLAH_ARABIC = "بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ"
private const val BISMILLAH_URDU = "اللہ کے نام سے جو بہت مہربان رحم کرنے والا ہے"
private const val SURAH_WITHOUT_BISMILLAH = 9

// The header's page/juz/revelation-place figures are Quran-content facts, like Bismillah above —
// shown in Arabic regardless of the app's own interface locale, using Arabic-Indic digits
// (٠١٢٣٤٥٦٧٨٩ — distinct from the Urdu block above) rather than the localized English
// "Page %d"/"Meccan · %d verses" strings used elsewhere in the app.
private val ARABIC_DIGITS = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
private fun Int.toArabicNumeral(): String = toString().map { ARABIC_DIGITS[it - '0'] }.joinToString("")
private const val MECCAN_ARABIC = "مكية"
private const val MEDINAN_ARABIC = "مدنية"
private const val PAGE_LABEL_ARABIC = "صفحة"
private const val JUZ_LABEL_ARABIC = "الجزء"
private const val VERSES_LABEL_ARABIC = "آية"

@Composable
fun QuranReaderScreen(
    uiState: QuranReaderUiState,
    onChapterInfoClick: (Int) -> Unit,
    onBookmarkToggle: (Int) -> Unit,
) {
    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = SalatPrimary)
        }
        return
    }

    val chapter = uiState.chapter ?: return
    val readerPage = uiState.pages.getOrNull(uiState.currentPageIndex)
    val pageVerse = uiState.verses.firstOrNull { it.pageNumber == readerPage?.pageNumber }?.verseNumber
    val listState = rememberLazyListState()

    LaunchedEffect(readerPage?.pageNumber) {
        if (readerPage != null) listState.scrollToItem(0)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        pageVerse?.let { verseNumber ->
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = { onBookmarkToggle(verseNumber) }) {
                    Icon(
                        painterResource(R.drawable.ic_bookmark),
                        stringResource(if (verseNumber in uiState.bookmarkedVerses) R.string.quran_remove_bookmark else R.string.quran_add_bookmark),
                        tint = SalatPrimary,
                    )
                }
            }
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        ) {
            if (uiState.currentPageIndex == 0) {
                item {
                    SurahHeaderFrame(
                        chapter = chapter,
                        pageNumber = readerPage?.pageNumber ?: uiState.verses.firstOrNull()?.pageNumber,
                        juzNumber = uiState.verses.firstOrNull { it.pageNumber == readerPage?.pageNumber }?.juzNumber
                            ?: uiState.verses.firstOrNull()?.juzNumber,
                        onClick = { onChapterInfoClick(chapter.number) },
                    )
                    Spacer(Modifier.height(14.dp))
                    if (chapter.number != 1 && chapter.number != SURAH_WITHOUT_BISMILLAH) {
                        BismillahLine()
                    }
                    Spacer(Modifier.height(14.dp))
                }
            }
            if (readerPage != null) {
                itemsIndexed(readerPage.lines) { index, line ->
                    ReaderLineBlock(
                        line = line,
                        isFirstLine = uiState.currentPageIndex == 0 && index == 0,
                    )
                }
            } else {
                // Robustness fallback only — shouldn't trigger in practice, since Mushaf data is
                // bundled for every verse.
                items(uiState.verses, key = { it.verseNumber }) { verse ->
                    Text(
                        text = verse.textUthmani,
                        color = SalatOnSurface, fontSize = 22.sp, fontFamily = AppUiFontFamily,
                        textAlign = TextAlign.End, lineHeight = 36.sp,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    uiState.translations[verse.verseNumber]?.let { translation ->
                        Spacer(Modifier.height(8.dp))
                        UrduTranslation(verseTranslationWithNumber(translation, verse.verseNumber))
                    }
                    Spacer(Modifier.height(18.dp))
                }
            }
        }
    }
}

/** A simple double-ruled frame around the surah banner, in the spirit of a printed Mushaf's own
 * page border — an original Compose-drawn frame, not a reproduction of any specific publisher's
 * artwork, with the page/juz figures we already have (see [Verse.pageNumber]/[Verse.juzNumber])
 * in its corners. The real Mushaf's ruku-count box isn't included: that needs ruku-boundary data
 * this app doesn't have bundled yet. */
@Composable
private fun SurahHeaderFrame(chapter: Chapter, pageNumber: Int?, juzNumber: Int?, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SalatOutlineVariant, RoundedCornerShape(5.dp))
            .padding(2.dp)
            .border(1.dp, SalatOutlineVariant, RoundedCornerShape(3.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        if (pageNumber != null || juzNumber != null) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = pageNumber?.let { "$PAGE_LABEL_ARABIC ${it.toArabicNumeral()}" } ?: "",
                    color = SalatOnSurfaceVariant,  fontSize = 12.sp, fontFamily = AppUiFontFamily,
                )
                Text(
                    text = juzNumber?.let { "$JUZ_LABEL_ARABIC ${it.toArabicNumeral()}" } ?: "",
                    color = SalatOnSurfaceVariant,  fontSize = 12.sp, fontFamily = AppUiFontFamily,
                )
            }
            Spacer(Modifier.height(4.dp))
        }
        ChapterHeader(chapter, onClick = onClick)
    }
}

@Composable
private fun ChapterHeader(chapter: Chapter, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Text(chapter.nameArabic, color = SalatOnSurface, fontSize = 30.sp, fontFamily = AmiriQuranFamily)
        Spacer(Modifier.height(2.dp))
        Text(
            text = "${if (chapter.revelationPlace == RevelationPlace.MECCAN) MECCAN_ARABIC else MEDINAN_ARABIC} · " +
                "${chapter.versesCount.toArabicNumeral()} $VERSES_LABEL_ARABIC",
            color = SalatOnSurfaceVariant, fontSize = 13.sp, fontFamily = AppUiFontFamily,
        )
    }
}

@Composable
private fun BismillahLine() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(BISMILLAH_ARABIC, color = SalatOnSurface, fontSize = 26.sp, fontFamily = AmiriQuranFamily, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        UrduTranslation(BISMILLAH_URDU, textAlign = TextAlign.Center)
    }
}

@Composable
private fun ReaderLineBlock(line: ReaderLine, isFirstLine: Boolean) {
    // A ruled pair of cells mirrors a printed Urdu Mushaf page while deliberately omitting the
    // vertical word-by-word columns until that feature is added.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SalatOutlineVariant),
    ) {
        if (isFirstLine) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                MushafLineArabic(line, textColor = Color.White)
            }
        } else {
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp)) {
                MushafLineArabic(line, textColor = SalatOnSurface)
            }
        }
        if (line.translationChunks.isNotEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(SalatOutlineVariant))
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp)) {
                UrduTranslation(line.translationChunks.joinToString(" ") { it.text })
            }
        }
    }
}

@Composable
private fun UrduTranslation(text: String, textAlign: TextAlign = TextAlign.Center) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val availableWidthPx = with(density) { maxWidth.toPx() }
        val fontSize = remember(text, textAlign, availableWidthPx) {
            val referenceSize = 14.sp
            val measuredWidth = textMeasurer.measure(
                text = text,
                style = TextStyle(fontFamily = NastaliqUrduFamily, fontSize = referenceSize, textDirection = TextDirection.Rtl),
            ).size.width
            if (measuredWidth <= 0) referenceSize else referenceSize *
                (availableWidthPx * 0.98f / measuredWidth).coerceAtMost(1f)
        }
        Text(
            text = text,
            color = SalatOnSurfaceVariant,
            fontSize = fontSize,
            fontFamily = NastaliqUrduFamily,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            style = TextStyle(textDirection = TextDirection.Rtl, textAlign = textAlign),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** One real printed Mushaf line's Arabic, in that page's own print-accurate QCF glyph font —
 * matching [QuranMushafScreen]. The QCF glyphs are designed for a fixed print width, so measure
 * the complete row first and scale it to the reader's actual width instead of letting Compose
 * reflow the final words. */
@Composable
private fun MushafLineArabic(line: ReaderLine, textColor: Color) {
    val context = LocalContext.current
    LaunchedEffect(line.pageNumber) { requestMushafAssets(context) }
    val fontFamily = remember(line.pageNumber) {
        mushafFontFile(context, line.pageNumber)?.let { FontFamily(Font(it)) } ?: AmiriQuranFamily
    }
    val lineText = remember(line.words) { line.words.joinToString(" ") { it.glyphCode } }
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val availableWidthPx = with(density) { maxWidth.toPx() }
        val fontSize = remember(lineText, fontFamily, availableWidthPx) {
            val referenceSize = 32.sp
            val measuredWidth = textMeasurer.measure(
                text = lineText,
                style = TextStyle(fontFamily = fontFamily, fontSize = referenceSize, textDirection = TextDirection.Rtl),
            ).size.width
            if (measuredWidth <= 0) referenceSize else referenceSize *
                (availableWidthPx * 0.98f / measuredWidth).coerceAtMost(1f)
        }
        Text(
            text = lineText,
            fontFamily = fontFamily,
            color = textColor,
            fontSize = fontSize,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            style = TextStyle(textDirection = TextDirection.Rtl, textAlign = TextAlign.Center),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun QuranReaderScreenPreview() {
    QuranReaderScreen(
        uiState = QuranReaderUiState(
            isLoading = false,
            chapter = Chapter(
                number = 2, nameArabic = "البقرة",
                nameTransliteration = "Al-Baqarah", nameTranslation = "The Cow",
                versesCount = 286, revelationPlace = RevelationPlace.MEDINAN, revelationOrder = 87,
            ),
            verses = listOf(
                Verse(2, 1, "الٓمٓ", 1, 1, 1, 2),
                Verse(2, 2, "ذَٰلِكَ ٱلْكِتَـٰبُ لَا رَيْبَ ۛ فِيهِ ۛ هُدًى لِّلْمُتَّقِينَ", 1, 1, 1, 2),
            ),
        ),
        onChapterInfoClick = {},
        onBookmarkToggle = {},
    )
}
