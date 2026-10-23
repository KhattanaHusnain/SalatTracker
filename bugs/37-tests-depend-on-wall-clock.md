# 37 — Tests depend on the real date/time (flaky around midnight, month ends, Fridays)

**Severity:** Low · **Area:** QA / test reliability

## Problem
ViewModels and use cases read `LocalDate.now()` / `LocalTime.now()` directly, and so do the tests that check them. Results then depend on when the suite runs:
- a test that seeds "today" and the ViewModel that reads "today" can straddle midnight;
- week/month grouping assertions change at month boundaries;
- Jumu'ah relabelling only triggers on Fridays;
- "is this prayer markable" depends on the current time of day.

No production code accepts an injectable `Clock`.

## Where (tests using the wall clock)
- `test/.../feature/calendar/CalendarViewModelTest.kt` (7 uses)
- `test/.../feature/stats/StatsViewModelTest.kt` (4)
- `test/.../feature/goals/GoalDetailsViewModelTest.kt`, `GoalsViewModelTest.kt`
- `test/.../feature/qaza/MissedPrayersViewModelTest.kt`, `QazaViewModelTest.kt`

## Suggested fix
Inject a `java.time.Clock` (provided by Hilt as `Clock.systemDefaultZone()`) into ViewModels/use cases that need "now", and use `Clock.fixed(...)` in tests. Add explicit tests for a Friday, a month boundary and 23:59→00:00.

## Done when
No test reads the real clock; the suite gives the same result at any date and time.
