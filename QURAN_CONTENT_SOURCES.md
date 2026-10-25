# Quran content sources — licensing research

_Researched: 2026-10-21, for the full Quran reader feature (see the on-device prayer-times and
Ja'fari-school precedent for why this matters: content that ships inside the app needs its own
clear right to be there, independent of whatever app's code inspired the feature)._

**Why this file exists.** [AlfaazPlus/QuranApp](https://github.com/alfaazplus/quranapp) is
GPL-3.0 licensed end-to-end, including the `inventory/` folder where its Quran text,
translations and other Quran data live. That blanket license doesn't establish that
AlfaazPlus holds relicensable rights over that content — most of it originates from, and stays
governed by, its own original source (a translator, a tafsir author, a reciter), independent of
the app code sitting around it. So this document sources each content type directly from its own
origin and records exactly what was found, rather than treating "it's in a GPL repo" as a
clearance. Same discipline this project already applies elsewhere — see `FUTURE_FEATURES.md`'s
"Content licences" section (hadith-api confirmed Unlicense; Hisnul Muslim adhkar data left
unresolved and un-built until a source is confirmed).

**How to read this:** ✅ cleared enough to bundle, with the stated conditions honoured. ⚠️
candidate, but needs one more confirmation before use. 🚫 flagged — a real problem was found, not
just "unconfirmed."

---

## Arabic text (Uthmani script)

✅ **[Tanzil Project](https://tanzil.net)** — confirmed directly from `tanzil.net/download/`'s own
terms of use:
> "Permission is granted to copy and distribute verbatim copies of the Quran text provided here, but **changing it is not allowed**... provided that its source (Tanzil Project) is clearly indicated, and a reference is made to this website (tanzil.net)."

So: copy/distribute freely, verbatim only, credit "Tanzil Project" + link to tanzil.net somewhere
in the app (an About/credits screen, same place the app already credits the AlAdhan API and
Quran.com for the verse-of-the-day feature). No commercial-use restriction stated. This is the
de facto standard source for this exact reason — most other Quran apps' Arabic text traces back
to Tanzil, directly or indirectly (confirmed in fawazahmed0/quran-api's own `References.md`,
which lists `tanzil.net/trans/` among its sources).

✅ **Backup/alternate: [alquran.cloud](https://alquran.cloud)** (Islamic Network's API) — its own
terms page states text may be reproduced freely for non-commercial use, and commercially with
attribution as "the minimum courtesy," provided the Uthmani diacritics/orthography stay intact
and the text isn't altered or mixed with non-Quranic material in a confusing way. Notable because
the *same* API also serves translations and audio (below) — worth evaluating as a single
consolidated source rather than stitching three separate ones together, once Phase 3 gets to
actual integration.

🚫→✅ **Correction (2026-10-22):** the bundled `chapters.json`/`verses.json` assets were
originally fetched from **api.quran.com** (Quran Foundation's own v4 API), reasoning that the
underlying *text* traces back to Tanzil. That reasoning doesn't hold up: fetching through Quran
Foundation's API makes the *act of fetching* subject to Quran Foundation's own developer terms
(`api-docs.quran.foundation/legal/developer-terms/`), independent of the text's own copyright
status — and those terms explicitly prohibit it: *"QF Content is not sold, sublicensed, or
redistributed"*, caching is capped at one week outside their separate Content Sync mechanism, and
even Content Sync *"does not itself authorize distributing a prepackaged database or build-time
bundle."* Building a permanent bundled asset from regular API responses, which is exactly what
this app does, is squarely what that forbids.

**Fixed:** both assets were regenerated from **alquran.cloud** instead (edition `quran-uthmani`
for the Arabic text, same source already cleared above and already used for the translation) —
a provider whose own terms explicitly permit storing/embedding. Per-verse juz/page/sajda came
directly from alquran.cloud's response; hizb number was derived from its `hizbQuarter` field
(`hizb = ceil(hizbQuarter / 4)`, since each hizb is 4 quarters). The one field alquran.cloud
doesn't expose — revelation order — was kept at its original values, re-justified as independent,
publicly-documented scholarly data (the "Egyptian Standard Chronological Order") rather than
QF-proprietary content: cross-checked against Wikipedia's *List of chapters in the Quran*, which
matches exactly (Al-Fatihah 5, Al-Baqarah 87, Al-'Alaq 1, An-Nas 21, etc.).

**The lesson, for the rest of this document and this project generally:** "the content is freely
licensed" and "the API you fetched it through has its own usage contract" are two separate
questions, and both need checking — a source clearing the *content* doesn't clear whatever
*service* happened to hand it to you.

## Translations

⚠️ **[fawazahmed0/quran-api](https://github.com/fawazahmed0/quran-api)** — confirmed **Unlicense**
at the repo level (checked via `gh api repos/fawazahmed0/quran-api` → `license.spdx_id: "Unlicense"`,
same author/pattern as the `hadith-api` already cited in `FUTURE_FEATURES.md`). It aggregates
440+ translations from many original sources (its own `References.md` lists Tanzil, GlobalQuran,
Quran.com, and a dozen more). The repo's own Unlicense doesn't by itself establish the right to
relicense every individual translator's copyrighted work — same caveat as above. Treat this as a
**convenient catalog/discovery source**, not final legal authority per translation; confirm each
individual translation's own stated terms before bundling it. Many of the obvious first picks
(Saheeh International, Dr. Mustafa Khattab's *The Clear Quran*) are translations their own
authors/publishers have explicitly released for free Islamic distribution — good starting
candidates, still worth a one-line confirmation each when actually added.

✅ **[GlobalQuran/data](https://github.com/GlobalQuran/data)** — confirmed **Apache-2.0** at the
repo level. Plain-text format, one ayah per line, with a metadata header on every file naming the
translator, language, and original source — exactly the per-item provenance the fawazahmed0
aggregator lacks. Best-documented option found; a strong first choice for the initial translation
packs.

✅ **alquran.cloud** (see above) — explicit terms: free for non-commercial use; commercial use
permitted with the translator attributed by name on republish. Clean, single-source legal story.

**Recommendation:** start the first translation packs from GlobalQuran/data and/or alquran.cloud
(both have clear, direct terms); use fawazahmed0/quran-api as a cross-check/catalog of what
exists, not as the primary source to copy bytes from.

**First translation shipped (2026-10-21): Saheeh International (`en.sahih`).** Fetched via
alquran.cloud's `v1/quran/en.sahih` endpoint — the same cleared source as above — all 6,236
verses, bundled at `app/src/main/assets/quran/translations/en_sahih.json`. Saheeh International
is one of the most widely free-distributed English translations (its own publisher, Al-Muntada
Al-Islami, makes it freely available for Islamic dawah purposes), consistent with this being a
safe first pick per the per-translation confirmation this file calls for.

**Urdu translation shipped (2026-10-22): Fateh Muhammad Jalandhry (`ur.jalandhry`).** Fetched via
alquran.cloud's `v1/quran/ur.jalandhry` endpoint — same cleared source and terms as above — all
6,236 verses, bundled at `app/src/main/assets/quran/translations/ur_jalandhry.json`. This is the
new default translation shown in the main reader (see the "Mushaf-style per-verse reader" note
under Word-by-word below) — Jalandhry is one of the most widely used, broadly-accepted Urdu
translations (also independently hosted by Tanzil.net itself), making it the safest neutral
default among the several Urdu editions alquran.cloud offers (others include Ahmed Raza Khan's
Kanzul Iman and Abul A'ala Maududi's, which carry more specific sectarian/organisational
associations).

## Removed optional content (2026-10-25)

The initial tafsir and word-by-word datasets were removed from the application because their
seeded data and reader UI were unused. They are not bundled, downloaded, or displayed. The
research below remains as provenance only if either feature is reintroduced with an explicit
user-facing implementation and renewed licence review.

### Tafsir

⚠️ **[Quran-Tafseer/tafseer_api](https://github.com/Quran-Tafseer/tafseer_api)** — the API
server's own repo is confirmed **MIT**. That covers the server code, not necessarily the tafsir
*text* it serves. Not used — superseded by the cleared source below, found in the follow-up pass.

✅ **[QuranEnc.com](https://quranenc.com)** (Encyclopedia of the Noble Quran) — confirmed directly
from its own pages (checked on both a translation page and a tafsir page, e.g.
`quranenc.com/en/browse/arabic_moyassar/2/1`), the same explicit, numbered terms appear site-wide:

> "Contents of the translations can be downloaded and re-published, with the following terms and conditions: 1. No modification, addition, or deletion of the content. 2. Clearly referring to the publisher and the source (QuranEnc.com). 3. Mentioning the version number when re-publishing the translation. 4. Keeping the transcript information inside the document. 5. Notifying the source (QuranEnc.com) of any note on the translation. 6. Updating the translation according to the latest version issued from the source (QuranEnc.com). 7. Inappropriate advertisements must not be included when displaying translations of the Noble Quran."

Workable conditions for an app: no modification, attribution to QuranEnc.com, note the version
number, periodic re-sync to their latest version, no inappropriate ads. QuranEnc hosts several
tafsirs under this same site-wide policy — **At-Tafsir Al-Muyassar** (King Fahd Complex, Madinah)
is the first pick: an official, modern, verse-by-verse Saudi government tafsir, widely used
across Quran apps. Also available under the same terms: Tafsir al-Saadi, Ibn Kathir, Al-Tabari,
Al-Mukhtasar. A real JSON API exists for bulk fetching, confirmed working:
`quranenc.com/api/v1/translation/sura/arabic_moyassar/{sura}` returns one entry per verse with
`arabic_text` + `translation` (the tafsir commentary) fields — the same shape as the translation
fetch already used for Saheeh International, so building this pack followed the established
pattern. This content is not currently bundled or displayed.

### Word-by-word (morphology + per-word gloss)

🚫 **[Quranic Arabic Corpus](https://corpus.quran.com)** (University of Leeds) — confirmed
**GPL v3** directly from its own `license.jsp`. Bundling GPL-licensed *data* into Istiqamah
risks the same copyleft entanglement this whole plan exists to avoid with QuranApp's GPL *code*.
**Not used.**

🚫 **[QuranWBW.com](https://quranwbw.com)** — its own site states *"All the data used on this
website belongs to their respective owners/authors and copying them is not allowed"* — more
restrictive than merely unconfirmed. **Not used.**

✅ **alquran.cloud** (see Arabic text / Translations above) — carries its own word-by-word
editions (`quran-wordbyword`: Arabic word + English gloss; `quran-wordbyword-2`, explicitly
credited to "Dr. Shehnaz Shaikh" — the *same original compiler* the Quranic Arabic Corpus itself
credits as its source before the Corpus's own GPL-licensed modifications: *"derived from work
originally compiled by Shehnaz Shaikh and Kausar Khatri, but has been modified and improved"*).
Fetching this data from alquran.cloud instead of corpus.quran.com sidesteps the GPL entirely —
it's a different distributor serving a different layer of the same underlying compiled work,
under alquran.cloud's own already-cleared terms (store/embed freely, non-commercial; commercial
with attribution), not the Corpus's GPL. Confirmed working via
`api.alquran.cloud/v1/ayah/{verseKey}/quran-wordbyword`, returning one `$`-delimited entry per
word (Arabic text, English gloss, plus positioning numbers for their own font rendering that
this app doesn't need). This content is not currently bundled or displayed.

🚫 **Urdu word-by-word — searched 2026-10-22, not found with clear terms.** A traditional
lafzi-tarjuma (word-by-word Urdu gloss) layout was requested, matching classic Urdu Mushaf prints.
Checked: **QUL (qul.tarteel.ai)**'s one "Urdu wbw translation" resource traces straight back to
**QuranWBW.com** per QUL's own Credits page — the exact source already rejected above for this
project ("copying them is not allowed"); QUL re-hosting it doesn't clear that restriction, so it's
equally unusable. **QuranEnc.com** was also checked directly (same already-cleared vendor as the
Arabic tafsir) — it has Urdu content (`urdu_junagarhi` full translation, `urdu_saadi` tafsir) but
no word-by-word resource for any language. Several GitHub/HuggingFace Quran datasets were checked
too; none had a clearly-licensed Urdu word-level dataset. **Not built** — the Mushaf-style reader
(below) ships with full-verse Urdu translation only; a true per-word Urdu gloss stays unresolved
until a source is confirmed, same discipline as `FUTURE_FEATURES.md`'s unresolved Hisnul Muslim
entry.

## Mushaf-style per-verse reader (QCF font + Urdu translation)

The main reader (`QuranReaderScreen`) was redesigned (2026-10-22) to render each verse's Arabic
using the same bundled QCF V2 glyph data as Mushaf mode (print-accurate, not a reflowing Unicode
font) with the Urdu translation (`ur.jalandhry`, above) shown beneath — no new content sourcing
beyond what's documented above, just a new rendering/layout approach reusing already-cleared data.
It was later reworked to flow real printed Mushaf *lines* (reusing `MushafRepository.getLinesForChapter`)
rather than one block per verse, so a verse that doesn't fit on one line continues naturally onto
the next, same as print — and a verse's translation only appears once, after the line it actually
finishes on.

## Display fonts (surah name + Urdu text)

✅ **[Amiri Quran](https://github.com/aliftype/amiri)** (Khaled Hosny — SIL OFL-1.1, confirmed via
`gh api repos/aliftype/amiri` → `license.spdx_id: "OFL-1.1"`) — a classical Naskh revival
typeface specifically tuned for Quranic text, used for the chapter-name banner and the Bismillah
line. The *official* Uthmani Unicode font (KFGQPC Hafs / "UthmanicHafs") was considered first
since it would have matched the QCF body-text style exactly, but its own license explicitly
prohibits reproduction/modification without the Complex's written approval — the same restriction
already ruled out a different KFGQPC font earlier in this document — so it wasn't used. The bundled
per-page QCF fonts themselves can't substitute either: they're pure PUA glyph-substitution fonts
for their own page's specific word codes (see `MushafRepository`), not general-purpose Unicode
Arabic text renderers, so they can't render an arbitrary string like a chapter name at all.

✅ **[Noto Nastaliq Urdu](https://github.com/notofonts/NotoNastaliqUrdu)** (Google Fonts — SIL
OFL-1.1, confirmed the same way) — replaces the plain system-fallback rendering the Urdu
translation text used until now with an authentic Nastaliq-script face, the traditional and
preferred style for Urdu readers.

Both fonts are credited on `QuranCreditsScreen` per the OFL's attribution requirement.

## Recitation audio

✅ **alquran.cloud** — the clearest audio terms found: personal/educational streaming and
download are always permitted; commercial bundling is allowed, but **each reciter individually
holds copyright on their recording and may request its removal** — meaning any reciter picked
needs a documented "remove on request" path in the app (a real product requirement, not just
paperwork — worth a line in Phase 5's design when recitation download actually gets built).

⚠️ **EveryAyah.com / QuranicAudio.com** — long-standing free hosting used by most Quran apps
(including QuranApp), but no explicit written terms were found on either site in this pass.
Treat as a secondary/fallback source, not primary legal authority, unless their own stated terms
turn up later.

## Mushaf page layout + glyph font (print-accurate paginated reader)

✅ **[Quran Foundation API](https://api-docs.quran.foundation)** (King Fahd Glorious Quran
Printing Complex's official digital distributor) — this is a deliberate exception to the "don't
build a prepackaged bundle from Quran Foundation's API" correction above: its separate
[Mushaf Fonts and Images terms](https://api-docs.quran.com/legal/mushaf-fonts-and-images/) carve
out exactly this use case:

> "Section 2.2's redistribution terms apply, except for caching and bundling font files and Mushaf images as described below... may be distributed only as an integrated part of the Application... [requires] an active account in the Developer Console... credit Quran Foundation in a reasonably accessible place in the Application or its associated credits."

So: the QCF V2 page fonts (one TrueType file per printed page, 604 total) and their per-word glyph
codes/line numbers are specifically cleared for permanent bundling, unlike the general
chapter/verse/translation endpoints — on the conditions of a registered developer account and
in-app attribution (added to `QuranCreditsScreen`). Fetched via a registered production developer
account (`apis.quran.foundation/content/api/v4/verses/by_page/{page}`, OAuth2 client-credentials
grant) for all 604 pages' layout, and the fonts from `verses.quran.foundation/fonts/quran/hafs/v2/
ttf/p{page}.ttf` (a public, unauthenticated CDN). Both are bundled as build-time assets
(`app/src/main/assets/quran/mushaf_words.json` and `app/src/main/assets/quran/mushaf_fonts/v2/`)
rather than fetched at runtime — the app itself carries no Quran Foundation API credentials or
network dependency for this feature once built; the **King Fahd Glorious Quran Printing Complex**
is credited as the fonts' ultimate rights holder alongside Quran Foundation.

A third-party font-mirror site (OnlineWebFonts.com) was checked as a possible shortcut for a
single page and rejected: it claimed a generic "CC BY 4.0, just credit us" license over what is
actually King Fahd Complex's proprietary font, which that site has no apparent standing to
relicense — the official API path above was used instead.

---

## Summary

**The currently shipped content types are cleared.** Optional tafsir and word-by-word research is
retained above, but those datasets are not shipped as of 2026-10-25.

| Content type | Status | Lead source | Built? |
|---|---|---|---|
| Arabic text (Uthmani) | ✅ Cleared | alquran.cloud (`quran-uthmani`, Tanzil-derived) | ✅ Seeded — 114 chapters, 6,236 verses |
| Translations | ✅ Cleared | alquran.cloud (Saheeh Intl. `en.sahih` + Jalandhry `ur.jalandhry` shipped) | ✅ Two shipped; more are Phase 3 |
| Tafsir | ✅ Cleared | QuranEnc.com | ❌ Not shipped |
| Word-by-word (English) | ✅ Cleared | alquran.cloud | ❌ Not shipped |
| Word-by-word (Urdu) | 🚫 No cleared source found | — (QUL traces to rejected QuranWBW.com; QuranEnc has none) | ❌ Not shipped |
| Mushaf layout + QCF fonts | ✅ Cleared (bundling exemption) | Quran Foundation API | ✅ Bundled as an on-demand Play asset pack — 604 pages, 604 fonts |
| Recitation audio | ✅ Cleared (reciter-level removal risk) | alquran.cloud | ⏳ Not yet built |

**Next steps (all Phase 3+ implementation work, not further sourcing research):**
1. Add 1–2 more translations from GlobalQuran/data or alquran.cloud, each with a one-line terms
   confirmation (same discipline as Saheeh International above).
2. Add a pack-selection UI if optional translations, tafsir, or word-by-word content is
   reintroduced.
3. Pick the first 1–2 reciters from alquran.cloud for the initial recitation pack (Phase 5).
4. Keep the About/credits screen aligned with bundled content and its required attribution.
