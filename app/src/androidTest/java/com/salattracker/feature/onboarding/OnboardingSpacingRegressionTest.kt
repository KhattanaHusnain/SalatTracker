package com.salattracker.feature.onboarding

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.*
import androidx.test.platform.app.InstrumentationRegistry
import androidx.core.view.WindowCompat
import com.salattracker.core.theme.SalatTrackerTheme
import com.salattracker.domain.model.*
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File
import java.util.Locale

/** Measures the user's bottom-gap complaint separately from CSS font/padding parity.
 * A 40%-of-usable-height gap is a diagnostic review threshold, NOT a rule in the HTML.
 * 264x592 is the export's 276x604 phone interior after its 6px border on every side.
 */
@RunWith(Parameterized::class)
class OnboardingSpacingRegressionTest(private val width: Int, private val height: Int, private val scale: Float) {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val native get() = width == 0
    private var renderedDensity = 1f
    private var usableHeightPx = 1f
    private val state = mutableStateOf(OnboardingUiState(themeMode = ThemeMode.LIGHT))
    private val output get() = File(InstrumentationRegistry.getInstrumentation().targetContext.externalCacheDir,
        "onboarding-spacing").apply { mkdirs() }
    private val case get() = if (native) { if (scale > 0f) "native-font$scale" else "native-device" } else "${width}x$height-$scale"

