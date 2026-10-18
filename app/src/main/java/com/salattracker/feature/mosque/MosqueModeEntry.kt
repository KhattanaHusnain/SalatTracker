package com.salattracker.feature.mosque

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.salattracker.domain.model.LocationAccess

/**
 * Nav entry-point for Mosque Mode. Owns the permission flow (foreground location, then Play's
 * background-location disclosure, then "allow all the time"), the same split SettingsEntry uses.
 */
@Composable
fun MosqueModeEntry(viewModel: MosqueModeViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showDisclosure by remember { mutableStateOf(false) }
    var needsSettings by remember { mutableStateOf(false) }

    // Re-read permissions whenever the user returns, e.g. from the system settings page.
    LifecycleResumeEffect(Unit) {
        viewModel.onAccessChanged(context.locationAccess())
        onPauseOrDispose {}
    }

    fun afterRequest(wantedEnable: Boolean) {
        val access = context.locationAccess()
        viewModel.onAccessChanged(access)
        if (access == LocationAccess.FULL) {
            needsSettings = false
            if (wantedEnable) viewModel.onEnabledChanged(true)
        } else {
            needsSettings = true
        }
    }

    var enableAfterGrant by remember { mutableStateOf(false) }

    val backgroundLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        afterRequest(enableAfterGrant)
    }
    val foregroundLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        val access = context.locationAccess()
        viewModel.onAccessChanged(access)
        when (access) {
            LocationAccess.FULL -> afterRequest(enableAfterGrant)
            LocationAccess.FOREGROUND_ONLY -> showDisclosure = true
            else -> needsSettings = true
        }
    }

    // Add / Find nearby only need the foreground permission, and must not trigger the
    // background-location disclosure that enabling the feature does.
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val actionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        viewModel.onAccessChanged(context.locationAccess())
        if (context.hasFineLocation()) pendingAction?.invoke()
        pendingAction = null
    }
    fun withLocation(action: () -> Unit) {
        if (context.hasFineLocation()) {
            action()
        } else {
            pendingAction = action
            actionLauncher.launch(FOREGROUND_PERMISSIONS)
        }
    }

    fun requestAccess(thenEnable: Boolean) {
        enableAfterGrant = thenEnable
        when (context.locationAccess()) {
            LocationAccess.FULL -> if (thenEnable) viewModel.onEnabledChanged(true)
            LocationAccess.FOREGROUND_ONLY -> showDisclosure = true
            else -> foregroundLauncher.launch(FOREGROUND_PERMISSIONS)
        }
    }

    MosqueModeScreen(
        uiState = uiState,
        accessActionIsSettings = needsSettings,
        showDisclosure = showDisclosure,
        onEnabledChange = { on -> if (on) requestAccess(thenEnable = true) else viewModel.onEnabledChanged(false) },
        onAccessAction = {
            if (needsSettings) context.openAppSettings() else requestAccess(thenEnable = false)
        },
        onTurnOnLocation = { context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) },
        onRestoreSound = viewModel::onRestoreSound,
        onAddCurrentLocation = { withLocation(viewModel::onAddCurrentLocation) },
        onFindNearby = { withLocation(viewModel::onFindNearby) },
        onMosqueClick = viewModel::onMosqueClicked,
        onDraftNameChange = viewModel::onDraftNameChanged,
        onDraftRadiusChange = viewModel::onDraftRadiusChanged,
        onDraftSave = viewModel::onSaveMosque,
        onDraftDismiss = viewModel::onDraftDismiss,
        onDeleteClick = viewModel::onDeleteClick,
        onOpenInMaps = { lat, lon, name -> context.openInMaps(lat, lon, name) },
        onDeleteConfirm = viewModel::onConfirmDelete,
        onDeleteDismiss = viewModel::onDeleteDismiss,
        onNearbySelect = viewModel::onNearbySelected,
        onNearbyRetry = viewModel::onNearbyRetry,
        onNearbyDismiss = viewModel::onNearbyDismiss,
        onDisclosureContinue = {
            showDisclosure = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                backgroundLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            } else {
                afterRequest(enableAfterGrant)
            }
        },
        onDisclosureDismiss = { showDisclosure = false },
        onMessageDismiss = viewModel::onMessageDismiss,
    )
}

// Android 12+ ignores a request for fine location on its own, so both are always asked together.
private val FOREGROUND_PERMISSIONS = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

private fun Context.granted(permission: String) =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

private fun Context.hasFineLocation() = granted(Manifest.permission.ACCESS_FINE_LOCATION)

private fun Context.locationAccess(): LocationAccess {
    if (!hasFineLocation()) return LocationAccess.NONE
    val background = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || granted(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    return if (background) LocationAccess.FULL else LocationAccess.FOREGROUND_ONLY
}

/** Prefers Google Maps, then any map app, then the browser. */
private fun Context.openInMaps(latitude: Double, longitude: Double, name: String) {
    val label = Uri.encode(name.ifBlank { "Mosque" })
    val geo = Intent(Intent.ACTION_VIEW, Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude($label)"))
    val web = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=$latitude,$longitude"))
    val attempts = listOf(Intent(geo).setPackage("com.google.android.apps.maps"), geo, web)
    for (intent in attempts) {
        try {
            startActivity(intent)
            return
        } catch (e: ActivityNotFoundException) {
            // try the next option
        }
    }
}

private fun Context.openAppSettings() {
    startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)))
}
