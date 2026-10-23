# 44 — Stats: no change vs last period is shown in red

**Severity:** Low · **Area:** Stats / visual feedback

## Problem
The comparison chip uses `if (delta > 0) tint else SalatError`, so a delta of exactly 0 renders **"0% vs last" in the error colour**, signalling a decline when nothing changed. The text is also "0%" with no sign, while positives get "+".

Related: the delta compares against the previous period even when that period only has a handful of records (e.g. 2 days of data right after install), so the comparison can swing wildly. Consider requiring a minimum amount of data before showing it.

## Where
- `app/src/main/java/com/salattracker/feature/stats/StatsScreen.kt:212-218`
- `app/src/main/java/com/salattracker/feature/stats/StatsViewModel.kt:141`

## Suggested fix
Three states: positive → success colour with "+", negative → error with "−", zero → neutral (`SalatOnSurfaceVariant`) and "No change". Hide the delta when the previous period's countable total is below a threshold (e.g. fewer than 10 prayers).

## Done when
Zero change is never shown as a decline.
