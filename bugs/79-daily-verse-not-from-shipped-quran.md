# 79 — The daily ayah isn't taken from the shipped Quran (shows a made-up line, no Arabic, not daily)

**Severity:** High · **Area:** Bug / religious content accuracy · **Supersedes [04](04-home-verse-always-english.md)**

## Problem
`VerseRepositoryImpl` is meant to pick a verse from the Quran bundled with the app (Arabic `verses.json` + the `en_sahih` translation, copied into the database by `QuranSeedWorker`). What users actually see on Home, Calendar and the prayer alarm differs from that in four ways:

1. **A line that isn't from the Quran.** If the bundled data isn't in the database yet (first launch, while `QuranSeedWorker` is still running, or if seeding failed and is retrying), `getRandomVerse()` returns a hardcoded `FALLBACK_VERSE`: *"Maintain your prayers with care and devotion."* with no citation. It's shown in the same verse card, in quotation marks, as if it were an ayah. Home loads the verse **once** when its ViewModel is created and never reloads, so on a fresh install this invented line stays for the whole session.
2. **The shipped Arabic is never shown.** The bundled Arabic text (`Verse.textUthmani`) is never read. `VerseInfo` has only `text` and `citation`, so the card shows an English translation with no Arabic at all.
3. **Not daily.** A different random verse is picked on every load: Home, Calendar and the alarm each show a different one, and it changes whenever a screen is recreated (rotation, language change, coming back to the tab). There's no "verse of the day".
4. **Always English** (was bug 04). The Urdu translation (`ur.jalandhry`) ships with the app but isn't used, so Urdu and Arabic users get English.

The pool is also only 8 curated verses (`CURATED_VERSE_KEYS`).

## Where
- `app/src/main/java/com/salattracker/data/repository/VerseRepositoryImpl.kt:16-41` — curated keys, `FALLBACK_VERSE`, random pick, `DEFAULT_TRANSLATION_PACK_ID`
- `app/src/main/java/com/salattracker/domain/model/PrayerModels.kt:42-45` — `VerseInfo(text, citation)` (no Arabic)
- `app/src/main/java/com/salattracker/domain/model/QuranContentModels.kt:22-31` — `Verse.textUthmani` (unused here)
- `app/src/main/java/com/salattracker/feature/home/HomeViewModel.kt:198-202` — loaded once
- `app/src/main/java/com/salattracker/feature/calendar/CalendarViewModel.kt:79`, `notification/PrayerReminderActivity.kt:61` — separate random picks
- `app/src/main/java/com/salattracker/notification/QuranSeedWorker.kt:63-75` — seeding, `Result.retry()` on failure

## Suggested fix
- **Verse of the day:** pick deterministically from the date (e.g. `curated[epochDay % size]`) so every screen shows the same ayah all day. Expand the curated list (e.g. 30–60 prayer-related verses).
- **Arabic + translation:** add `arabic: String` to the verse model and show the Uthmani text first, then the translation in the app language (`ur.jalandhry` for Urdu; `en.sahih` otherwise; Arabic UI can show the Arabic alone).
- **No invented text:** remove `FALLBACK_VERSE`. Read the day's verse straight from the bundled asset files if the database isn't seeded yet, or hide the card until it is.
- **Reload when ready:** expose the verse as a `Flow` (or re-query when the seed job finishes) so Home updates once seeding completes.
- Tests: same date → same verse; Urdu language → Urdu translation; unseeded DB → no fabricated text.

## Done when
Every verse shown in the app is an actual ayah from the bundled Quran, in Arabic with a translation in the user's language, the same on every screen for the whole day.
