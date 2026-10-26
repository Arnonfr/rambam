package com.example.data.chumash

import android.content.Context
import android.icu.util.HebrewCalendar
import android.icu.util.TimeZone
import androidx.core.text.HtmlCompat
import com.example.domain.chumash.ChumashAliya
import com.example.domain.chumash.ChumashParasha
import com.example.domain.chumash.ChumashVerse
import com.example.domain.chumash.DailyChumashLesson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class ChumashRepository(private val context: Context) {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS).readTimeout(10, TimeUnit.SECONDS).build()
    private val parashotById = mutableMapOf<String, JSONObject>()
    private val memoryCache = mutableMapOf<String, ChumashParasha>()
    private val weeklySchedules = mutableMapOf<Boolean, List<Pair<LocalDate, String>>>()
    private var scheduleStart = LocalDate.MAX
    private var scheduleEnd = LocalDate.MIN

    init {
        val root = readAsset("chumash_cache.json")
        val keys = root.keys()
        while (keys.hasNext()) {
            val obj = root.getJSONObject(keys.next())
            require(obj.getInt("contentSchemaVersion") == 3) { "Outdated Chumash boundaries" }
            parashotById[obj.getString("parashaId")] = obj
        }
        val schedule = readAsset("chumash_weekly_schedule.json")
        scheduleStart = LocalDate.parse(schedule.getString("start"))
        scheduleEnd = LocalDate.parse(schedule.getString("end"))
        for (inIsrael in listOf(true, false)) {
            val entries = schedule.getJSONObject("schedules")
                .getJSONArray(if (inIsrael) "israel" else "diaspora")
            weeklySchedules[inIsrael] = (0 until entries.length()).map {
                val obj = entries.getJSONObject(it)
                LocalDate.parse(obj.getString("date")) to obj.getString("parashaId")
            }
        }
    }

    private fun readAsset(name: String): JSONObject = context.assets.open(name)
        .bufferedReader().use { JSONObject(it.readText()) }

    private fun parashaById(id: String): ChumashParasha? = memoryCache[id] ?: parashotById[id]?.let {
        parseParashaJson(it).also { parasha -> memoryCache[id] = parasha }
    }

    private fun parseParashaJson(obj: JSONObject): ChumashParasha {
        val bookName = obj.getString("book")
        val aliyotArray = obj.getJSONArray("aliyot")
        val aliyot = (0 until aliyotArray.length()).map { i ->
            val aObj = aliyotArray.getJSONObject(i)
            val versesArray = aObj.getJSONArray("verses")
            val verses = (0 until versesArray.length()).map { j ->
                val vObj = versesArray.getJSONObject(j)
                val number = vObj.getInt("verseNumber")
                val rashi = vObj.getJSONArray("rashi")
                ChumashVerse(number, toHebrewNumeral(number),
                    cleanHtml(vObj.getString("textWithTeamim")),
                    cleanHtml(vObj.getString("textPlainNikud")),
                    (0 until rashi.length()).map { cleanRashiHtml(rashi.getString(it)) },
                    chapterNumber = vObj.getInt("chapterNumber"), bookName = bookName)
            }
            require(verses.isNotEmpty() && verses.map { it.positionId }.distinct().size == verses.size)
            ChumashAliya(aObj.getInt("aliyaIndex"), aObj.getString("aliyaName"),
                aObj.getString("dayName"), aObj.getString("ref"), aObj.getString("heRef"), verses)
        }
        require(aliyot.map { it.aliyaIndex } == (1..7).toList())
        return ChumashParasha(obj.getString("parasha"), bookName, obj.getString("fullRef"), aliyot)
    }

    suspend fun getParashaByName(parashaName: String): ChumashParasha? = withContext(Dispatchers.IO) {
        parashotById.entries.singleOrNull {
            normalizeParashaName(it.value.getString("parasha")) == normalizeParashaName(parashaName)
        }?.key?.let { parashaById(it) }
    }

    suspend fun getDailyChumashLesson(
        date: LocalDate,
        completedKeys: Set<String> = emptySet(),
        inIsrael: Boolean = true
    ): DailyChumashLesson? = withContext(Dispatchers.IO) {
        val currentIndex = date.dayOfWeek.value % 7 + 1
        val upcomingId = resolveUpcomingParashaId(date, inIsrael) ?: return@withContext null
        val calendar = HebrewCalendar(TimeZone.getTimeZone("UTC"))
        calendar.timeInMillis = date.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
        val month = calendar.get(HebrewCalendar.MONTH)
        val day = calendar.get(HebrewCalendar.DAY_OF_MONTH)
        val simchatTorah = if (inIsrael) 22 else 23

        // During the festival weeks Chumash continues the annual study cycle,
        // not the public holiday reading. On Simchat Torah finish the remaining
        // Vezot portions and catch up Bereishit to this weekday (unless Shabbat).
        val inTransition = upcomingId == "P01" && month == HebrewCalendar.TISHRI && day <= simchatTorah
        val parasha = parashaById(if (inTransition) "P54" else upcomingId) ?: return@withContext null
        if (inTransition && day == simchatTorah && currentIndex < 7) {
            val bereishit = parashaById("P01") ?: return@withContext null
            val portions = parasha.aliyot.filter { it.aliyaIndex >= currentIndex } +
                bereishit.aliyot.filter { it.aliyaIndex <= currentIndex }
            val name = "וזאת הברכה ובראשית"
            val key = "$name-$currentIndex"
            val today = ChumashAliya(currentIndex, parasha.aliyot[currentIndex - 1].aliyaName,
                parasha.aliyot[currentIndex - 1].dayName, portions.joinToString("; ") { it.ref },
                portions.joinToString("; ") { it.heRef }, portions.flatMap { it.verses },
                isCompleted = key in completedKeys)
            return@withContext DailyChumashLesson(date.toString(), name, "דברים ובראשית",
                currentIndex, today, listOf(today), if (today.isCompleted) 1 else 0)
        }
        val aliyot = parasha.aliyot.map { it.copy(isCompleted = "${parasha.parashaName}-${it.aliyaIndex}" in completedKeys) }
        DailyChumashLesson(date.toString(), parasha.parashaName, parasha.bookName, currentIndex,
            aliyot.single { it.aliyaIndex == currentIndex }, aliyot, aliyot.count { it.isCompleted })
    }

    private fun resolveUpcomingParashaId(date: LocalDate, inIsrael: Boolean): String? {
        if (date in scheduleStart..scheduleEnd) {
            weeklySchedules[inIsrael]?.firstOrNull { it.first >= date }?.let { return it.second }
        }
        // Separate, versioned date cache: ignore the old substring-based names
        // and old online aliyot, which could replace a combined portion by half.
        val cacheFile = File(context.cacheDir, "chumash/daily_v3_${inIsrael}_$date.txt")
        if (cacheFile.exists()) {
            cacheFile.readText().trim().takeIf { it in parashotById }?.let { return it }
        }
        return try {
            val url = "https://www.hebcal.com/hebcal?v=1&cfg=json&start=$date&end=${date.plusDays(35)}&s=on&i=${if (inIsrael) "on" else "off"}&leyning=off"
            httpClient.newCall(Request.Builder().url(url).build()).execute().use { response ->
                if (!response.isSuccessful) return null
                val items = JSONObject(response.body?.string() ?: return null).getJSONArray("items")
                val item = (0 until items.length()).map { items.getJSONObject(it) }
                    .filter { it.optString("category") == "parashat" }
                    .minByOrNull { it.getString("date") } ?: return null
                val name = normalizeParashaName(item.getString("hebrew").removePrefix("פרשת "))
                val id = parashotById.entries.singleOrNull {
                    normalizeParashaName(it.value.getString("parasha")) == name
                }?.key ?: return null
                cacheFile.parentFile?.mkdirs()
                cacheFile.writeText(id)
                id
            }
        } catch (_: Exception) { null }
    }

    companion object {
        fun normalizeParashaName(name: String): String {
            var result = name.replace(Regex("[\\s\\-־–\"״\\u0591-\\u05C7]"), "")
            for ((from, to) in listOf("בחוקותי" to "בחוקתי", "חוקת" to "חקת",
                "מצרע" to "מצורע", "שלחלך" to "שלח", "אחרימות" to "אחרי",
                "קדשים" to "קדושים", "קורח" to "קרח", "בהעלתך" to "בהעלותך", "נוח" to "נח")) {
                result = result.replace(from, to)
            }
            return result
        }

        private val CANTILLATION_PATTERN = Pattern.compile("[\u0591-\u05AF]")

        fun cleanHtml(text: String): String {
            val source = text.replace("&thinsp;", " ").replace("&thinsp", " ")
                .replace("&ensp;", " ").replace("&emsp;", " ")
            return HtmlCompat.fromHtml(source, HtmlCompat.FROM_HTML_MODE_LEGACY)
                .toString().replace('\u00A0', ' ').trim()
        }

        fun cleanRashiHtml(text: String): String {
            // Keep the opening words bold, while decoding entities and other markup.
            val protected = text.replace("<b>", "\uE000").replace("</b>", "\uE001")
            return cleanHtml(protected).replace("\uE000", "<b>").replace("\uE001", "</b>")
        }

        fun parseRashiResponse(json: JSONObject, chapterLengths: List<Int> = emptyList()): List<List<String>> {
            val hebrew = json.optJSONArray("he") ?: return emptyList()
            val spanning = json.optJSONArray("sections")?.optInt(0) !=
                json.optJSONArray("toSections")?.optInt(0)
            val verses = mutableListOf<JSONArray>()
            if (spanning) {
                for (chapter in 0 until hebrew.length()) {
                    val chapterVerses = hebrew.getJSONArray(chapter)
                    for (verse in 0 until chapterVerses.length()) {
                        verses.add(chapterVerses.getJSONArray(verse))
                    }
                    repeat(((chapterLengths.getOrNull(chapter) ?: chapterVerses.length()) - chapterVerses.length()).coerceAtLeast(0)) {
                        verses.add(JSONArray())
                    }
                }
            } else {
                for (verse in 0 until hebrew.length()) verses.add(hebrew.getJSONArray(verse))
                repeat(((chapterLengths.firstOrNull() ?: hebrew.length()) - hebrew.length()).coerceAtLeast(0)) {
                    verses.add(JSONArray())
                }
            }
            // Empty comment arrays are meaningful: never shift following comments up a verse.
            return verses.map { comments ->
                (0 until comments.length()).map { cleanRashiHtml(comments.getString(it)) }
                    .filter { it.isNotBlank() }
            }
        }

        fun removeCantillation(text: String): String {
            return CANTILLATION_PATTERN.matcher(text).replaceAll("")
        }

        fun extractBookName(ref: String): String {
            return when {
                ref.startsWith("Genesis") -> "בראשית"
                ref.startsWith("Exodus") -> "שמות"
                ref.startsWith("Leviticus") -> "ויקרא"
                ref.startsWith("Numbers") -> "במדבר"
                ref.startsWith("Deuteronomy") -> "דברים"
                else -> "תורה"
            }
        }

        fun toHebrewNumeral(number: Int): String {
            if (number <= 0) return number.toString()
            val letters = arrayOf(
                1000 to "ת", 900 to "תתק", 800 to "תת", 700 to "תש", 600 to "תר",
                500 to "תק", 400 to "ת", 300 to "ש", 200 to "ר", 100 to "ק",
                90 to "צ", 80 to "פ", 70 to "ע", 60 to "ס", 50 to "נ",
                40 to "מ", 30 to "ל", 20 to "כ", 10 to "י",
                9 to "ט", 8 to "ח", 7 to "ז", 6 to "ו", 5 to "ה",
                4 to "ד", 3 to "ג", 2 to "ב", 1 to "א"
            )

            var n = number
            val sb = StringBuilder()
            if (n == 15) return "ט״ו"
            if (n == 16) return "ט״ז"

            for ((value, letter) in letters) {
                while (n >= value) {
                    sb.append(letter)
                    n -= value
                }
            }
            val res = sb.toString()
            return if (res.length == 1) "$res׳" else "${res.dropLast(1)}״${res.takeLast(1)}"
        }
    }
}
