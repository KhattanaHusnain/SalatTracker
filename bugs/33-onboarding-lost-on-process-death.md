# 33 — Onboarding progress is lost if Android kills the app mid-setup

**Severity:** Medium · **Area:** QA / state restoration

## Problem
`OnboardingViewModel` keeps everything (current step, back stack, language, gender, school, location, reminder choices) in plain in-memory state; it doesn't use `SavedStateHandle`. Onboarding naturally sends users out of the app — to the system permission dialog, location settings, and (after bugs 16–18 are fixed) exact-alarm / full-screen settings. If the system kills the process while they're there, the user returns to the **Welcome** screen with every choice reset.

Rotation is fine (ViewModel survives); process death is not.

## Where
- `app/src/main/java/com/salattracker/feature/onboarding/OnboardingViewModel.kt` (no `SavedStateHandle`; `backStack` is a plain `MutableList`)

## Steps to reproduce
1. Start onboarding, get to the Reminders step.
2. Enable "Don't keep activities" in Developer options (or `adb shell am kill com.salattracker` while the app is in the background).
3. Switch away and back.
4. Onboarding restarts at Welcome.

## Suggested fix
Persist the step, back stack and the user's choices in `SavedStateHandle` (all are primitives/enums; `CityResult` can be split into fields or made `@Parcelize`).

## Done when
The repro above returns the user to the same step with their choices intact. Add a ViewModel unit test that rebuilds the VM from a `SavedStateHandle` and checks the state.
