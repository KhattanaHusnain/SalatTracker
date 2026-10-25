package com.salattracker.feature.onboarding

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.core.view.WindowCompat
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.*
import androidx.test.platform.app.InstrumentationRegistry
import com.salattracker.core.theme.SalatTrackerTheme
import com.salattracker.domain.model.ThemeMode
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File
import java.util.Locale

/** Reference: supplied Architectural Teal HTML .loc/.rp/.pn, .spin and .tg.
 * Test clock samples actual production pixels; callbacks only update in-memory fixtures.
 * No preferences, permission requests, sound playback or prayer records are changed.
 */
@RunWith(Parameterized::class)
class OnboardingMotionRegressionTest(private val width: Int, private val height: Int, private val scale: Float) {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val native get() = width == 0
    private val state = mutableStateOf(OnboardingUiState(themeMode = ThemeMode.LIGHT))
    private val output get() = File(InstrumentationRegistry.getInstrumentation().targetContext.externalCacheDir,
        "onboarding-motion").apply { mkdirs() }
    private val case get() = if (native) "native-device" else "${width}x$height-$scale"

    @Test fun locationHero_changesDuringReferencePulseCycle() {
        render(OnboardingStep.LOCATION_INTRO)
        // Target the actual hero: its position adapts to the available window height.
        val first = frame("location-0", "onboarding-location-hero")
        val changes = listOf(700L, 700L, 700L).mapIndexed { index, elapsed ->
            compose.mainClock.advanceTimeBy(elapsed)
            changed(first, frame("location-${(index + 1) * 700}", "onboarding-location-hero"))
        }
        finish("location", "changed pixels at 700/1400/2100ms=$changes", changes.any { it > 4 },
            "Location hero is static; reference requires two ripples (2.8s, 1.4s offset) and a bobbing pin.")
    }

    @Test fun reducedMotionHero_remainsVisibleAndStill() {
        render(OnboardingStep.LOCATION_INTRO, reducedMotion = true)
        compose.onNodeWithTag("onboarding-location-hero").assertIsDisplayed()
        val first = frame("reduced-motion-0", "onboarding-location-hero")
        compose.mainClock.advanceTimeBy(1400)
        val count = changed(first, frame("reduced-motion-1400", "onboarding-location-hero"))
        finish("reduced-motion", "changedPixels=$count", count == 0, "Reduced-motion hero must remain static.")
    }

    @Test fun searchingIndicator_animatesWithoutMovingText() {
        render(OnboardingStep.CITY_SEARCH, searching = true)
        val label = compose.onNodeWithText("Searching…")
        val beforeBounds = label.fetchSemanticsNode().boundsInRoot
        val first = frame("search-0")
        compose.mainClock.advanceTimeBy(240)
        val next = frame("search-240")
        val count = changed(first, next)
        val afterBounds = label.fetchSemanticsNode().boundsInRoot
        finish("search", "changedPixels=$count; before=$beforeBounds; after=$afterBounds",
            count > 4 && beforeBounds == afterBounds,
            "Searching indicator must animate while its adjacent label remains stationary.")
    }

