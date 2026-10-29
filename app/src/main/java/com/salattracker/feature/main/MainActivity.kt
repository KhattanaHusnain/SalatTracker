package com.salattracker.feature.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.salattracker.R
import com.salattracker.core.theme.*
import com.salattracker.core.ui.LocalUse24HourFormat
import com.salattracker.core.ui.LocalHijriAdjustmentDays
import com.salattracker.core.ui.ArchitecturalFrieze
import com.salattracker.domain.model.QuranReadHistoryEntry as QuranReadingPosition
import com.salattracker.domain.model.isDark
import com.salattracker.feature.backup.BackupEntry
import com.salattracker.feature.developers.DevelopersEntry
import com.salattracker.feature.donate.DonateEntry
import com.salattracker.feature.bulkupdate.BulkUpdateEntry
import com.salattracker.feature.calendar.CalendarEntry
import com.salattracker.feature.calendar.CalendarV4ViewModel
import com.salattracker.feature.goals.AddGoalEntry
import com.salattracker.feature.goals.EditGoalEntry
import com.salattracker.feature.goals.GoalDetailsEntry
import com.salattracker.feature.goals.GoalDetailsTopBarControls
import com.salattracker.feature.goals.GoalLogDayEntry
import com.salattracker.feature.goals.GoalTemplate
import com.salattracker.feature.goals.GoalsEntry
import com.salattracker.feature.tasbih.TasbihEntry
import com.salattracker.feature.home.HomeEntry
import com.salattracker.feature.mosque.MosqueModeEntry
import com.salattracker.core.ui.MadhabPromptDialog
import com.salattracker.feature.onboarding.OnboardingEntry
import com.salattracker.feature.settings.EditCoordinatesEntry
import com.salattracker.feature.qaza.QazaEntry
import com.salattracker.feature.quran.QuranBookmarksEntry
import com.salattracker.feature.quran.QuranChapterInfoEntry
import com.salattracker.feature.quran.QuranMushafEntry
import com.salattracker.feature.quran.QuranPageControls
import com.salattracker.feature.quran.QuranChapterListEntry
import com.salattracker.feature.quran.QuranCreditsEntry
import com.salattracker.feature.quran.QuranReadHistoryEntry
import com.salattracker.feature.quran.QuranReaderEntry
import com.salattracker.feature.reminders.PrayerRemindersEntry
import com.salattracker.feature.settings.SettingsEntry
import com.salattracker.feature.stats.StatsEntry
import com.salattracker.core.locale.LocaleManager
import com.salattracker.domain.repository.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var settingsRepository: SettingsRepository

    /** Set from a notification's EXTRA_OPEN_ROUTE (onCreate for a cold start, onNewIntent for a
     * running instance — see android:launchMode="singleTask" on this activity in the manifest).
     * Only read from the retained intent on a fresh start (savedInstanceState == null); otherwise
     * an activity recreation (rotation, the language-change recreation in LocaleManager.apply)
     * would re-push the same route every time. The still-unconsumed value survives recreation
     * via onSaveInstanceState below instead. */
    private var pendingRoute by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        pendingRoute = if (savedInstanceState == null) {
            intent.openRouteOrNull()
        } else {
            savedInstanceState.getString(STATE_PENDING_ROUTE)
        }

        // Reconcile the app-wide locale with the saved language on launch. No-op once they match;
        // recreates the activity in the right locale otherwise (e.g. first run after this update,
        // when a previously-saved language had never actually been applied).
        lifecycleScope.launch {
            LocaleManager.apply(settingsRepository.getCurrentPrefs().language)
        }

        setContent {
            val prefs by settingsRepository.observeSettings().collectAsState(
                initial = com.salattracker.domain.repository.UserPrefs(),
            )
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { }

            val locationSyncViewModel: LocationSyncViewModel = hiltViewModel()
            var onboardingLocationResult by remember { mutableStateOf<((Boolean) -> Unit)?>(null) }
            val locationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { granted ->
                onboardingLocationResult?.invoke(granted)
                onboardingLocationResult = null
                if (granted) locationSyncViewModel.syncDeviceLocation()
            }

            val appStartViewModel: AppStartViewModel = hiltViewModel()
            val appStart by appStartViewModel.state.collectAsState()
            val onboardingCompleted = appStart.onboardingCompleted

            // Returning-user startup: request notification + location permissions and sync location.
            // First-run users go through onboarding instead, which requests these itself.
            LaunchedEffect(onboardingCompleted) {
                if (onboardingCompleted != true) return@LaunchedEffect
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }

                if (locationSyncViewModel.isDeviceLocationEnabled()) {
                    if (ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        locationSyncViewModel.syncDeviceLocation()
                    } else {
                        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                }
            }

            SalatTrackerTheme(darkTheme = appStart.themeMode.isDark(isSystemInDarkTheme())) {
                CompositionLocalProvider(
                    LocalUse24HourFormat provides prefs.use24HourFormat,
                    LocalHijriAdjustmentDays provides prefs.hijriAdjustmentDays,
                ) {
                when (onboardingCompleted) {
                    // null = DataStore not read yet — plain background avoids a flash of the wrong screen.
                    null  -> Box(modifier = Modifier.fillMaxSize().background(SalatBackground))
                    false -> OnboardingEntry(
                        onAllowLocation = { onResult ->
                            onboardingLocationResult = onResult
                            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        },
                        onRequestNotifications = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                ContextCompat.checkSelfPermission(
                                    this@MainActivity,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        },
                    )
                    true  -> {
                        MainScaffold(initialRoute = pendingRoute, onRouteConsumed = { pendingRoute = null })
                        if (appStart.needsMadhabPrompt) {
                            MadhabPromptDialog(onSave = appStartViewModel::onMadhabChosen)
                        }
                    }
                }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingRoute = intent.openRouteOrNull()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_PENDING_ROUTE, pendingRoute)
    }

    /** Only known deep-link targets are accepted — an exported, singleTask activity should
     * never hand an arbitrary caller-supplied string straight to navigate(). */
    private fun Intent?.openRouteOrNull(): String? =
        this?.getStringExtra(EXTRA_OPEN_ROUTE)?.takeIf { it == ROUTE_BACKUP || it == ROUTE_GOALS || it == ROUTE_MOSQUE_MODE }

    companion object {
        /** Intent extra a notification sets to deep-link straight to a route once the app is up. */
        const val EXTRA_OPEN_ROUTE = "open_route"
        const val ROUTE_BACKUP = "backup"
        const val ROUTE_GOALS = "goals"
        const val ROUTE_MOSQUE_MODE = "mosque_mode"
        private const val STATE_PENDING_ROUTE = "pending_route"
    }
}

