package com.salattracker.domain.repository

/**
 * Reschedules prayer-reminder alarms after something that could shift prayer times (location or
 * calculation-method change). An interface (mirrors [GoalReminderScheduler]'s shape) so
 * ViewModels don't need a raw `WorkManager` dependency — it has no fake-friendly surface (it's
 * an abstract class with many unrelated members) — and can be unit-tested instead.
 */
interface PrayerReminderRescheduler {
    fun rescheduleAll()
}
