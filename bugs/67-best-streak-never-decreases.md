# 67 — "Best streak" can never go down, so a correction leaves a wrong record forever

**Severity:** Medium · **Area:** Correctness / streaks

## Problem
Streak recalculation starts each prayer's `highest` from the **previously cached** best value and only ever raises it:

```kotlin
StreakTrack(highest = existing[type]?.highestStreak ?: 0)
...
if (track.current > track.highest) track.highest = track.current
```

So any mistake that once produced a long streak is permanent. The easiest way to make that mistake is built into the app: **Bulk Update defaults to "Congregation"**. A user who bulk-fills six months while setting up, then realises the status was wrong and changes it, keeps "Best: 180" on Home forever. The same applies to restores (the backup carries `highestStreak` and recalculation keeps it).

## Where
- `app/src/main/java/com/salattracker/data/repository/PrayerRepositoryImpl.kt:157-206` (`recalculateAndCacheStreak`)
- `app/src/main/java/com/salattracker/feature/bulkupdate/BulkUpdateViewModel.kt:48` (`selectedStatus = CONGREGATION` default)
- `app/src/main/java/com/salattracker/data/backup/BackupFormat.kt` (`highestStreaks` restored)

## Suggested fix
- Compute the best streak from history (a single pass over all entries per prayer type, oldest → newest) instead of ratcheting a cached value. The cache can still store the result.
- Make Bulk Update's default status neutral (no preselection, or Individual), so a bulk write is a deliberate choice.
- Add tests: correcting a past day lowers the best streak; restore + recalc yields the history-derived best.

## Done when
"Best streak" always matches what the recorded history actually shows.
