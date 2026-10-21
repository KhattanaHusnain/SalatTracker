package com.salattracker.domain.model

/**
 * Which kind of downloadable Quran content a [QuranContentPack] is. Recitation audio isn't
 * Room-backed like the others — it's raw files managed by its own download manager, added in a
 * later phase — but shares this enum/registry so all four "what have I downloaded" states show
 * in one place.
 */
enum class QuranContentPackType { TRANSLATION, TAFSIR, WORD_BY_WORD, RECITATION }

/** English translation pack, bundled and seeded on first run (see `QuranSeedWorker`) — used by
 * the verse-of-the-day widget ([com.salattracker.data.repository.VerseRepositoryImpl]), which
 * stays English regardless of the main reader's language. */
const val DEFAULT_TRANSLATION_PACK_ID = "en.sahih"

/** The main reader's default translation (see `QuranReaderViewModel`) — Urdu, shown until a
 * pack-selection UI exists for picking among several downloaded translations. */
const val DEFAULT_URDU_TRANSLATION_PACK_ID = "ur.jalandhry"

/** The one tafsir pack bundled in the app and seeded on first run (see `QuranSeedWorker`) —
 * At-Tafsir Al-Muyassar (King Fahd Complex, Madinah), in Arabic. */
const val DEFAULT_TAFSIR_PACK_ID = "ar.muyassar"

/** The one word-by-word translation pack bundled in the app and seeded on first run (see
 * `QuranSeedWorker`) — English glosses per word. The base Arabic word segmentation itself isn't
 * pack-scoped (it's language-independent) and lives directly in QuranCoreDatabase instead. */
const val DEFAULT_WORD_BY_WORD_PACK_ID = "en.wordbyword"
