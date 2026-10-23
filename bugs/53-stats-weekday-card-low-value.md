# 53 — Stats: By weekday card gives no actionable information

**Severity:** Low · **Area:** Stats / product · Part of [50](50-stats-redesign-question-led.md)

## Problem
On 30D each weekday has about four days of data, so one missed prayer decides which day is "weakest". Even with more data, "Wednesday is your weakest day" rarely leads to a change in behaviour. The card takes a full screen section, shows no numbers (bug 48), and its callout is noisy (bug 47).

## Where
- `app/src/main/java/com/salattracker/feature/stats/StatsScreen.kt` — `WeekdayCard`, `WeekdayBar`
- `app/src/main/java/com/salattracker/domain/usecase/StatsSummary.kt` — `weekdayCompletion`, `weakestWeekday`

## Suggested fix
Remove the card (and its now-unused use-case functions and tests). If a weekday pattern is wanted later, surface it only as a plain-language insight (bug 59) when it's strong and well-sampled (e.g. a 90-day window and a ≥ 15-point gap).

## Done when
Stats has no section whose output is mostly sampling noise.
