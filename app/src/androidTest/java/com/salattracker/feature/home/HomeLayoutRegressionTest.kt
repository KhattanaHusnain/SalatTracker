package com.salattracker.feature.home

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.*
import androidx.core.view.WindowCompat
import androidx.test.platform.app.InstrumentationRegistry
import com.salattracker.core.theme.SalatTrackerTheme
import com.salattracker.domain.model.*
import com.salattracker.domain.usecase.NextPrayer
import com.salattracker.feature.main.TopBar
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale

/** Production composables and bundled fonts/image. All callbacks modify in-memory fixtures only. */
@RunWith(Parameterized::class)
class HomeLayoutRegressionTest(private val width: Int, private val height: Int, private val fontScale: Float,
    private val language: String, private val dark: Boolean) {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val state = mutableStateOf(fixture())
    private var menuOpenCount = 0
    private val output get() = File(InstrumentationRegistry.getInstrumentation().targetContext.externalCacheDir, "home-layout-v27").apply { mkdirs() }
    private val case get() = "${if (width == 0) "native" else "${width}x$height"}-$fontScale-$language-${if (dark) "dark" else "light"}"

    @Test fun layouts_realFixturesKeepTextReachable() {
        render()
        compose.mainClock.autoAdvance = false
        val problems = mutableListOf<String>()
        val base = fixture()
        val fixtures = listOf(
            "men-asr" to base,
            "women-asr" to base.copy(gender = Gender.FEMALE, jumuahEnabled = false),
            "friday-men" to base.copy(todayDate = LocalDate.of(2026, 10, 30), now = LocalDate.of(2026, 10, 30).atTime(13, 0), currentPrayer = PrayerType.DHUHR),
            "friday-women" to base.copy(gender = Gender.FEMALE, jumuahEnabled = false, todayDate = LocalDate.of(2026, 10, 30), currentPrayer = PrayerType.DHUHR),
            "sunrise-gap" to base.copy(now = base.todayDate.atTime(8, 35), currentPrayer = null,
                nextPrayer = NextPrayer(PrayerType.DHUHR, LocalTime.of(11, 47), false), minutesUntilNext = 192),
            "midnight-isha" to base.copy(now = base.todayDate.plusDays(1).atTime(0, 30), currentPrayer = PrayerType.ISHA,
                nextPrayer = NextPrayer(PrayerType.FAJR, LocalTime.of(4, 46), false), minutesUntilNext = 256,
                prayers = base.prayers.map { it.copy(isMarkable = true) }),
            "missing-location" to base.copy(locationLabel = null, currentPrayer = null, nextPrayer = null,
                prayers = base.prayers.map { it.copy(time = null, windowEnd = null, isMarkable = false) }),
            "badge-hidden-photo" to base.copy(appearance = HomeAppearance(streakBadge = true, showPhotoInfo = false)),
        )
        fixtures.forEach { (name, value) ->
            show(value)
            screenshot(name)
            val photoNodes = compose.onAllNodesWithTag("home-photo-info").fetchSemanticsNodes()
            if (photoNodes.isNotEmpty()) {
                val dateBounds = compose.onNodeWithTag("home-date-block").fetchSemanticsNode().boundsInRoot
                val photoBounds = photoNodes.single().boundsInRoot
                if (dateBounds.bottom > photoBounds.top + 1f)
                    problems += "$name hero blocks overlap: dateBottom=${dateBounds.bottom}, photoTop=${photoBounds.top}"
            }
            val nodes = compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult), useUnmergedTree = true)
                .fetchSemanticsNodes()
            compose.runOnIdle {
                nodes.forEach { node ->
                    val layouts = mutableListOf<TextLayoutResult>()
                    node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
                    val bounds = node.boundsInRoot
                    layouts.forEach { text ->
                        val excess = (0 until text.lineCount).maxOfOrNull { text.getLineRight(it) - text.getLineLeft(it) - text.size.width } ?: 0f
                        // Compose can report didOverflowWidth when glyph rounding is <= 1 px and reports
                        // truncated line ends for intentional ellipsis. Positive painted excess and height
                        // overflow are the reliable clipping signals for this mixed-script screen.
                        if (text.didOverflowHeight || (text.didOverflowWidth && excess > 1f))
                            problems += "$name '${text.layoutInput.text}': widthOverflow=${text.didOverflowWidth}, heightOverflow=${text.didOverflowHeight}, excessPx=$excess, bounds=$bounds, size=${text.size}"
                    }
                }
            }
        }
        finish("text-layout", problems)
    }

    @Test fun controls_statusPhotoAndStreakActionsUseRealCallbacks() {
        render()
        compose.onNodeWithTag("home-location-city", useUnmergedTree = true)
            .assertTextEquals("Karachi").assertIsDisplayed()
        val pick = compose.onNodeWithTag("home-pick-hero-ASR-CONGREGATION")
        pick.performClick()
        compose.waitUntil(3_000) { compose.onAllNodesWithTag("home-figure-ASR-CONGREGATION").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("home-status-hero-ASR").assertIsDisplayed()
        compose.onNodeWithTag("home-figure-ASR-CONGREGATION").assertIsDisplayed()
        screenshot("hero-full-figure")
        compose.onNodeWithTag("home-status-row-ASR").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithTag("home-pick-hero-ASR-CONGREGATION").performScrollTo().assertExists()
        compose.onNodeWithTag("home-pick-row-MAGHRIB-CONGREGATION").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("home-photo-hide").performScrollTo().performClick()
        compose.onNodeWithTag("home-photo-show").assertIsDisplayed().performClick()
        compose.onNodeWithTag("home-photo-hide").assertIsDisplayed()
        show(fixture().copy(gender = Gender.FEMALE))
        compose.onNodeWithTag("home-pick-hero-ASR-CONGREGATION").assertDoesNotExist()
        compose.onNodeWithTag("home-pick-hero-ASR-INDIVIDUAL").assertIsEnabled()
        compose.onNodeWithTag("home-pick-hero-ASR-MENSTRUATION").assertIsEnabled()
        val streak = compose.onNodeWithTag("home-streak")
        val moveActions = streak.fetchSemanticsNode().config[SemanticsActions.CustomActions]
        compose.runOnIdle { assertTrue(moveActions.single().action()) }
        compose.runOnIdle { assertTrue(state.value.appearance.streakBadge) }
        compose.onNodeWithTag("home-streak").performScrollTo().performClick()
        compose.onNodeWithTag("home-streak-sheet").assertIsDisplayed()
        screenshot("streak-sheet", "home-streak-sheet")
        dismissSheet()
        compose.onNodeWithTag("home-menu-open").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, menuOpenCount) }
        compose.onNodeWithTag("home-appearance-sheet").assertDoesNotExist()
    }

    @Test fun weekDaySheet_andFigureUndoFollowV27Flow() {
        render()
        compose.onNodeWithTag("home-day-2026-10-25").performClick()
        compose.mainClock.advanceTimeBy(1_000)
        compose.waitForIdle()
        compose.onNodeWithTag("home-day-sheet").assertIsDisplayed()
        screenshot("past-day-sheet", "home-day-sheet")
        dismissSheet()
        compose.onNodeWithTag("home-pick-hero-ASR-INDIVIDUAL").performClick()
        compose.waitUntil(3_000) { compose.onAllNodesWithTag("home-figure-ASR-INDIVIDUAL").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("home-figure-ASR-INDIVIDUAL").assertIsDisplayed().performClick()
        compose.waitUntil(3_000) { compose.onAllNodesWithTag("home-pick-hero-ASR-INDIVIDUAL").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(PrayerStatus.NOT_RECORDED, state.value.prayers.first { it.type == PrayerType.ASR }.status)
    }

    @Test fun verseTranslation_isExpandable() {
        render()
        compose.onNodeWithTag("home-translation-toggle").performScrollTo().performClick()
        compose.onNodeWithTag("home-translation-text").performScrollTo().assertIsDisplayed()
        screenshot("verse-expanded")
    }

    @Test fun appSettings_homeFeedbackControlsPersistCallbacks() {
        val appearance = mutableStateOf(HomeAppearance())
        renderSettings(appearance)
        compose.onNodeWithTag("home-appearance-settings").assertExists()
        compose.onNodeWithTag("home-setting-message").performScrollTo().performClick()
        compose.runOnIdle { assertFalse(appearance.value.showAppreciation) }
        compose.onNodeWithTag("home-setting-chime").performScrollTo().performClick()
        compose.runOnIdle { assertFalse(appearance.value.chime) }
        screenshot("app-settings-home-feedback", "settings-test-root")
    }

    @Test fun shellHostedHome_placesContentBelowTransparentSharedBar() {
        render(hostedBelowSharedTopBar = true)
        compose.onNodeWithTag("home-screen").assertIsDisplayed()
        compose.onNodeWithTag("home-menu-open").assertDoesNotExist()
        compose.onNodeWithTag("app-top-bar").assertIsDisplayed()
        compose.onNodeWithTag("home-shared-top-bar-space").assertIsDisplayed()
        val hero = compose.onNodeWithTag("home-hero").fetchSemanticsNode().boundsInRoot
        val bar = compose.onNodeWithTag("app-top-bar").fetchSemanticsNode().boundsInRoot
        val date = compose.onNodeWithTag("home-date-block").fetchSemanticsNode().boundsInRoot
        assertTrue("Hero must paint behind the shared bar: hero=$hero bar=$bar", hero.top < bar.bottom)
        assertTrue("Hero content must remain below the shared bar: date=$date bar=$bar", date.top >= bar.bottom)
        screenshot("shell-hosted-transparent-bar")
    }

    private fun render(hostedBelowSharedTopBar: Boolean = false) {
        compose.activityRule.scenario.onActivity { WindowCompat.setDecorFitsSystemWindows(it.window, false) }
        compose.setContent {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val device = LocalDensity.current
                val density = if (width == 0) Density(device.density, if (fontScale == 0f) device.fontScale else fontScale)
                    else Density(minOf(device.density, constraints.maxWidth.toFloat() / width, constraints.maxHeight.toFloat() / height), fontScale)
                val configuration = android.content.res.Configuration(LocalConfiguration.current).apply { setLocale(Locale.forLanguageTag(language)) }
                val context = LocalContext.current.createConfigurationContext(configuration)
                CompositionLocalProvider(LocalDensity provides density, LocalConfiguration provides configuration, LocalContext provides context,
                    LocalLayoutDirection provides if (language in listOf("ar", "ur")) LayoutDirection.Rtl else LayoutDirection.Ltr) {
                    SalatTrackerTheme(darkTheme = dark) {
                        Box((if (width == 0) Modifier.fillMaxSize() else Modifier.requiredSize(width.dp, height.dp)).testTag("home-test-root")) {
                            HomeScreen(state.value, onPrayerCardClick = {}, onSheetDismiss = {}, onStatusSave = {},
                                onQazaClick = {}, onDayClick = {}, onAppearanceChanged = { state.value = state.value.copy(appearance = it) },
                                onMenuClick = { menuOpenCount++ },
                                hostedBelowSharedTopBar = hostedBelowSharedTopBar,
                                onQuickStatusSave = { type, status -> state.value = state.value.copy(prayers = state.value.prayers.map {
                                    if (it.type == type) it.copy(status = status) else it }) },
                                onReminderToggle = { type -> state.value = state.value.copy(prayers = state.value.prayers.map {
                                    if (it.type == type) it.copy(reminderEnabled = !it.reminderEnabled) else it }) })
                            if (hostedBelowSharedTopBar) {
                                TopBar(
                                    title = "Today",
                                    isTopLevel = true,
                                    backupNeedsAttention = false,
                                    onMenuClick = {},
                                    onBackClick = {},
                                    showAddGoal = false,
                                    onAddGoalClick = {},
                                    goalDetailsControls = null,
                                    showCalendarEdit = false,
                                    onCalendarEditClick = {},
                                    showQuranSearch = false,
                                    onQuranSearchClick = {},
                                    showQuranResume = false,
                                    quranResumeEnabled = false,
                                    onQuranResumeClick = {},
                                    showMushafButton = false,
                                    onMushafButtonClick = {},
                                    quranPageControls = null,
                                    calendarAwayFromToday = false,
                                    onCalendarTodayClick = {},
                                    overlaysHomeHero = true,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    private fun renderSettings(appearance: MutableState<HomeAppearance>) {
        compose.activityRule.scenario.onActivity { WindowCompat.setDecorFitsSystemWindows(it.window, false) }
        compose.setContent {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val device = LocalDensity.current
                val density = if (width == 0) Density(device.density, if (fontScale == 0f) device.fontScale else fontScale)
                    else Density(minOf(device.density, constraints.maxWidth.toFloat() / width, constraints.maxHeight.toFloat() / height), fontScale)
                val configuration = android.content.res.Configuration(LocalConfiguration.current).apply { setLocale(Locale.forLanguageTag(language)) }
                val context = LocalContext.current.createConfigurationContext(configuration)
                CompositionLocalProvider(LocalDensity provides density, LocalConfiguration provides configuration, LocalContext provides context,
                    LocalLayoutDirection provides if (language in listOf("ar", "ur")) LayoutDirection.Rtl else LayoutDirection.Ltr) {
                    SalatTrackerTheme(darkTheme = dark) {
                        Column((if (width == 0) Modifier.fillMaxSize() else Modifier.requiredSize(width.dp, height.dp))
                            .verticalScroll(rememberScrollState()).padding(20.dp).testTag("settings-test-root")) {
                            HomeFeedbackSettingsCard(appearance.value) { appearance.value = it }
                        }
                    }
                }
            }
        }
    }
    private fun show(value: HomeUiState) {
        compose.runOnIdle { state.value = value }
        if (!compose.mainClock.autoAdvance) compose.mainClock.advanceTimeBy(2_000)
        compose.waitForIdle()
        compose.onNodeWithTag("home-scroll").performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, -100000f) }
        compose.waitForIdle()
    }
    private fun screenshot(name: String, tag: String = "home-test-root") {
        val bitmap = compose.onNodeWithTag(tag).captureToImage().asAndroidBitmap()
        File(output, "$name-$case.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
    private fun dismissSheet() {
        compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.Dismiss))
            .performSemanticsAction(SemanticsActions.Dismiss)
        compose.waitForIdle()
    }
    private fun finish(name: String, errors: List<String>) {
        File(output, "$name-$case.txt").writeText("findings=${errors.size}\n" + errors.joinToString("\n"))
        assertTrue("$case:\n" + errors.joinToString("\n"), errors.isEmpty())
    }
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "width={0} font={2} locale={3} dark={4}")
        fun cases(): List<Array<Any>> = listOf(arrayOf(0, 0, 0f, "en", false), arrayOf(0, 0, 1.3f, "en", false),
            arrayOf(320, 640, 1f, "en", false), arrayOf(320, 640, 1.3f, "en", false),
            arrayOf(360, 800, 1f, "en", false), arrayOf(360, 800, 1.3f, "en", false),
            arrayOf(600, 960, 1f, "en", false), arrayOf(600, 960, 1.3f, "en", false),
            arrayOf(0, 0, 1.3f, "ar", false), arrayOf(0, 0, 1.3f, "ur", false), arrayOf(0, 0, 1.3f, "fr", false), arrayOf(0, 0, 0f, "en", true))

        private fun fixture(): HomeUiState {
            val date = LocalDate.of(2026, 10, 26)
            val times = listOf(LocalTime.of(4, 46), LocalTime.of(11, 47), LocalTime.of(15, 2), LocalTime.of(17, 27), LocalTime.of(18, 44))
            val entries = PrayerType.values().mapIndexed { index, type -> PrayerEntryUi(type, times[index], null,
                when (index) { 0 -> PrayerStatus.CONGREGATION; 1 -> PrayerStatus.INDIVIDUAL; else -> PrayerStatus.NOT_RECORDED }, index <= 2,
                windowEnd = when (index) { 0 -> date.atTime(6, 4); 4 -> date.plusDays(1).atTime(times[0]); else -> date.atTime(times[index + 1]) },
                loggedOnTime = if (index < 2) index == 0 else null) }
            return HomeUiState(isLoading = false, now = date.atTime(16, 3), todayDate = date, prayers = entries,
                currentPrayer = PrayerType.ASR, nextPrayer = NextPrayer(PrayerType.MAGHRIB, times[3], false), minutesUntilNext = 84,
                gender = Gender.MALE, locationLabel = "Karachi, Sindh, Pakistan", qazaPendingTotal = 18,
                streaks = PrayerType.values().associateWith { StreakInfo(7, 14) },
                lastSevenDays = (1..6).associate { date.minusDays(it.toLong()) to PrayerType.values().associateWith { PrayerStatus.CONGREGATION } },
                verse = VerseInfo("Maintain with care the obligatory prayers and the middle prayer.", "Al-Baqarah 2:238",
                    "حَافِظُوا عَلَى الصَّلَوَاتِ وَالصَّلَاةِ الْوُسْطَىٰ"))
        }
    }
}
