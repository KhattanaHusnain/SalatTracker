# 54 — Stats: Goals card duplicates the Goals tab

**Severity:** Low · **Area:** Stats / product · Part of [50](50-stats-redesign-question-led.md)

## Problem
The Goals card on Stats lists each goal with "periods hit / periods counted". The Goals tab already shows each goal with a 7-day strip and today's progress, and Goal details shows streaks, totals and a calendar. The Stats version adds less detail and its own truncation bug (45).

## Where
- `app/src/main/java/com/salattracker/feature/stats/StatsScreen.kt` — `GoalsCard`, `GoalStatRow`
- `app/src/main/java/com/salattracker/feature/stats/StatsViewModel.kt` — `loadGoals`, `observeGoalChanges`

## Suggested fix
Remove the card and the goal loading from `StatsViewModel` (also removes one of the double-loads in bug 49). If a goals summary is wanted, add a "This period" row to the Goals tab instead.

## Done when
Goal statistics live in one place.
