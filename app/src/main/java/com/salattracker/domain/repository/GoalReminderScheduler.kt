package com.salattracker.domain.repository

/**
 * Keeps goal reminder alarms in step with goal changes. An interface so ViewModels don't need an
 * Android Context (and can be unit-tested); the implementation lives in notification/.
 */
interface GoalReminderScheduler {
    /** Cancels the pending alarm for a deleted or archived goal immediately. */
    fun cancel(goalId: Long)

    /** Re-evaluates every goal's next reminder — call after any create/edit/archive/restore. */
    fun rescheduleAll()
}
