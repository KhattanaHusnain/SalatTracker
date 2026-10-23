# 64 — Bundled Quran data that no screen uses (tafsir, word-by-word, word segments)

**Severity:** Medium · **Area:** Feature value / dead weight

## Problem
Three bundled datasets are copied into the database on first launch but never shown anywhere:

| Asset | Size | Status |
|---|---|---|
| `assets/quran/word_segments.json` | 6.1 MB | Seeded; no screen reads word segments |
| `assets/quran/wordbyword/` | 5.2 MB | Seeded; no screen shows word-by-word |
| `assets/quran/tafsirs/` | 2.8 MB | Seeded and **loaded by `QuranReaderViewModel` on every chapter open**, but the tafsir UI was removed — the query runs for nothing |

That's ~14 MB of install size, extra first-launch seeding time (which feeds bug 34), and a wasted query per chapter, for no user-visible value.

## Where
- `app/src/main/assets/quran/{word_segments.json, wordbyword/, tafsirs/}`
- `app/src/main/java/com/salattracker/notification/QuranSeedWorker.kt` (seeds them)
- `app/src/main/java/com/salattracker/feature/quran/QuranReaderViewModel.kt:64-68` (`tafsirs` loaded, never rendered)

## Decision needed
- **Use them:** bring back a tafsir sheet (tap an ayah → Tafsir Al-Muyassar), or a word-by-word mode.
- **Or remove them** from assets, the seed worker and the reader ViewModel until a feature needs them. If tafsir returns later, ship it as a downloadable pack (the app already has the pack-database architecture for this).

Keep the licensing notes in `QURAN_CONTENT_SOURCES.md` in sync either way.

## Done when
Every bundled dataset is used by a visible feature, or isn't shipped.
