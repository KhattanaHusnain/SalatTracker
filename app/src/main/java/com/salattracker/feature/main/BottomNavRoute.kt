package com.salattracker.feature.main

import com.salattracker.R

sealed class BottomNavRoute(
    val route: String,
    val label: String,
    val iconOutlinedRes: Int,
    val iconFilledRes: Int,
) {
    object Home     : BottomNavRoute("home",     "Home",     R.drawable.ic_nav_home,     R.drawable.ic_nav_home_filled)
    object Calendar : BottomNavRoute("calendar", "Calendar", R.drawable.ic_nav_calendar, R.drawable.ic_nav_calendar_filled)
    object Qaza     : BottomNavRoute("qaza",     "Qaza",     R.drawable.ic_nav_qaza,     R.drawable.ic_nav_qaza_filled)
    object Stats    : BottomNavRoute("stats",    "Stats",    R.drawable.ic_nav_stats,    R.drawable.ic_nav_stats_filled)
    object Settings : BottomNavRoute("settings", "Settings", R.drawable.ic_nav_settings, R.drawable.ic_nav_settings_filled)

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