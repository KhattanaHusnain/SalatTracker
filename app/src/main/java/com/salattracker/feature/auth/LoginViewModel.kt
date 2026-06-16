package com.salattracker.feature.auth

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

// ── UI state ─────────────────────────────────────────────────────────────────

data class LoginUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val navigateToMain: Boolean = false,
)

// ── ViewModel ────────────────────────────────────────────────────────────────

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val googleSignInClient: GoogleSignInClient,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    /** Returns the Intent the caller should launch for the Google sign-in chooser. */
    fun getGoogleSignInIntent(): Intent = googleSignInClient.signInIntent

    /** Called by the Activity after receiving the result from the Google sign-in chooser. */
    fun handleGoogleSignInResult(data: Intent?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val account = GoogleSignIn.getSignedInAccountFromIntent(data)
                    .getResult(ApiException::class.java)
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                auth.signInWithCredential(credential).await()
                _uiState.update { it.copy(isLoading = false, navigateToMain = true) }
            } catch (e: ApiException) {
                _uiState.update {
                    it.copy(isLoading = false, error = "Google sign-in failed (${e.statusCode})")
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.localizedMessage ?: "Sign-in failed")
                }
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }

    fun navigationHandled() = _uiState.update { it.copy(navigateToMain = false) }
}