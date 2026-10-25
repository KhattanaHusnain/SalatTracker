package com.salattracker.domain.model

import java.util.Locale

/**
 * A prayer-time calculation method. [id] follows the AlAdhan API's own numbering
 * (https://api.aladhan.com/v1/methods — fetched 2026; ids 0–23, no id 6) purely as a stable,
 * already-chosen storage key for [com.salattracker.domain.repository.UserPrefs.calculationMethodId]
 * — prayer times are calculated on-device (see `data/prayertimes/CalculationParametersFactory`),
 * which maps each [id] to the matching `adhan2` preset or, for methods the library has no preset
 * for, to the equivalent custom Fajr/Isha angles published at the URL above. No network call is
 * made for this anymore.
 *
 * [displayName] is the method's institution/place name — a proper noun, so unlike every other
 * enum in this app it is NOT localized via a string resource: "Umm Al-Qura University, Makkah"
 * reads the same in every language, the same way competing prayer apps don't translate it either.
 *
 * [id] excludes AlAdhan's `99 = CUSTOM`, which needs extra angle parameters (Fajr/Isha angles,
 * or a fixed-interval rule) this app doesn't collect anywhere.
 */
enum class CalculationMethod(val id: Int, val displayName: String) {
    SHIA_ITHNA_ASHARI(0, "Shia Ithna-Ashari, Leva Institute, Qum"),
    KARACHI(1, "University of Islamic Sciences, Karachi"),
    ISNA(2, "Islamic Society of North America (ISNA)"),
    MWL(3, "Muslim World League"),
    UMM_AL_QURA(4, "Umm Al-Qura University, Makkah"),
    EGYPT(5, "Egyptian General Authority of Survey"),
    TEHRAN(7, "Institute of Geophysics, University of Tehran"),
    GULF_REGION(8, "Gulf Region"),
    KUWAIT(9, "Kuwait"),
    QATAR(10, "Qatar"),
    SINGAPORE(11, "Majlis Ugama Islam Singapura, Singapore"),
    FRANCE(12, "Union Organization Islamic de France"),
    TURKEY(13, "Diyanet İşleri Başkanlığı, Turkey"),
    RUSSIA(14, "Spiritual Administration of Muslims of Russia"),
    MOONSIGHTING_COMMITTEE(15, "Moonsighting Committee Worldwide"),
    DUBAI(16, "Dubai"),
    JAKIM_MALAYSIA(17, "Jabatan Kemajuan Islam Malaysia (JAKIM)"),
    TUNISIA(18, "Tunisia"),
    ALGERIA(19, "Algeria"),
    INDONESIA(20, "Kementerian Agama Republik Indonesia"),
    MOROCCO(21, "Morocco"),
    LISBON(22, "Comunidade Islamica de Lisboa"),
    JORDAN(23, "Ministry of Awqaf, Islamic Affairs and Holy Places, Jordan");

    companion object {
        /** AlAdhan's own default (MWL) — used whenever a stored id doesn't match any method
         * (e.g. corrupted preference, or a future id this list doesn't know about yet). */
        val DEFAULT = MWL

        fun fromId(id: Int): CalculationMethod = values().firstOrNull { it.id == id } ?: DEFAULT

        /** The regional preset for an ISO 3166-1 alpha-2 country code. Unknown countries use MWL. */
        fun suggestedFor(countryCode: String?): CalculationMethod = when (countryCode?.uppercase(Locale.ROOT)) {
            "PK", "IN", "BD" -> KARACHI
            "US", "CA" -> ISNA
            "SA" -> UMM_AL_QURA
            "EG" -> EGYPT
            "AE" -> DUBAI
            "BH", "OM" -> GULF_REGION
            "KW" -> KUWAIT
            "QA" -> QATAR
            "SG" -> SINGAPORE
            "TR" -> TURKEY
            "RU" -> RUSSIA
            "MY" -> JAKIM_MALAYSIA
            "ID" -> INDONESIA
            "FR" -> FRANCE
            "TN" -> TUNISIA
            "DZ" -> ALGERIA
            "MA" -> MOROCCO
            "PT" -> LISBON
            "JO" -> JORDAN
            "IR" -> TEHRAN
            else -> DEFAULT
        }
    }
}
