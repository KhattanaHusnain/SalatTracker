# 13 — Switches and primary buttons styled inconsistently

**Severity:** Low

## Problem
**Switches:** a shared `salatSwitchColors()` exists in `core/ui` (used by Settings), but 8 switches define their own `SwitchDefaults.colors(...)` with slightly different values:
- `feature/goals/GoalComponents.kt:268`
- `feature/goals/GoalForm.kt:218, 234, 265`
- `feature/reminders/PrayerRemindersScreen.kt:132, 252, 451`
- `feature/onboarding/OnboardingScreen.kt:739`

Also check `feature/mosque/MosqueModeScreen.kt:209`.

**Primary buttons:** four heights (48/52/56/64 dp) and several corner radii (12/14/16 dp) across screens, e.g. Onboarding 56 dp/16, Edit Location 52 dp/14, Mosque Mode 52 dp, Qaza 48 dp, Stats 64 dp.

## Suggested fix
- Replace the custom switch colours with `salatSwitchColors()`.
- Add a shared `SalatPrimaryButton` (one height, one shape) in `core/ui` and migrate full-width primary actions to it.

## Done when
All switches use `salatSwitchColors()`; full-width primary buttons share one component.
