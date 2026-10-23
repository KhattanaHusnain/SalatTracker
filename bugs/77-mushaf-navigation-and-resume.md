# 77 — Mushaf: no swipe, spinner on every page, stuck at Juz end, position not always saved

**Severity:** Medium · **Area:** UX / Quran Mushaf mode

## Problems
1. **No swipe to turn pages.** Pages change only via two small chevron buttons in the bottom bar. There's no swipe or pager anywhere in the Quran feature, though swiping is how every Mushaf app (and a physical Mushaf) works.
2. **Full-screen spinner on every page turn.** `loadPage` sets `isLoading = true`, which replaces the page with a centred spinner until the next page loads, so each turn flashes. Neighbouring pages aren't preloaded.
3. **Stuck at the end of a Juz.** Opened from the Juz list, the Mushaf is limited to that Juz's pages (`firstPageNumber..lastPageNumber`). At its last page "next" is disabled; the reader has to go back and pick the next Juz.
4. **Reading position only saved for Juz-opened pages.** `recordRead` runs only when `selectedJuzNumber != null`. Mushaf opened from the reader's Mushaf button (`Routes.quranMushaf(page)` without a Juz) never updates the resume position.
5. Error text "Couldn't load this page" is hardcoded English (see also bug 12).

## Where
- `app/src/main/java/com/salattracker/feature/quran/QuranMushafScreen.kt` (no gestures)
- `app/src/main/java/com/salattracker/feature/quran/QuranMushafViewModel.kt:58-105`
- `app/src/main/java/com/salattracker/feature/main/MainActivity.kt:325, 557-567` (bottom-bar controls)

## Suggested fix
- Use `HorizontalPager` (RTL page order) with the existing chevrons kept as a secondary control.
- Keep the current page visible while the next loads; prefetch ±1 page.
- Let a Juz-opened Mushaf continue past the Juz end (the Juz only sets the starting page).
- Record the read position for every page shown, with or without a Juz.

## Done when
A user can read continuously from any page by swiping, without flashes, and Resume always returns to the last page seen.
