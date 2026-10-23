# 70 — Marking a prayer from its notification records it on the wrong day after midnight

**Severity:** High · **Area:** Bug / data correctness

## Problem
The prayer alarm intent carries only the prayer **type**, not its date. Every way of marking from outside the app uses the date at the moment of tapping:
- Notification actions (Congregation / Individual / Qaza) → `PrayerReminderReceiver.markAndDismiss` → `upsertPrayerStatus(LocalDate.now(), type, status)`
- Full-screen alarm tiles → `PrayerReminderActivity.markAndFinish` → `LocalDate.now()`
- Snoozed reminders → `scheduleSnooze` passes only the type

Scenario: the Isha notification arrives at 20:00. The user prays and taps "Individual" on the still-visible notification at 00:30. The app records **tomorrow's** Isha as prayed — a prayer that hasn't happened — and leaves the real Isha unrecorded, so it becomes Qaza (bug 61). The streak breaks, and tomorrow's Isha is already "done". The full-screen alarm and snoozed notifications also show the label for the new date (e.g. Jumu'ah relabelling).

## Where
- `app/src/main/java/com/salattracker/notification/AlarmScheduler.kt:142` (only `EXTRA_PRAYER_TYPE`)
- `app/src/main/java/com/salattracker/notification/PrayerReminderReceiver.kt:63, 88, scheduleSnooze`
- `app/src/main/java/com/salattracker/notification/PrayerReminderActivity.kt:60, 72, 97`

## Suggested fix
- Put the prayer's date (ISO string) in the alarm, notification-action, snooze and full-screen intents, and use it everywhere instead of `LocalDate.now()`.
- Optionally expire the notification (`setTimeoutAfter`) at the end of the prayer's window.
- Test: an action intent with date D marks D regardless of the current date.

## Done when
Acting on a prayer notification always records that prayer's own date.
