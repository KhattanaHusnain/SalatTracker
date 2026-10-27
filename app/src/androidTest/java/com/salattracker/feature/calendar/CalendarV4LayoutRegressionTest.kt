package com.salattracker.feature.calendar

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.salattracker.core.theme.SalatTrackerTheme
import com.salattracker.domain.model.Gender
import com.salattracker.domain.model.PrayerEntry
import com.salattracker.domain.model.PrayerStatus
import com.salattracker.domain.model.PrayerType
import com.salattracker.domain.repository.PrayerSlot
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class CalendarV4LayoutRegressionTest(private val width: Int, private val fontScale: Float) {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val today = LocalDate.of(2026, 10, 18)

    @Test
    fun monthAndEditBar_fitViewport() {
        render(sampleState(isEditMode = true, selected = setOf(PrayerSlot(today.minusDays(1), PrayerType.FAJR))))
        compose.onNodeWithTag("calendar-month-grid").assertIsDisplayed()
        compose.onNodeWithTag("calendar-edit-bar").assertIsDisplayed()
        screenshot("edit")
        assertSingleLine("October 2026")
        assertSingleLine("1 selected")
        assertSingleLine("All")
        assertSingleLine("Maghrib")
    }

    @Test
    fun daySheet_statusControlsRemainVisible() {
        val friday = LocalDate.of(2026, 10, 16)
        render(sampleState(daySheetDate = friday))
        compose.onNodeWithTag("calendar-day-sheet").assertIsDisplayed()
        compose.onNodeWithText("Jumu'ah").assertIsDisplayed()
        compose.onNodeWithTag("calendar-sheet-status-row-FAJR").assertIsDisplayed()
        val ishaInSheet = hasText("Isha") and
            hasAnyAncestor(hasTestTag("calendar-sheet-prayer-row-ISHA"))
        compose.onNode(ishaInSheet).performScrollTo()
        repeat(2) { compose.onNodeWithTag("calendar-day-sheet-content").performTouchInput { swipeUp() } }
        compose.onNode(ishaInSheet).assertIsDisplayed()
        screenshot("sheet")
    }

    private fun assertSingleLine(text: String) {
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText(text).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        val layout = layouts.single()
        val paintedExcess = (0 until layout.lineCount).maxOfOrNull {
            layout.getLineRight(it) - layout.getLineLeft(it) - layout.size.width
        } ?: 0f
        assertEquals("$text line count at ${width}dp/$fontScale", 1, layout.lineCount)
        assertFalse(
            "$text overflow width=${layout.didOverflowWidth} height=${layout.didOverflowHeight} paintedExcess=$paintedExcess",
            layout.didOverflowHeight || (layout.didOverflowWidth && paintedExcess > 1f),
        )
        assertEquals(text.length, layout.getLineEnd(0))
    }

    private fun render(state: CalendarV4UiState) {
        compose.setContent {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val device = LocalDensity.current
                val viewportDensity = minOf(device.density, constraints.maxWidth.toFloat() / width)
                CompositionLocalProvider(LocalDensity provides Density(viewportDensity, fontScale)) {
                    SalatTrackerTheme {
                        Box(Modifier.requiredSize(width.dp, 800.dp).testTag("calendar-v4-root")) {
                            CalendarV4Screen(
                                state = state,
                                onPreviousMonth = {}, onNextMonth = {}, onDayTapped = {}, onQazaOnly = {},
                                onPrayerFilter = {}, onEditToggle = {}, onClearSelection = {}, onSelectRange = { _, _ -> },
                                onCatchUp = {}, onPrepareBatch = {}, onFillEmptyOnly = {}, onDismissPreview = {},
                                onApplyPreview = {}, onUndo = {}, onDismissDay = {}, onSaveDayStatus = { _, _ -> },
                                onErrorDismiss = {}, onOpenQaza = {},
                            )
                        }
                    }
                }
            }
        }
    }

    private fun sampleState(
        isEditMode: Boolean = false,
        selected: Set<PrayerSlot> = emptySet(),
        daySheetDate: LocalDate? = null,
    ): CalendarV4UiState {
        val friday = LocalDate.of(2026, 10, 16)
        val entries = mapOf(
            friday to mapOf(
                PrayerType.FAJR to PrayerEntry(1, "local", friday, PrayerType.FAJR, PrayerStatus.CONGREGATION, 1, false),
                PrayerType.DHUHR to PrayerEntry(2, "local", friday, PrayerType.DHUHR, PrayerStatus.QAZA_PENDING, 1, true),
            ),
        )
        return CalendarV4UiState(
            today = today,
            visibleMonth = YearMonth.from(today),
            trackingStart = today.minusMonths(2),
            entries = entries,
            selectedTimes = PrayerType.values().associateWith { LocalTime.of(12, 0) },
            todayTimes = PrayerType.values().associateWith { LocalTime.of(4, 0) },
            gender = Gender.MALE,
            isLoading = false,
            isEditMode = isEditMode,
            selectedSlots = selected,
            daySheetDate = daySheetDate,
        )
    }

    private fun screenshot(name: String) {
        if ((width == 320 && fontScale == 1.3f) || (width == 360 && fontScale == 1f) || width == 600) {
            val dir = File(InstrumentationRegistry.getInstrumentation().targetContext.externalCacheDir, "calendar-v4").apply { mkdirs() }
            val target = if (name == "sheet") "calendar-day-sheet" else "calendar-v4-root"
            val bitmap = compose.onNodeWithTag(target).captureToImage().asAndroidBitmap()
            File(dir, "$name-${width}dp-$fontScale.png").outputStream().use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        }
    }

    companion object {
        @JvmStatic @Parameterized.Parameters(name = "width={0}dp fontScale={1}")
        fun cases(): List<Array<Any>> = listOf(320, 360, 600).flatMap { width ->
            listOf(1f, 1.3f).map { scale -> arrayOf<Any>(width, scale) }
        }
    }
}
