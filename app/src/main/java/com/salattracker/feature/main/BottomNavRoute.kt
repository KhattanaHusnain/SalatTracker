package com.salattracker.feature.main

import com.salattracker.R

sealed class BottomNavRoute(
    val route: String,
    val labelRes: Int,
    val iconOutlinedRes: Int,
    val iconFilledRes: Int,
) {
    object Home     : BottomNavRoute("home",     R.string.nav_home,     R.drawable.ic_nav_home,     R.drawable.ic_nav_home_filled)
    object Calendar : BottomNavRoute("calendar", R.string.nav_calendar, R.drawable.ic_nav_calendar, R.drawable.ic_nav_calendar_filled)
    object Qaza     : BottomNavRoute("qaza",     R.string.nav_qaza,     R.drawable.ic_nav_qaza,     R.drawable.ic_nav_qaza_filled)
    object Stats    : BottomNavRoute("stats",    R.string.nav_stats,    R.drawable.ic_nav_stats,    R.drawable.ic_nav_stats_filled)
    object Settings : BottomNavRoute("settings", R.string.nav_settings, R.drawable.ic_nav_settings, R.drawable.ic_nav_settings_filled)

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