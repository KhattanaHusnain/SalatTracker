package com.salattracker.feature.quran

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.Gson
import com.salattracker.core.theme.SalatTrackerTheme
import com.salattracker.domain.model.Chapter
import com.salattracker.domain.model.MushafLine
import com.salattracker.domain.model.MushafPage
import com.salattracker.domain.model.MushafWord
import com.salattracker.domain.model.RevelationPlace
import com.salattracker.domain.model.TranslatedVerseText
import com.salattracker.domain.model.VerseLocation
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/** Uses the actual page fonts and glyphs: a JVM-only test cannot detect Android text reflow. */
@RunWith(Parameterized::class)
class QuranArabicLineLayoutTest(private val widthDp: Int, private val fontScale: Float) {
    @get:Rule val compose = createComposeRule()

    @Test fun translatedJuz_keepsPrintedLinesAndUrduChunksTogether() {
        val page = page(1)
        val chunks = readerLines(page).associate { it.words.first().lineNumber to it.translationChunks }
        showReader {
            QuranMushafScreen(
                QuranMushafUiState(
                    isLoading = false,
                    page = page,
                    juzStartVerses = setOf(VerseLocation(1, 1)),
                    juzStartVerse = VerseLocation(1, 1),
                    showTranslations = true,
                    translationChunks = chunks,
                )
            )
        }
        saveScreenshot("juz-translated")
        assertPageOneRegressions(page)
        for (line in page.lines) {
            assertSingleLine(line)
            val urdu = chunks[line.lineNumber].orEmpty().joinToString(" ") { it.text }
            if (urdu.isNotEmpty()) {
                val arabicNode = compose.onNodeWithText(line.glyphText())
                val urduNode = compose.onNodeWithText(urdu)
                val arabicBounds = arabicNode.fetchSemanticsNode().boundsInRoot
                val urduBounds = urduNode.fetchSemanticsNode().boundsInRoot
                assertTrue("Urdu must follow its Arabic row", urduBounds.top >= arabicBounds.bottom)
                val results = mutableListOf<TextLayoutResult>()
                urduNode.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
                assertTrue(results.isNotEmpty())
                assertFalse("Urdu must stay outside the white-on-black Arabic highlight",
                    results.single().layoutInput.style.color == androidx.compose.ui.graphics.Color.White)
            }
        }
    }

    @Test fun arabicOnlyMushaf_keepsPageOneOnItsPrintedLines() {
        val page = page(1)
        showReader {
            QuranMushafScreen(QuranMushafUiState(isLoading = false, page = page))
        }
        saveScreenshot("mushaf-arabic")
        assertPageOneRegressions(page)
        page.lines.forEach(::assertSingleLine)
    }

    @Test fun surah_keepsItsExistingLineLayout() {
        val page = page(1)
        val lines = readerLines(page)
        showReader {
            QuranReaderScreen(
                uiState = QuranReaderUiState(
                    isLoading = false,
                    chapter = Chapter(1, "الفاتحة", "Al-Fatihah", "The Opening", 7, RevelationPlace.MECCAN, 5),
                    lines = lines,
                    pages = listOf(ReaderPage(1, lines)),
                ),
                onChapterInfoClick = {},
                onBookmarkToggle = {},
            )
        }
        saveScreenshot("surah")
        assertPageOneRegressions(page)
        page.lines.forEach(::assertSingleLine)
    }

    @Test fun croppedJuzPage_startsAtBoundaryAndKeepsRemainingLinesIntact() {
        // Juz 7 begins at 5:82 partway down physical page 121.
        val page = page(121)
        val start = VerseLocation(5, 82)
        val firstIndex = page.lines.indexOfFirst { line ->
            line.words.any { it.chapterNumber == 5 && it.verseNumber == 82 && it.position == 1 }
        }
        assertTrue("Fixture must exercise a shared page", firstIndex > 0)
        val chunks = readerLines(page).associate { it.words.first().lineNumber to it.translationChunks }
        showReader {
            QuranMushafScreen(
                QuranMushafUiState(
                    isLoading = false,
                    pageNumber = 121,
                    page = page,
                    juzStartVerses = setOf(start),
                    juzStartVerse = start,
                    showTranslations = true,
                    translationChunks = chunks,
                )
            )
        }
        page.lines.take(firstIndex).forEach { line ->
            compose.onNodeWithText(line.glyphText()).assertDoesNotExist()
        }
        saveScreenshot("juz-cropped")
        assertSingleLine(page.lines[firstIndex])
        page.lines.drop(firstIndex).forEach(::assertSingleLine)
    }

