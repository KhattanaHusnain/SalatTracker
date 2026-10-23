# 16 — Exact-alarm setup: Android 12 revocation unhandled; `USE_EXACT_ALARM` needs a Play declaration

**Severity:** Medium · **Area:** UX / permissions

> **Corrected 2026-10-23.** The first version of this bug said exact alarms are denied by default on Android 14+. That was wrong. The manifest also declares `USE_EXACT_ALARM`, which Android 13+ grants automatically and the user can't revoke, so exact alarms work there.

## Remaining problems
1. **Android 12 / 12L (API 31–32):** `USE_EXACT_ALARM` doesn't exist there, so the app relies on `SCHEDULE_EXACT_ALARM`. It's granted by default, but the user can revoke it in system settings. The app never checks `AlarmManager.canScheduleExactAlarms()`. `AlarmScheduler` catches the `SecurityException` and silently falls back to inexact `setAndAllowWhileIdle`, so reminders can arrive minutes late with no explanation.
2. **Play policy:** `USE_EXACT_ALARM` is restricted to apps whose core function is an alarm clock or calendar, and needs a declaration in the Play Console. A prayer-time alarm app is a reasonable fit, but if the declaration is rejected the app must fall back to `SCHEDULE_EXACT_ALARM` (denied by default on Android 14+) — at which point problem 1 applies to every modern device.
3. **Manifest hygiene:** `SCHEDULE_EXACT_ALARM` should carry `android:maxSdkVersion="32"` when `USE_EXACT_ALARM` is declared, so the app doesn't request both on 13+.

## Where
- `app/src/main/AndroidManifest.xml:11-12`
- `app/src/main/java/com/salattracker/notification/AlarmScheduler.kt` (exact → inexact fallback)

## Suggested fix
- Add `android:maxSdkVersion="32"` to `SCHEDULE_EXACT_ALARM`.
- Check `canScheduleExactAlarms()` on Prayer Reminders. If false, show a warning row that opens `Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM`. This covers Android 12 and the case where Play rejects `USE_EXACT_ALARM`.
- Confirm the `USE_EXACT_ALARM` declaration is accepted in the Play Console.

## Done when
On any Android version where exact alarms aren't available, the user is told and offered a fix. The Play declaration is in place.
