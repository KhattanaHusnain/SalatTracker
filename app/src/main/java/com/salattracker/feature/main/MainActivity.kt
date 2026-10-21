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
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.salattracker.R
import com.salattracker.core.theme.*
import com.salattracker.core.ui.label
import com.salattracker.domain.model.PrayerStatus
import com.salattracker.domain.model.isDark
import com.salattracker.domain.usecase.CalendarViewType
import com.salattracker.feature.backup.BackupEntry
import com.salattracker.feature.developers.DevelopersEntry
import com.salattracker.feature.donate.DonateEntry
import com.salattracker.feature.bulkupdate.BulkUpdateEntry
import com.salattracker.feature.calendar.CalendarEntry
import com.salattracker.feature.calendar.CalendarViewModel
import com.salattracker.feature.goals.AddGoalEntry
import com.salattracker.feature.goals.EditGoalEntry
import com.salattracker.feature.goals.GoalDetailsEntry
import com.salattracker.feature.goals.GoalsEntry
import com.salattracker.feature.home.HomeEntry
import com.salattracker.feature.mosque.MosqueModeEntry
import com.salattracker.core.ui.MadhabPromptDialog
import com.salattracker.feature.onboarding.OnboardingEntry
import com.salattracker.feature.qaza.MissedPrayersEntry
import com.salattracker.feature.qaza.QazaEntry
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
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { }

            val locationSyncViewModel: LocationSyncViewModel = hiltViewModel()
            val locationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { granted -> if (granted) locationSyncViewModel.syncDeviceLocation() }

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
                when (onboardingCompleted) {
                    // null = DataStore not read yet — plain background avoids a flash of the wrong screen.
                    null  -> Box(modifier = Modifier.fillMaxSize().background(SalatBackground))
                    false -> OnboardingEntry(
                        onAllowLocation = {
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
    val isTopLevel    = isTabRoute(currentDest?.route)

    val drawerState    = rememberDrawerState(DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    val shellViewModel: MainShellViewModel = hiltViewModel()
    val backupNeedsAttention by shellViewModel.backupNeedsAttention.collectAsState()

    // Scoped here (not inside the NavHost's Calendar composable) so the exact same instance
    // backs both CalendarEntry's screen and the Today/Month controls in TopBar below — two
    // independent hiltViewModel() calls at those different scopes would otherwise create two
    // unrelated ViewModels with their own separate state.
    val calendarViewModel: CalendarViewModel = hiltViewModel()
    val calendarUiState by calendarViewModel.uiState.collectAsState()

    // A notification's deep link, delivered after this scaffold (and its NavHost) already
    // exist — navigate once, then let the caller clear it so a config change or
    // recomposition doesn't navigate again.
    LaunchedEffect(initialRoute) {
        if (initialRoute != null) {
            navController.navigate(initialRoute)
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
                onBulkUpdateClick      = { navController.push(Routes.bulkUpdate()) },
                onPrayerRemindersClick = { navController.push(Routes.PRAYER_REMINDERS) },
                onMosqueModeClick = { navController.push(Routes.MOSQUE_MODE) },
                onBackupClick          = { navController.push(Routes.BACKUP) },
                onDonateClick          = { navController.push(Routes.DONATE) },
                onDevelopersClick      = { navController.push(Routes.DEVELOPERS) },
                onSettingsClick        = { navController.push(Routes.SETTINGS) },
            )
        },
    ) {
        Scaffold(
            containerColor = SalatBackground,
            topBar = {
                TopBar(
                    title                = topBarTitle(currentDest?.route),
                    isTopLevel           = isTopLevel,
                    backupNeedsAttention = backupNeedsAttention,
                    onMenuClick          = { coroutineScope.launch { drawerState.open() } },
                    onBackClick          = { backDispatcher?.onBackPressed() },
                    showAddGoal          = currentDest?.route == BottomNavRoute.Goals.route,
                    onAddGoalClick       = { navController.push(Routes.ADD_GOAL) },
                    showBulkUpdate       = currentDest?.route == BottomNavRoute.Calendar.route,
                    onBulkUpdateClick    = { navController.push(Routes.bulkUpdate()) },
                    calendarView             = calendarUiState.view,
                    onCalendarViewChanged    = calendarViewModel::onViewChanged,
                    calendarAwayFromToday    = calendarUiState.awayFromToday,
                    onCalendarTodayClick     = calendarViewModel::onTodayClick,
                )
            },
            bottomBar = {
                AnimatedVisibility(
                    visible = isTopLevel,
                    enter   = slideInVertically(initialOffsetY = { it }),
                    exit    = slideOutVertically(targetOffsetY = { it }),
                ) {
                    SalatBottomNavBar(
                        routes      = BottomNavRoute.all,
                        currentDest = currentDest,
                        onNavigate  = { route -> navController.navigateToTab(route.route) },
                    )
                }
            },
        ) { innerPadding ->
            NavHost(
                navController    = navController,
                startDestination = BottomNavRoute.Home.route,
                modifier         = Modifier.padding(innerPadding),
            ) {
                composable(BottomNavRoute.Home.route) {
                    HomeEntry(
                        onQazaClick = { navController.navigateToTab(BottomNavRoute.Qaza.route) },
                        onDayClick  = { date ->
                            calendarViewModel.onOpenDay(date)
                            navController.navigateToTab(BottomNavRoute.Calendar.route)
                        },
                    )
                }
                composable(BottomNavRoute.Calendar.route) {
                    CalendarEntry(calendarViewModel)
                }
                composable(BottomNavRoute.Qaza.route) {
                    QazaEntry(
                        onViewAllClick   = { navController.push(Routes.missedPrayers()) },
                        onPrayerClick    = { prayer -> navController.push(Routes.missedPrayers(prayer)) },
                        onOpenDay        = { date ->
                            calendarViewModel.onOpenDay(date)
                            navController.navigateToTab(BottomNavRoute.Calendar.route)
                        },
                        onAddMissedClick = { navController.push(Routes.bulkUpdate(PrayerStatus.QAZA_PENDING)) },
                    )
                }
                composable(BottomNavRoute.Goals.route) {
                    GoalsEntry(
                        onGoalClick    = { goalId -> navController.push(Routes.goalDetails(goalId)) },
                        onAddGoalClick = { navController.push(Routes.ADD_GOAL) },
                    )
                }
                composable(BottomNavRoute.Stats.route) {
                    StatsEntry(onAddGoalClick = { navController.push(Routes.ADD_GOAL) })
                }
                composable(Routes.SETTINGS) {
                    SettingsEntry()
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
                composable(
                    Routes.BULK_UPDATE,
                    arguments = listOf(navArgument("status") { type = NavType.StringType; nullable = true; defaultValue = null }),
                ) {
                    BulkUpdateEntry()
                }
                composable(Routes.ADD_GOAL) {
                    AddGoalEntry(onSaved = { navController.popBackStack() })
                }
                composable(
                    Routes.GOAL_DETAILS,
                    arguments = listOf(navArgument("goalId") { type = NavType.LongType }),
                ) {
                    GoalDetailsEntry(
                        onEdit    = { goalId -> navController.push(Routes.editGoal(goalId)) },
                        onDeleted = { navController.popBackStack() },
                    )
                }
                composable(
                    Routes.EDIT_GOAL,
                    arguments = listOf(navArgument("goalId") { type = NavType.LongType }),
                ) {
                    EditGoalEntry(onSaved = { navController.popBackStack() })
                }
                composable(
                    Routes.MISSED_PRAYERS,
                    arguments = listOf(navArgument("prayer") { type = NavType.StringType; nullable = true; defaultValue = null }),
                ) {
                    MissedPrayersEntry(
                        onBackToDashboard = { navController.popBackStack() },
                        onOpenDay         = { date ->
                            calendarViewModel.onOpenDay(date)
                            navController.navigateToTab(BottomNavRoute.Calendar.route)
                        },
                    )
                }
            }
        }
    }
}

// ── Top app bar ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    title: String,
    isTopLevel: Boolean,
    backupNeedsAttention: Boolean,
    onMenuClick: () -> Unit,
    onBackClick: () -> Unit,
    showAddGoal: Boolean,
    onAddGoalClick: () -> Unit,
    showBulkUpdate: Boolean,
    onBulkUpdateClick: () -> Unit,
    calendarView: CalendarViewType,
    onCalendarViewChanged: (CalendarViewType) -> Unit,
    calendarAwayFromToday: Boolean,
    onCalendarTodayClick: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(
                text       = title,
                color      = SalatPrimary,
                fontSize   = 20.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = MontserratFamily,
            )
        },
        navigationIcon = {
            // The five tabs show the menu icon (with an attention dot when backup needs it);
            // every pushed screen shows a back arrow instead, routed through the same
            // OnBackPressedDispatcher as the system back gesture so both always agree —
            // Settings' own BackHandler gets first chance to intercept either one.
            if (isTopLevel) {
                IconButton(onClick = onMenuClick) {
                    BadgedBox(badge = { if (backupNeedsAttention) Badge(containerColor = SalatError) }) {
                        Icon(painterResource(R.drawable.ic_menu), stringResource(R.string.cd_menu), tint = SalatPrimary)
                    }
                }
            } else {
                IconButton(onClick = onBackClick) {
                    Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.cd_back), tint = SalatPrimary)
                }
            }
        },
        actions = {
            if (showAddGoal) {
                IconButton(onClick = onAddGoalClick) {
                    Icon(painterResource(R.drawable.ic_add), stringResource(R.string.action_add_goal), tint = SalatPrimary)
                }
            }
            if (showBulkUpdate) {
                if (calendarAwayFromToday) {
                    IconButton(onClick = onCalendarTodayClick) {
                        Icon(painterResource(R.drawable.ic_calendar_today), stringResource(R.string.calendar_today), tint = SalatPrimary)
                    }
                }
                CalendarViewMenu(currentView = calendarView, onViewChanged = onCalendarViewChanged)
                IconButton(onClick = onBulkUpdateClick) {
                    Icon(painterResource(R.drawable.ic_edit_square), stringResource(R.string.action_bulk_update), tint = SalatPrimary)
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = SalatBackground),
    )
}

