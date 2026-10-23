# 71 — A prayer's notification stays after the prayer is logged in the app

**Severity:** Low · **Area:** Bug / notifications

## Problem
Logging a prayer on Home or Calendar doesn't dismiss that prayer's notification (or a pending snooze). The reminder sits in the shade asking about a prayer that's already recorded. Tapping one of its actions later overwrites the status (e.g. Congregation → Individual) — or, after midnight, writes the wrong day (bug 70).

## Where
- `app/src/main/java/com/salattracker/feature/home/HomeViewModel.kt` — `onStatusSave`
- `app/src/main/java/com/salattracker/feature/calendar/CalendarViewModel.kt` — `onStatusSave`
- Notifications are posted with id `type.ordinal` in `notification/PrayerReminderReceiver.kt`; nothing outside the receiver/alarm activity cancels them.

## Suggested fix
After a successful status write for **today**, cancel notification `type.ordinal` and any pending snooze for that prayer. Put this behind a small `ReminderNotifications` interface so ViewModels stay testable.

## Done when
Logging a prayer anywhere in the app clears its reminder.
