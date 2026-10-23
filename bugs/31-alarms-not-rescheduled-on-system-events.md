# 31 — Prayer alarms only follow a location change when the app is opened; the daily chain can lapse

**Severity:** Medium · **Area:** QA / reliability

> **Corrected 2026-10-23 (re-audit).** The first version claimed Android clears an app's alarms on update and that time-zone changes shift them. Both were wrong. AlarmManager keeps alarms across app updates (it only drops them on force-stop or uninstall). The alarms are absolute instants, so a time-zone change alone doesn't make them wrong for the same location.

## Remaining problems
1. **Travelling.** With "Device location" on, the stored location is refreshed only when the app is opened (`LocationSyncViewModel.syncDeviceLocation` from `MainActivity`). A user who travels to another city and doesn't open the app keeps getting alarms for the old city's prayer times. Time-zone changes are a cheap signal that the user has moved, but nothing listens for them.
2. **Force-stop.** Force-stopping the app (or some OEM "clean" tools) removes its alarms and stops WorkManager until the app is opened again. That's expected Android behaviour, but nothing tells the user that reminders are now off.
3. **Next-occurrence chain.** `PrayerReminderReceiver` doesn't schedule the next day's alarm when one fires. Tomorrow's alarms are set only by the next run of `PrayerReminderWorker` (24 h periodic) or an app open. If that periodic job is deferred (Doze, restricted standby bucket), a day's reminders can be skipped.

## Where
- `app/src/main/java/com/salattracker/feature/main/LocationSyncViewModel.kt`
- `app/src/main/AndroidManifest.xml` — `BootReceiver` listens only for `BOOT_COMPLETED`
- `app/src/main/java/com/salattracker/notification/PrayerReminderReceiver.kt` — `showNotification` doesn't reschedule
- `app/src/main/java/com/salattracker/notification/PrayerReminderWorker.kt`

## Suggested fix
- After posting a prayer notification, enqueue `PrayerReminderWorker` (one-time) so the next alarm for that prayer is always set.
- Listen for `TIMEZONE_CHANGED`: when device location is on, refresh the location fix and reschedule (or at least reschedule).
- Optionally also listen for `MY_PACKAGE_REPLACED` to reschedule after updates (defensive; cheap).

## How to verify
`adb shell dumpsys alarm | grep -A3 salattracker` after each scenario: alarm firing, time-zone change while device location is on, and an update via `adb install -r`.