    private fun showReader(content: @Composable () -> Unit) {
        compose.setContent {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                // Fit the whole requested dp viewport on a phone, including the 600dp case.
                val density = minOf(LocalDensity.current.density, constraints.maxWidth.toFloat() / widthDp)
                CompositionLocalProvider(LocalDensity provides Density(density, fontScale)) {
                    SalatTrackerTheme(darkTheme = false) {
                        Box(Modifier.requiredWidth(widthDp.dp).fillMaxHeight().testTag("reader")) {
                            content()
                        }
                    }
                }
            }
        }
    }

    private fun assertPageOneRegressions(page: MushafPage) {
        val combined = page.lines.single { it.lineNumber == 11 }
        assertEquals(setOf(3, 4), combined.words.map { it.verseNumber }.toSet())
        assertSingleLine(combined)
        val ihdina = page.lines.single { it.lineNumber == 12 }
        assertTrue(ihdina.words.any { it.verseNumber == 6 && it.position == 1 })
        assertSingleLine(ihdina)
    }

    private fun assertSingleLine(line: MushafLine) {
        val text = line.glyphText()
        compose.onNode(hasScrollAction()).performScrollToNode(hasText(text))
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText(text).performSemanticsAction(SemanticsActions.GetTextLayoutResult) {
            it(layouts)
        }
        assertEquals("One text layout for printed line ${line.lineNumber}", 1, layouts.size)
        val layout = layouts.single()
        assertEquals("Printed line ${line.lineNumber} must not wrap", 1, layout.lineCount)
        assertFalse(
            "Printed line ${line.lineNumber} must fit without clipping: " +
                "widthOverflow=${layout.didOverflowWidth}, heightOverflow=${layout.didOverflowHeight}, " +
                "size=${layout.size}, paragraphWidth=${layout.multiParagraph.width}, " +
                "paragraphHeight=${layout.multiParagraph.height}, fontSize=${layout.layoutInput.style.fontSize}",
            layout.hasVisualOverflow,
        )
        assertEquals("Every word and ayah marker must remain visible", text.length, layout.getLineEnd(0))
    }

    private fun saveScreenshot(mode: String) {
        val bitmap = compose.onNodeWithTag("reader").captureToImage().asAndroidBitmap()
        File(context.externalCacheDir ?: context.cacheDir, "quran-layout-$mode-$widthDp-$fontScale.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    private fun page(number: Int): MushafPage {
        val words = allWords.filter { it.pageNumber == number }
        return MushafPage(
            number,
            words.groupBy { it.lineNumber }.map { (line, lineWords) -> MushafLine(line, lineWords) },
            VerseLocation(words.first().chapterNumber, words.first().verseNumber),
            VerseLocation(words.last().chapterNumber, words.last().verseNumber),
        )
    }

    private fun readerLines(page: MushafPage): List<ReaderLine> =
        page.lines.flatMap { it.words }.map { it.chapterNumber }.distinct().flatMap { chapter ->
            allWords.filter { it.chapterNumber == chapter }
                .groupBy { it.pageNumber to it.lineNumber }
                .map { (_, words) -> MushafLine(words.first().lineNumber, words) }
                .toReaderLines(allTranslations.filter { it.chapterNumber == chapter }.associate { it.verseNumber to it.text })
        }.filter { it.pageNumber == page.pageNumber }

    private fun MushafLine.glyphText() = words.joinToString(" ") { it.glyphCode }

    companion object {
        private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
        private val allWords: List<MushafWord> by lazy {
            context.assets.open("quran/mushaf_words.json").reader().use {
                Gson().fromJson(it, Array<MushafWord>::class.java).toList()
            }
        }
        private val allTranslations: List<TranslatedVerseText> by lazy {
            context.assets.open("quran/translations/ur_jalandhry.json").reader().use {
                Gson().fromJson(it, Array<TranslatedVerseText>::class.java).toList()
            }
        }

        @JvmStatic @Parameterized.Parameters(name = "width={0}dp fontScale={1}")
        fun viewports(): List<Array<Any>> = listOf(320, 360, 600).flatMap { width ->
            listOf(1f, 1.3f).map { scale -> arrayOf<Any>(width, scale) }
        }
    }
}
