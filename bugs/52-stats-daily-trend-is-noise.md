# 52 — Stats: daily trend line is mostly noise

**Severity:** Medium · **Area:** Stats / product · Part of [50](50-stats-redesign-question-led.md)

## Problem
On 7D/30D/40D the trend plots one point per day. A day has only five prayers, so each point can only be 0, 20, 40, 60, 80 or 100%. The line is either jagged or, for a consistent user, flat at 100. It doesn't show whether someone is improving. Axis issues are covered separately in bug 41.

## Where
- `app/src/main/java/com/salattracker/domain/usecase/StatsSummary.kt` — `trendBucketFor` (daily ≤ 40 days)
- `app/src/main/java/com/salattracker/feature/stats/StatsScreen.kt` — `TrendCard`

## Suggested fix
- Always bucket weekly (or a 7-day rolling average).
- Show the card only when there are at least 4 weeks with data; otherwise show "Your trend appears after 4 weeks of tracking".
- Pair it with a one-line insight (bug 59) so the chart has a takeaway.

## Done when
The trend answers "am I improving?" at a glance and doesn't render meaningless day-to-day jumps.
