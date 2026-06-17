package com.salattracker.feature.auth

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.salattracker.data.remote.dto.UserProfileDto
import com.salattracker.data.remote.firestore.FirestoreDataSource
import com.salattracker.data.repository.PrayerRepositoryImpl
import com.salattracker.data.sync.SyncOrchestrator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class LoginUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val navigateToMain: Boolean = false,
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val googleSignInClient: GoogleSignInClient,
    private val firestoreDs: FirestoreDataSource,
    private val prayerRepository: PrayerRepositoryImpl,
    private val syncOrchestrator: SyncOrchestrator,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun getGoogleSignInIntent(): Intent = googleSignInClient.signInIntent

    fun handleGoogleSignInResult(data: Intent?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val account    = GoogleSignIn.getSignedInAccountFromIntent(data)
                    .getResult(ApiException::class.java)
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                val result     = auth.signInWithCredential(credential).await()
                val user       = result.user ?: throw Exception("Sign-in failed")
                val email      = user.email ?: throw Exception("No email on account")

                // Upsert user profile in Firestore (keyed by email)
                firestoreDs.upsertProfile(
                    email = email,
                    dto   = UserProfileDto(
                        uid         = user.uid,
                        email       = email,
                        displayName = user.displayName ?: "",
                    ),
                )

                // Pull any existing data from Firestore into Room
                prayerRepository.pullFromFirestoreOnLogin()

                // Start periodic background sync
                syncOrchestrator.schedulePeriodicSync()

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

    fun clearError()        = _uiState.update { it.copy(error = null) }
    fun navigationHandled() = _uiState.update { it.copy(navigateToMain = false) }
}