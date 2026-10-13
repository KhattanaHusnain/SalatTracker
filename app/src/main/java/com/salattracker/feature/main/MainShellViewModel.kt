package com.salattracker.feature.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salattracker.domain.model.BackupAttention
import com.salattracker.domain.repository.BackupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Shell-level state that outlives any single tab — currently just the drawer's
 * attention dot for Backup & Restore. */
@HiltViewModel
class MainShellViewModel @Inject constructor(
    backupRepository: BackupRepository,
) : ViewModel() {

    val backupNeedsAttention: StateFlow<Boolean> = backupRepository.observeStatus()
        .map { it.attention != BackupAttention.NONE }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
}
