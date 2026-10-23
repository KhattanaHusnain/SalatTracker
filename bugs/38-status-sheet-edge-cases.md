# 38 — Prayer status sheet edge cases (save with nothing chosen; hidden current status)

**Severity:** Low · **Area:** QA / edge cases

## Problems
1. **Save is enabled before a status is chosen.** For an unrecorded prayer the sheet pre-selects `NOT_RECORDED` (which isn't one of the visible options), so no row is highlighted but "Save status" is still enabled. Tapping it writes a `NOT_RECORDED` entry and closes the sheet — it looks like a successful save but nothing changed.
2. **Current status hidden by gender.** If a day was recorded as Congregation and the user is now set to Female (or Menstruation and Male), that status is filtered out of the list. The sheet opens with no row highlighted, and Save re-saves the hidden status.

## Where
- `app/src/main/java/com/salattracker/core/ui/PrayerStatusBottomSheet.kt` — `selected` initial value, `enabled = !isSaving` on Save, `isOfferedFor` filter

## Suggested fix
- Disable Save until `selected` is one of the visible options (and differs from the current status).
- When the current status is hidden by gender, still show it (marked as current) so the user understands what's recorded, but don't offer it as a new choice — or show a one-line note.

## Done when
Save can only commit a visible, deliberate choice, and the sheet always explains the day's current status.
