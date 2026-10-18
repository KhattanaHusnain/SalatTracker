package com.salattracker.feature.main

import com.salattracker.R
import com.salattracker.domain.model.PrayerStatus
import com.salattracker.domain.model.PrayerType

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
    const val SETTINGS         = "settings"
    const val BACKUP           = "backup"
    const val BULK_UPDATE      = "bulk_update?status={status}"
    const val PRAYER_REMINDERS = "prayer_reminders"
    const val MOSQUE_MODE      = "mosque_mode"
    const val ADD_GOAL         = "add_goal"
    const val EDIT_GOAL        = "edit_goal/{goalId}"
    const val GOAL_DETAILS     = "goal_details/{goalId}"
    const val MISSED_PRAYERS   = "missed_prayers?prayer={prayer}"

    fun editGoal(goalId: Long) = "edit_goal/$goalId"
    fun goalDetails(goalId: Long) = "goal_details/$goalId"

    /** Opens the full Qaza backlog, optionally pre-filtered to one prayer (e.g. tapping a prayer
     * tile on the Qaza dashboard). Omit [prayer] for the unfiltered list ("View all"). */
    fun missedPrayers(prayer: PrayerType? = null) = "missed_prayers?prayer=${prayer?.name ?: ""}"

    /** Opens Bulk Update with [status] preselected — used by the Qaza tab's "Add missed prayers"
     * shortcut, which preselects [PrayerStatus.QAZA_PENDING]. */
    fun bulkUpdate(status: PrayerStatus? = null) = "bulk_update?status=${status?.name ?: ""}"
}

/** True for a tab route — including null, the graph's transient state before the first
 * destination settles, so the shell never flashes a back arrow on launch. */
fun isTabRoute(route: String?): Boolean = route == null || BottomNavRoute.all.any { it.route == route }
