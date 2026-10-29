package com.salattracker.notification

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.test.platform.app.InstrumentationRegistry
import com.salattracker.core.theme.SalatTrackerTheme
import com.salattracker.domain.model.Gender
import com.salattracker.domain.model.Madhab
import com.salattracker.domain.model.PrayerType
import com.salattracker.domain.model.VerseInfo
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

enum class ReminderFixture { MALE_WITH_VERSE, FEMALE_WITHOUT_VERSE }

/**
 * Full-screen alert coverage using the production composable, bundled fonts and localized strings.
 * Viewports are simulated through density; they do not replace checks on distinct physical devices.
 */
@RunWith(Parameterized::class)
class PrayerReminderLayoutRegressionTest(
    private val width: Int,
    private val height: Int,
    private val fontScale: Float,
    private val language: String,
    private val fixture: ReminderFixture,
) {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun fullScreenAlert_keepsContentVisibleAndUnclipped() {
        compose.mainClock.autoAdvance = false
        render()
        screenshot()
        compose.mainClock.autoAdvance = true

        val requiredTags = buildList {
            add("prayer-reminder-clock")
            add("prayer-reminder-action-individual")
            add("prayer-reminder-action-qaza")
            add("prayer-reminder-action-snooze")
            add("prayer-reminder-dismiss")
            if (fixture == ReminderFixture.MALE_WITH_VERSE) {
                add("prayer-reminder-action-congregation")
            }
        }

        val root = compose.onNodeWithTag("prayer-reminder-screen").assertIsDisplayed()
            .fetchSemanticsNode().boundsInRoot
        requiredTags.forEach { tag ->
            val node = compose.onNodeWithTag(tag).assertIsDisplayed().fetchSemanticsNode()
            assertContained(tag, node.boundsInRoot, root)
        }
        if (fixture == ReminderFixture.FEMALE_WITHOUT_VERSE) {
            compose.onNodeWithTag("prayer-reminder-action-congregation").assertDoesNotExist()
        } else {
            val center = compose.onNodeWithTag("prayer-reminder-center-scroll")
            listOf("prayer-reminder-verse-arabic", "prayer-reminder-verse-translation").forEach { tag ->
                center.performScrollToNode(hasTestTag(tag))
                val verseNode = compose.onNodeWithTag(tag).assertIsDisplayed().fetchSemanticsNode()
                assertContained(tag, verseNode.boundsInRoot, root)
            }
        }

        assertTextLayoutsDoNotClip()
    }

    private fun render() {
        compose.activityRule.scenario.onActivity {
            WindowCompat.setDecorFitsSystemWindows(it.window, false)
        }
        compose.setContent {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val deviceDensity = LocalDensity.current
                val viewportDensity = minOf(
                    deviceDensity.density,
                    constraints.maxWidth.toFloat() / width,
                    constraints.maxHeight.toFloat() / height,
                )
                val locale = Locale.forLanguageTag(language)
                val configuration = android.content.res.Configuration(LocalConfiguration.current).apply {
                    setLocale(locale)
                }
                val localizedContext = LocalContext.current.createConfigurationContext(configuration)
                CompositionLocalProvider(
                    LocalDensity provides Density(viewportDensity, fontScale),
                    LocalConfiguration provides configuration,
                    LocalContext provides localizedContext,
                    LocalLayoutDirection provides if (language == "ur") LayoutDirection.Rtl else LayoutDirection.Ltr,
                ) {
                    SalatTrackerTheme {
                        Box(
                            Modifier
                                .requiredSize(width.dp, height.dp)
                                .testTag("prayer-reminder-test-root"),
                        ) {
                            PrayerReminderScreen(
                                prayerType = PrayerType.ASR,
                                date = LocalDate.of(2026, 10, 29),
                                jumuahEnabled = true,
                                madhab = Madhab.STANDARD,
                                gender = if (fixture == ReminderFixture.FEMALE_WITHOUT_VERSE) Gender.FEMALE else Gender.MALE,
                                scheduledTime = LocalTime.of(15, 20),
                                verse = if (fixture == ReminderFixture.MALE_WITH_VERSE) VERSE else null,
                                snoozeMinutes = 30,
                                use24HourFormat = false,
                                onMark = {},
                                onSnooze = {},
                                onDismiss = {},
                            )
                        }
                    }
                }
            }
        }
    }

    private fun assertTextLayoutsDoNotClip() {
        val textNodes = compose.onAllNodes(
            androidx.compose.ui.test.SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult),
            useUnmergedTree = true,
        ).fetchSemanticsNodes()
        val failures = mutableListOf<String>()

        compose.runOnIdle {
            textNodes.forEach { node ->
                val layouts = mutableListOf<TextLayoutResult>()
                node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
                layouts.forEach { layout ->
                    val paintedExcess = (0 until layout.lineCount).maxOfOrNull { line ->
                        layout.getLineRight(line) - layout.getLineLeft(line) - layout.size.width
                    } ?: 0f
                    val visibleEnd = if (layout.lineCount == 0) 0 else layout.getLineEnd(layout.lineCount - 1)
                    val omittedCharacters = visibleEnd != layout.layoutInput.text.length
                    if (layout.didOverflowHeight ||
                        (layout.didOverflowWidth && paintedExcess > 1f) ||
                        omittedCharacters
                    ) {
                        failures += "'${layout.layoutInput.text}' widthOverflow=${layout.didOverflowWidth} " +
                            "heightOverflow=${layout.didOverflowHeight} paintedExcessPx=$paintedExcess " +
                            "visibleEnd=$visibleEnd/${layout.layoutInput.text.length} " +
                            "size=${layout.size} bounds=${node.boundsInRoot}"
                    }
                }
            }
        }

        assertTrue(
            "Text clipped at ${width}x${height}dp font=$fontScale language=$language fixture=$fixture:\n" +
                failures.joinToString("\n"),
            failures.isEmpty(),
        )
    }

    private fun assertContained(tag: String, child: Rect, root: Rect) {
        val tolerance = 1f
        assertFalse("$tag has no size: $child", child.isEmpty)
        assertTrue(
            "$tag is outside the full-screen viewport: child=$child root=$root " +
                "case=${width}x${height}dp/$fontScale/$language/$fixture",
            child.left >= root.left - tolerance &&
                child.top >= root.top - tolerance &&
                child.right <= root.right + tolerance &&
                child.bottom <= root.bottom + tolerance,
        )
    }

    private fun screenshot() {
        val output = File(
            InstrumentationRegistry.getInstrumentation().targetContext.externalCacheDir,
            "prayer-reminder-layout",
        ).apply { mkdirs() }
        val name = "${width}x${height}-$fontScale-$language-${fixture.name.lowercase()}.png"
        val bitmap = compose.onNodeWithTag("prayer-reminder-test-root")
            .captureToImage().asAndroidBitmap()
        File(output, name).outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    companion object {
        private val VERSE = VerseInfo(
            arabic = "حَافِظُوا عَلَى الصَّلَوَاتِ وَالصَّلَاةِ الْوُسْطَىٰ وَقُومُوا لِلَّهِ قَانِتِينَ",
            text = "Maintain with care the obligatory prayers, especially the middle prayer, and stand before Allah in devotion.",
            citation = "Al-Baqarah 2:238",
        )

        @JvmStatic
        @Parameterized.Parameters(name = "viewport={0}x{1} font={2} language={3} fixture={4}")
        fun cases(): List<Array<Any>> {
            val simulated = listOf(
                320 to 568,
                360 to 800,
                600 to 960,
            ).flatMap { (width, height) ->
                listOf(1f, 1.3f).flatMap { fontScale ->
                    languageFixtures(width, height, fontScale)
                }
            }
            // Connected SM-A075F: 720x1600 at 300 dpi and system font scale 1.08.
            val connectedDeviceNative = languageFixtures(384, 853, 1.08f)
            return simulated + connectedDeviceNative
        }

        private fun languageFixtures(width: Int, height: Int, fontScale: Float): List<Array<Any>> =
            listOf("en", "ur").flatMap { language ->
                ReminderFixture.entries.map { fixture ->
                    arrayOf<Any>(width, height, fontScale, language, fixture)
                }
            }
    }
}
