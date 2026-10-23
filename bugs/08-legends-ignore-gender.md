# 08 — Stats and Calendar legends ignore the gender rule

**Severity:** Medium

## Problem
Status pickers now hide Congregation for women and Menstruation for men (`PrayerStatus.isOfferedFor(gender)` in `core/ui/Localized.kt`). The legends and breakdown in Stats and Calendar still list both statuses for everyone, so a woman sees "Congregation: 0" and a man sees an "Excused" legend entry.

## Where
- `app/src/main/java/com/salattracker/feature/stats/StatsScreen.kt:229` (legend counts) and the stacked bar around line 171-177
- `app/src/main/java/com/salattracker/feature/calendar/CalendarScreen.kt:328` and `:332` (Month legend), `:339-342` (Year legend)

## Suggested fix
- Add `gender` to `StatsUiState` (StatsViewModel needs `SettingsRepository`) and to the Calendar legend call (CalendarViewModel already exposes `gender`).
- Hide a legend entry when `!status.isOfferedFor(gender)` **and** its count in the visible range is 0 — so historical data recorded before gender was set still shows.

## Done when
A female user never sees a Congregation legend/count (unless she has historical Congregation entries); a male user never sees Menstruation/Excused (same caveat); `gender == null` shows everything.
