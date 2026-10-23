# 18 — "Full-screen alert" can be switched on but do nothing on Android 14+

**Severity:** Medium · **Area:** UX / permissions

## Problem
On Android 14+, `USE_FULL_SCREEN_INTENT` can be revoked by the system (Play only auto-grants it to calling/alarm apps, and the user can turn it off). The app never checks `NotificationManager.canUseFullScreenIntent()`, so the "Full-screen alert" switch (onboarding and Prayer Reminders) can be on while the alarm only ever appears as a normal notification.

## Where
- `app/src/main/AndroidManifest.xml:13`
- `app/src/main/java/com/salattracker/notification/PrayerReminderReceiver.kt` (`setFullScreenIntent` with no capability check)
- Full-screen toggles: `feature/reminders/PrayerRemindersScreen.kt`, `feature/onboarding/OnboardingScreen.kt` (Reminders step)

## Suggested fix
When full-screen alert is on and `canUseFullScreenIntent()` is false (API 34+), show an inline note under the toggle with a button to `Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT`.

## Done when
A switch that can't work says so and links to the fix.
