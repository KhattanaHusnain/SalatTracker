# 55 — Stats: period options are arbitrary (40D does nothing special, 1Y is weak)

**Severity:** Low · **Area:** Stats / product · Part of [50](50-stats-redesign-question-led.md)

## Problem
Five chips (7D, 30D, 40D, 90D, 1Y):
- **40D** looks like a nod to the hadith about praying 40 days in congregation, but it just shows the same completion stats over 40 days. Nothing about congregation is measured.
- **1Y** has partial-month and data-gap problems (bug 41) and little extra insight over 90D.
- 30D and 40D are nearly identical views.

## Where
- `app/src/main/java/com/salattracker/feature/stats/StatsViewModel.kt` — `StatsPeriod`
- `app/src/main/java/com/salattracker/feature/stats/StatsScreen.kt` — `PeriodToggle`

## Suggested fix
Use **Week / Month / 3 months** (calendar-aligned or rolling, but consistent). Move the 40-day idea into the Congregation card as a real challenge (bug 56).

## Done when
Every period option produces a meaningfully different view, and nothing implies a feature that isn't there.
