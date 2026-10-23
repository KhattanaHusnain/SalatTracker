# 47 — Stats: "weakest prayer/day" callouts fire on trivial differences

**Severity:** Low · **Area:** Stats / insight quality

## Problem
`weakestPrayer` and `weakestWeekday` flag the lowest value whenever it's below the highest by any amount, with no minimum sample:
- Fajr 99% vs others 100% → "Fajr needs the most attention" in red.
- On 30D each weekday has only ~4 dates; one missed prayer can make it "your weakest day".

These red callouts are the most prominent "insight" on the screen, so noisy ones undermine trust.

## Where
- `app/src/main/java/com/salattracker/domain/usecase/StatsSummary.kt:58-65, 109-116`
- `app/src/main/java/com/salattracker/feature/stats/StatsScreen.kt:292-304, 346-360`

## Suggested fix
Only call out when the gap to the average is meaningful (e.g. ≥ 10 percentage points) and the weakest item has a minimum number of countable prayers (e.g. ≥ 10). Add tests for the threshold.

## Done when
Callouts appear only for real, statistically meaningful differences.
