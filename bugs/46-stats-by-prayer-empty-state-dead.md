# 46 — Stats: "By prayer" never shows its empty state

**Severity:** Low · **Area:** Stats / empty states

## Problem
`PrayerBreakdownCard` shows "No prayers recorded in this period" when `breakdown.isEmpty()`. But `PrayerRepositoryImpl.getPrayerBreakdown` always returns five rows (zero-filled), so that branch never runs. An empty period shows five grey bars with "—" instead of the message, unlike the Summary and Trend cards.

## Where
- `app/src/main/java/com/salattracker/feature/stats/StatsScreen.kt:286`
- `app/src/main/java/com/salattracker/data/repository/PrayerRepositoryImpl.kt:135-143`

## Suggested fix
Check `breakdown.all { it.counts.countable == 0 }` instead of `isEmpty()`.

## Done when
An empty period shows the same "No prayers recorded" message in every card.
