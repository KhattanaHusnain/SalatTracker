# 21 — Deleting a Quran bookmark or clearing reading history is instant, with no confirmation or undo

**Severity:** Low (downgraded in re-evaluation) · **Area:** UX / destructive actions · **Blocked by [62](62-quran-bookmarks-history-unreachable.md)** — both screens are currently unreachable, so no user can hit this. Fix together with 62 if those screens are kept; close if they're removed.

## Problem
- **Bookmarks:** tapping the trash icon on a bookmark deletes it immediately. The icon sits right next to the row's tap target, so an accidental tap loses the bookmark (and any note) with no way back.
- **Reading history:** "Clear history" wipes the entire history in one tap.

Elsewhere the app protects destructive actions (Goal delete, Mosque delete and Backup disconnect use a confirm dialog; Missed Prayers uses Undo), so these two are inconsistent.

## Where
- `app/src/main/java/com/salattracker/feature/quran/QuranBookmarksScreen.kt:89` → `QuranBookmarksViewModel.onDelete`
- `app/src/main/java/com/salattracker/feature/quran/QuranReadHistoryScreen.kt:58` → `QuranReadHistoryViewModel.onClearHistory`

## Suggested fix
- Bookmark delete: Undo snackbar (keep the deleted row in memory for ~6 s), matching Missed Prayers.
- Clear history: confirmation dialog ("Clear all reading history? This can't be undone.").

## Done when
Neither action can lose data from a single accidental tap.
