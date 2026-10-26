package com.salattracker.feature.home

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/** Small Settings-screen adapter around the Home presentation preference store. */
@HiltViewModel
class HomeAppearanceSettingsViewModel @Inject constructor(
    private val store: HomeAppearanceStore,
) : ViewModel() {
    val appearance: StateFlow<HomeAppearance> = store.state

    fun onAppearanceChanged(value: HomeAppearance) = store.save(value)
}
