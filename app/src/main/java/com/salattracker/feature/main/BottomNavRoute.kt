package com.salattracker.feature.main

import com.salattracker.R

// Tabler Icons (MIT, https://tabler.io/icons) has no filled/outlined pairs, so each tab
// now has a single icon; the active tab is shown by the pill indicator + colour instead
// (same convention as the CMS mobile apps' RoleBottomNavScaffold).
sealed class BottomNavRoute(
    val route: String,
    val labelRes: Int,
    val iconRes: Int,
) {
    object Home     : BottomNavRoute("home",     R.string.nav_home,     R.drawable.ic_nav_home)
    object Calendar : BottomNavRoute("calendar", R.string.nav_calendar, R.drawable.ic_nav_calendar)
    object Qaza     : BottomNavRoute("qaza",     R.string.nav_qaza,     R.drawable.ic_nav_qaza)
    object Stats    : BottomNavRoute("stats",    R.string.nav_stats,    R.drawable.ic_nav_stats)
    object Settings : BottomNavRoute("settings", R.string.nav_settings, R.drawable.ic_nav_settings)

    companion object {
        val all: List<BottomNavRoute> by lazy {
            listOf(
                Home,
                Calendar,
                Qaza,
                Stats,
                Settings
            )
        }
    }
}