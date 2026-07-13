package com.salattracker.feature.qaza

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Nav entry-point for the Qaza tab.
 * Wire into MainActivity NavHost:
 *   composable(BottomNavRoute.Qaza.route) { QazaEntry(onViewAllClick = { navController.navigate("missed_prayers") }) }
 */
@Composable
fun QazaEntry(onViewAllClick: () -> Unit, viewModel: QazaViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    QazaScreen(
        uiState         = uiState,
        onMarkCompleted = viewModel::markCompleted,
        onViewAllClick  = onViewAllClick,
        onErrorDismiss  = viewModel::clearError,
    )
}
