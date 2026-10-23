# 04 — Home verse is always the English translation

> **Merged into [79](79-daily-verse-not-from-shipped-quran.md)** (2026-10-23). The language problem is one of four issues with the daily verse; track and fix them together there.

**Severity:** Medium (was High — re-evaluated: decorative content, not core function)

## Problem
The verse card on Home (and the full-screen alarm) always uses the English translation, even though the Urdu translation ships with the app and the rest of the UI follows the chosen language.

## Where
- `app/src/main/java/com/salattracker/data/repository/VerseRepositoryImpl.kt:34` — `getTranslationText(DEFAULT_TRANSLATION_PACK_ID, …)` where `DEFAULT_TRANSLATION_PACK_ID = "en.sahih"`.
- Same file, line 21 — `FALLBACK_VERSE` is an English literal.
- `DEFAULT_URDU_TRANSLATION_PACK_ID = "ur.jalandhry"` exists in `domain/model/QuranModels.kt:18` but isn't used here.

## Suggested fix
- Read `settingsRepository.getCurrentPrefs().language` in `VerseRepositoryImpl` and pick the translation pack: Urdu → `ur.jalandhry`, others → `en.sahih` (until Arabic/French packs exist — for Arabic, consider showing the Arabic verse text itself).
- Move the fallback text to a string resource.

## Done when
With the app in Urdu, Home and the alarm show the Urdu translation; the fallback is localized.
