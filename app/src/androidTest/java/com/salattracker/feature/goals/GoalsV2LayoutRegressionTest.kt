package com.salattracker.feature.goals

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.salattracker.core.theme.SalatTrackerTheme
import com.salattracker.domain.model.GoalCategory
import com.salattracker.domain.model.GoalFrequency
import com.salattracker.domain.model.SpiritualGoal
import com.salattracker.domain.usecase.GoalDayState
import com.salattracker.domain.usecase.GoalStatus
import com.salattracker.domain.usecase.GoalStreak
import com.salattracker.domain.usecase.GoalTotals
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

enum class GoalsV2Case { EMPTY, LIST, NEW, DETAILS, LOG_DAY, EDIT_ADVANCED }

@RunWith(Parameterized::class)
class GoalsV2LayoutRegressionTest(
    private val width: Int,
    private val fontScale: Float,
    private val screen: GoalsV2Case,
    private val rtl: Boolean,
) {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val today = LocalDate.of(2026, 10, 18)

    @Test fun fullViewportActionsRemainReachable() {
        render()
        val tag = when (screen) {
            GoalsV2Case.EMPTY, GoalsV2Case.LIST -> "goals-v2-screen"
            GoalsV2Case.NEW -> "goal-new-screen"
            GoalsV2Case.DETAILS -> "goal-detail-screen"
            GoalsV2Case.LOG_DAY -> "goal-log-day-screen"
            GoalsV2Case.EDIT_ADVANCED -> "goal-edit-screen"
        }
        compose.onNodeWithTag(tag).assertIsDisplayed()
        when (screen) {
            GoalsV2Case.EMPTY -> assertSingleLine("Read Quran")
            GoalsV2Case.LIST -> {
                compose.onNodeWithTag("goals-hero").assertIsDisplayed()
                assertSingleLine("1 of 3")
                compose.onNodeWithText("Archived (1)").performScrollTo().performClick()
                compose.onNodeWithText("Old Quran goal").assertIsDisplayed()
            }
            GoalsV2Case.NEW, GoalsV2Case.EDIT_ADVANCED ->
                compose.onNodeWithTag("goal-form-save").assertIsDisplayed()
            GoalsV2Case.DETAILS -> compose.onNodeWithText("October 2026").assertIsDisplayed()
            GoalsV2Case.LOG_DAY -> compose.onNodeWithTag("goal-log-done").assertIsDisplayed()
        }
        screenshot(tag)
    }

    private fun assertSingleLine(text: String) {
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText(text).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        val layout = layouts.single()
        assertEquals("$text line count at ${width}dp/$fontScale", 1, layout.lineCount)
        assertFalse("$text overflow width=${layout.didOverflowWidth} height=${layout.didOverflowHeight}", layout.hasVisualOverflow)
        assertEquals(text.length, layout.getLineEnd(0))
    }

    private fun render() {
        compose.setContent {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val device = LocalDensity.current
                val viewportDensity = minOf(device.density, constraints.maxWidth.toFloat() / width)
                CompositionLocalProvider(
                    LocalDensity provides Density(viewportDensity, fontScale),
                    LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
                ) {
                    SalatTrackerTheme {
                        Box(Modifier.requiredWidth(width.dp).fillMaxHeight().testTag("goals-v2-root")) {
                            when (screen) {
                                GoalsV2Case.EMPTY -> GoalsScreen(
                                    GoalsUiState(isLoading = false), {}, {}, { _, _ -> }, {}, {}, {},
                                )
                                GoalsV2Case.LIST -> GoalsScreen(
                                    populatedState(), {}, {}, { _, _ -> }, {}, {}, {},
                                )
                                GoalsV2Case.NEW -> AddGoalScreen(
                                    AddGoalUiState(form = GoalFormState().withTemplate(GoalTemplate.READ_QURAN, "Read Quran")),
                                    previewFormActions(), {}, {}, {},
                                )
                                GoalsV2Case.DETAILS -> GoalDetailsScreen(
                                    detailState(), {}, {}, {}, {}, {}, {},
                                )
                                GoalsV2Case.LOG_DAY -> GoalLogDayScreen(
                                    GoalLogDayUiState(false, "Read Quran", today, 0, 5, "Pages", false, true),
                                    {}, {}, {}, {}, {}, {}, {},
                                )
                                GoalsV2Case.EDIT_ADVANCED -> EditGoalScreen(
                                    EditGoalUiState(isLoading = false, form = GoalFormState(name = "Morning adhkar", category = GoalCategory.DHIKR, usesTasbihCounter = true)),
                                    previewFormActions().copy(onTemplateSelected = null), {}, {}, {},
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun populatedState(): GoalsUiState {
        val goal = goal()
        val weekStart = today.minusDays(6)
        val strip = (0..6).map { index ->
            val date = weekStart.plusDays(index.toLong())
            GoalDayUi(date, if (index == 2) GoalDayState.MISSED else GoalDayState.DONE, if (index == 2) 0 else 1, date == today)
        }
        val cards = listOf(
            GoalCardUi(goal, 3, 0, true, false, null, strip),
            GoalCardUi(goal.copy(id = 2, name = "Morning adhkar", category = GoalCategory.DHIKR), 12, 1, true, false, null, strip),
            GoalCardUi(goal.copy(id = 3, name = "Tahajjud", category = GoalCategory.SALAT, targetAmount = 2, unit = "Rak'ahs"), 1, 0, true, false, null, strip),
        )
        return GoalsUiState(
            isLoading = false,
            today = TodaySummary(1, 3),
            active = cards,
            archived = listOf(ArchivedGoalUi(goal.copy(id = 9, name = "Old Quran goal", isActive = false), GoalStatus.ARCHIVED)),
        )
    }

    private fun detailState(): GoalDetailsUiState {
        val month = YearMonth.of(2026, 10)
        val cells = goalMonthGridDates(month).map { date ->
            val state = when {
                date.isAfter(today) -> GoalDayState.LOCKED
                date.dayOfMonth % 5 == 0 -> GoalDayState.MISSED
                date.dayOfMonth % 4 == 0 -> GoalDayState.PARTIAL
                else -> GoalDayState.DONE
            }
            GoalDayUi(date, state, if (state == GoalDayState.PARTIAL) 1 else 0, date == today, YearMonth.from(date) == month)
        }
        return GoalDetailsUiState(false, goal(), GoalStatus.ACTIVE, GoalStreak(3, 3), GoalTotals(5, 5, 6, 0), null, month, true, false, cells)
    }

    private fun goal() = SpiritualGoal(
        1, "Read Quran", GoalCategory.QURAN, GoalFrequency.DAILY, 1, "Pages", true,
        LocalTime.of(4, 30), LocalDate.of(2026, 10, 12), 30, true,
    )

    private fun screenshot(tag: String) {
        if ((width == 320 && fontScale == 1.3f) || (width == 360 && fontScale == 1f) || width == 600) {
            val dir = File(InstrumentationRegistry.getInstrumentation().targetContext.externalCacheDir, "goals-v2").apply { mkdirs() }
            val bitmap = compose.onNodeWithTag("goals-v2-root").captureToImage().asAndroidBitmap()
            File(dir, "$tag-${width}dp-$fontScale-${if (rtl) "rtl" else "ltr"}.png").outputStream().use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        }
    }

    companion object {
        @JvmStatic @Parameterized.Parameters(name = "width={0} font={1} screen={2} rtl={3}")
        fun cases(): List<Array<Any>> = listOf(320, 360, 600).flatMap { width ->
            listOf(1f, 1.3f).flatMap { scale ->
                GoalsV2Case.entries.flatMap { screen ->
                    listOf(false, true).map { rtl -> arrayOf<Any>(width, scale, screen, rtl) }
                }
            }
        }
    }
}
