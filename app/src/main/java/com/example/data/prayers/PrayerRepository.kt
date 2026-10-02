package com.example.data.prayers

import android.content.Context
import org.json.JSONObject
import java.time.LocalDate
import java.time.DayOfWeek

data class PrayerSection(val id: String, val title: String, val source: String, val paragraphs: List<String>,
    val bookmarkKey: String = "prayer_text_v2")
data class PrayerBook(val edition: String, val sections: List<PrayerSection>)

class PrayerRepository(private val context: Context) {
    fun load(date: LocalDate = LocalDate.now()): PrayerBook {
        val json = JSONObject(context.assets.open("prayers_torah_or.json").bufferedReader().use { it.readText() })
        val sections = json.getJSONArray("sections")
        val prayers = (0 until sections.length()).map { index ->
            val section = sections.getJSONObject(index)
            val paragraphs = section.getJSONArray("paragraphs")
            PrayerSection(section.getString("id"), section.getString("title"), section.getString("source"),
                (0 until paragraphs.length()).map { paragraphs.getString(it) })
        }.filter { it.id != "washing" }.map { section ->
            section.copy(title = if (section.id == "morning") "מודה אני" else section.title,
                paragraphs = PrayerTextSelection.select(section.id, section.paragraphs))
        }.toMutableList()
        val closing = prayers.indexOfFirst { it.id == "closing" }
        prayers.add(closing + 1, shirShelYom(date, json))
        return PrayerBook(json.getString("edition"), prayers)
    }

    private fun shirShelYom(date: LocalDate, sourceBook: JSONObject): PrayerSection {
        val psalms = JSONObject(context.assets.open("tehillim.json").bufferedReader().use { it.readText() })
        fun verses(chapter: Int, limit: Int = Int.MAX_VALUE): List<String> {
            val array = psalms.getJSONArray(chapter.toString())
            return (0 until minOf(array.length(), limit)).map { i ->
                array.getString(i).replace(Regex("<[^>]+>"), "")
                    .replace("&thinsp;", " ").replace("&nbsp;", " ")
                    .replace(Regex("\\{[פס]\\}"), "").replace(Regex("[\\u0591-\\u05AF]"), "").trim()
            }
        }
        val day = date.dayOfWeek
        val dayName = when (day) {
            DayOfWeek.SUNDAY -> "רִאשׁוֹן"; DayOfWeek.MONDAY -> "שֵׁנִי"; DayOfWeek.TUESDAY -> "שְׁלִישִׁי"
            DayOfWeek.WEDNESDAY -> "רְבִיעִי"; DayOfWeek.THURSDAY -> "חֲמִישִׁי"; DayOfWeek.FRIDAY -> "שִׁשִּׁי"
            DayOfWeek.SATURDAY -> "שַׁבָּת"
        }
        val paragraphs = mutableListOf(if (day == DayOfWeek.SATURDAY)
            "הַיּוֹם יוֹם שַׁבָּת קֹדֶשׁ שֶׁבּוֹ הָיוּ הַלְוִיִּם אוֹמְרִים בְּבֵית הַמִּקְדָּשׁ:"
            else "הַיּוֹם יוֹם $dayName בַּשַּׁבָּת שֶׁבּוֹ הָיוּ הַלְוִיִּם אוֹמְרִים בְּבֵית הַמִּקְדָּשׁ:")
        paragraphs += verses(ShirShelYom.chapter(day))
        if (day == DayOfWeek.WEDNESDAY) paragraphs += verses(95, 3)
        val sections = sourceBook.getJSONArray("sections")
        val closing = (0 until sections.length()).map { sections.getJSONObject(it) }.single { it.getString("id") == "closing" }
        paragraphs += closing.getJSONArray("paragraphs").getString(78) // הושיענו and closing verses.
        return PrayerSection("shir_shel_yom", "שיר של יום · $dayName", closing.getString("source"),
            paragraphs, "shir_${date}")
    }
}

object ShirShelYom {
    // Tamid 7:4; Chabad custom adds Psalm 95:1–3 on Wednesday.
    fun chapter(day: DayOfWeek): Int = when (day) {
        DayOfWeek.SUNDAY -> 24; DayOfWeek.MONDAY -> 48; DayOfWeek.TUESDAY -> 82
        DayOfWeek.WEDNESDAY -> 94; DayOfWeek.THURSDAY -> 81; DayOfWeek.FRIDAY -> 93
        DayOfWeek.SATURDAY -> 92
    }
}

/** Reviewed indices for the bundled Torah Or edition, not a vowel/keyword heuristic.
 * Mishnah/korbanot recited as part of prayer remain. Standalone halachic commentary
 * and duplicated incomplete weekday introductions are excluded. Short variant labels
 * remain so seasonal passages cannot be mistaken for unconditional daily text.
 */
object PrayerTextSelection {
    private val excluded = mapOf(
        "morning" to setOf(0, 1, 2, 4, 5),
        "blessings" to setOf(1, 18),
        "offerings" to setOf(0, 51),
        "shema" to setOf(0, 4),
        "tachanun" to setOf(0, 72),
        "closing" to (setOf(0, 24, 51, 53, 56) + (76..80)),
        "grace" to (setOf(3, 4, 11, 47) + (20..30))
    )
    fun select(id: String, paragraphs: List<String>): List<String> = paragraphs.mapIndexedNotNull { index, text ->
        if (index in excluded[id].orEmpty()) null else when (id to index) {
            "psalms" to 19 -> text.replace("כאן צריך להפסיק ", "")
            "shema" to 2 -> text.removePrefix("חזן ")
            "shema" to 3 -> text.removePrefix("קהל וחזן ")
            "tachanun" to 89 -> text.removePrefix("הש\"ץ אומר חצי קדיש ")
            "closing" to 54 -> text.removePrefix("חזן ")
            "closing" to 55 -> text.removePrefix("והקהל אומרים ")
            "blessings" to 7 -> "תשעה באב ויום כיפור: ללא הברכה הבאה"
            "amidah" to 19 -> "עננו · שליח הציבור בתענית"
            "amidah" to 29 -> "נחם · מנחת תשעה באב"
            "grace" to 7 -> text.replace("ועונין המסובין ", "")
                .replace("(המברך אומר ברשות מרנן ורבותי)", "ברשות מרנן ורבותי:")
                .replace("ומי שלא אכל עמהם עונה ", "מי שלא אכל:")
                .replace("ואם הם עשרה אומר המברך ", "זימון בעשרה:")
                .replace("ומי שלא אכל עונה ", "מי שלא אכל:")
            else -> text
        }
    }
}