// ── Main scaffold ─────────────────────────────────────────────────────────────

/** Tab switches preserve save/restore state across the bottom nav; everything else is a
 * plain push, deduped against repeat taps by [launchSingleTop]. */
private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState    = true
    }
}

private fun NavHostController.push(route: String) {
    navigate(route) { launchSingleTop = true }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold(initialRoute: String? = null, onRouteConsumed: () -> Unit = {}) {
    val navController = rememberNavController()
    val navBackStack  by navController.currentBackStackEntryAsState()
    val currentDest   = navBackStack?.destination
    val currentRoute  = currentDest?.route
    val isTopLevel    = currentRoute == null || isTabRoute(currentRoute)

    val drawerState    = rememberDrawerState(DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    val shellViewModel: MainShellViewModel = hiltViewModel()
    val backupNeedsAttention by shellViewModel.backupNeedsAttention.collectAsState()
    val qazaPendingTotal by shellViewModel.qazaPendingTotal.collectAsState()
    val hideQazaBadge by shellViewModel.hideQazaBadge.collectAsState()
    val isHome = currentRoute == null || currentRoute == BottomNavRoute.Home.route
    val view = LocalView.current
    val background = SalatBackground
    DisposableEffect(isHome, background) {
        val window = (view.context as? android.app.Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val previous = controller?.isAppearanceLightStatusBars
        controller?.isAppearanceLightStatusBars = !isHome && background.luminance() > .5f
        onDispose { previous?.let { controller?.isAppearanceLightStatusBars = it } }
    }

    // Scoped here (not inside the NavHost's Calendar composable) so the exact same instance
    // backs both CalendarEntry's screen and the Today/Month controls in TopBar below — two
    // independent hiltViewModel() calls at those different scopes would otherwise create two
    // unrelated ViewModels with their own separate state.
    val calendarViewModel: CalendarV4ViewModel = hiltViewModel()
    val calendarUiState by calendarViewModel.uiState.collectAsState()

    // Mirrors QuranReaderScreen's currently-loaded chapter's first page, so the top bar's Mushaf
    // button (a route-level action, a sibling of the NavHost rather than a child of the reader
    // screen) knows which page to jump to — see QuranReaderEntry's onPageLoaded.
    var quranReaderPageNumber by remember { mutableStateOf(1) }
    var quranPageControls by remember { mutableStateOf<QuranPageControls?>(null) }
    var quranSearchRequest by remember { mutableStateOf(0) }
    var quranLastRead by remember { mutableStateOf<QuranReadingPosition?>(null) }
    var goalDetailsControls by remember { mutableStateOf<GoalDetailsTopBarControls?>(null) }
    val isQuranPageRoute = currentDest?.route == Routes.QURAN_READER || currentDest?.route == Routes.QURAN_MUSHAF

    // A notification's deep link, delivered after this scaffold (and its NavHost) already
    // exist — navigate once, then let the caller clear it so a config change or
    // recomposition doesn't navigate again.
    LaunchedEffect(initialRoute) {
        if (initialRoute != null) {
            if (isTabRoute(initialRoute)) navController.navigateToTab(initialRoute)
            else navController.push(initialRoute)
            onRouteConsumed()
        }
    }

    // The drawer only ever leads to a pushed (non-tab) screen — close it as soon as we land
    // on one, so a config change or back-navigation never finds it stuck open.
    LaunchedEffect(isTopLevel) {
        if (!isTopLevel) drawerState.close()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        // Closed -> gestures off, so an edge-swipe on e.g. Stats' horizontally-scrolling
        // charts can never pull the drawer open; it only opens from the menu icon. Once
        // open, gestures turn back on so swiping it shut still works.
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            AppDrawer(
                backupNeedsAttention   = backupNeedsAttention,
                onPrayerRemindersClick = { navController.push(Routes.PRAYER_REMINDERS) },
                onMosqueModeClick = { navController.push(Routes.MOSQUE_MODE) },
                onBackupClick          = { navController.push(Routes.BACKUP) },
                onDevelopersClick      = { navController.push(Routes.DEVELOPERS) },
                onStatsClick           = { navController.push(Routes.STATS) },
                onTasbihClick          = { navController.push(Routes.tasbih()) },
                onQuranCreditsClick    = { navController.push(Routes.QURAN_CREDITS) },
                onSettingsClick        = { navController.push(Routes.SETTINGS) },
            )
        },
    ) {
        Scaffold(
            containerColor = SalatBackground,
            contentWindowInsets = ScaffoldDefaults.contentWindowInsets,
            topBar = {
                TopBar(
                    title                = if (currentDest?.route == Routes.GOAL_DETAILS) goalDetailsControls?.title ?: topBarTitle(currentDest?.route) else topBarTitle(currentDest?.route),
                    isTopLevel           = isTopLevel,
                    backupNeedsAttention = backupNeedsAttention,
                    onMenuClick          = { coroutineScope.launch { drawerState.open() } },
                    onBackClick          = { backDispatcher?.onBackPressed() },
                    showAddGoal          = currentDest?.route == BottomNavRoute.Goals.route,
                    onAddGoalClick       = { navController.push(Routes.addGoal()) },
                    goalDetailsControls  = if (currentDest?.route == Routes.GOAL_DETAILS) goalDetailsControls else null,
                    showCalendarEdit     = currentDest?.route == BottomNavRoute.Calendar.route,
                    onCalendarEditClick  = calendarViewModel::toggleEditMode,
                    showQuranSearch      = currentDest?.route == BottomNavRoute.Quran.route,
                    onQuranSearchClick   = { quranSearchRequest++ },
                    showQuranResume      = currentDest?.route == BottomNavRoute.Quran.route,
                    quranResumeEnabled   = quranLastRead != null,
                    onQuranResumeClick   = {
                        quranLastRead?.let { position ->
                            val juzNumber = position.juzNumber
                            if (juzNumber != null) {
                                navController.push(Routes.quranMushaf(position.pageNumber ?: 1, juzNumber))
                            } else {
                                navController.push(Routes.quranReader(position.chapterNumber, position.verseNumber))
                            }
                        }
                    },
                    showMushafButton     = currentDest?.route == Routes.QURAN_READER ||
                        (currentDest?.route == Routes.QURAN_MUSHAF && quranPageControls?.showMushafButton == true),
                    onMushafButtonClick  = {
                        quranPageControls?.takeIf { it.showMushafButton }?.onMushafButtonClick
                            ?.invoke()
                            ?: navController.push(Routes.quranMushaf(quranReaderPageNumber))
                    },
                    quranPageControls    = if (isQuranPageRoute) quranPageControls else null,
                    calendarAwayFromToday    = calendarUiState.awayFromToday,
                    onCalendarTodayClick     = calendarViewModel::onTodayClick,
                    overlaysHomeHero         = isHome,
                )
            },
            bottomBar = {
                AnimatedVisibility(
                    visible = isTopLevel && !(currentDest?.route == BottomNavRoute.Calendar.route && calendarUiState.isEditMode),
                    enter   = slideInVertically(initialOffsetY = { it }),
                    exit    = slideOutVertically(targetOffsetY = { it }),
                ) {
                    SalatBottomNavBar(
                        routes      = BottomNavRoute.all,
                        currentRoute = currentDest?.route,
                        qazaPendingTotal = if (hideQazaBadge) 0 else qazaPendingTotal,
                        onNavigate  = { route -> navController.navigateToTab(route.route) },
                    )
                }
            },
        ) { innerPadding ->
            val layoutDirection = LocalLayoutDirection.current
            // Home owns the top inset so its hero can paint behind the shared transparent bar.
            // Other destinations continue consuming Scaffold's complete content padding.
            val contentModifier = if (isHome) {
                Modifier.padding(
                    start = innerPadding.calculateStartPadding(layoutDirection),
                    end = innerPadding.calculateEndPadding(layoutDirection),
                    bottom = innerPadding.calculateBottomPadding(),
                )
            } else {
                Modifier.padding(innerPadding)
            }
            NavHost(
                navController    = navController,
                startDestination = BottomNavRoute.Home.route,
                modifier         = contentModifier,
            ) {
                composable(BottomNavRoute.Home.route) {
                    HomeEntry(
                        onQazaClick = { navController.navigateToTab(BottomNavRoute.Qaza.route) },
                        onMenuClick = { coroutineScope.launch { drawerState.open() } },
                        hostedBelowSharedTopBar = true,
                        onLocationClick = { navController.push(Routes.SETTINGS) },
                        onDonateClick = { navController.push(Routes.DONATE) },
                        onCatchUpClick = { navController.push(Routes.bulkUpdate()) },
                        onDayClick  = { date ->
                            calendarViewModel.onOpenDay(date)
                            navController.navigateToTab(BottomNavRoute.Calendar.route)
                        },
                    )
                }
                composable(BottomNavRoute.Quran.route) {
                    QuranChapterListEntry(
                        onChapterClick = { number -> navController.push(Routes.quranReader(number)) },
                        onJuzClick = { juzNumber, pageNumber ->
                            navController.push(Routes.quranMushaf(pageNumber, juzNumber))
                        },
                        searchRequest = quranSearchRequest,
                        onSearchRequestConsumed = { quranSearchRequest = 0 },
                        onLastReadChanged = { lastRead -> quranLastRead = lastRead },
                    )
                }
                composable(
                    Routes.QURAN_READER,
                    arguments = listOf(
                        navArgument("chapterNumber") { type = NavType.IntType },
                        navArgument("verseNumber") { type = NavType.IntType; defaultValue = 1 },
                    ),
                ) {
                    QuranReaderEntry(
                        onChapterInfoClick = { number -> navController.push(Routes.quranChapterInfo(number)) },
                        onPageLoaded = { page -> quranReaderPageNumber = page },
                        onPageControlsChanged = { controls -> quranPageControls = controls },
                    )
                }
                composable(
                    Routes.QURAN_CHAPTER_INFO,
                    arguments = listOf(navArgument("chapterNumber") { type = NavType.IntType }),
                ) {
                    QuranChapterInfoEntry(onReadClick = { number -> navController.push(Routes.quranReader(number)) })
                }
                composable(
                    Routes.QURAN_MUSHAF,
                    arguments = listOf(
                        navArgument("pageNumber") { type = NavType.IntType },
                        navArgument("juzNumber") { type = NavType.IntType; defaultValue = 0 },
                    ),
                ) {
                    QuranMushafEntry(onPageControlsChanged = { controls -> quranPageControls = controls })
                }
                composable(Routes.QURAN_BOOKMARKS) {
                    QuranBookmarksEntry(onBookmarkClick = { number -> navController.push(Routes.quranReader(number)) })
                }
                composable(Routes.QURAN_READ_HISTORY) {
                    QuranReadHistoryEntry(onEntryClick = { number -> navController.push(Routes.quranReader(number)) })
                }
                composable(BottomNavRoute.Calendar.route) {
                    CalendarEntry(calendarViewModel) { navController.navigateToTab(BottomNavRoute.Qaza.route) }
                }
                composable(BottomNavRoute.Qaza.route) {
                    QazaEntry()
                }
                composable(BottomNavRoute.Goals.route) {
                    GoalsEntry(
                        onGoalClick    = { goalId -> navController.push(Routes.goalDetails(goalId)) },
                        onAddGoalClick = { template -> navController.push(Routes.addGoal(template?.name)) },
                        onDayClick = { goalId, date -> navController.push(Routes.goalLogDay(goalId, date.toString())) },
                        onTasbihClick = { goalId -> navController.push(Routes.tasbih(goalId)) },
                    )
                }
                composable(Routes.SETTINGS) {
                    SettingsEntry(onEditLocationClick = { navController.push(Routes.EDIT_COORDINATES) })
                }
                composable(Routes.EDIT_COORDINATES) {
                    EditCoordinatesEntry(onDone = { navController.popBackStack() })
                }
                composable(Routes.PRAYER_REMINDERS) {
                    PrayerRemindersEntry()
                }
                composable(Routes.BACKUP) {
                    BackupEntry()
                }
                composable(Routes.MOSQUE_MODE) {
                    MosqueModeEntry()
                }
                composable(Routes.DONATE) {
                    DonateEntry()
                }
                composable(Routes.DEVELOPERS) {
                    DevelopersEntry()
                }
                composable(Routes.QURAN_CREDITS) {
                    QuranCreditsEntry()
                }
                composable(Routes.STATS) {
                    StatsEntry()
                }
                composable(
                    Routes.TASBIH,
                    arguments = listOf(navArgument("goalId") { type = NavType.LongType; defaultValue = -1L }),
                ) {
                    TasbihEntry()
                }
                composable(
                    Routes.BULK_UPDATE,
                    arguments = listOf(navArgument("status") { type = NavType.StringType; nullable = true; defaultValue = null }),
                ) {
                    BulkUpdateEntry()
                }
                composable(
                    Routes.ADD_GOAL,
                    arguments = listOf(navArgument("template") { type = NavType.StringType; nullable = true; defaultValue = null }),
                ) { entry ->
                    val template = entry.arguments?.getString("template")?.takeIf(String::isNotBlank)?.let { runCatching { GoalTemplate.valueOf(it) }.getOrNull() }
                    AddGoalEntry(onSaved = { navController.popBackStack() }, initialTemplate = template)
                }
                composable(
                    Routes.GOAL_DETAILS,
                    arguments = listOf(navArgument("goalId") { type = NavType.LongType }),
                ) {
                    GoalDetailsEntry(
                        onEdit    = { goalId -> navController.push(Routes.editGoal(goalId)) },
                        onLogDay = { goalId, date -> navController.push(Routes.goalLogDay(goalId, date.toString())) },
                        onDeleted = { navController.popBackStack() },
                        onTopBarControls = { goalDetailsControls = it },
                    )
                }
                composable(
                    Routes.GOAL_LOG_DAY,
                    arguments = listOf(
                        navArgument("goalId") { type = NavType.LongType },
                        navArgument("date") { type = NavType.StringType },
                    ),
                ) {
                    GoalLogDayEntry(onDone = { navController.popBackStack() })
                }
                composable(
                    Routes.EDIT_GOAL,
                    arguments = listOf(navArgument("goalId") { type = NavType.LongType }),
                ) {
                    EditGoalEntry(onSaved = { navController.popBackStack() })
                }
            }
        }
    }
}

// ── Top app bar ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TopBar(
    title: String,
    isTopLevel: Boolean,
    backupNeedsAttention: Boolean,
    onMenuClick: () -> Unit,
    onBackClick: () -> Unit,
    showAddGoal: Boolean,
    onAddGoalClick: () -> Unit,
    goalDetailsControls: GoalDetailsTopBarControls?,
    showCalendarEdit: Boolean,
    onCalendarEditClick: () -> Unit,
    showQuranSearch: Boolean,
    onQuranSearchClick: () -> Unit,
    showQuranResume: Boolean,
    quranResumeEnabled: Boolean,
    onQuranResumeClick: () -> Unit,
    showMushafButton: Boolean,
    onMushafButtonClick: () -> Unit,
    quranPageControls: QuranPageControls?,
    calendarAwayFromToday: Boolean,
    onCalendarTodayClick: () -> Unit,
    overlaysHomeHero: Boolean = false,
) {
    val barSurface by animateColorAsState(
        targetValue = if (overlaysHomeHero) Color.Transparent else SalatBackground,
        label = "top-bar-surface",
    )
    val barPrimary by animateColorAsState(
        targetValue = if (overlaysHomeHero) Color.White else SalatPrimary,
        label = "top-bar-primary",
    )
    val barSecondary by animateColorAsState(
        targetValue = if (overlaysHomeHero) Color.White else SalatOnSurface,
        label = "top-bar-secondary",
    )
    TopAppBar(
        modifier = Modifier.testTag("app-top-bar"),
        title = {
            Box(Modifier.fillMaxWidth()) {
                ArchitecturalFrieze(
                    modifier = Modifier.align(Alignment.CenterEnd).size(112.dp, 18.dp),
                    color = barPrimary,
                    alpha = .16f,
                )
                Text(
                    text       = title,
                    color      = barPrimary,
                    fontSize   = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = AppTitleFontFamily,
                )
            }
        },
        navigationIcon = {
            // The five tabs show the menu icon (with an attention dot when backup needs it);
            // every pushed screen shows a back arrow instead, routed through the same
            // OnBackPressedDispatcher as the system back gesture so both always agree —
            // Settings' own BackHandler gets first chance to intercept either one.
            if (isTopLevel) {
                IconButton(onClick = onMenuClick) {
                    BadgedBox(badge = { if (backupNeedsAttention) Badge(containerColor = SalatError) }) {
                        Icon(painterResource(R.drawable.ic_menu), stringResource(R.string.cd_menu), tint = barPrimary)
                    }
                }
            } else {
                IconButton(onClick = onBackClick) {
                    Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.cd_back), tint = barPrimary)
                }
            }
        },
        actions = {
            quranPageControls?.let { controls ->
                IconButton(onClick = controls.onPreviousPage, enabled = controls.hasPreviousPage) {
                    Icon(painterResource(R.drawable.ic_chevron_left), stringResource(R.string.quran_mushaf_previous_page), tint = barPrimary)
                }
                Text(
                    text = stringResource(R.string.quran_mushaf_page_label, controls.pageNumber),
                    color = barSecondary,
                    fontSize = 12.sp,
                    fontFamily = AppUiFontFamily,
                )
                IconButton(onClick = controls.onNextPage, enabled = controls.hasNextPage) {
                    Icon(painterResource(R.drawable.ic_chevron_right), stringResource(R.string.quran_mushaf_next_page), tint = barPrimary)
                }
            }
            if (showAddGoal) {
                IconButton(onClick = onAddGoalClick) {
                    Icon(painterResource(R.drawable.ic_add), stringResource(R.string.action_add_goal), tint = barPrimary)
                }
            }
            goalDetailsControls?.let { controls ->
                var expanded by remember { mutableStateOf(false) }
                IconButton(onClick = controls.onEdit) {
                    Icon(painterResource(R.drawable.ic_edit_square), stringResource(R.string.goal_action_edit), tint = barPrimary)
                }
                Box {
                    IconButton(onClick = { expanded = true }) {
                        Icon(painterResource(R.drawable.ic_more_vert), stringResource(R.string.goals_v2_more_options), tint = barPrimary)
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        if (controls.status != com.salattracker.domain.usecase.GoalStatus.COMPLETED) {
                            DropdownMenuItem(
                                text = { Text(stringResource(if (controls.status == com.salattracker.domain.usecase.GoalStatus.ARCHIVED) R.string.goal_action_restore else R.string.goal_action_archive)) },
                                onClick = { expanded = false; controls.onArchiveToggle() },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_delete), color = SalatError) },
                            onClick = { expanded = false; controls.onDelete() },
                        )
                    }
                }
            }
            if (showCalendarEdit) {
                if (calendarAwayFromToday) {
                    IconButton(onClick = onCalendarTodayClick) {
                        Icon(painterResource(R.drawable.ic_calendar_today), stringResource(R.string.calendar_today), tint = barPrimary)
                    }
                }
                IconButton(onClick = onCalendarEditClick) {
                    Icon(painterResource(R.drawable.ic_edit_square), stringResource(R.string.calendar_v4_edit), tint = barPrimary)
                }
            }
            if (showQuranSearch) {
                IconButton(onClick = onQuranSearchClick) {
                    Icon(painterResource(R.drawable.ic_search), stringResource(R.string.quran_jump_title), tint = barPrimary)
                }
            }
            if (showQuranResume) {
                IconButton(onClick = onQuranResumeClick, enabled = quranResumeEnabled) {
                    Icon(painterResource(R.drawable.ic_play), stringResource(R.string.quran_continue_reading), tint = barPrimary)
                }
            }
            if (showMushafButton) {
                IconButton(onClick = onMushafButtonClick) {
                    Icon(painterResource(R.drawable.ic_nav_quran), stringResource(R.string.quran_mushaf_mode), tint = barPrimary)
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = barSurface,
            scrolledContainerColor = barSurface,
            navigationIconContentColor = barPrimary,
            titleContentColor = barPrimary,
            actionIconContentColor = barPrimary,
        ),
    )
}