    @Test fun measureBottomGap_flagLargeUnusedArea() {
        render()
        val records = mutableListOf("step,usableHeightDp,bodyHeightDp,textToFooterGapDp,gapPercentOfUsableHeight,scrollable")
        val problems = mutableListOf<String>()
        val city = CityResult("Karachi", "Sindh", "Pakistan", 24.8607, 67.0011, "PK")
        OnboardingStep.values().forEach { step ->
            show(state.value.copy(step = step, confirmedCity = city, calculationMethod = CalculationMethod.KARACHI))
            screenshot(step.name.lowercase())
            val root = compose.onNodeWithTag("spacing-root").fetchSemanticsNode().boundsInRoot
            val density = renderedDensity
            val footer = compose.onNodeWithTag("onboarding-footer").fetchSemanticsNode().boundsInRoot
            val body = compose.onNode(hasScrollAction()).fetchSemanticsNode()
            val range = body.config[SemanticsProperties.VerticalScrollAxisRange]
            val scrollable = range.maxValue() > 1f
            val textBottom = compose.onAllNodes(
                hasAnyAncestor(hasScrollAction()) and SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult),
                useUnmergedTree = true).fetchSemanticsNodes().map { it.boundsInRoot }
                .filter { it.height > 0f }.maxOf { it.bottom }
            // If content extends past the viewport it is not empty space: it is scrollable content.
            val gap = if (scrollable) 0f else (footer.top - textBottom).coerceAtLeast(0f)
            val usableHeight = usableHeightPx
            val fraction = gap / usableHeight
            if (native && step == OnboardingStep.WELCOME) {
                // The complete theme tray (40dp controls + 2dp padding on each side)
                // must fit initially on the real device, including at enlarged font scale.
                val trayHeight = compose.onNodeWithTag("onboarding-theme-tray").fetchSemanticsNode().boundsInRoot.height / density
                if (trayHeight < 43f) problems += "WELCOME: theme tray is clipped; visibleHeight=${trayHeight}dp"
            }
            records += "${step.name},${usableHeight / density},${body.boundsInRoot.height / density},${gap / density},${fraction * 100},$scrollable"
            if (fraction > .40f) problems += "$step: ${(fraction * 100).toInt()}% of usable screen is a gap above footer (${gap / density}dp)"
        }
        File(output, "gaps-$case.csv").writeText(records.joinToString("\n"))
        finish("gap-review", problems)
    }

    @Test fun typography_notSmallerThanExportTokens() {
        render()
        val problems = mutableListOf<String>()
        val records = mutableListOf("step,text,actualSp,exportCssPx,lineHeightSp,lineCount,widthOverflow,heightOverflow")
        val checks = listOf(
            Triple(OnboardingStep.WELCOME, "Welcome to Istiqamah: The Salah Tracker", 24f),
            Triple(OnboardingStep.WELCOME, "English", 13f),
            Triple(OnboardingStep.WELCOME, "Continue", 13f),
            Triple(OnboardingStep.ABOUT_YOU, "Tell us about you", 24f),
            Triple(OnboardingStep.ABOUT_YOU, "Male", 13f),
            Triple(OnboardingStep.ABOUT_YOU, "Standard tracking", 10.5f),
            Triple(OnboardingStep.REMINDERS, "Never miss a prayer", 24f),
            Triple(OnboardingStep.REMINDERS, "Prayer Reminders", 13f),
            Triple(OnboardingStep.REMINDERS, "At each adhan time", 10.5f),
            Triple(OnboardingStep.REMINDERS, "Preview", 12f),
        )
        checks.forEach { (step, text, reference) ->
            show(state.value.copy(step = step))
            // Select the text itself, not a merged selectable row whose layout action may
            // return the label's layout when this fixture targets its description.
            val node = compose.onNodeWithText(text, useUnmergedTree = true)
            runCatching { node.performScrollTo() }
            val layouts = mutableListOf<TextLayoutResult>()
            node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            val layout = layouts.single()
            val style = layout.layoutInput.style
            records += "$step,\"$text\",${style.fontSize.value},$reference,${style.lineHeight.value},${layout.lineCount},${layout.didOverflowWidth},${layout.didOverflowHeight}"
            if (style.fontSize.value + .01f < reference) problems += "$step '$text': ${style.fontSize} below export ${reference}px"
            if (layout.didOverflowHeight || layout.getLineEnd(layout.lineCount - 1) != text.length)
                problems += "$step '$text': incomplete text; widthOverflow=${layout.didOverflowWidth}, heightOverflow=${layout.didOverflowHeight}"
        }
        File(output, "fonts-$case.csv").writeText(records.joinToString("\n"))
        finish("font-minimums", problems)
    }

    @Test fun optionRows_preserveExportVerticalPadding() {
        render()
        val problems = mutableListOf<String>()
        val records = mutableListOf("step,row,topPaddingDp,bottomPaddingDp,rowHeightDp")
        listOf(OnboardingStep.ABOUT_YOU, OnboardingStep.CATCH_UP_RANGE).forEach { step ->
            show(state.value.copy(step = step))
            val density = renderedDensity
            val rows = compose.onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)).fetchSemanticsNodes()
            rows.forEachIndexed { index, row ->
                val match = SemanticsMatcher("option row ${row.id}") { it.id == row.id }
                compose.onNode(match).performScrollTo()
                val bounds = compose.onNode(match).fetchSemanticsNode().boundsInRoot
                val texts = compose.onAllNodes(hasAnyAncestor(match) and SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult),
                    useUnmergedTree = true).fetchSemanticsNodes().map { it.boundsInRoot }
                val top = (texts.minOf { it.top } - bounds.top) / density
                val bottom = (bounds.bottom - texts.maxOf { it.bottom }) / density
                records += "$step,$index,$top,$bottom,${bounds.height / density}"
                // Export: --g=12, padding=(12*.8+3)=12.6 CSS px. Allow 1dp rounding/native parity tolerance.
                if (top < 11.6f || bottom < 11.6f) problems += "$step row $index: top=$top bottom=$bottom dp, expected ~12.6"
            }
        }
        File(output, "rows-$case.csv").writeText(records.joinToString("\n"))
        finish("row-padding", problems)
    }

    private fun render() {
        // Match MainActivity's edge-to-edge setting; native runs retain real safeDrawing insets.
        compose.activityRule.scenario.onActivity { WindowCompat.setDecorFitsSystemWindows(it.window, false) }
        compose.setContent {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val deviceDensity = LocalDensity.current
                val viewportDensity = if (native) {
                    if (scale > 0f) Density(deviceDensity.density, scale) else deviceDensity
                } else Density(
                    minOf(deviceDensity.density, constraints.maxWidth.toFloat() / width, constraints.maxHeight.toFloat() / height), scale)
                val topInset = WindowInsets.safeDrawing.getTop(deviceDensity)
                val bottomInset = WindowInsets.safeDrawing.getBottom(deviceDensity)
                SideEffect {
                    renderedDensity = viewportDensity.density
                    usableHeightPx = if (native) (constraints.maxHeight - topInset - bottomInset).toFloat()
                        else height * viewportDensity.density
                    File(output, "viewport-$case.txt").writeText(
                        "native=$native\nrootConstraintsPx=${constraints.maxWidth}x${constraints.maxHeight}\n" +
                        "density=${viewportDensity.density}\nfontScale=${viewportDensity.fontScale}\n" +
                        "systemInsetsPx=$topInset,$bottomInset\nusableHeightPx=$usableHeightPx\n")
                }
                val configuration = android.content.res.Configuration(LocalConfiguration.current).apply { setLocale(Locale.ENGLISH) }
                val context = LocalContext.current.createConfigurationContext(configuration)
                CompositionLocalProvider(LocalDensity provides viewportDensity, LocalConfiguration provides configuration, LocalContext provides context) {
                    SalatTrackerTheme(darkTheme = false) {
                        // Treat the matrix dimensions as usable content after system bars, as in the HTML frame.
                        val viewport = if (native) Modifier.fillMaxSize() else
                            Modifier.requiredSize(width.dp, height.dp).consumeWindowInsets(WindowInsets.safeDrawing)
                        Box(viewport.testTag("spacing-root")) {
                            OnboardingScreen(state.value,
                                onLanguageSelected = {}, onThemeSelected = {}, onWelcomeNext = {}, onRestoreFromBackup = {},
                                onGenderSelected = {}, onAboutYouNext = {}, onMadhabSelected = {}, onSchoolNext = {},
                                onAllowLocationClick = {}, onEnterCityInstead = {}, onCityQueryChanged = {}, onCitySelected = {},
                                onCitySearchContinue = {}, onUseMyLocationInstead = {}, onConfirmLocation = {}, onChangeLocationClick = {},
                                onCalculationMethodSheetOpen = {}, onCalculationMethodSheetDismiss = {}, onCalculationMethodSelected = {},
                                onPrayerRemindersChanged = {}, onFullScreenAlertChanged = {}, onRemindersSkip = {}, onRemindersNext = {},
                                onCatchUpSkip = {}, onCatchUpDurationChanged = {}, onCatchUpCustomRangeChanged = { _, _ -> },
                                onApplyCatchUp = {}, onUndoCatchUp = {}, onBack = {}, onFinish = {})
                        }
                    }
                }
            }
        }
    }

    private fun show(fixture: OnboardingUiState) {
        compose.runOnIdle { state.value = fixture }
        compose.waitForIdle()
        compose.onNode(hasScrollAction()).performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, -100000f) }
        compose.waitForIdle()
    }
    private fun screenshot(name: String) {
        val bitmap = compose.onNodeWithTag("spacing-root").captureToImage().asAndroidBitmap()
        File(output, "$name-$case.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        if (native) {
            val display = requireNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
            val displayName = if (scale > 0f) "$name-$case-full-display.png" else "$name-full-display.png"
            File(output, displayName).outputStream().use { display.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
    private fun finish(name: String, problems: List<String>) {
        File(output, "$name-$case.txt").writeText("findings=${problems.size}\n" + problems.joinToString("\n"))
        assertTrue("$name at $case:\n" + problems.joinToString("\n"), problems.isEmpty())
    }
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "viewport={0}x{1} fontScale={2}")
        fun viewports(): List<Array<Any>> {
            val device = listOf(arrayOf<Any>(0, 0, 0f), arrayOf<Any>(0, 0, 1.3f))
            return if (InstrumentationRegistry.getArguments().getString("spacing_matrix") == "true") device +
                listOf(264 to 592, 320 to 640, 360 to 640, 360 to 800, 600 to 960)
                    .flatMap { (w, h) -> listOf(1f, 1.3f).map { arrayOf<Any>(w, h, it) } }
            else device
        }
    }
}
