package com.salattracker.feature.onboarding

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.salattracker.core.theme.SalatTrackerTheme
import com.salattracker.domain.model.CalculationMethod
import com.salattracker.domain.model.CityResult
import com.salattracker.domain.model.Gender
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File
import java.util.Locale

/** Diagnostic tests against Architectural Teal (black text)_ onboarding flow.html.
 * Asserts design contracts as well as responsive text/control layout.
 * Fixtures exercise UI only: callbacks never persist preferences, request permissions or add prayers.
 */
@RunWith(Parameterized::class)
class OnboardingLayoutRegressionTest(
    private val widthDp: Int,
    private val heightDp: Int,
    private val fontScale: Float,
) {
    @get:Rule val compose = createComposeRule()
    private val state = mutableStateOf(OnboardingUiState())
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val output get() = File(context.externalCacheDir ?: context.cacheDir, "onboarding-layout").apply { mkdirs() }
    private val caseName get() = "${widthDp}x${heightDp}-$fontScale"

    @Test fun allStates_textFitsAndRemainsReachable() {
        showOnboarding()
        val problems = mutableListOf<String>()
        fixtures().forEach { (name, fixture) ->
            show(fixture)
            screenshot(name)
            val texts = compose.onAllNodes(
                SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult),
                useUnmergedTree = true,
            ).fetchSemanticsNodes().map { it.id }
            texts.forEach { id ->
                val node = compose.onNode(SemanticsMatcher("node $id") { it.id == id }, useUnmergedTree = true)
                // Scrollable screens are allowed to reflow; every label must remain reachable.
                runCatching { node.performScrollTo() }
                val layouts = mutableListOf<TextLayoutResult>()
                node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
                val bounds = node.fetchSemanticsNode().boundsInRoot
                val switches = compose.onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch))
                    .fetchSemanticsNodes().map { it.boundsInRoot }
                layouts.forEach { layout ->
                    val text = layout.layoutInput.text.text.replace('\n', ' ')
                    // Intrinsic-width Text can round down by a fraction of a physical pixel.
                    // Record actual line extent and allow only that rounding, not truncated content.
                    val excess = (0 until layout.lineCount).maxOfOrNull {
                        layout.getLineRight(it) - layout.getLineLeft(it) - layout.size.width
                    } ?: 0f
                    if (layout.didOverflowHeight || (layout.didOverflowWidth && excess > 1f)) problems +=
                        "$name: '$text': widthOverflow=${layout.didOverflowWidth}, heightOverflow=${layout.didOverflowHeight}, size=${layout.size}, excessPx=$excess"
                    if (text.isNotBlank() && (bounds.width < 1f || bounds.height < 1f))
                        problems += "$name: '$text' has no visible area after scrolling"
                    if (bounds.width + 2f < layout.size.width)
                        problems += "$name: '$text' clipped by ancestor: visibleWidth=${bounds.width}, textWidth=${layout.size.width}"
                    if ((text == primaryAction(fixture) || text in setOf("Change", "Preview", "Light", "Dark", "System")) && layout.lineCount != 1)
                        problems += "$name: control '$text' wraps into ${layout.lineCount} lines"
                    if (switches.any { it.overlaps(bounds) })
                        problems += "$name: '$text' overlaps a reminder switch"
                }
            }
        }
        finish("text-layout", problems)
    }

    @Test fun export_primaryActionStaysInFooter() {
        showOnboarding()
        val problems = mutableListOf<String>()
        fixtures().forEach { (name, fixture) ->
            show(fixture)
            val root = compose.onNodeWithTag("onboarding").fetchSemanticsNode().boundsInRoot
            val action = compose.onNodeWithText(primaryAction(fixture))
            val bounds = action.fetchSemanticsNode().boundsInRoot
            // The export reserves a bottom footer outside the scrolling body (.body flex:1).
            val footerStart = root.bottom - root.height * .18f
            if (bounds.height < 1f || bounds.top < footerStart || bounds.bottom > root.bottom + 1f)
                problems += "$name: primary action outside bottom footer: action=$bounds, root=$root"
            runCatching { action.assertIsDisplayed() }.onFailure {
                problems += "$name: primary action is not initially visible"
            }
        }
        finish("export-footer", problems)
    }

    @Test fun export_headingsAndNavigationMatchFlatLayout() {
        showOnboarding()
        val problems = mutableListOf<String>()
        fixtures().filter { it.first in setOf("welcome", "about-you", "location-intro", "location-confirm", "reminders", "catch-up", "done") }
            .forEach { (name, fixture) ->
                show(fixture)
                val title = compose.onNodeWithText(title(fixture))
                val layouts = mutableListOf<TextLayoutResult>()
                title.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
                if (layouts.single().layoutInput.style.textAlign == TextAlign.Center)
                    problems += "$name: heading is centered; export h2 is left aligned"
                if (layouts.single().layoutInput.style.color != Color.Black)
                    problems += "$name: heading is not black as specified in the updated export"
                if (fixture.step == OnboardingStep.WELCOME) {
                    if (compose.onAllNodesWithText("Step 1 of 5").fetchSemanticsNodes().isEmpty())
                        problems += "$name: missing Step 1 of 5 progress header"
                } else if (fixture.step != OnboardingStep.DONE) {
                    val back = compose.onNodeWithContentDescription("Back")
                    val backBounds = back.fetchSemanticsNode().boundsInRoot
                    val root = compose.onNodeWithTag("onboarding").fetchSemanticsNode().boundsInRoot
                    if (backBounds.top < root.bottom - root.height * .18f)
                        problems += "$name: Back is at top; export places Back beside the footer action"
                }
            }
        finish("export-structure", problems)
    }

    @Test fun export_heroContainsFadedCornerArtwork() {
        showOnboarding()
        val problems = mutableListOf<String>()
        fixtures().filter { it.first in setOf("welcome", "about-you", "location-intro", "location-confirm", "reminders", "catch-up", "done") }
            .forEach { (name, fixture) ->
                show(fixture)
                val bitmap = compose.onNodeWithTag("onboarding").captureToImage().asAndroidBitmap()
                // Probe the right side of the hero for gold artwork, excluding the left underline.
                // A tiled vector has gaps: requiring ink in a narrow outer strip gives false failures.
                // Presence is asserted here; lantern shape and fading are reviewed in screenshots.
                val x0 = (bitmap.width * (widthDp - 160f) / widthDp).toInt()
                val x1 = bitmap.width
                val y0 = (bitmap.height * 16f / heightDp).toInt()
                val y1 = (bitmap.height * 160f / heightDp).toInt()
                var differentPixels = 0
                for (y in y0 until y1) for (x in x0 until x1) {
                    val pixel = bitmap.getPixel(x, y)
                    if (android.graphics.Color.red(pixel) > android.graphics.Color.green(pixel) + 6 &&
                        android.graphics.Color.green(pixel) > android.graphics.Color.blue(pixel) + 12) differentPixels++
                }
                if (differentPixels == 0) problems += "$name: gold Arch lantern artwork is absent from the hero"
            }
        finish("export-artwork", problems)
    }

    private fun showOnboarding() {
        compose.setContent {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val density = minOf(LocalDensity.current.density,
                    constraints.maxWidth.toFloat() / widthDp,
                    constraints.maxHeight.toFloat() / heightDp)
                val configuration = android.content.res.Configuration(LocalConfiguration.current).apply { setLocale(Locale.ENGLISH) }
                val localizedContext = LocalContext.current.createConfigurationContext(configuration)
                CompositionLocalProvider(
                    LocalDensity provides Density(density, fontScale),
                    LocalConfiguration provides configuration,
                    LocalContext provides localizedContext,
                ) {
                    SalatTrackerTheme(darkTheme = false) {
                        Box(Modifier.requiredSize(widthDp.dp, heightDp.dp).testTag("onboarding")) {
                            OnboardingScreen(
                                uiState = state.value,
                                onLanguageSelected = {}, onThemeSelected = {}, onWelcomeNext = {}, onRestoreFromBackup = {},
                                onGenderSelected = {}, onAboutYouNext = {}, onMadhabSelected = {}, onSchoolNext = {},
                                onAllowLocationClick = {}, onEnterCityInstead = {}, onCityQueryChanged = {}, onCitySelected = {},
                                onCitySearchContinue = {}, onUseMyLocationInstead = {}, onConfirmLocation = {}, onChangeLocationClick = {},
                                onCalculationMethodSheetOpen = {}, onCalculationMethodSheetDismiss = {}, onCalculationMethodSelected = {},
                                onPrayerRemindersChanged = {}, onFullScreenAlertChanged = {}, onRemindersSkip = {}, onRemindersNext = {},
                                onCatchUpSkip = {}, onCatchUpDurationChanged = {}, onCatchUpCustomRangeChanged = { _, _ -> },
                                onApplyCatchUp = {}, onUndoCatchUp = {}, onBack = {}, onFinish = {}, onCatchUpNext = {},
                            )
                        }
                    }
                }
            }
        }
    }

    private fun show(fixture: OnboardingUiState) {
        compose.runOnIdle { state.value = fixture }
        compose.waitForIdle()
        // A scroll position from another fixture must not change the initial-screen contract.
        compose.onAllNodes(hasScrollAction()).fetchSemanticsNodes().forEach { scroll ->
            compose.onNode(SemanticsMatcher("scroll ${scroll.id}") { it.id == scroll.id })
                .performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, -100000f) }
        }
        compose.waitForIdle()
    }

    private fun screenshot(name: String) {
        compose.onNodeWithTag("onboarding").captureToImage().asAndroidBitmap().let { bitmap ->
            File(output, "$name-$caseName.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    private fun finish(check: String, problems: List<String>) {
        File(output, "$check-$caseName.txt").writeText(
            "viewport=$caseName\nfindings=${problems.size}\n" + problems.joinToString("\n"))
        assertTrue("$check at $caseName:\n" + problems.joinToString("\n"), problems.isEmpty())
    }

    private fun primaryAction(s: OnboardingUiState) = when (s.step) {
        OnboardingStep.SCHOOL -> "Next"
        OnboardingStep.LOCATION_INTRO -> if (s.isLocating) "Getting location…" else "Allow Location"
        OnboardingStep.LOCATION_CONFIRM -> "Confirm location"
        OnboardingStep.REMINDERS -> "Turn on reminders"
        OnboardingStep.CATCH_UP -> "Add missed prayers"
        OnboardingStep.CATCH_UP_RANGE -> "Add ${s.catchUpPrayerCount} prayers"
        OnboardingStep.DONE -> "Start tracking"
        else -> "Continue"
    }

    private fun title(s: OnboardingUiState) = when (s.step) {
        OnboardingStep.WELCOME -> "Welcome to Istiqamah: The Salah Tracker"
        OnboardingStep.ABOUT_YOU -> "Tell us about you"
        OnboardingStep.LOCATION_INTRO -> "Precise Prayer Times"
        OnboardingStep.LOCATION_CONFIRM -> "Is this right?"
        OnboardingStep.REMINDERS -> "Never miss a prayer"
        OnboardingStep.CATCH_UP -> "Missed prayers to make up?"
        OnboardingStep.DONE -> "You're all set"
        else -> error("No reference title for ${s.step}")
    }

    private fun fixtures(): List<Pair<String, OnboardingUiState>> {
        val city = CityResult("Karachi", "Sindh", "Pakistan", 24.8607, 67.0011, "PK")
        val base = OnboardingUiState(confirmedCity = city, calculationMethod = CalculationMethod.KARACHI)
        return OnboardingStep.values().map { step ->
            step.name.lowercase().replace('_', '-') to base.copy(step = step,
                confirmedCity = if (step == OnboardingStep.CITY_SEARCH) null else city)
        } + listOf(
            "about-you-selected" to base.copy(step = OnboardingStep.ABOUT_YOU, gender = Gender.FEMALE),
            "location-error" to base.copy(step = OnboardingStep.LOCATION_INTRO, locationError = true),
            "city-typing" to base.copy(step = OnboardingStep.CITY_SEARCH, cityQuery = "Ka", isSearchingCity = true, confirmedCity = null),
            "city-results" to base.copy(step = OnboardingStep.CITY_SEARCH, cityQuery = "Kar", cityResults = listOf(city, city.copy(name = "Karak"))),
            "city-no-results" to base.copy(step = OnboardingStep.CITY_SEARCH, cityQuery = "Qwerty", confirmedCity = null),
            "catch-up-custom" to base.copy(step = OnboardingStep.CATCH_UP_RANGE, catchUpDuration = CatchUpDuration.CUSTOM),
        )
    }

    companion object {
        @JvmStatic @Parameterized.Parameters(name = "viewport={0}x{1}dp fontScale={2}")
        fun viewports(): List<Array<Any>> = listOf(320 to 640, 360 to 800, 600 to 960).flatMap { (w, h) ->
            listOf(1f, 1.3f).map { scale -> arrayOf<Any>(w, h, scale) }
        }
    }
}
