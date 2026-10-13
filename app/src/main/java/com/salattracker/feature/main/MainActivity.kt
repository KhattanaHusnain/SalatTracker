package com.salattracker.feature.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.salattracker.R
import com.salattracker.core.theme.*
import com.salattracker.feature.backup.BackupEntry
import com.salattracker.feature.bulkupdate.BulkUpdateEntry
import com.salattracker.feature.calendar.CalendarEntry
import com.salattracker.feature.goals.AddGoalEntry
import com.salattracker.feature.goals.EditGoalEntry
import com.salattracker.feature.goals.GoalsEntry
import com.salattracker.feature.home.HomeScreen
import com.salattracker.feature.home.HomeViewModel
import com.salattracker.feature.onboarding.OnboardingEntry
import com.salattracker.feature.qaza.MissedPrayersEntry
import com.salattracker.feature.qaza.QazaEntry
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
     * running instance — see android:launchMode="singleTask" on this activity in the manifest). */
    private var pendingRoute by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        pendingRoute = intent?.getStringExtra(EXTRA_OPEN_ROUTE)

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

            SalatTrackerTheme {
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
                    true  -> MainScaffold(initialRoute = pendingRoute, onRouteConsumed = { pendingRoute = null })
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingRoute = intent.getStringExtra(EXTRA_OPEN_ROUTE)
    }

    companion object {
        /** Intent extra a notification sets to deep-link straight to a route once the app is up. */
        const val EXTRA_OPEN_ROUTE = "open_route"
        const val ROUTE_BACKUP = "backup"
    }
}

// ── Main scaffold ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold(initialRoute: String? = null, onRouteConsumed: () -> Unit = {}) {
    val navController = rememberNavController()
    val navBackStack  by navController.currentBackStackEntryAsState()
    val currentDest   = navBackStack?.destination

    // A backup notification's deep link, delivered after this scaffold (and its NavHost)
    // already exist — navigate once, then let the caller clear it so a config change or
    // recomposition doesn't navigate again.
    LaunchedEffect(initialRoute) {
        if (initialRoute != null) {
            navController.navigate(initialRoute)
            onRouteConsumed()
        }
    }

    Scaffold(
        containerColor = SalatBackground,
        topBar = {
            TopBar(
                title             = topBarTitle(currentDest?.route),
                showAddGoal       = currentDest?.route == "goals",
                onAddGoalClick    = { navController.navigate("add_goal") },
                onBulkUpdateClick = { navController.navigate("bulk_update") },
            )
        },
        bottomBar = {
            SalatBottomNavBar(
                routes      = BottomNavRoute.all,
                currentDest = currentDest,
                onNavigate  = { route ->
                    navController.navigate(route.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState    = true
                    }
                },
            )
        },
    ) { innerPadding ->
        NavHost(
            navController    = navController,
            startDestination = BottomNavRoute.Home.route,
            modifier         = Modifier.padding(innerPadding),
        ) {
            composable(BottomNavRoute.Home.route) {
                val vm: HomeViewModel = hiltViewModel()
                val state by vm.uiState.collectAsState()
                HomeScreen(
                    uiState           = state,
                    onPrayerCardClick = vm::onPrayerCardClick,
                    onSheetDismiss    = vm::onSheetDismiss,
                    onStatusSave      = vm::onStatusSave,
                    onGoalsClick      = { navController.navigate("goals") },
                )
            }
            composable(BottomNavRoute.Calendar.route) {
                CalendarEntry()
            }
            composable(BottomNavRoute.Qaza.route) {
                QazaEntry(onViewAllClick = { navController.navigate("missed_prayers") })
            }
            composable(BottomNavRoute.Stats.route) {
                StatsEntry()
            }
            composable(BottomNavRoute.Settings.route) {
                SettingsEntry(onBackupClick = { navController.navigate("backup") })
            }
            composable("backup") {
                BackupEntry()
            }
            composable("bulk_update") {
                BulkUpdateEntry()
            }
            composable("goals") {
                GoalsEntry(onEditGoal = { goalId -> navController.navigate("edit_goal/$goalId") })
            }
            composable("add_goal") {
                AddGoalEntry(onSaved = { navController.popBackStack() })
            }
            composable(
                "edit_goal/{goalId}",
                arguments = listOf(navArgument("goalId") { type = NavType.LongType }),
            ) {
                EditGoalEntry(
                    onSaved   = { navController.popBackStack() },
                    onDeleted = { navController.popBackStack() },
                )
            }
            composable("missed_prayers") {
                MissedPrayersEntry(onBackToDashboard = { navController.popBackStack() })
            }
        }
    }
}

// ── Top app bar ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    title: String,
    showAddGoal: Boolean,
    onAddGoalClick: () -> Unit,
    onBulkUpdateClick: () -> Unit,
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
            IconButton(onClick = {}) {
                Icon(painterResource(R.drawable.ic_menu), stringResource(R.string.cd_menu), tint = SalatPrimary)
            }
        },
        actions = {
            if (showAddGoal) {
                IconButton(onClick = onAddGoalClick) {
                    Icon(painterResource(R.drawable.ic_add), stringResource(R.string.action_add_goal), tint = SalatPrimary)
                }
            }
            IconButton(onClick = onBulkUpdateClick) {
                Icon(painterResource(R.drawable.ic_edit_square), stringResource(R.string.action_bulk_update), tint = SalatPrimary)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = SalatBackground),
    )
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
        BottomNavRoute.Stats.route    -> R.string.topbar_stats
        BottomNavRoute.Settings.route -> R.string.topbar_settings
        "backup"                      -> R.string.topbar_backup
        "bulk_update"                 -> R.string.topbar_bulk_update
        "goals"                       -> R.string.topbar_goals
        "add_goal"                    -> R.string.topbar_new_goal
        "missed_prayers"              -> R.string.topbar_missed_prayers
        else -> if (route?.startsWith("edit_goal") == true) R.string.topbar_edit_goal else R.string.topbar_default
    }
)
