package com.salattracker.feature.splash

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import com.salattracker.core.theme.SalatTrackerTheme
import com.salattracker.feature.auth.LoginActivity
import com.salattracker.feature.main.MainActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SplashActivity : ComponentActivity() {

    private val viewModel: SplashViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Draw edge-to-edge so the pattern fills the status bar area
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            SalatTrackerTheme {
                // Render the splash UI unconditionally
                SplashScreen()

                // Observe navigation destination
                val destination by viewModel.destination.collectAsState()

                LaunchedEffect(destination) {
                    when (destination) {
                        is SplashDestination.Main -> navigateTo(MainActivity::class.java)
                        is SplashDestination.Login -> navigateTo(LoginActivity::class.java)
                        SplashDestination.Idle -> Unit // still loading, do nothing
                    }
                }
            }
        }
    }

    private fun <T : ComponentActivity> navigateTo(target: Class<T>) {
        startActivity(Intent(this, target))
        // Slide the splash out gracefully
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }
}