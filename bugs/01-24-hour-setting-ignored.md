# 01 — 24-hour time setting is saved but never used

**Severity:** Medium (was High — re-evaluated: a setting that does nothing, but times still display correctly)

## Problem
Settings → Appearance has a "24-hour time" switch. It's persisted (`UserPrefs.use24HourFormat`) but no screen reads it — every time on screen uses a hardcoded pattern, and those patterns disagree with each other.

## Where
12-hour (`hh:mm a`):
- `app/src/main/java/com/salattracker/feature/home/HomeScreen.kt:144`
- `app/src/main/java/com/salattracker/feature/home/HomeScreen.kt:330`
- `app/src/main/java/com/salattracker/feature/calendar/CalendarScreen.kt:222`
- `app/src/main/java/com/salattracker/feature/reminders/PrayerRemindersScreen.kt:46`

24-hour (`HH:mm`):
- `app/src/main/java/com/salattracker/notification/PrayerReminderScreen.kt:63` (full-screen alarm)
- `app/src/main/java/com/salattracker/feature/goals/AddGoalScreen.kt:212`

Also mixed into date+time formatters: `BackupScreen.kt:301`, `QuranReadHistoryScreen.kt:99` (`h:mm a`).

## Impact
The same prayer shows "03:20 PM" on Home and "15:20" on the alarm, and toggling the setting changes nothing.

## Suggested fix
- Add one shared time formatter (e.g. in `core/ui`) that takes `use24HourFormat` and the app locale.
- Expose `use24HourFormat` in the UiState of Home, Calendar, Prayer Reminders, Goals, Backup, Quran history, and read it in `PrayerReminderActivity` from `getCurrentPrefs()`.
- Replace every hardcoded time pattern with the shared formatter.

## Done when
- Toggling the setting changes every displayed time in the app, including the full-screen alarm.
- No `ofPattern("hh:mm a")` / `ofPattern("HH:mm")` remains outside the shared helper.
