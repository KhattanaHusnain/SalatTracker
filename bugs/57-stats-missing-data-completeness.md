# 57 — Stats: no indication of how complete the data is

**Severity:** Medium · **Area:** Stats / product · **Type:** Missing feature · Part of [50](50-stats-redesign-question-led.md)

## Problem
Every Stats percentage is computed only from prayers that were logged (bug 40). A user who logged 40 of 150 prayers in a month and prayed all 40 sees **100%**, with nothing telling them the number rests on a quarter of their prayers. Without coverage, the headline can't be trusted.

## Proposal
Under the headline: **"Based on 132 of 150 prayers logged"**, counting expected prayers from the later of the period start and the user's first recorded day, excluding today's not-yet-due prayers.
- If coverage < ~70%, show a gentle prompt: "Log past prayers in Calendar or Bulk Update for accurate stats."
- Excused (Menstruation) prayers count as logged.

## Where it would go
- `domain/usecase/StatsSummary.kt` (new `loggingCoverage(...)`, needs prayer times or a fixed 5/day)
- `feature/stats/StatsViewModel.kt`, `StatsScreen.kt`

## Done when
Every percentage on Stats is accompanied by how much data it's based on.
