package com.example.domain.tanya

/** Sefaria's calendar supplies the opening segment for these end-of-epistle days,
 * not the full daily assignment. Checked against Chabad's 23/24 Tishrei lessons. */
object TanyaDailyReference {
    fun complete(ref: String): String = when (ref) {
        "Tanya, Part IV; Iggeret HaKodesh 23:10" -> "Tanya, Part IV; Iggeret HaKodesh 23:10-14"
        "Tanya, Part IV; Iggeret HaKodesh 24:1" -> "Tanya, Part IV; Iggeret HaKodesh 24:1-9"
        "Tanya, Part IV; Iggeret HaKodesh 25:36" -> "Tanya, Part IV; Iggeret HaKodesh 25:36-40"
        else -> ref
    }
}
