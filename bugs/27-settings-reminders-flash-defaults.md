# 27 — Settings and Prayer Reminders briefly show default values before loading

**Severity:** Low · **Area:** UX / loading states

## Problem
Both ViewModels have an `isLoading` flag but the screens never read it (only the `@Preview`s do). On open, Settings briefly shows defaults ("English", "System", coordinates `0.0000, 0.0000`, switches in default positions) and then jumps to the real values. Prayer Reminders briefly shows only the Jumu'ah card, then the five prayer cards pop in above it. Mosque Mode has the same pattern.

Users perceive this as flicker, and can tap a control in its wrong state during that moment.

## Where
- `app/src/main/java/com/salattracker/feature/settings/SettingsScreen.kt` (no `isLoading` handling)
- `app/src/main/java/com/salattracker/feature/reminders/PrayerRemindersScreen.kt` (no `isLoading` handling)
- `app/src/main/java/com/salattracker/feature/mosque/MosqueModeScreen.kt` (0 references)

## Suggested fix
While `isLoading`, show the app's existing loading pattern (or render nothing / a skeleton) instead of default-valued controls.

## Done when
Opening these screens never shows values that aren't the user's real settings.