/** All three calendar views are enabled; grows automatically if CalendarViewType ever does. */
private val ENABLED_CALENDAR_VIEWS = CalendarViewType.values().toList()

/** Compact "Month ▾" dropdown chip for the top bar's actions row — switches between
 * Calendar's Month/Week/Year views. Lives here (not in feature.calendar) since this is now
 * the only place it's used. */
@Composable
private fun CalendarViewMenu(
    currentView: CalendarViewType,
    onViewChanged: (CalendarViewType) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Surface(
            onClick = { expanded = true },
            shape   = RoundedCornerShape(50),
            color   = SalatSurfaceContainerLow,
            border  = BorderStroke(1.dp, SalatOutlineVariant),
        ) {
            Row(
                modifier              = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(currentView.label(), color = SalatOnSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = InterFamily)
                Icon(
                    painter            = painterResource(R.drawable.ic_expand_more),
                    contentDescription = stringResource(R.string.cd_change_view),
                    tint               = SalatOnSurfaceVariant,
                    modifier           = Modifier.size(14.dp),
                )
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ENABLED_CALENDAR_VIEWS.forEach { view ->
                DropdownMenuItem(
                    text    = { Text(view.label()) },
                    onClick = { onViewChanged(view); expanded = false },
                )
            }
        }
    }
}

