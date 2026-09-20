package com.example.data.tanya

import android.content.Context
import com.example.domain.tanya.DailyTanyaLesson
import com.example.domain.tanya.TanyaSection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.net.URLEncoder
import java.time.LocalDate
import java.util.concurrent.TimeUnit

class TanyaRepository(private val context: Context) {

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .build()
    }

    private val memoryCache = mutableMapOf<String, DailyTanyaLesson>()

    init {
        loadBundledCache()
    }

    private fun loadBundledCache() {
        try {
            val jsonString = context.assets.open("tanya_cache.json").bufferedReader().use { it.readText() }
            val root = JSONObject(jsonString)
            val keys = root.keys()
            while (keys.hasNext()) {
                val dateKey = keys.next()
                val obj = root.getJSONObject(dateKey)
                val paragraphsArr = obj.getJSONArray("paragraphs")
                val sections = mutableListOf<TanyaSection>()
                for (i in 0 until paragraphsArr.length()) {
                    val (rawText, hebIdx) = when (val item = paragraphsArr.get(i)) {
                        is JSONObject -> Pair(item.optString("textWithNikud", ""), item.optString("indexHebrew", toHebrewNumber(i + 1)))
                        else -> Pair(item.toString(), toHebrewNumber(i + 1))
                    }
                    val cleanText = cleanText(rawText)
                    val plainText = stripNikud(cleanText)
                    sections.add(
                        TanyaSection(
                            sectionIndex = i + 1,
                            sectionHebrew = if (hebIdx.isNotBlank()) hebIdx else toHebrewNumber(i + 1),
                            textWithNikud = cleanText,
                            textPlain = plainText
                        )
                    )
                }

                val lesson = DailyTanyaLesson(
                    date = obj.optString("date", dateKey),
                    hebrewDate = obj.optString("hebrewDate", ""),
                    fullRef = obj.optString("fullRef", obj.optString("ref", "")),
                    heRef = obj.optString("heRef", ""),
                    bookTitle = obj.optString("bookTitle", "ספר התניא"),
                    chapterTitle = obj.optString("chapterTitle", obj.optString("sectionTitle", "")),
                    sections = sections,
                    isCompleted = false
                )
                memoryCache[dateKey] = lesson
            }
        } catch (_: Exception) {
            // Asset load fallback
        }
    }

    suspend fun getDailyTanyaLesson(
        date: LocalDate,
        isCompleted: Boolean = false
    ): DailyTanyaLesson? = withContext(Dispatchers.IO) {
        val dateKey = date.toString()

        // 1. Check memory cache
        memoryCache[dateKey]?.let {
            return@withContext it.copy(isCompleted = isCompleted)
        }

        // 2. Check disk cache
        val diskLesson = loadFromDiskCache(dateKey)
        if (diskLesson != null) {
            memoryCache[dateKey] = diskLesson
            return@withContext diskLesson.copy(isCompleted = isCompleted)
        }

        // 3. Try Sefaria API
        val fetchedLesson = fetchFromSefaria(date)
        if (fetchedLesson != null) {
            memoryCache[dateKey] = fetchedLesson
            saveToDiskCache(dateKey, fetchedLesson)
            return@withContext fetchedLesson.copy(isCompleted = isCompleted)
        }

        // 4. Fallback: Return closest date from memoryCache or first
        val fallback = memoryCache.values.firstOrNull()
        fallback?.copy(isCompleted = isCompleted)
    }

    private fun loadFromDiskCache(dateKey: String): DailyTanyaLesson? {
        try {
            val file = File(context.cacheDir, "tanya/lesson_$dateKey.json")
            if (!file.exists()) return null
            val obj = JSONObject(file.readText())
            val paragraphsArr = obj.getJSONArray("paragraphs")
            val sections = mutableListOf<TanyaSection>()
            for (i in 0 until paragraphsArr.length()) {
                val cleanText = paragraphsArr.getString(i)
                sections.add(
                    TanyaSection(
                        sectionIndex = i + 1,
                        sectionHebrew = toHebrewNumber(i + 1),
                        textWithNikud = cleanText,
                        textPlain = stripNikud(cleanText)
                    )
                )
            }
            return DailyTanyaLesson(
                date = obj.getString("date"),
                hebrewDate = obj.optString("hebrewDate", ""),
                fullRef = obj.optString("fullRef", ""),
                heRef = obj.optString("heRef", ""),
                bookTitle = obj.optString("bookTitle", "תניא"),
                chapterTitle = obj.optString("chapterTitle", ""),
                sections = sections,
                isCompleted = false
            )
        } catch (_: Exception) {
            return null
        }
    }

    private fun saveToDiskCache(dateKey: String, lesson: DailyTanyaLesson) {
        try {
            val dir = File(context.cacheDir, "tanya")
            dir.mkdirs()
            val file = File(dir, "lesson_$dateKey.json")
            val obj = JSONObject()
            obj.put("date", lesson.date)
            obj.put("hebrewDate", lesson.hebrewDate)
            obj.put("fullRef", lesson.fullRef)
            obj.put("heRef", lesson.heRef)
            obj.put("bookTitle", lesson.bookTitle)
            obj.put("chapterTitle", lesson.chapterTitle)
            val arr = org.json.JSONArray()
            lesson.sections.forEach { arr.put(it.textWithNikud) }
            obj.put("paragraphs", arr)
            file.writeText(obj.toString())
        } catch (_: Exception) {}
    }

    private fun fetchFromSefaria(date: LocalDate): DailyTanyaLesson? {
        try {
            val calUrl = "https://www.sefaria.org/api/calendars?year=${date.year}&month=${date.monthValue}&day=${date.dayOfMonth}&diaspora=0"
            val calReq = Request.Builder().url(calUrl).header("User-Agent", "DailyStudyApp/1.0").build()
            val calResp = httpClient.newCall(calReq).execute()
            if (!calResp.isSuccessful) return null
            val calBody = calResp.body?.string() ?: return null

            val root = JSONObject(calBody)
            val items = root.getJSONArray("calendar_items")
            var tanyaRef = ""
            var tanyaHebDate = ""
            for (i in 0 until items.length()) {
                val it = items.getJSONObject(i)
                val titleEn = it.optJSONObject("title")?.optString("en") ?: ""
                if (titleEn == "Tanya Yomi") {
                    tanyaRef = it.optString("ref", "")
                    tanyaHebDate = it.optJSONObject("displayValue")?.optString("he", "") ?: ""
                    break
                }
            }

            if (tanyaRef.isBlank()) return null

            val textUrl = "https://www.sefaria.org/api/texts/${URLEncoder.encode(tanyaRef, "UTF-8")}?context=0"
            val textReq = Request.Builder().url(textUrl).header("User-Agent", "DailyStudyApp/1.0").build()
            val textResp = httpClient.newCall(textReq).execute()
            if (!textResp.isSuccessful) return null
            val textBody = textResp.body?.string() ?: return null

            val textObj = JSONObject(textBody)
            val heTitle = textObj.optString("heTitle", "תניא")
            val heRef = textObj.optString("heRef", "")

            val rawParagraphs = mutableListOf<String>()
            val heRaw = textObj.opt("he")
            if (heRaw is org.json.JSONArray) {
                for (i in 0 until heRaw.length()) {
                    val p = heRaw.optString(i, "")
                    if (p.isNotBlank()) rawParagraphs.add(p)
                }
            } else if (heRaw is String && heRaw.isNotBlank()) {
                rawParagraphs.add(heRaw)
            }

            if (rawParagraphs.isEmpty()) return null

            val sections = rawParagraphs.mapIndexed { idx, raw ->
                val clean = cleanText(raw)
                TanyaSection(
                    sectionIndex = idx + 1,
                    sectionHebrew = toHebrewNumber(idx + 1),
                    textWithNikud = clean,
                    textPlain = stripNikud(clean)
                )
            }

            return DailyTanyaLesson(
                date = date.toString(),
                hebrewDate = tanyaHebDate,
                fullRef = tanyaRef,
                heRef = heRef,
                bookTitle = heTitle,
                chapterTitle = heRef,
                sections = sections,
                isCompleted = false
            )
        } catch (_: Exception) {
            return null
        }
    }

    private fun cleanText(text: String): String {
        return text.replace(Regex("<[^>]+>"), "")
            .replace("&nbsp;", " ")
            .replace("&#39;", "'")
            .replace("&quot;", "\"")
            .trim()
    }

    private fun stripNikud(text: String): String {
        return text.replace(Regex("[\u0591-\u05C7]"), "")
    }

    private fun toHebrewNumber(num: Int): String {
        val ones = listOf("", "א", "ב", "ג", "ד", "ה", "ו", "ז", "ח", "ט")
        val tens = listOf("", "י", "כ", "ל", "מ", "נ", "ס", "ע", "פ", "צ")
        val hundreds = listOf("", "ק", "ר", "ש", "ת")

        if (num <= 0) return ""
        if (num == 15) return "ט״ו"
        if (num == 16) return "ט״ז"

        val h = (num / 100) % 10
        val t = (num % 100) / 10
        val o = num % 10

        val str = (hundreds.getOrElse(h) { "" }) + (tens.getOrElse(t) { "" }) + (ones.getOrElse(o) { "" })
        return if (str.length == 1) "$str׳" else "${str.dropLast(1)}״${str.takeLast(1)}"
    }
}
