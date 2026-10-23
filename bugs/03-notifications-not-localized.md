# 03 — Prayer notifications are always in English

**Severity:** High

## Problem
The prayer-time notification's action buttons and prayer names are hardcoded English strings, so Arabic/Urdu/French users get English notifications while the rest of the app is in their language.

## Where
- `app/src/main/java/com/salattracker/notification/PrayerReminderReceiver.kt:102-107` — action labels "Congregation", "Individual", "Qaza", "Snooze ${n}m".
- `app/src/main/java/com/salattracker/notification/PrayerReminderReceiver.kt:220` — private `PrayerType.label()` returns "Fajr", "Jumu'ah", "Zuhrain", etc. (comment in the file already notes it's not localized).
- `app/src/main/java/com/salattracker/notification/PrayerReminderChannels.kt:72` — channel names "Fajr", "Dhuhr"… (visible in Android's notification settings).

## Suggested fix
- Use `context.getString(...)` with existing resources: `R.string.status_congregation`, `status_individual`, `status_qaza_pending`, `reminder_snooze`, and `PrayerType.nameRes` / `prayer_jumuah` / `prayer_zuhrain` etc. from `core/ui/Localized.kt`.
- Extract a non-composable `PrayerType.labelRes(date, jumuahEnabled, madhab): Int` in `Localized.kt` so the Receiver and the composable `label()` share one rule.
- Note: `context` in a BroadcastReceiver follows the per-app locale only if the app locale is applied via `AppCompatDelegate.setApplicationLocales`; verify on a device with the app in Urdu.

## Done when
With the app set to Arabic or Urdu, the notification title, body and all action buttons are in that language.
