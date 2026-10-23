# 17 — Denying notifications is silent; reminders look "on" but never arrive

**Severity:** High · **Area:** UX / permissions

## Problem
`POST_NOTIFICATIONS` is requested with an empty result callback. If the user denies it (or later turns notifications off in system settings), every reminder switch in the app still shows **on**, and nothing tells the user reminders won't appear. There's no recovery path back to system settings.

## Where
- `app/src/main/java/com/salattracker/feature/main/MainActivity.kt:119` — `RequestPermission()` launcher with `{ }`.
- Prayer Reminders screen never checks `NotificationManagerCompat.areNotificationsEnabled()` (only Mosque Mode does, `notification/MosqueModeNotifierImpl.kt:32`).

## Suggested fix
- On Prayer Reminders (and Home, near the next-prayer card) show a banner when `areNotificationsEnabled()` is false: "Notifications are off — reminders won't appear" with a button to `Settings.ACTION_APP_NOTIFICATION_SETTINGS`.
- After a denial during onboarding, show the same message on the Done summary ("Reminders: Blocked by system").

## Done when
With notifications denied, the app clearly says reminders are blocked and offers a one-tap way to fix it.
