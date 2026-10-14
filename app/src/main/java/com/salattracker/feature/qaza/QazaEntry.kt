package com.salattracker.feature.qaza

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.salattracker.domain.model.PrayerType
import java.time.LocalDate

/**
 * Nav entry-point for the Qaza tab.
 * Wire into MainActivity NavHost:
 *   composable(BottomNavRoute.Qaza.route) {
 *       QazaEntry(
 *           onViewAllClick   = { navController.push(Routes.missedPrayers()) },
 *           onPrayerClick    = { prayer -> navController.push(Routes.missedPrayers(prayer)) },
 *           onOpenDay        = { date -> calendarViewModel.onOpenDay(date); navController.navigateToTab(BottomNavRoute.Calendar.route) },
 *           onAddMissedClick = { navController.push(Routes.bulkUpdate(PrayerStatus.QAZA_PENDING)) },
 *       )
 *   }
 */
@Composable
fun QazaEntry(
    onViewAllClick: () -> Unit,
    onPrayerClick: (PrayerType) -> Unit,
    onOpenDay: (LocalDate) -> Unit,
    onAddMissedClick: () -> Unit,
    viewModel: QazaViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    // Re-derives "today" on tab entry, same as Goals — avoids an always-running ticker just to
    // catch a midnight rollover.
    LaunchedEffect(Unit) { viewModel.refresh() }

    QazaScreen(
        uiState               = uiState,
        onMakeUp              = viewModel::onMakeUp,
        onMakeUpDay           = viewModel::onMakeUpDay,
        onUndo                = viewModel::onUndo,
        onUndoDismiss         = viewModel::onUndoDismiss,
        onRecentUndo          = viewModel::onRecentUndo,
        onRecentExpandToggle  = viewModel::onRecentExpandToggle,
        onLogClick            = viewModel::onLogClick,
        onLogCountChanged     = viewModel::onLogCountChanged,
        onLogConfirm          = viewModel::onLogConfirm,
        onLogDismiss          = viewModel::onLogDismiss,
        onViewAllClick        = onViewAllClick,
        onPrayerClick         = onPrayerClick,
        onOpenDay             = onOpenDay,
        onAddMissedClick      = onAddMissedClick,
        onErrorDismiss        = viewModel::clearError,
    )
}
