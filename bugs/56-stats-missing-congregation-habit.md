# 56 — Stats: no congregation metric (and no real 40-day challenge)

**Severity:** Medium · **Area:** Stats / product · **Type:** Missing feature · Part of [50](50-stats-redesign-question-led.md)

## Problem
The app records Congregation vs Individual for every prayer, but Stats never answers "how often do I pray in congregation?". It only appears as a raw count in the legend. For many male users this is the habit they're trying to build. The 40-day congregation practice is well known, and the 40D chip hints at it without delivering.

## Proposal
A **Congregation** card, shown only when gender ≠ Female (consistent with how Congregation is hidden elsewhere):
- Congregation share of prayed prayers for the period, vs previous period.
- Per-prayer congregation % (e.g. "Fajr 20% · Isha 75%").
- **40-day challenge**: consecutive days with all five prayers in congregation, shown as "Day 12 of 40", with the current best run.

## Where it would go
- New use case in `domain/usecase/` (pure, testable from `DayStatusCounts` / `PrayerBreakdown`)
- `feature/stats/StatsViewModel.kt` (needs `gender`, see bug 08)
- `feature/stats/StatsScreen.kt`

## Done when
A male user can see their congregation habit and 40-day progress; a female user never sees this card.
