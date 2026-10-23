# 20 — Catch Up writes hundreds of Qaza entries with no confirmation or undo

**Severity:** Medium (downgraded in re-evaluation — the button label already states the count, e.g. "Add 915 prayers", so the write isn't hidden; the gap is recovery from a wrong choice) · **Area:** UX / destructive bulk action

## Problem
In onboarding, "Add 915 prayers" (Last 6 months) immediately writes every prayer in the range as Qaza pending and jumps to the Done screen. There's no confirmation, no summary of what's about to happen, and no undo. The only way to reverse it is to make up or re-mark 900+ prayers by hand later.

Compare: Bulk Update shows a confirmation dialog before writing (`feature/bulkupdate/BulkUpdateScreen.kt`), and the Missed Prayers screen offers a 6-second Undo snackbar.

## Where
- `app/src/main/java/com/salattracker/feature/onboarding/OnboardingViewModel.kt` — `onApplyCatchUp()`
- `app/src/main/java/com/salattracker/feature/onboarding/OnboardingScreen.kt` — `CatchUpAddStep` primary button

## Suggested fix
- Show a confirmation dialog with the date range and count (reuse Bulk Update's dialog pattern/strings).
- On Done, show "Qaza: 915 added · Undo" with an Undo that reverts those entries (set back to NOT_RECORDED / delete) before setup finishes.
- Also fix bug 02 (don't include today's upcoming prayers) so the count shown is accurate.

## Done when
A user can't add hundreds of Qaza entries by a single accidental tap, and can reverse it before leaving onboarding.
