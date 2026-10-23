# 40 — Stats: unrecorded past prayers are invisible, inflating the completion %

**Severity:** Medium (was High — re-evaluated: inflated numbers on a secondary screen) · **Area:** Stats / calculation

## Problem
Completion % is `prayed / (prayed + qaza)`. A prayer with no entry at all isn't in either number. The only thing that turns a forgotten prayer into a counted miss is `QazaAutoMarkWorker`, and it only looks at **today and yesterday**. If the worker doesn't run for two days (phone off, battery restriction, force-stop, restricted standby bucket), everything older stays unrecorded forever and Stats simply ignores it.

Effect: a user who stops logging for a week and comes back still sees e.g. "92% prayed"; those days disappear from the trend line, By prayer and By weekday too. Perfect days excludes them as "nothing recorded".

## Where
- `app/src/main/java/com/salattracker/notification/QazaAutoMarkWorker.kt` — `markOverdue(today.minusDays(1))`, `markOverdue(today)` only
- `app/src/main/java/com/salattracker/domain/repository/PrayerRepository.kt` — `StatusCounts.countable` excludes unrecorded
- `app/src/main/java/com/salattracker/data/local/dao/PrayerDao.kt` — `getDailyStatusCounts` returns only dates with rows

## Suggested fix (needs a product decision)
- **Option A:** make the auto-mark worker catch up on every day since it last ran (store a "last processed date"), so gaps are always filled.
- **Option B:** in Stats, count past prayers with no entry since the user's first recorded day as "Not recorded" and show them in the summary (and in the denominator, or at least as a visible count).

## Done when
A week with nothing logged shows up in Stats as missed or not recorded, not as nothing.
