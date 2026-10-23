# 12 — Raw exception text shown to users

**Severity:** Medium

## Problem
Most ViewModels catch failures with `error = e.message` and show that string in a snackbar. It's untranslated, often technical (SQLite/IO messages), and can be null (empty snackbar).

## Where (count of `error = e.message`)
- `feature/bulkupdate/BulkUpdateViewModel.kt` (1)
- `feature/calendar/CalendarViewModel.kt` (2)
- `feature/goals/AddGoalViewModel.kt` (1), `EditGoalViewModel.kt` (1), `GoalDetailsViewModel.kt` (3), `GoalsViewModel.kt` (3)
- `feature/home/HomeViewModel.kt` (2)
- `feature/qaza/MissedPrayersViewModel.kt` (3), `QazaViewModel.kt` (4)
- `feature/settings/SettingsViewModel.kt` (4)
- `feature/stats/StatsViewModel.kt` (3)

`EditCoordinatesViewModel` already uses a localized `errorRes: Int?` instead.

## Suggested fix
Switch these UiStates to `errorRes: Int?` (or a small sealed `UiError`) with a few generic localized messages ("Couldn't save. Please try again.", "Couldn't load data."), and log the exception instead of showing it.

## Done when
No user-visible error text comes from `Throwable.message`; all error snackbars are localized.
