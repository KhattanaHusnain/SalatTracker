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
    object Goals    : BottomNavRoute("goals",    R.string.nav_goals,    R.drawable.ic_nav_goals)
    object Stats    : BottomNavRoute("stats",    R.string.nav_stats,    R.drawable.ic_nav_stats)

    companion object {
        val all: List<BottomNavRoute> by lazy {
            listOf(Home, Calendar, Qaza, Goals, Stats)
        }
    }
}

/** Non-tab destinations, reached by pushing onto the back stack rather than switching tabs. */
object Routes {
    const val SETTINGS       = "settings"
    const val BACKUP         = "backup"
    const val BULK_UPDATE    = "bulk_update"
    const val ADD_GOAL       = "add_goal"
    const val EDIT_GOAL      = "edit_goal/{goalId}"
    const val MISSED_PRAYERS = "missed_prayers"

    fun editGoal(goalId: Long) = "edit_goal/$goalId"
}

/** True for a tab route — including null, the graph's transient state before the first
 * destination settles, so the shell never flashes a back arrow on launch. */
fun isTabRoute(route: String?): Boolean = route == null || BottomNavRoute.all.any { it.route == route }
