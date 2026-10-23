# 39 — Stats: fully-excused days count as failed "Perfect days"

**Severity:** Medium (was High — re-evaluated: wrong numbers on a secondary screen; still fix early) · **Area:** Stats / calculation

## Problem
The Perfect days tile is documented as treating Menstruation days as neutral, but a day where **all five** prayers are marked Menstruation is counted as an imperfect day:

```kotlin
// domain/usecase/StatsSummary.kt — perfectDays()
if (counts.countable == 0 && counts.excused == 0) continue        // only skips days with NO rows
...
counted++
val isPerfect = counts.missed == 0 && counts.prayed > 0 && ...      // prayed == 0 → false
```

For an all-excused day, `countable == 0` but `excused == 5`, so it isn't skipped, `counted` goes up, and `prayed > 0` fails. A woman with 5–7 excused days a month sees "18/30 perfect" when it should be "18/23". Partially-excused days (e.g. 4 prayed + 1 excused) are handled correctly.

## Where
- `app/src/main/java/com/salattracker/domain/usecase/StatsSummary.kt:31-43`
- Test gap: `app/src/test/java/com/salattracker/domain/usecase/StatsSummaryTest.kt` covers a partially-excused day only.

## Steps to reproduce
1. Mark all five prayers of yesterday as Menstruation; mark the day before fully prayed.
2. Stats → 7D: Perfect days shows **1/2**. Expected **1/1**.

## Suggested fix
Skip any day with nothing countable: `if (counts.countable == 0) continue`. Add a test for an all-excused past day and an all-excused today.

## Done when
Excused days never change the Perfect days numerator or denominator.
