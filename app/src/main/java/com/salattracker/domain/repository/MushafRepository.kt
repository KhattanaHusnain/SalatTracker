package com.salattracker.domain.repository

import com.salattracker.domain.model.MushafEdition
import com.salattracker.domain.model.MushafLine
import com.salattracker.domain.model.MushafPage
import com.salattracker.domain.model.MushafWord

/** Print-accurate Mushaf page data (QCF V2) — bundled in the APK, like the rest of
 * [com.salattracker.domain.model.Chapter]/[com.salattracker.domain.model.Verse], not fetched over
 * the network. The glyph font itself is a bundled asset too (assets/quran/mushaf_fonts/v2/pN.ttf,
 * loaded directly by the reader UI), so this mode works fully offline from first launch. */
interface MushafRepository {
    /** The page's lines and words, with glyph codes ready to render in that page's font. */
    suspend fun getPage(pageNumber: Int, edition: MushafEdition = MushafEdition.QCF_V2): MushafPage

    /** This chapter's words grouped into real printed Mushaf lines, in reading order — the same
     * line boundaries [getPage] uses, just pre-filtered to one chapter (a line shared with the
     * previous/next chapter only contributes this chapter's own words). Every line's words share
     * one page, readable from any word's [MushafWord.pageNumber]. Lets the main reader flow verses
     * continuously exactly as printed — including a verse that doesn't fit on one line — rather
     * than forcing one verse per visual block. */
    suspend fun getLinesForChapter(chapterNumber: Int, edition: MushafEdition = MushafEdition.QCF_V2): List<MushafLine>
}
