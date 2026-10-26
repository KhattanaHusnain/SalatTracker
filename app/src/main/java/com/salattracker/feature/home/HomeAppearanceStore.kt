package com.salattracker.feature.home

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Presentation-only preferences; never stores or modifies prayer history. */
data class HomeAppearance(val streakBadge: Boolean = false, val showPhotoInfo: Boolean = true,
    val showAppreciation: Boolean = true, val confetti: Boolean = true,
    val voice: Boolean = true, val chime: Boolean = true, val vibration: Boolean = true,
    val arabicWords: Boolean = false, val speechRate: Float = .95f, val voiceName: String? = null)

@Singleton
class HomeAppearanceStore @Inject constructor(@ApplicationContext context: Context) {
    private val prefs = context.getSharedPreferences("home_appearance", Context.MODE_PRIVATE)
    private val current = MutableStateFlow(HomeAppearance(prefs.getBoolean("badge", false),
        prefs.getBoolean("photo_info", true), prefs.getBoolean("appreciation", true), prefs.getBoolean("confetti", true),
        prefs.getBoolean("voice", true), prefs.getBoolean("chime", true), prefs.getBoolean("vibration", true),
        prefs.getBoolean("arabic_words", false), prefs.getFloat("speech_rate", .95f), prefs.getString("voice_name", null)))
    val state = current.asStateFlow()
    fun save(value: HomeAppearance) {
        prefs.edit().putBoolean("badge", value.streakBadge).putBoolean("photo_info", value.showPhotoInfo)
            .putBoolean("appreciation", value.showAppreciation).putBoolean("confetti", value.confetti)
            .putBoolean("voice", value.voice).putBoolean("chime", value.chime).putBoolean("vibration", value.vibration)
            .putBoolean("arabic_words", value.arabicWords)
            .putFloat("speech_rate", value.speechRate.coerceIn(.7f, 1.2f)).putString("voice_name", value.voiceName).apply()
        current.value = value
    }
}
