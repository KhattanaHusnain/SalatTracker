# 42 — Stats: "Prayed on Time" headline doesn't measure timeliness

**Severity:** Medium · **Area:** Stats / copy

## Problem
The main number is titled **"Prayed on Time"**, but it's `(congregation + individual) / (congregation + individual + qaza)`. The app records nothing about *when* a prayer was prayed — a prayer logged as Individual at the very end of its window counts the same as one at the adhan. Users reasonably read the number as punctuality, which it isn't.

## Where
- `app/src/main/res/values*/strings.xml` — `stats_prayed_on_time` (all four locales)
- `app/src/main/java/com/salattracker/feature/stats/StatsScreen.kt:197`

## Suggested fix
Rename to what it is, e.g. **"Prayers completed"** or **"Prayed (not missed)"**, in all four languages. If on-time tracking is wanted later, it needs a timestamp per entry — a separate feature.

## Done when
The headline describes the metric that's actually computed.
