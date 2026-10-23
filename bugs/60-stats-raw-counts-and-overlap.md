# 60 — Stats: raw status counts and Perfect days add little; overlap with Home

**Severity:** Low · **Area:** Stats / product · Part of [50](50-stats-redesign-question-led.md)

## Problem
- **Status bar + four-item legend** ("Congregation: 18 · Individual: 7 · Qaza done: 3 · Qaza: 2") lists raw counts that don't suggest any action. The one meaningful split, Congregation share, deserves its own card (bug 56).
- **Perfect days (x/y)** is motivating but overlaps Home's per-prayer streaks and week rings. It's also currently wrong for female users (bug 39).

## Where
- `app/src/main/java/com/salattracker/feature/stats/StatsScreen.kt` — `SummaryCard` (`StatusBar`, `LegendCount`, `StatTile` for perfect days)

## Suggested fix
- Keep the stacked bar as a compact visual under the headline, but drop the four-count legend. Move counts into a tap-to-expand detail or TalkBack description (bug 48).
- Keep Perfect days as one small tile once bug 39 is fixed. Consider showing "best perfect-day run" instead of x/y, so it complements Home's streaks rather than repeating them.

## Done when
The summary card shows one headline and at most two supporting numbers, each with a clear meaning.
