package com.salattracker.feature.quran

/** Traditional names of the thirty ajzaa, taken from each juz's opening phrase. */
data class JuzTitle(val arabic: String, val transliteration: String)

val JUZ_TITLES: Map<Int, JuzTitle> = mapOf(
    1 to JuzTitle("آلم", "Alif Lam Mim"),
    2 to JuzTitle("سَيَقُولُ", "Sayaqulu"),
    3 to JuzTitle("تِلْكَ الرُّسُلُ", "Tilka ar-Rusul"),
    4 to JuzTitle("لَنْ تَنَالُوا", "Lan Tana Loo"),
    5 to JuzTitle("وَالْمُحْصَنَاتُ", "Wal Muhsanat"),
    6 to JuzTitle("لَا يُحِبُّ اللَّهُ", "La Yuhibbullah"),
    7 to JuzTitle("وَإِذَا سَمِعُوا", "Wa Iza Sami'u"),
    8 to JuzTitle("وَلَوْ أَنَّنَا", "Wa Lau Annana"),
    9 to JuzTitle("قَالَ الْمَلَأُ", "Qalal Mala'u"),
    10 to JuzTitle("وَاعْلَمُوا", "Wa A'lamu"),
    11 to JuzTitle("يَعْتَذِرُونَ", "Ya'tazirun"),
    12 to JuzTitle("وَمَا مِنْ دَابَّةٍ", "Wa Ma Min Dabbah"),
    13 to JuzTitle("وَمَا أُبَرِّئُ", "Wa Ma Ubarri'u"),
    14 to JuzTitle("رُبَمَا", "Rubama"),
    15 to JuzTitle("سُبْحَانَ الَّذِي", "Subhana Alladhi"),
    16 to JuzTitle("قَالَ أَلَمْ", "Qala Alam"),
    17 to JuzTitle("اقْتَرَبَ لِلنَّاسِ", "Iqtaraba linnas"),
    18 to JuzTitle("قَدْ أَفْلَحَ", "Qad Aflaha"),
    19 to JuzTitle("وَقَالَ الَّذِينَ", "Wa Qalalladhina"),
    20 to JuzTitle("أَمَّنْ خَلَقَ", "Amman Khalaqa"),
    21 to JuzTitle("اتْلُ مَا أُوحِيَ", "Utlu Ma Uhiya"),
    22 to JuzTitle("وَمَنْ يَقْنُتْ", "Wa Man Yaqnut"),
    23 to JuzTitle("وَمَا لِيَ", "Wa Mali"),
    24 to JuzTitle("فَمَنْ أَظْلَمُ", "Faman Azlamu"),
    25 to JuzTitle("إِلَيْهِ يُرَدُّ", "Ilayhi Yuraddu"),
    26 to JuzTitle("حم", "Ha Mim"),
    27 to JuzTitle("قَالَ فَمَا خَطْبُكُمْ", "Qala Fama Khatbukum"),
    28 to JuzTitle("قَدْ سَمِعَ اللَّهُ", "Qad Sami'a Allah"),
    29 to JuzTitle("تَبَارَكَ الَّذِي", "Tabaraka Alladhi"),
    30 to JuzTitle("عَمَّ", "Amma"),
)
