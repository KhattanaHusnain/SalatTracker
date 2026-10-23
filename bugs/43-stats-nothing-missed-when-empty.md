# 43 — Stats: "Nothing missed" shown when nothing was recorded

**Severity:** Low · **Area:** Stats / empty states

## Problem
The "Qaza made up" tile shows **"Nothing missed"** whenever `missed == 0`, including when the period has no records at all (a brand-new user, or a period before they started). It congratulates the user right next to "—" and "No prayers recorded in this period".

## Where
- `app/src/main/java/com/salattracker/feature/stats/StatsScreen.kt:244-248`

## Suggested fix
Show "—" when `counts.countable == 0`; only show "Nothing missed" when there's countable data and `missed == 0`.

## Done when
An empty period shows "—" in every tile.