// ── Bottom navigation bar ─────────────────────────────────────────────────────

@Composable
internal fun SalatBottomNavBar(
    routes: List<BottomNavRoute>,
    currentRoute: String?,
    onNavigate: (BottomNavRoute) -> Unit,
    qazaPendingTotal: Int = 0,
) {
    val surface = SalatSurface
    val muted = SalatOnSurfaceVariant
    Surface(Modifier.testTag("bottom-navigation"), color = surface) {
        Column {
            HorizontalDivider(color = muted.copy(alpha = .2f))
            Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(top = 6.dp, bottom = 8.dp)) {
                routes.forEach { route ->
                    val selected = route.route == currentRoute
                    val label = stringResource(route.labelRes)
                    Column(Modifier.weight(1f).heightIn(min = 52.dp)
                        .testTag("bottom-nav-${route.route}")
                        .selectable(selected = selected, role = Role.Tab) { onNavigate(route) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center) {
                        BadgedBox(badge = {
                            if (route.route == BottomNavRoute.Qaza.route && qazaPendingTotal > 0)
                                Badge(containerColor = SalatOutline, contentColor = SalatOnPrimary) {
                                    Text(if (qazaPendingTotal > 99) "99+" else "$qazaPendingTotal", fontSize = 10.sp)
                                }
                        }) {
                            Icon(painterResource(route.iconRes), label, Modifier.size(20.dp),
                                tint = if (selected) SalatPrimary else muted)
                        }
                        Text(label, Modifier.testTag("bottom-nav-label-${route.route}"),
                            fontFamily = AppUiFontFamily,
                            fontSize = 11.sp, color = if (selected) SalatPrimary else muted,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 1)
                    }
                }
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

@Composable
private fun topBarTitle(route: String?): String = stringResource(
    when (route) {
        BottomNavRoute.Home.route     -> R.string.topbar_today
        BottomNavRoute.Calendar.route -> R.string.topbar_calendar
        BottomNavRoute.Qaza.route     -> R.string.topbar_qaza
        BottomNavRoute.Goals.route    -> R.string.topbar_goals
        BottomNavRoute.Quran.route    -> R.string.topbar_quran
        Routes.SETTINGS               -> R.string.topbar_settings
        Routes.EDIT_COORDINATES       -> R.string.topbar_edit_location
        Routes.BACKUP                 -> R.string.topbar_backup
        Routes.BULK_UPDATE            -> R.string.topbar_bulk_update
        Routes.PRAYER_REMINDERS       -> R.string.prayer_reminders
        Routes.MOSQUE_MODE            -> R.string.topbar_mosque_mode
        Routes.DONATE                 -> R.string.topbar_donate
        Routes.DEVELOPERS             -> R.string.topbar_developers
        Routes.STATS                  -> R.string.topbar_stats
        Routes.TASBIH                 -> R.string.tasbih_title
        Routes.ADD_GOAL               -> R.string.topbar_new_goal
        Routes.GOAL_LOG_DAY            -> R.string.goals_v2_log_day
        Routes.QURAN_READER           -> R.string.topbar_quran
        Routes.QURAN_CHAPTER_INFO     -> R.string.topbar_quran
        Routes.QURAN_MUSHAF           -> R.string.topbar_quran
        Routes.QURAN_BOOKMARKS        -> R.string.topbar_quran_bookmarks
        Routes.QURAN_READ_HISTORY     -> R.string.topbar_quran_read_history
        Routes.QURAN_CREDITS          -> R.string.topbar_quran_credits
        else -> when {
            route?.startsWith("edit_goal") == true    -> R.string.topbar_edit_goal
            route?.startsWith("goal_details") == true -> R.string.topbar_goal_details
            else -> R.string.topbar_default
        }
    }
)
