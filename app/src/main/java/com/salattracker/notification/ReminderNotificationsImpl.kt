package com.salattracker.notification

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.salattracker.domain.model.PrayerType
import com.salattracker.domain.repository.ReminderNotifications
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderNotificationsImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : ReminderNotifications {

    override fun dismiss(type: PrayerType) {
        NotificationManagerCompat.from(context).cancel(type.ordinal)
        PrayerReminderReceiver.cancelSnooze(context, type)
    }
}
