# 68 — Every prayer save recalculates streaks with one database query per day of history

**Severity:** Medium · **Area:** Performance · **Needs measurement on a device**

## Problem
`upsertPrayerStatus` and `bulkUpsertPrayerStatus` both call `recalculateAndCacheStreak()`, which walks backwards from today **one day at a time with a separate query per day** (`prayerDao.getEntriesForDate`) until all five streaks have broken. For a consistent user with a 1-year streak that's 365+ sequential queries on **every** status tap. It also runs:
- every 15 minutes from `QazaAutoMarkWorker`, once per auto-marked prayer;
- after every Undo on the Missed Prayers screen.

Each recalculation also re-emits `observeStreaks()`, which makes Stats reload its overview and breakdown queries (bug 49), so a single tap on Home fans out into hundreds of queries. The cost grows with how well the user is doing.

## Where
- `app/src/main/java/com/salattracker/data/repository/PrayerRepositoryImpl.kt:50, 68, 157-206`
- `app/src/main/java/com/salattracker/feature/stats/StatsViewModel.kt:184-195`

## Suggested fix
- Load the relevant range in one query (e.g. all entries since the earliest still-active streak start, or the whole table, which is small) and walk it in memory.
- Or update streaks incrementally: a change to date D only affects streaks that span D.
- Let Stats observe a cheaper change signal than the streak recalculation.

## How to verify
Seed 365 days of Congregation in a test database, time `upsertPrayerStatus` for today before and after; target < 20 ms.
