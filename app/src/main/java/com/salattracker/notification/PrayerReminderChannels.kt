package com.salattracker.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.salattracker.R
import com.salattracker.domain.model.PrayerType
import com.salattracker.domain.repository.DEVICE_DEFAULT_SOUND_URI
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One notification channel per prayer, since a channel's sound and vibration are fixed at
 * creation on API 26+ — the only way to change either is to delete the channel and recreate it
 * with the same ID. [ensure] is safe to call on every notification fire (an unchanged channel
 * config is a documented no-op); [recreate] is the one that actually applies a new sound or
 * vibration choice.
 */
@Singleton
class PrayerReminderChannels @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun channelId(type: PrayerType): String = "prayer_reminder_${type.name}"

    fun ensure(type: PrayerType, soundUri: String?, vibrationEnabled: Boolean) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        manager().createNotificationChannel(buildChannel(type, soundUri, vibrationEnabled))
    }

    fun recreate(type: PrayerType, soundUri: String?, vibrationEnabled: Boolean) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        manager().deleteNotificationChannel(channelId(type))
        ensure(type, soundUri, vibrationEnabled)
    }

    private fun buildChannel(type: PrayerType, soundUri: String?, vibrationEnabled: Boolean): NotificationChannel {
        val channel = NotificationChannel(channelId(type), channelName(type), NotificationManager.IMPORTANCE_HIGH)
        channel.description = channelName(type)
        channel.enableVibration(vibrationEnabled)
        // Null means "never touched" — the app's own default (the bundled Azan) applies, not
        // the device's. DEVICE_DEFAULT_SOUND_URI is the explicit opt-out into the device's own
        // default; "" is Silent (see SettingsRepository.setReminderSoundUri for the full encoding).
        val sound = when (soundUri) {
            null -> azanUri()
            DEVICE_DEFAULT_SOUND_URI -> Settings.System.DEFAULT_NOTIFICATION_URI
            ""   -> null // Silent
            else -> Uri.parse(soundUri)
        }
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        channel.setSound(sound, attributes)
        return channel
    }

    private fun channelName(type: PrayerType): String = "${type.channelLabel} Reminders"

    private fun azanUri(): Uri = Uri.parse("android.resource://${context.packageName}/${R.raw.azan}")

    private fun manager() = context.getSystemService(NotificationManager::class.java)
}

// Mirrors the existing hardcoded, not-yet-localized labels already used for notification text
// in PrayerReminderReceiver.kt — channel names are a separate, system-settings-only surface.
private val PrayerType.channelLabel: String get() = when (this) {
    PrayerType.FAJR    -> "Fajr"
    PrayerType.DHUHR   -> "Dhuhr"
    PrayerType.ASR     -> "Asr"
    PrayerType.MAGHRIB -> "Maghrib"
    PrayerType.ISHA    -> "Isha"
}
