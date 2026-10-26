package com.salattracker.feature.home

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.view.HapticFeedbackConstants
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

internal class HomeFeedbackEffects(val voices: List<Pair<String, String>>, val speechReady: Boolean,
    val play: (String, Boolean) -> Unit)

/** Foreground appreciation only. Never schedules audio or overrides silent / Do Not Disturb. */
@Composable internal fun rememberHomeFeedbackEffects(appearance: HomeAppearance, configureVoice: Boolean): HomeFeedbackEffects {
    val context = LocalContext.current
    val view = LocalView.current
    val locale = LocalConfiguration.current.locales[0]
    val latest by rememberUpdatedState(appearance)
    var ready by remember { mutableStateOf(false) }
    val engine = if (appearance.voice || configureVoice) remember(context) {
        TextToSpeech(context) { ready = it == TextToSpeech.SUCCESS }
    } else null
    val tone = remember { runCatching { ToneGenerator(AudioManager.STREAM_NOTIFICATION, 25) }.getOrNull() }
    DisposableEffect(engine) { onDispose { engine?.stop(); engine?.shutdown(); ready = false } }
    DisposableEffect(tone) { onDispose { tone?.release() } }
    val voices = if (ready && engine != null) engine.voices.orEmpty().filter {
        it.locale.language == locale.language || (locale.language == "ur" && it.locale.language == "hi")
    }
        .sortedByDescending { it.quality }.map { it.name to it.name } else emptyList()
    return HomeFeedbackEffects(voices, ready && voices.isNotEmpty()) { text, celebrate ->
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val notifications = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val quiet = audio.ringerMode != AudioManager.RINGER_MODE_NORMAL ||
            notifications.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
        if (celebrate && !quiet) {
            if (latest.chime) tone?.startTone(ToneGenerator.TONE_PROP_ACK, 160)
            if (latest.voice && ready && engine != null) speakAppreciation(engine, text, locale.language, latest)
        }
        if (celebrate && latest.vibration && !quiet) view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
    }
}

private fun speakAppreciation(engine: TextToSpeech, text: String, language: String, appearance: HomeAppearance) {
    val available = engine.voices.orEmpty()
    val base = available.firstOrNull { it.name == appearance.voiceName } ?: available
        .filter { it.locale.language == language || (language == "ur" && it.locale.language == "hi") }
        .maxByOrNull(Voice::getQuality) ?: return
    val arabic = available.filter { it.locale.language == "ar" }.maxByOrNull(Voice::getQuality)
    engine.setSpeechRate(appearance.speechRate.coerceIn(.7f, 1.2f))
    engine.stop()
    if (!appearance.arabicWords || language !in listOf("en", "fr") || arabic == null) {
        engine.voice = base
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "home-appreciation")
        return
    }
    val arabicWords = mapOf(
        "MashaAllah" to "ما شاء الله", "Machaallah" to "ما شاء الله", "Allah" to "الله",
        "Fajr" to "الفجر", "Dhuhr" to "الظهر", "Asr" to "العصر", "Maghrib" to "المغرب",
        "Isha" to "العشاء", "Jumu'ah" to "الجمعة", "Joumou'a" to "الجمعة",
    )
    val token = Regex("(MashaAllah|Machaallah|Jumu'ah|Joumou'a|Fajr|Dhuhr|Asr|Maghrib|Isha|Allah)")
    var cursor = 0
    var first = true
    token.findAll(text).forEach { match ->
        if (match.range.first > cursor) {
            engine.voice = base
            engine.speak(text.substring(cursor, match.range.first), if (first) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD,
                null, "home-base-$cursor")
            first = false
        }
        engine.voice = arabic
        engine.speak(arabicWords.getValue(match.value), if (first) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD,
            null, "home-arabic-$cursor")
        first = false
        cursor = match.range.last + 1
    }
    if (cursor < text.length) {
        engine.voice = base
        engine.speak(text.substring(cursor), if (first) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD,
            null, "home-base-end")
    }
}
