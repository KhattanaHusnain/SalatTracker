# 62 — Quran Bookmarks and Reading History screens can't be reached, and bookmarks can't be created

**Severity:** Medium · **Area:** Feature value / dead feature

## Problem
- `Routes.QURAN_BOOKMARKS` and `Routes.QURAN_READ_HISTORY` are registered in the nav graph, but **nothing navigates to them**: no button, menu item or drawer entry pushes either route.
- `QuranReaderViewModel.onBookmarkToggle()` exists but **is never called**. The bookmark icon was removed from the reader ("removed bookmark and tafseer icon for now"), so no new bookmark can be created.
- Reading history *is* recorded on every page view (`recordRead` in the Reader and Mushaf ViewModels), but the user can never see the list. It's only used indirectly by the Quran tab's resume action.

The result is two complete screens with ViewModels, DAOs and strings in four languages that deliver zero user value, plus the unaddressed delete/clear issues in bug 21.

## Where
- `app/src/main/java/com/salattracker/feature/main/MainActivity.kt:400-405` (routes registered)
- `app/src/main/java/com/salattracker/feature/quran/QuranReaderViewModel.kt:132` (`onBookmarkToggle`, no callers)
- `app/src/main/java/com/salattracker/feature/quran/QuranBookmarks*.kt`, `QuranReadHistory*.kt`

## Decision needed
- **Option A — make them useful:** add a bookmark action back to the reader (long-press an ayah, or a top-bar icon), and add "Bookmarks" and "History" entries to the Quran tab's top bar or overflow menu.
- **Option B — remove them:** delete the two screens, their ViewModels, routes and strings. Keep `recordRead` only if the resume action needs it.

## Done when
Every Quran screen in the nav graph can be reached and does something, or it's gone.
