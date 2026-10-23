# 23 — Denying location in Settings leaves "Device Location" on with no explanation

**Severity:** Medium · **Area:** UX / permissions

## Problem
Switching on Settings → Device Location saves `useDeviceLocation = true` **before** asking for permission. If the user denies, the switch stays on, coordinates don't change, and there's no message. On later launches the app keeps re-prompting (MainActivity requests location on start while the flag is on), which turns into an annoying loop once the system blocks the prompt.

The Edit Location screen's "Use my location" has the same silent-denial behaviour.

## Where
- `app/src/main/java/com/salattracker/feature/settings/SettingsEntry.kt:25-44`
- `app/src/main/java/com/salattracker/feature/settings/EditCoordinatesEntry.kt:28-30`
- `app/src/main/java/com/salattracker/feature/main/MainActivity.kt` (launch-time re-request)

## Suggested fix
- Only persist `useDeviceLocation = true` after permission is granted; on denial, flip the switch back and show a snackbar ("Location permission denied — you can enter a city instead") with a Settings shortcut when permanently denied.
- Don't auto re-request on every launch after a permanent denial.

## Done when
The switch always reflects whether device location is actually working, and denial produces a visible explanation.
