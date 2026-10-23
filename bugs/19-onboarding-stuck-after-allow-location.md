# 19 — Onboarding gets stuck after "Allow location" (no loading, denial or failure feedback)

**Severity:** High · **Area:** UX / onboarding

## Problem
On the Location step, "Allow location" launches the permission prompt, and the step only advances when a GPS fix later gets written to settings. Meanwhile:
- **While waiting** for the fix (can take several seconds), nothing on screen changes — no spinner, the button is still tappable.
- **If the user denies** the permission, nothing happens at all.
- **If location services are off** or no fix arrives, the user waits forever with no message.

The only way forward is noticing the small "Enter city instead" link.

## Where
- `app/src/main/java/com/salattracker/feature/onboarding/OnboardingViewModel.kt` — `observeDeviceLocation()` advances only on a non-zero lat/lon write; there's no `isLocating` state.
- `app/src/main/java/com/salattracker/feature/onboarding/OnboardingEntry.kt` — `onAllowLocationClick = onAllowLocation` (fire-and-forget).
- `app/src/main/java/com/salattracker/feature/main/MainActivity.kt:124` — launcher ignores denial.

## Suggested fix
- Have the Activity report the permission result back (e.g. `onAllowLocation: ((granted: Boolean) -> Unit) -> Unit`, or let OnboardingEntry own its launcher like `EditCoordinatesEntry` does).
- Add `isLocating` + `locationError` to `OnboardingUiState`: show a spinner on the button while locating; on denial or a ~10 s timeout, show a message ("Couldn't get your location") and promote "Enter city instead" to a primary button.

## Done when
Every outcome of "Allow location" (granted+fix, granted+no fix, denied, permanently denied) gives visible feedback and a clear next step.
