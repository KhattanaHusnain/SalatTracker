package com.salattracker.core.ui

import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import androidx.annotation.RawRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

class AzanPreviewPlayer internal constructor(
    private val playAction: () -> Unit,
    private val stopAction: () -> Unit,
) {
    fun play() = playAction()
    fun stop() = stopAction()
}

/** Plays a bundled Azan on the notification stream and releases it when the host leaves composition. */
@Composable
fun rememberAzanPreviewPlayer(@RawRes azanRes: Int): AzanPreviewPlayer {
    val context = LocalContext.current
    var activePlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    val stop = remember {
        {
            runCatching { activePlayer?.stop() }
            runCatching { activePlayer?.release() }
            activePlayer = null
        }
    }
    val player = remember(context, azanRes, stop) {
        AzanPreviewPlayer(
            playAction = {
                stop()
                val attributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                activePlayer = MediaPlayer.create(context, azanRes, attributes, AudioManager.AUDIO_SESSION_ID_GENERATE)
                    ?.also { mediaPlayer -> mediaPlayer.setOnCompletionListener { completed -> runCatching { completed.release() } } }
                activePlayer?.start()
            },
            stopAction = stop,
        )
    }
    DisposableEffect(player) { onDispose { player.stop() } }
    return player
}
