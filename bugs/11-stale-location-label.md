# 11 — Saved city name can go stale after the location changes

**Severity:** Medium

## Problem
`UserPrefs.locationLabel` (e.g. "Karachi, Sindh, Pakistan") is set by onboarding and the Edit Location screen, but two other paths update the coordinates without touching the label:
- The automatic GPS refresh on every app launch (`LocationSyncViewModel.syncDeviceLocation`).
- Turning on the "Device Location" switch in Settings (`SettingsViewModel.fetchDeviceLocation`).

After travelling, Settings → Location can show the old city while prayer times use the new coordinates.

There are also two controls for the same thing: the Settings "Device Location" switch and "Use my location" inside Edit Location.

## Where
- `app/src/main/java/com/salattracker/feature/main/LocationSyncViewModel.kt:27-41`
- `app/src/main/java/com/salattracker/feature/settings/SettingsViewModel.kt:103`
- `app/src/main/java/com/salattracker/feature/settings/EditCoordinatesViewModel.kt` (`onUseMyLocationClick` — the correct version)

## Suggested fix
- When the GPS fix moves meaningfully (LocationSyncViewModel already compares at ~1 km precision before rescheduling), reverse-geocode and update the label, or clear it (fall back to coordinates).
- Make the Settings switch go through the same path as "Use my location", or remove one of the two controls.

## Done when
The label shown in Settings always describes the coordinates currently in use (or is absent).
