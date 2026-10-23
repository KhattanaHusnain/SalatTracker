# 49 — Stats: doesn't roll over at midnight, and loads everything twice on open

**Severity:** Low · **Area:** Stats / data freshness & performance

## Problems
1. **Stale after midnight.** Each load captures `LocalDate.now()` once. If Stats stays open (or in the back stack) past midnight, the period still ends "yesterday" until the user changes the period or a prayer write triggers a reload.
2. **Double load on open.** `init` calls `loadAllForPeriod(...)`, then `observePrayerChanges()` immediately gets the first `observeStreaks()` emission and reloads Overview and By prayer again (cancelling the first job mid-flight). Each open runs the overview queries (current + previous period) twice. Same for Goals via `observeGoalChanges()`.

## Where
- `app/src/main/java/com/salattracker/feature/stats/StatsViewModel.kt:101-105, 123, 159, 174, 184-205`

## Suggested fix
- Drive loads from one combined flow: `combine(periodFlow, dateTicker, observeStreaks().drop(0)…)` with `distinctUntilChanged`, or simply skip the first emission of the change-signal flows (`drop(1)`).
- Add a day-change ticker (as `HomeViewModel` does) so the range rolls over at midnight.
- Inject a `Clock` (see bug 37) so this is testable.

## Done when
Opening Stats runs each query once, and the range updates at midnight without user action.
