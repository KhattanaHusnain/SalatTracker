package com.salattracker.feature.main

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.salattracker.R
import com.salattracker.core.theme.*
import com.salattracker.feature.home.HomeScreen
import com.salattracker.feature.home.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            SalatTrackerTheme {
                MainScaffold()
            }
        }
    }
}

// ── Main scaffold with bottom nav ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold() {
    val navController = rememberNavController()
    val navBackStack  by navController.currentBackStackEntryAsState()
    val currentDest   = navBackStack?.destination

    Scaffold(
        containerColor = SalatBackground,
        topBar = {
            TopBar(
                title = topBarTitle(currentDest?.route),
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
                    onBulkUpdateClick = {
                        navController.navigate("bulk_update")
                    },
                )
            }
            composable(BottomNavRoute.Calendar.route) {
                // CalendarScreen(hiltViewModel()) — implement next
                com.salattracker.feature.calendar.CalendarEntry()
            }
            composable(BottomNavRoute.Qaza.route) {
                com.salattracker.feature.qaza.QazaEntry()
            }
            composable(BottomNavRoute.Stats.route) {
                PlaceholderScreen("Analytics")
            }
            composable(BottomNavRoute.Settings.route) {
                PlaceholderScreen("Settings")
            }
            composable("bulk_update") {
                PlaceholderScreen("Bulk Update")
            }
        }
    }
}

// ── Top app bar ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(title: String) {
    TopAppBar(
        title = {
            Text(
                text = title,
                color = SalatPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = MontserratFamily,
            )
        },
        navigationIcon = {
            IconButton(onClick = { /* open drawer */ }) {
                Icon(
                    painter = painterResource(R.drawable.ic_menu),
                    contentDescription = "Menu",
                    tint = SalatPrimary,
                )
            }
        },
        actions = {
            IconButton(onClick = { /* open calendar picker */ }) {
                Icon(
                    painter = painterResource(R.drawable.ic_calendar_today),
                    contentDescription = "Pick date",
                    tint = SalatPrimary,
                )
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
        routes.forEachIndexed { index, route ->
            Log.d("BOTTOM_NAV", "[$index] = $route")
        }
//        routes.forEach { route ->
//
//
//            val isSelected = currentDest?.hierarchy?.any { it.route == route.route } == true
//
//            NavigationBarItem(
//                selected = isSelected,
//                onClick  = { onNavigate(route) },
//                label    = {
//                    Text(
//                        text = route.label,
//                        fontSize = 11.sp,
//                        fontFamily = InterFamily,
//                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
//                    )
//                },
//                icon = {
//                    Icon(
//                        painter = painterResource(
//                            if (isSelected) route.iconFilledRes else route.iconOutlinedRes
//                        ),
//                        contentDescription = route.label,
//                    )
//                },
//                colors = NavigationBarItemDefaults.colors(
//                    selectedIconColor   = SalatOnSecondaryContainer,
//                    selectedTextColor   = SalatOnSecondaryContainer,
//                    indicatorColor      = SalatSecondaryContainer,
//                    unselectedIconColor = SalatOnSurfaceVariant,
//                    unselectedTextColor = SalatOnSurfaceVariant,
//                ),
//            )
//        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun topBarTitle(route: String?) = when (route) {
    BottomNavRoute.Home.route     -> "Today, 24 Rajab 1445"
    BottomNavRoute.Calendar.route -> "Calendar"
    BottomNavRoute.Qaza.route     -> "Qaza Balance"
    BottomNavRoute.Stats.route    -> "Analytics"
    BottomNavRoute.Settings.route -> "Settings"
    else                          -> "Salat Tracker"
}

@Composable
private fun PlaceholderScreen(name: String) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center,
    ) {
        Text(text = "$name — Coming soon", color = SalatOnSurfaceVariant, fontFamily = InterFamily)
    }
}