    @Test fun reminderSwitch_animatesAndUpdatesCheckedState() {
        render(OnboardingStep.REMINDERS)
        val switches = compose.onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch))
        switches[0].assertIsOn()
        val first = frame("switch-on")
        switches[0].performClick()
        compose.mainClock.advanceTimeBy(48)
        val middle = frame("switch-48")
        compose.mainClock.advanceTimeBy(400)
        val last = frame("switch-off")
        switches[0].assertIsOff()
        // Restrict comparison to the switch, excluding touch ripples elsewhere.
        val bounds = switches[0].fetchSemanticsNode().boundsInRoot
        val root = compose.onNodeWithTag("motion-root").fetchSemanticsNode().boundsInRoot
        val x0 = (bounds.left - root.left).toInt().coerceAtLeast(0)
        val x1 = (bounds.right - root.left).toInt().coerceAtMost(first.width)
        val y0 = (bounds.top - root.top).toInt().coerceAtLeast(0)
        val y1 = (bounds.bottom - root.top).toInt().coerceAtMost(first.height)
        val initialChange = changed(first, middle, y0, y1, x0, x1)
        val remainingChange = changed(middle, last, y0, y1, x0, x1)
        finish("switch", "initialChange=$initialChange; remainingChange=$remainingChange",
            initialChange > 4 && remainingChange > 4, "Switch must show an intermediate animation and settle into the new state.")
    }

    private fun render(step: OnboardingStep, searching: Boolean = false, reducedMotion: Boolean = false) {
        state.value = state.value.copy(step = step, cityQuery = if (searching) "Ka" else "", isSearchingCity = searching)
        compose.mainClock.autoAdvance = false
        compose.activityRule.scenario.onActivity { WindowCompat.setDecorFitsSystemWindows(it.window, false) }
        compose.setContent {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val density = if (native) LocalDensity.current else Density(
                    minOf(LocalDensity.current.density, constraints.maxWidth.toFloat() / width,
                        constraints.maxHeight.toFloat() / height), scale)
                val configuration = android.content.res.Configuration(LocalConfiguration.current).apply { setLocale(Locale.ENGLISH) }
                val context = LocalContext.current.createConfigurationContext(configuration)
                CompositionLocalProvider(LocalDensity provides density,
                    LocalConfiguration provides configuration, LocalContext provides context) {
                    SalatTrackerTheme(darkTheme = false) {
                        Box((if (native) Modifier.fillMaxSize() else Modifier.requiredSize(width.dp, height.dp)).testTag("motion-root")) {
                            if (reducedMotion) OnboardingDesign(ThemeMode.LIGHT) { OnboardingLocationHero(motionEnabled = false) }
                            else OnboardingScreen(state.value,
                                onLanguageSelected = {}, onThemeSelected = {}, onWelcomeNext = {}, onRestoreFromBackup = {},
                                onGenderSelected = {}, onAboutYouNext = {}, onMadhabSelected = {}, onSchoolNext = {},
                                onAllowLocationClick = {}, onEnterCityInstead = {}, onCityQueryChanged = {}, onCitySelected = {},
                                onCitySearchContinue = {}, onUseMyLocationInstead = {}, onConfirmLocation = {}, onChangeLocationClick = {},
                                onCalculationMethodSheetOpen = {}, onCalculationMethodSheetDismiss = {}, onCalculationMethodSelected = {},
                                onPrayerRemindersChanged = { state.value = state.value.copy(prayerRemindersEnabled = it) },
                                onFullScreenAlertChanged = { state.value = state.value.copy(fullScreenAlertEnabled = it) },
                                onRemindersSkip = {}, onRemindersNext = {}, onCatchUpSkip = {}, onCatchUpDurationChanged = {},
                                onCatchUpCustomRangeChanged = { _, _ -> }, onApplyCatchUp = {}, onUndoCatchUp = {}, onBack = {}, onFinish = {})
                        }
                    }
                }
            }
        }
        compose.mainClock.advanceTimeBy(32)
        compose.waitForIdle()
    }

    private fun frame(name: String, tag: String = "motion-root"): Bitmap {
        compose.waitForIdle()
        return compose.onNodeWithTag(tag).captureToImage().asAndroidBitmap().also { bitmap ->
            File(output, "$name-$case.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    private fun changed(a: Bitmap, b: Bitmap, y0: Int = 0, y1: Int = a.height, x0: Int = 0, x1: Int = a.width): Int {
        var count = 0
        for (y in y0 until y1) for (x in x0 until x1) {
            val p = a.getPixel(x, y); val q = b.getPixel(x, y)
            if (kotlin.math.abs(android.graphics.Color.red(p) - android.graphics.Color.red(q)) > 8 ||
                kotlin.math.abs(android.graphics.Color.green(p) - android.graphics.Color.green(q)) > 8 ||
                kotlin.math.abs(android.graphics.Color.blue(p) - android.graphics.Color.blue(q)) > 8) count++
        }
        return count
    }

    private fun finish(name: String, details: String, passed: Boolean, failure: String) {
        File(output, "$name-$case.txt").writeText("viewport=$case\npassed=$passed\n$details\n")
        assertTrue("$failure ($case; $details)", passed)
    }

    companion object {
        @JvmStatic @Parameterized.Parameters(name = "viewport={0}x{1} fontScale={2}")
        fun viewports(): List<Array<Any>> = listOf(arrayOf<Any>(0, 0, 0f)) + listOf(320 to 640, 360 to 800, 600 to 960).flatMap { (w, h) ->
            listOf(1f, 1.3f).map { arrayOf<Any>(w, h, it) }
        }
    }
}
