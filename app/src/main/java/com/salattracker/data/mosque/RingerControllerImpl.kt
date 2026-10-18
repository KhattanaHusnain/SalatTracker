package com.salattracker.data.mosque

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.Vibrator
import android.os.VibratorManager
import com.salattracker.domain.model.RingerMode
import com.salattracker.domain.repository.RingerController
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RingerControllerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : RingerController {

    private val audio get() = context.getSystemService(AudioManager::class.java)

    // Without a vibrator, "vibrate" quietly becomes "silent", which can't be undone without Do Not Disturb access.
    override val canVibrate: Boolean
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator?.hasVibrator() == true
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)?.hasVibrator() == true
        }

    override fun current(): RingerMode = when (audio?.ringerMode) {
        AudioManager.RINGER_MODE_VIBRATE -> RingerMode.VIBRATE
        AudioManager.RINGER_MODE_SILENT -> RingerMode.SILENT
        else -> RingerMode.NORMAL
    }

    override fun set(mode: RingerMode): Boolean {
        val manager = audio ?: return false
        val target = when (mode) {
            RingerMode.NORMAL -> AudioManager.RINGER_MODE_NORMAL
            RingerMode.VIBRATE -> AudioManager.RINGER_MODE_VIBRATE
            RingerMode.SILENT -> AudioManager.RINGER_MODE_SILENT
        }
        return try {
            manager.ringerMode = target
            manager.ringerMode == target
        } catch (e: SecurityException) {
            false
        }
    }
}
