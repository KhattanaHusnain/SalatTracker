# 59 — Stats: charts without takeaways; no plain-language insight

**Severity:** Low · **Area:** Stats / product · **Type:** Missing feature · Part of [50](50-stats-redesign-question-led.md)

## Problem
Stats shows numbers and charts but rarely says what they mean. The only text insights are the noisy "weakest prayer/day" callouts (bug 47). Users have to interpret a line chart to learn whether they're improving.

## Proposal
One or two generated sentences at the top of Stats, from a small fixed set of rules with thresholds, e.g.:
- "Fajr improved from 60% to 85% compared with last month."
- "You prayed Isha in congregation 12 times this week — your best yet."
- "You made up 47 Qaza prayers this month."
- "Asr is your most-missed prayer this month (6 missed)."

Show nothing rather than a weak insight. All strings localized with plurals.

## Where it would go
- New pure use case `domain/usecase/StatsInsights.kt` (fully unit-testable)
- `feature/stats/StatsScreen.kt` (insight row under the period selector)

## Done when
A user who glances at Stats for three seconds leaves with one true, useful sentence.
