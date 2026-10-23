# 51 — Stats: "Qaza made up" tile measures the wrong thing

**Severity:** Medium · **Area:** Stats / product · Part of [50](50-stats-redesign-question-led.md)

## Problem
The tile shows `qazaCompleted / missed` for prayers **whose original date falls in the selected period**. "3/5" on 30D means "of the 5 prayers missed in the last 30 days, 3 are now made up". Users read it as their Qaza progress overall. A user who made up 200 old prayers this month but missed 5 recent ones sees "0/5".

The question users actually have ("how much do I still owe, and is it going down?") is answered on the Qaza tab (`QazaProgress`: outstanding, pace, ETA), not here.

## Where
- `app/src/main/java/com/salattracker/feature/stats/StatsScreen.kt:244-248`

## Suggested fix
Remove the tile. Replace it with the Qaza recovery card from bug 58 (backlog trend + "made up this period" by *made-up date*), linking to the Qaza tab.

## Done when
Nothing on Stats suggests overall Qaza progress using only prayers missed inside the period.
