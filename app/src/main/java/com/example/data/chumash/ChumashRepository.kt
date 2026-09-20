package com.example.data.chumash

import android.content.Context
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
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class ChumashRepository(private val context: Context) {

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    // In-memory memory cache of loaded parashot
    private val memoryCache = mutableMapOf<String, ChumashParasha>()

    // Pre-bundled parashot from assets
    private var bundledCacheLoaded = false

    init {
        loadBundledCache()
    }

    private fun loadBundledCache() {
        if (bundledCacheLoaded) return
        try {
            val jsonString = context.assets.open("chumash_cache.json").bufferedReader().use { it.readText() }
            val root = JSONObject(jsonString)
            val keys = root.keys()
            while (keys.hasNext()) {
                val pName = keys.next()
                val pObj = root.getJSONObject(pName)
                val parasha = parseParashaJson(pObj)
                memoryCache[pName] = parasha
            }
            bundledCacheLoaded = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun parseParashaJson(obj: JSONObject): ChumashParasha {
        val parashaName = obj.getString("parasha")
        val bookName = obj.optString("book", "")
        val fullRef = obj.optString("fullRef", "")
        val aliyotArray = obj.getJSONArray("aliyot")
        val aliyot = mutableListOf<ChumashAliya>()

        for (i in 0 until aliyotArray.length()) {
            val aObj = aliyotArray.getJSONObject(i)
            val aliyaIndex = aObj.getInt("aliyaIndex")
            val aliyaName = aObj.getString("aliyaName")
            val dayName = aObj.getString("dayName")
            val ref = aObj.getString("ref")
            val heRef = aObj.getString("heRef")

            val versesArray = aObj.getJSONArray("verses")
            val verses = mutableListOf<ChumashVerse>()
            for (j in 0 until versesArray.length()) {
                val vObj = versesArray.getJSONObject(j)
                val vNum = vObj.getInt("verseNumber")
                val withTeamim = vObj.getString("textWithTeamim")
                val plainNikud = vObj.optString("textPlainNikud", removeCantillation(withTeamim))
                val rashiList = mutableListOf<String>()
                if (vObj.has("rashi")) {
                    val rArr = vObj.getJSONArray("rashi")
                    for (k in 0 until rArr.length()) {
                        rashiList.add(rArr.getString(k))
                    }
                }
                verses.add(
                    ChumashVerse(
                        verseNumber = vNum,
                        verseHebrew = toHebrewNumeral(vNum),
                        textWithTeamim = withTeamim,
                        textPlainNikud = plainNikud,
                        rashi = rashiList
                    )
                )
            }

            aliyot.add(
                ChumashAliya(
                    aliyaIndex = aliyaIndex,
                    aliyaName = aliyaName,
                    dayName = dayName,
                    ref = ref,
                    heRef = heRef,
                    verses = verses,
                    isCompleted = false
                )
            )
        }

        return ChumashParasha(
            parashaName = parashaName,
            bookName = bookName,
            fullRef = fullRef,
            aliyot = aliyot
        )
    }

    suspend fun getDailyChumashLesson(
        date: LocalDate,
        completedKeys: Set<String> = emptySet()
    ): DailyChumashLesson? = withContext(Dispatchers.IO) {
        val currentAliyaIndex = when (date.dayOfWeek) {
            DayOfWeek.SUNDAY -> 1
            DayOfWeek.MONDAY -> 2
            DayOfWeek.TUESDAY -> 3
            DayOfWeek.WEDNESDAY -> 4
            DayOfWeek.THURSDAY -> 5
            DayOfWeek.FRIDAY -> 6
            DayOfWeek.SATURDAY -> 7
            else -> 1
        }

        // Try getting parasha for this date
        val parasha = getParashaForDate(date) ?: memoryCache["וזאת הברכה"] ?: memoryCache.values.firstOrNull()
        if (parasha == null) return@withContext null

        val aliyotWithCompletion = parasha.aliyot.map { aliya ->
            val key = "${parasha.parashaName}-${aliya.aliyaIndex}"
            aliya.copy(isCompleted = completedKeys.contains(key))
        }

        val todayAliya = aliyotWithCompletion.find { it.aliyaIndex == currentAliyaIndex }
            ?: aliyotWithCompletion.firstOrNull()

        val completedCount = aliyotWithCompletion.count { it.isCompleted }

        DailyChumashLesson(
            date = date.toString(),
            parashaName = parasha.parashaName,
            bookName = parasha.bookName,
            currentAliyaIndex = currentAliyaIndex,
            todayAliya = todayAliya,
            allAliyot = aliyotWithCompletion,
            completedCount = completedCount
        )
    }

    suspend fun getParashaByName(parashaName: String): ChumashParasha? = withContext(Dispatchers.IO) {
        memoryCache[parashaName] ?: loadParashaFromDiskCache(parashaName)
    }

    private suspend fun getParashaForDate(date: LocalDate): ChumashParasha? {
        // Chitas calendar rules for Tishrei 5787:
        // Up to Shabbat Ha'azinu (Sep 19, 2026): Ha'azinu
        // From Sunday after Shabbat Ha'azinu (Sep 20, 2026) until Simchat Torah (Oct 4, 2026): V'Zot HaBerachah
        if (date >= LocalDate.of(2026, 9, 20) && date <= LocalDate.of(2026, 10, 4)) {
            val vzot = memoryCache["וזאת הברכה"] ?: loadParashaFromDiskCache("וזאת הברכה")
            if (vzot != null) return vzot
        } else if (date <= LocalDate.of(2026, 9, 19)) {
            val haazinu = memoryCache["האזינו"] ?: loadParashaFromDiskCache("האזינו")
            if (haazinu != null) return haazinu
        } else if (date in LocalDate.of(2026, 10, 5)..LocalDate.of(2026, 10, 10)) {
            val bereishit = memoryCache["בראשית"] ?: loadParashaFromDiskCache("בראשית")
            if (bereishit != null) return bereishit
        }

        // Check disk cache for this specific date mapping
        val dateCacheFile = File(context.cacheDir, "chumash/date_${date}.txt")
        if (dateCacheFile.exists()) {
            val cachedName = dateCacheFile.readText().trim()
            if (TORAH_PARASHOT.any { cachedName.contains(it) }) {
                val cachedParasha = memoryCache[cachedName] ?: loadParashaFromDiskCache(cachedName)
                if (cachedParasha != null) return cachedParasha
            }
        }

        // Try fetching calendar item from Sefaria
        try {
            val url = "https://www.sefaria.org/api/calendars?year=${date.year}&month=${date.monthValue}&day=${date.dayOfMonth}&diaspora=0"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "DailyStudyApp/1.0")
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string()
                if (body != null) {
                    val root = JSONObject(body)
                    val items = root.getJSONArray("calendar_items")
                    var bestParashaName: String? = null
                    var bestRef: String? = null
                    var bestAliyotJson: JSONArray? = null

                    for (i in 0 until items.length()) {
                        val item = items.getJSONObject(i)
                        val titleObj = item.optJSONObject("title")
                        val titleEn = titleObj?.optString("en") ?: ""
                        val displayVal = item.optJSONObject("displayValue")?.optString("he") ?: ""

                        // Only accept real Torah Parashot (not holiday readings like "סוכות חג ראשון")
                        val matchedTorahName = TORAH_PARASHOT.find { displayVal.contains(it) }
                        if (matchedTorahName != null) {
                            if (titleEn == "Parashat Hashavua" || bestParashaName == null) {
                                bestParashaName = matchedTorahName
                                bestRef = item.optString("ref")
                                bestAliyotJson = item.optJSONObject("extraDetails")?.optJSONArray("aliyot")
                                if (titleEn == "Parashat Hashavua") break
                            }
                        }
                    }

                    if (bestParashaName != null) {
                        try {
                            dateCacheFile.parentFile?.mkdirs()
                            dateCacheFile.writeText(bestParashaName)
                        } catch (_: Exception) {}

                        var parasha = memoryCache[bestParashaName] ?: loadParashaFromDiskCache(bestParashaName)
                        if (parasha != null) return parasha

                        if (bestAliyotJson != null && bestRef != null) {
                            parasha = fetchAndCacheParasha(bestParashaName, bestRef, bestAliyotJson)
                            if (parasha != null) return parasha
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Offline or network error - fallback to bundled
        }

        // Fallback: Check if bundled cache has an appropriate parasha
        return memoryCache["וזאת הברכה"] ?: memoryCache["האזינו"] ?: memoryCache.values.firstOrNull()
    }

    private fun fetchAndCacheParasha(
        parashaName: String,
        fullRef: String,
        aliyotRefs: JSONArray
    ): ChumashParasha? {
        val aliyaNames = listOf("ראשון", "שני", "שלישי", "רביעי", "חמישי", "ששי", "שביעי")
        val dayNames = listOf("יום ראשון", "יום שני", "יום שלישי", "יום רביעי", "יום חמישי", "יום שישי", "שבת קודש")
        val aliyotList = mutableListOf<ChumashAliya>()

        val pObj = JSONObject()
        pObj.put("parasha", parashaName)
        pObj.put("book", extractBookName(fullRef))
        pObj.put("fullRef", fullRef)
        val aliyotArr = JSONArray()

        val count = minOf(7, aliyotRefs.length())
        for (idx in 0 until count) {
            val ref = aliyotRefs.getString(idx)
            val (heRef, verses) = fetchVersesFromSefaria(ref)
            val rashiPerVerse = fetchRashiFromSefaria(ref)

            val aObj = JSONObject()
            aObj.put("aliyaIndex", idx + 1)
            aObj.put("aliyaName", aliyaNames[idx])
            aObj.put("dayName", dayNames[idx])
            aObj.put("ref", ref)
            aObj.put("heRef", heRef)

            val vArr = JSONArray()
            val domainVerses = mutableListOf<ChumashVerse>()
            verses.forEachIndexed { vIdx, vText ->
                val vNum = vIdx + 1
                val plain = removeCantillation(vText)
                val verseRashi = if (vIdx < rashiPerVerse.size) rashiPerVerse[vIdx] else emptyList()
                val singleV = JSONObject()
                singleV.put("verseNumber", vNum)
                singleV.put("textWithTeamim", vText)
                singleV.put("textPlainNikud", plain)
                val rArr = JSONArray()
                verseRashi.forEach { rArr.put(it) }
                singleV.put("rashi", rArr)
                vArr.put(singleV)

                domainVerses.add(
                    ChumashVerse(
                        verseNumber = vNum,
                        verseHebrew = toHebrewNumeral(vNum),
                        textWithTeamim = vText,
                        textPlainNikud = plain,
                        rashi = verseRashi
                    )
                )
            }
            aObj.put("verses", vArr)
            aliyotArr.put(aObj)

            aliyotList.add(
                ChumashAliya(
                    aliyaIndex = idx + 1,
                    aliyaName = aliyaNames[idx],
                    dayName = dayNames[idx],
                    ref = ref,
                    heRef = heRef,
                    verses = domainVerses
                )
            )
        }

        pObj.put("aliyot", aliyotArr)

        // Save to disk cache
        try {
            val cacheDir = File(context.cacheDir, "chumash")
            cacheDir.mkdirs()
            File(cacheDir, "parasha_${parashaName}.json").writeText(pObj.toString(2))
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val parasha = ChumashParasha(
            parashaName = parashaName,
            bookName = extractBookName(fullRef),
            fullRef = fullRef,
            aliyot = aliyotList
        )
        memoryCache[parashaName] = parasha
        return parasha
    }

    private fun fetchVersesFromSefaria(ref: String): Pair<String, List<String>> {
        val formattedRef = ref.replace(" ", ".")
        val url = "https://www.sefaria.org/api/texts/$formattedRef?context=0"
        try {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "DailyStudyApp/1.0")
                .build()
            val res = httpClient.newCall(req).execute()
            if (res.isSuccessful) {
                val body = res.body?.string() ?: return Pair(ref, emptyList())
                val json = JSONObject(body)
                val heRef = json.optString("heRef", ref)
                val heElement = json.opt("he")
                val verses = mutableListOf<String>()
                flattenHebrewArray(heElement, verses)
                return Pair(heRef, verses)
            }
        } catch (_: Exception) {}
        return Pair(ref, emptyList())
    }

    private fun flattenHebrewArray(element: Any?, target: MutableList<String>) {
        when (element) {
            is JSONArray -> {
                for (i in 0 until element.length()) {
                    flattenHebrewArray(element.get(i), target)
                }
            }
            is String -> {
                val clean = cleanHtml(element)
                if (clean.isNotBlank()) target.add(clean)
            }
        }
    }

    private fun fetchRashiFromSefaria(ref: String): List<List<String>> {
        val formattedRef = ref.replace(" ", "_").replace(":", ".")
        val rashiRef = "Rashi_on_$formattedRef"
        val url = "https://www.sefaria.org/api/texts/$rashiRef?context=0"
        try {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "DailyStudyApp/1.0")
                .build()
            val res = httpClient.newCall(req).execute()
            if (res.isSuccessful) {
                val body = res.body?.string() ?: return emptyList()
                val json = JSONObject(body)
                val heElement = json.opt("he")
                val rashiPerVerse = mutableListOf<List<String>>()
                if (heElement is JSONArray) {
                    for (i in 0 until heElement.length()) {
                        val verseRashi = mutableListOf<String>()
                        val vObj = heElement.get(i)
                        if (vObj is JSONArray) {
                            for (j in 0 until vObj.length()) {
                                val rText = vObj.optString(j, "")
                                if (rText.isNotBlank()) verseRashi.add(rText)
                            }
                        } else if (vObj is String && vObj.isNotBlank()) {
                            verseRashi.add(vObj)
                        }
                        rashiPerVerse.add(verseRashi)
                    }
                }
                return rashiPerVerse
            }
        } catch (_: Exception) {}
        return emptyList()
    }

    private fun loadParashaFromDiskCache(parashaName: String): ChumashParasha? {
        try {
            val file = File(context.cacheDir, "chumash/parasha_${parashaName}.json")
            if (file.exists()) {
                val json = JSONObject(file.readText())
                val p = parseParashaJson(json)
                memoryCache[parashaName] = p
                return p
            }
        } catch (_: Exception) {}
        return null
    }

    companion object {
        private val TORAH_PARASHOT = listOf(
            "בראשית", "נח", "לך לך", "וירא", "חיי שרה", "תולדות", "ויצא", "וישלח", "וישב", "מקץ", "ויגש", "ויחי",
            "שמות", "וארא", "בא", "בשלח", "יתרו", "משפטים", "תרומה", "תצוה", "כי תשא", "ויקהל", "פקודי",
            "ויקרא", "צו", "שמיני", "תזריע", "מצורע", "אחרי מות", "קדושים", "אמור", "בהר", "בחוקתי",
            "במדבר", "נשא", "בהעלותך", "שלח", "שלח לך", "קרח", "חקת", "בלק", "פינחס", "מטות", "מסעי",
            "דברים", "ואתחנן", "עקב", "ראה", "שופטים", "כי תצא", "כי תבוא", "נצבים", "וילך", "האזינו", "וזאת הברכה"
        )
        private val HTML_TAG_PATTERN = Pattern.compile("<[^>]+>")
        private val CANTILLATION_PATTERN = Pattern.compile("[\u0591-\u05AF]")

        fun cleanHtml(text: String): String {
            return HTML_TAG_PATTERN.matcher(text).replaceAll("").replace("&nbsp;", " ").trim()
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
