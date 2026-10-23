# 69 — Tasbih (in progress): issues to address before merging

**Severity:** Medium · **Area:** Tasbih (uncommitted work) · **Pre-merge review**

The Tasbih feature is still uncommitted, so these are notes for review before it lands rather than shipped bugs.

## Problems
1. **No day rollover.** A session's count and "completed" state persist across days. A round finished yesterday blocks counting today (taps are ignored until "New round"). A round left at 20/33 yesterday finishes today and credits **today's** goal, even though most of the dhikr was done yesterday. For a daily goal, users expect a fresh counter each day.
2. **Duplicated due-date rule.** `TasbihRepositoryImpl.isDueForTasbih()` re-implements `SpiritualGoal.isDueOn()` from `domain/usecase/GoalSchedule.kt` on the entity. They match today, but any future change to goal scheduling has to be made twice, or Tasbih and Goals will disagree about whether a goal is due.
3. **Brittle unit check.** Crediting requires `goal.unit == "Time(s)"`, a stored English key. Works today (units are stored as English keys), but a renamed or localized unit would silently stop crediting. Prefer a typed unit or a constant shared with `GoalForm`.
4. **Uncapped credits.** Each completed round inserts a check-in of 1, with no check against the goal's daily target. Ten rounds on a "once a day" goal log 10. Confirm this is intended and that Goals/Stats display it sensibly.

*(Re-evaluation: an earlier point 5, "reachable from the drawer only", was wrong — Goals cards already have a "Count dhikr" button, `GoalsScreen.kt:263`. Removed.)*

## Where
- `app/src/main/java/com/salattracker/data/repository/TasbihRepositoryImpl.kt:36-57, 89-95`
- `app/src/main/java/com/salattracker/feature/tasbih/`
- `app/src/main/java/com/salattracker/domain/usecase/GoalSchedule.kt:18`

## Suggested fix
- Treat a session whose `completedOn` (or last-updated date) is before today as fresh for goal sessions (reset count on first tap of a new day).
- Map the entity to `SpiritualGoal` and reuse `isDueOn`.
- Use a shared unit constant.
- Decide on crediting caps; add tests for rollover and crediting.
