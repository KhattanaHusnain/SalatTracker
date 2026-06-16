package com.salattracker.feature.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import com.salattracker.core.theme.SalatTrackerTheme
import com.salattracker.feature.main.MainActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginActivity : ComponentActivity() {

    private val viewModel: LoginViewModel by viewModels()

    // ── Google Sign-In launcher ───────────────────────────────────────────────
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.handleGoogleSignInResult(result.data)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            SalatTrackerTheme {
                val uiState by viewModel.uiState.collectAsState()

                // Navigate to MainActivity once sign-in succeeds
                LaunchedEffect(uiState.navigateToMain) {
                    if (uiState.navigateToMain) {
                        viewModel.navigationHandled()
                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                        overridePendingTransition(
                            android.R.anim.fade_in,
                            android.R.anim.fade_out,
                        )
                        finish()
                    }
                }

                LoginScreen(
                    uiState = uiState,
                    onGoogleSignInClick = {
                        googleSignInLauncher.launch(viewModel.getGoogleSignInIntent())
                    },
                    onEmailSignInClick = {
                        // TODO: navigate to email sign-in screen when implemented
                    },
                    onErrorDismiss = viewModel::clearError,
                )
            }
        }
    }
}