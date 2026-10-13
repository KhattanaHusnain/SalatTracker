package com.salattracker.data.backup

import android.accounts.Account
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import com.google.android.gms.auth.api.identity.AuthorizationClient
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.ClearTokenRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

private const val DRIVE_APPDATA_SCOPE = "https://www.googleapis.com/auth/drive.appdata"
private const val GOOGLE_ACCOUNT_TYPE = "com.google"

/**
 * Thin wrapper over Play Services' AuthorizationClient for the single drive.appdata
 * scope. Behind an interface (rather than injected directly like LocationProvider)
 * so BackupRepositoryImpl's guard/upload logic can be unit tested with a fake that
 * never touches a real IntentSender.
 */
interface DriveAuthorizer {
    sealed interface Outcome {
        data class Token(val value: String) : Outcome
        data class ConsentRequired(val intentSender: IntentSender) : Outcome
        data class Failed(val message: String) : Outcome
    }

    /**
     * [accountEmail] pins the request to a known account (silent path, e.g. the daily
     * worker). [forceAccountChooser] overrides it to force Google's account picker —
     * use this for the user-initiated "Connect" / "Switch account" actions.
     */
    suspend fun authorize(accountEmail: String?, forceAccountChooser: Boolean = false): Outcome

    /** Call from the activity-result callback after launching [Outcome.ConsentRequired]'s intentSender. */
    fun completeConsent(data: Intent?): Outcome

    /** Drops a token that the server rejected (e.g. 401), so the next authorize() fetches a fresh one. */
    suspend fun invalidate(token: String)
}

/**
 * The first authorize() call for a scope shows Google's account chooser + consent
 * sheet via a resolvable Intent; once granted, later calls — including from a
 * background worker — return a fresh access token with no UI, as long as the grant
 * still stands and (when pinned) the same account is used.
 */
@Singleton
class PlayServicesDriveAuthorizer @Inject constructor(
    @ApplicationContext private val context: Context,
) : DriveAuthorizer {

    private val client: AuthorizationClient by lazy { Identity.getAuthorizationClient(context) }
    private val scope = Scope(DRIVE_APPDATA_SCOPE)

    override suspend fun authorize(accountEmail: String?, forceAccountChooser: Boolean): DriveAuthorizer.Outcome {
        val builder = AuthorizationRequest.builder().setRequestedScopes(listOf(scope))
        when {
            forceAccountChooser -> builder.setPrompt(AuthorizationRequest.Prompt.SELECT_ACCOUNT)
            accountEmail != null -> builder.setAccount(Account(accountEmail, GOOGLE_ACCOUNT_TYPE))
        }

        val result = client.authorize(builder.build()).awaitResult()
        return result.fold(
            onSuccess = { it.toOutcome() },
            onFailure = { e -> DriveAuthorizer.Outcome.Failed(e.message ?: "Authorization failed") },
        )
    }

    override fun completeConsent(data: Intent?): DriveAuthorizer.Outcome =
        try {
            client.getAuthorizationResultFromIntent(data).toOutcome()
        } catch (e: ApiException) {
            DriveAuthorizer.Outcome.Failed(e.message ?: "Authorization was not completed")
        }

    override suspend fun invalidate(token: String) {
        runCatching {
            client.clearToken(ClearTokenRequest.builder().setToken(token).build()).awaitResult()
        }
    }

    private fun AuthorizationResult.toOutcome(): DriveAuthorizer.Outcome {
        if (hasResolution()) {
            val intentSender = pendingIntent?.intentSender
                ?: return DriveAuthorizer.Outcome.Failed("Authorization needs the user but gave no way to ask")
            return DriveAuthorizer.Outcome.ConsentRequired(intentSender)
        }
        val token = accessToken ?: return DriveAuthorizer.Outcome.Failed("Google did not return an access token")
        return DriveAuthorizer.Outcome.Token(token)
    }

    private suspend fun <T> Task<T>.awaitResult(): Result<T> =
        suspendCancellableCoroutine { cont ->
            addOnSuccessListener { cont.resume(Result.success(it)) }
            addOnFailureListener { e -> cont.resume(Result.failure(e)) }
        }
}
