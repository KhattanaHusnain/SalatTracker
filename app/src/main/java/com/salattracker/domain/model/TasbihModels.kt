package com.salattracker.domain.model

import java.time.LocalDate

enum class TasbihPhrase(val id: String, val arabic: String) {
    SUBHANALLAH("subhanallah", "سُبْحَانَ اللَّهِ"),
    ALHAMDULILLAH("alhamdulillah", "الْحَمْدُ لِلَّهِ"),
    ALLAHU_AKBAR("allahu_akbar", "اللَّهُ أَكْبَرُ"),
    LA_ILAHA_ILLA_ALLAH("la_ilaha_illa_allah", "لَا إِلَٰهَ إِلَّا اللَّهُ"),
    ASTAGHFIRULLAH("astaghfirullah", "أَسْتَغْفِرُ اللَّهَ"),
    FREE("free", "");

    companion object {
        fun fromId(id: String): TasbihPhrase = entries.firstOrNull { it.id == id } ?: SUBHANALLAH
    }
}

data class TasbihSession(
    val sessionId: String,
    val goalId: Long? = null,
    val phrase: TasbihPhrase = TasbihPhrase.SUBHANALLAH,
    /** Zero is an open-ended free count. */
    val target: Int = 0,
    val count: Int = 0,
    val isCompleted: Boolean = false,
    val completedOn: LocalDate? = null,
)

sealed interface TasbihIncrementResult {
    data class Updated(val session: TasbihSession) : TasbihIncrementResult
    data class Completed(val session: TasbihSession, val goalCredited: Boolean) : TasbihIncrementResult
    data object Ignored : TasbihIncrementResult
}
