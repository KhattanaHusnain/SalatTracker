package com.salattracker.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.salattracker.domain.usecase.MosqueModeController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Geofence transitions and the notification's "Restore sound" action. */
@AndroidEntryPoint
class MosqueModeReceiver : BroadcastReceiver() {

    @Inject lateinit var controller: MosqueModeController

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    ACTION_RESTORE_SOUND -> controller.restoreSound()
                    ACTION_GEOFENCE -> handleGeofence(intent)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    // Error events are ignored on purpose: switching location off drops every geofence, and the
    // next sync re-registers them.
    private suspend fun handleGeofence(intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) return
        val ids = event.triggeringGeofences.orEmpty().mapNotNull { it.requestId.toLongOrNull() }
        if (ids.isEmpty()) return
        when (event.geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_DWELL -> controller.onDwell(ids)
            Geofence.GEOFENCE_TRANSITION_EXIT -> controller.onExit(ids)
        }
    }

    companion object {
        const val ACTION_GEOFENCE = "com.salattracker.action.MOSQUE_GEOFENCE"
        const val ACTION_RESTORE_SOUND = "com.salattracker.action.MOSQUE_RESTORE_SOUND"
    }
}
