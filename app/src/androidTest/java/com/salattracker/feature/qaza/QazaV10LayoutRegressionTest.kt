package com.salattracker.feature.qaza

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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.salattracker.core.theme.SalatTrackerTheme
import com.salattracker.domain.model.PrayerStatus
import com.salattracker.domain.model.PrayerType
import com.salattracker.domain.model.QazaActivity
import com.salattracker.domain.model.QazaActivitySource
import com.salattracker.domain.repository.QazaPreferences
import com.salattracker.domain.usecase.QazaSlot
import com.salattracker.domain.usecase.qazaProgress
import java.io.File
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class QazaV10LayoutRegressionTest(
    private val width: Int,
    private val fontScale: Float,
    private val page: QazaV10Page,
    private val rtl: Boolean,
) {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun pageActionsRemainReachableWithoutClipping() {
        render(sampleState(page))
        val pageTag = when (page) {
            QazaV10Page.LANDING -> "qaza-page-landing"
            QazaV10Page.ADD_MISSED -> "qaza-page-add"
            QazaV10Page.LOG_SEVERAL -> "qaza-page-log"
            QazaV10Page.OLDER_MISSED -> "qaza-page-older"
            QazaV10Page.DAILY_GOALS -> "qaza-page-goals"
        }
        compose.onNodeWithTag(pageTag).assertIsDisplayed()
        when (page) {
            QazaV10Page.LANDING -> {
                compose.onNodeWithTag("qaza-hero").assertIsDisplayed()
                compose.onNodeWithTag("qaza-plus-fajr").assertIsDisplayed()
            }
            QazaV10Page.ADD_MISSED, QazaV10Page.OLDER_MISSED ->
                compose.onNodeWithTag("qaza-preview").performScrollTo().assertIsDisplayed()
            QazaV10Page.LOG_SEVERAL ->
                compose.onNodeWithTag("qaza-log-confirm").performScrollTo().assertIsDisplayed()
            QazaV10Page.DAILY_GOALS -> Unit
        }
        screenshot(pageTag)
    }

    private fun render(state: QazaV10State) {
        compose.setContent {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val device = LocalDensity.current
                val viewportDensity = minOf(device.density, constraints.maxWidth.toFloat() / width)
                CompositionLocalProvider(
                    LocalDensity provides Density(viewportDensity, fontScale),
                    LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
                ) {
                    SalatTrackerTheme {
                        Box(Modifier.requiredSize(width.dp, 800.dp).testTag("qaza-v10-root")) {
                            QazaV10Screen(
                                state = state,
                                onOpen = {}, onBack = {}, onDailyGoal = {}, onOldestFirst = {},
                                onSound = {}, onHideBadge = {}, onReducedMotion = {}, onMakeUp = {},
                                onUndoLast = {}, onUndoActivity = {}, onAddMode = {}, onAddDates = { _, _ -> },
                                onTogglePrayer = {}, onAddCount = { _, _ -> }, onReason = {},
                                onPreviewAdd = {}, onConfirmAdd = {}, onLogCount = { _, _ -> },
                                onConfirmLog = {}, onOlderYears = {}, onExcusedDays = {},
                                onPreviewOlder = {}, onConfirmOlder = {}, onClearMessage = {},
                            )
                        }
                    }
                }
            }
        }
    }

    private fun sampleState(page: QazaV10Page): QazaV10State {
        val today = LocalDate.of(2026, 10, 27)
        val pending = PrayerType.values().flatMapIndexed { index, type ->
            List(index + 1) { QazaSlot(today.minusDays((it + 1).toLong()), type) }
        }
        return QazaV10State(
            isLoading = false,
            page = page,
            progress = qazaProgress(pending.size, 18, listOf(System.currentTimeMillis()), today),
            pending = pending,
            pendingByPrayer = pending.groupingBy { it.prayerType }.eachCount(),
            activities = listOf(
                QazaActivity(1, today.minusDays(1), PrayerType.FAJR, PrayerStatus.QAZA_PENDING, PrayerStatus.QAZA_COMPLETED, "Overslept", QazaActivitySource.QAZA, System.currentTimeMillis()),
            ),
            preferences = QazaPreferences(dailyGoal = 10),
            firstRecordedDate = today.minusMonths(2),
            gender = com.salattracker.domain.model.Gender.FEMALE,
            addForm = QazaAddForm(QazaAddMode.DATES, today.minusDays(3), today.minusDays(1)),
            logCounts = PrayerType.values().associateWith { if (it == PrayerType.FAJR) 1 else 0 },
            olderForm = QazaOlderForm(),
        )
    }

    private fun screenshot(name: String) {
        if ((width == 320 && fontScale == 1.3f) || (width == 360 && fontScale == 1f) || width == 600) {
            val dir = File(InstrumentationRegistry.getInstrumentation().targetContext.externalCacheDir, "qaza-v10").apply { mkdirs() }
            val bitmap = compose.onNodeWithTag("qaza-v10-root").captureToImage().asAndroidBitmap()
            File(dir, "$name-${width}dp-$fontScale-${if (rtl) "rtl" else "ltr"}.png").outputStream().use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        }
    }

    companion object {
        @JvmStatic @Parameterized.Parameters(name = "width={0} font={1} page={2} rtl={3}")
        fun cases(): List<Array<Any>> = listOf(320, 360, 600).flatMap { width ->
            listOf(1f, 1.3f).flatMap { scale ->
                QazaV10Page.values().flatMap { page -> listOf(false, true).map { rtl -> arrayOf<Any>(width, scale, page, rtl) } }
            }
        }
    }
}
