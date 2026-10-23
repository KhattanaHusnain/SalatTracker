# 34 — Quran tab is blank while the Quran text is still being loaded on first launch

**Severity:** Medium · **Area:** QA / first-run experience · **Needs device confirmation of duration**

## Problem
On first launch, `QuranSeedWorker` copies the bundled Quran data (chapters, verses, ~83k Mushaf words, translations, tafsir) into the database in the background. The chapter list observes the database directly; Room emits an empty list straight away, the ViewModel sets `isLoading = false`, and the screen shows nothing — no list, no spinner, no message — until seeding finishes. The chapter list screen doesn't read `isLoading` at all (only its preview does).

A user who opens the Quran tab right after onboarding sees an empty tab and may think the feature is broken. Opening the Mushaf during seeding hits the `check(entities.isNotEmpty())` error path in `MushafRepositoryImpl.getPage` (handled by `runCatching`, so an error state rather than a crash).

## Where
- `app/src/main/java/com/salattracker/feature/quran/QuranChapterListViewModel.kt:49-50`
- `app/src/main/java/com/salattracker/feature/quran/QuranChapterListScreen.kt` (no loading/empty state)
- `app/src/main/java/com/salattracker/notification/QuranSeedWorker.kt` (seeding job)
- `app/src/main/java/com/salattracker/data/repository/MushafRepositoryImpl.kt:21`

## Suggested fix
Observe the seed job's `WorkInfo` (unique name in `QuranSeedWorker`) and show "Preparing the Quran…" with progress while it's running; only show the list (or a real empty state) once it has succeeded. Do the same guard on Reader/Mushaf entry points.

## How to verify
Fresh install → finish onboarding fast → open Quran immediately. Measure how long the tab stays blank on a low-end device.