// ── Bottom navigation bar ─────────────────────────────────────────────────────

@Composable
private fun SalatBottomNavBar(
    routes: List<BottomNavRoute>,
    currentDest: androidx.navigation.NavDestination?,
    onNavigate: (BottomNavRoute) -> Unit,
) {
    NavigationBar(
        containerColor = SalatSurface,
        tonalElevation = 0.dp,
    ) {
        routes.forEach { route ->
            val isSelected = currentDest?.hierarchy?.any { it.route == route.route } == true
            NavigationBarItem(
                selected = isSelected,
                onClick  = { onNavigate(route) },
                label = {
                    Text(
                        stringResource(route.labelRes),
                        fontSize   = 11.sp,
                        fontFamily = InterFamily,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    )
                },
                icon = {
                    Icon(
                        painterResource(route.iconRes),
                        stringResource(route.labelRes),
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor   = SalatOnSecondaryContainer,
                    selectedTextColor   = SalatOnSecondaryContainer,
                    indicatorColor      = SalatSecondaryContainer,
                    unselectedIconColor = SalatOnSurfaceVariant,
                    unselectedTextColor = SalatOnSurfaceVariant,
                ),
            )
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
        BottomNavRoute.Stats.route    -> R.string.topbar_stats
        Routes.SETTINGS               -> R.string.topbar_settings
        Routes.BACKUP                 -> R.string.topbar_backup
        Routes.BULK_UPDATE            -> R.string.topbar_bulk_update
        Routes.PRAYER_REMINDERS       -> R.string.prayer_reminders
        Routes.MOSQUE_MODE            -> R.string.topbar_mosque_mode
        Routes.DONATE                 -> R.string.topbar_donate
        Routes.DEVELOPERS             -> R.string.topbar_developers
        Routes.ADD_GOAL               -> R.string.topbar_new_goal
        Routes.MISSED_PRAYERS         -> R.string.topbar_missed_prayers
        else -> when {
            route?.startsWith("edit_goal") == true    -> R.string.topbar_edit_goal
            route?.startsWith("goal_details") == true -> R.string.topbar_goal_details
            else -> R.string.topbar_default
        }
    }
)
