# 15 — Duplicated Azan preview, city search and helper components

**Severity:** Low (maintenance)

## Problem
The same logic exists in more than one place, so fixes have to be made twice.

- **Azan preview playback** (MediaPlayer + notification `AudioAttributes`):
  - `feature/onboarding/OnboardingScreen.kt:464`
  - `feature/reminders/PrayerRemindersScreen.kt:557`
- **Debounced city search** (query state, 350 ms debounce, `searchCities`):
  - `feature/onboarding/OnboardingViewModel.kt` (`onCityQueryChanged`, `CITY_SEARCH_DEBOUNCE_MILLIS`)
  - `feature/settings/EditCoordinatesViewModel.kt` (same)
- **Cross-feature import:** onboarding imports `feature/settings/CalculationMethodSheet` directly.
- **Private helper copies in onboarding:** `PrimaryButton`, `ToggleRow`, `SelectionIndicator`, `StepDots` in `feature/onboarding/OnboardingScreen.kt`, overlapping with styles elsewhere (see bug 13).

## Suggested fix
- Move Azan preview into a `core/ui` helper (e.g. `rememberAzanPreviewPlayer()`).
- Move the city-search state + debounce into a small reusable class (or a domain use case) used by both ViewModels.
- Move `CalculationMethodSheet` to `core/ui`.
- Replace the private onboarding helpers with shared components once bug 13 adds them.

## Done when
Each of these exists in one place.
