package com.salattracker.notification

import android.app.KeyguardManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.lifecycleScope
import com.salattracker.core.theme.SalatTrackerTheme
import com.salattracker.domain.model.Gender
import com.salattracker.domain.model.Madhab
import com.salattracker.domain.model.PrayerStatus
import com.salattracker.domain.model.PrayerType
import com.salattracker.domain.model.VerseInfo
import com.salattracker.domain.repository.PrayerRepository
import com.salattracker.domain.repository.SettingsRepository
import com.salattracker.domain.repository.VerseRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

/** Alarm-style full-screen reminder — shows over the lock screen when a prayer's time arrives. */
@AndroidEntryPoint
class PrayerReminderActivity : AppCompatActivity() {

    @Inject lateinit var prayerRepository: PrayerRepository
    @Inject lateinit var verseRepository: VerseRepository
    @Inject lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setShowWhenLocked(true)
        setTurnScreenOn(true)
        getSystemService(KeyguardManager::class.java)?.requestDismissKeyguard(this, null)

        val type = intent.prayerTypeOrNull()
        val date = intent.prayerDateOrToday()
        if (type == null) {
            finish()
            return
        }

        setContent {
            var scheduledTime by remember { mutableStateOf<LocalTime?>(null) }
            var verse by remember { mutableStateOf<VerseInfo?>(null) }
            var snoozeMinutes by remember { mutableStateOf(PrayerReminderReceiver.DEFAULT_SNOOZE_MINUTES) }
            var jumuahEnabled by remember { mutableStateOf(true) }
            var madhab by remember { mutableStateOf(Madhab.STANDARD) }
            var gender by remember { mutableStateOf<Gender?>(null) }

            LaunchedEffect(type) {
                scheduledTime = prayerRepository.getPrayerTimes(date)?.get(type)
                verse = verseRepository.getRandomVerse()
                val prefs = settingsRepository.getCurrentPrefs()
                snoozeMinutes = prefs.reminderSnoozeMinutes[type] ?: PrayerReminderReceiver.DEFAULT_SNOOZE_MINUTES
                jumuahEnabled = prefs.jumuahEnabled
                madhab = prefs.madhab
                gender = prefs.gender
            }

            SalatTrackerTheme {
                PrayerReminderScreen(
                    prayerType    = type,
                    date          = date,
                    jumuahEnabled = jumuahEnabled,
                    madhab        = madhab,
                    gender        = gender,
                    scheduledTime = scheduledTime,
                    verse         = verse,
                    snoozeMinutes = snoozeMinutes,
                    onMark        = { status -> markAndFinish(type, date, status) },
                    onSnooze      = { snoozeAndFinish(type, date, snoozeMinutes) },
                    onDismiss     = { finish() },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // A new prayer's alarm fired while this screen was already up — simplest
        // correct behavior is to restart onto the new prayer rather than merge state.
        recreate()
    }

    private fun markAndFinish(type: PrayerType, date: LocalDate, status: PrayerStatus) {
        lifecycleScope.launch {
            runCatching { prayerRepository.upsertPrayerStatus(date, type, status) }
            NotificationManagerCompat.from(this@PrayerReminderActivity).cancel(type.ordinal)
            finish()
        }
    }

    private fun snoozeAndFinish(type: PrayerType, date: LocalDate, snoozeMinutes: Int) {
        lifecycleScope.launch {
            NotificationManagerCompat.from(this@PrayerReminderActivity).cancel(type.ordinal)
            PrayerReminderReceiver.scheduleSnooze(this@PrayerReminderActivity, type, date, snoozeMinutes)
            finish()
        }
    }

    private fun Intent.prayerTypeOrNull(): PrayerType? =
        getStringExtra(AlarmScheduler.EXTRA_PRAYER_TYPE)?.let { name ->
            runCatching { PrayerType.valueOf(name) }.getOrNull()
        }
}
