package com.salattracker.domain.repository

import com.salattracker.domain.model.PrayerType

/** Removes the active reminder and any snoozed alarm for a recorded prayer. */
interface ReminderNotifications {
    fun dismiss(type: PrayerType)
}
