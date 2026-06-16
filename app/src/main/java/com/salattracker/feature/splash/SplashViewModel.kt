package com.salattracker.feature.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SplashDestination {
    object Idle : SplashDestination()
    object Main : SplashDestination()
    object Login : SplashDestination()
}

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _destination = MutableStateFlow<SplashDestination>(SplashDestination.Idle)
    val destination: StateFlow<SplashDestination> = _destination.asStateFlow()

    // Minimum splash display duration (ms) so the animation has time to play
    private val SPLASH_DURATION_MS = 2_500L

    init {
        checkAuthState()
    }

    private fun checkAuthState() {
        viewModelScope.launch {
            // Let the splash animation play for its minimum duration
            delay(SPLASH_DURATION_MS)
            _destination.value = if (auth.currentUser != null) {
                SplashDestination.Main
            } else {
                SplashDestination.Login
            }
        }
    }
}