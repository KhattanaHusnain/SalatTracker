# 63 — Mushaf fonts make the app 231 MB; cost far exceeds value for most users

**Severity:** High · **Area:** Feature value / install size & distribution

## Problem
The debug APK is **231 MB**. Of that, **200 MB** is the 604 per-page QCF fonts for Mushaf mode (`assets/quran/mushaf_fonts/`), plus 30 MB of other Quran data. Everyone downloads the full 231 MB, including people who only want prayer times and tracking, and who already read the Quran in a dedicated app.

Consequences:
- **Install conversion:** larger downloads measurably reduce installs, especially on mobile data in the app's target markets.
- **Play Store limits:** an app bundle's base module has a maximum compressed download size (currently 200 MB). Uncompressible font assets this large risk the upload failing, and Play warns users before large mobile-data downloads.
- **Updates:** unless only the code changes, every update re-downloads a large app.
- **Storage:** the data is also copied into the database on first run (bug 34), roughly doubling its on-device footprint.

## Where
- `app/src/main/assets/quran/mushaf_fonts/v2/p1..p604.ttf` (200 MB)
- `app/src/main/assets/quran/mushaf_words.json` (12 MB)
- `app/build.gradle.kts` (`androidResources { noCompress += "ttf" }`)

## Suggested fix
Keep the base app small (~20–30 MB) and fetch Mushaf assets only for users who open Mushaf mode:
- **Play Asset Delivery** on-demand asset pack for `mushaf_fonts` (and `mushaf_words`), downloaded the first time Mushaf mode is opened, with progress UI. Still fully offline afterwards.
- Alternatively, an in-app download from a CDN (Quran Foundation's font CDN is already the documented source in `QURAN_CONTENT_SOURCES.md`), with the same licence notes.
- Show "Mushaf mode needs a one-time 200 MB download" with Wi-Fi-only by default.

## Done when
A fresh install is under ~30 MB, and Mushaf mode downloads its data on first use and then works offline.
