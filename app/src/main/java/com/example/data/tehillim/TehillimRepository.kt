package com.example.data.tehillim

import android.content.Context
import com.example.domain.tehillim.DailyTehillimLesson
import com.example.domain.tehillim.TehillimChapter
import com.example.domain.tehillim.TehillimVerse
import com.example.ui.util.HebrewNumberFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.time.LocalDate

class TehillimRepository(private val context: Context) {

    private var cachedTehillimJson: JSONObject? = null

    private suspend fun getTehillimJson(): JSONObject = withContext(Dispatchers.IO) {
        cachedTehillimJson?.let { return@withContext it }
        try {
            val assetManager = context.assets
            val inputStream = assetManager.open("tehillim.json")
            val reader = BufferedReader(InputStreamReader(inputStream))
            val jsonString = reader.use { it.readText() }
            val json = JSONObject(jsonString)
            cachedTehillimJson = json
            json
        } catch (e: Exception) {
            e.printStackTrace()
            JSONObject()
        }
    }

    suspend fun getDailyTehillimLesson(
        dayOfMonth: Int,
        hebrewMonthName: String,
        hebrewDayOfMonthStr: String
    ): DailyTehillimLesson = withContext(Dispatchers.IO) {
        val json = getTehillimJson()
        
        // 1. Get standard chapters for this day of month
        val chaptersList = mutableListOf<TehillimChapter>()
        val chapterRanges = getChapterRangesForDay(dayOfMonth)

        for (chNum in chapterRanges) {
            val versesArray = json.optJSONArray(chNum.toString()) ?: continue
            val allVerses = mutableListOf<TehillimVerse>()
            
            for (vIdx in 0 until versesArray.length()) {
                val rawText = versesArray.getString(vIdx)
                val cleanWithTeamim = cleanTehillimText(rawText)
                val vNum = vIdx + 1
                
                // Specific filter for Chapter 119 split (Day 25 vs 26)
                if (chNum == 119) {
                    if (dayOfMonth == 25 && vNum > 96) continue
                    if (dayOfMonth == 26 && vNum <= 96) continue
                }

                allVerses.add(
                    TehillimVerse(
                        verseNumber = vNum,
                        verseHebrew = HebrewNumberFormatter.toHebrewNumeral(vNum) + "׳",
                        textWithTeamim = cleanWithTeamim,
                        textPlainNikud = removeCantillation(cleanWithTeamim)
                    )
                )
            }

            chaptersList.add(
                TehillimChapter(
                    chapterNumber = chNum,
                    chapterHebrew = "פרק " + HebrewNumberFormatter.toHebrewNumeral(chNum),
                    verses = allVerses
                )
            )
        }

        // 2. Get Elul/Tishrei custom 3 chapters if applicable
        val elulChapters = mutableListOf<TehillimChapter>()
        var elulDesc: String? = null
        
        val additionalChapters = getElulAdditionalChapters(hebrewMonthName, dayOfMonth)
        if (additionalChapters.isNotEmpty()) {
            for (chNum in additionalChapters) {
                val versesArray = json.optJSONArray(chNum.toString()) ?: continue
                val allVerses = mutableListOf<TehillimVerse>()
                for (vIdx in 0 until versesArray.length()) {
                    val rawText = versesArray.getString(vIdx)
                    val cleanWithTeamim = cleanTehillimText(rawText)
                    val vNum = vIdx + 1
                    allVerses.add(
                        TehillimVerse(
                            verseNumber = vNum,
                            verseHebrew = HebrewNumberFormatter.toHebrewNumeral(vNum) + "׳",
                            textWithTeamim = cleanWithTeamim,
                            textPlainNikud = removeCantillation(cleanWithTeamim)
                        )
                    )
                }
                elulChapters.add(
                    TehillimChapter(
                        chapterNumber = chNum,
                        chapterHebrew = "פרק " + HebrewNumberFormatter.toHebrewNumeral(chNum),
                        verses = allVerses
                    )
                )
            }
            elulDesc = "3 פרקים נוספים המתווספים מראש חודש אלול ועד יום הכיפורים (היום: פרקים " + 
                    additionalChapters.joinToString(", ") { HebrewNumberFormatter.toHebrewNumeral(it) } + ")"
        }

        DailyTehillimLesson(
            date = LocalDate.now().toString(),
            dayOfMonth = dayOfMonth,
            dayOfMonthHebrew = hebrewDayOfMonthStr,
            chapters = chaptersList,
            elulChapters = elulChapters,
            elulDaysDescription = elulDesc
        )
    }

    private fun getChapterRangesForDay(day: Int): List<Int> {
        return when (day) {
            1 -> (1..9).toList()
            2 -> (10..17).toList()
            3 -> (18..22).toList()
            4 -> (23..28).toList()
            5 -> (29..34).toList()
            6 -> (35..38).toList()
            7 -> (39..43).toList()
            8 -> (44..48).toList()
            9 -> (49..54).toList()
            10 -> (55..59).toList()
            11 -> (60..65).toList()
            12 -> (66..68).toList()
            13 -> (69..71).toList()
            14 -> (72..76).toList()
            15 -> (77..78).toList()
            16 -> (79..82).toList()
            17 -> (83..87).toList()
            18 -> (88..89).toList()
            19 -> (90..96).toList()
            20 -> (97..103).toList()
            21 -> (104..105).toList()
            22 -> (106..107).toList()
            23 -> (108..112).toList()
            24 -> (113..118).toList()
            25 -> listOf(119) // filtered 1-96 in parser
            26 -> listOf(119) // filtered 97-176 in parser
            27 -> (120..134).toList()
            28 -> (135..139).toList()
            29 -> (140..144).toList()
            30 -> (145..150).toList()
            else -> (1..9).toList()
        }
    }

    private fun getElulAdditionalChapters(month: String, day: Int): List<Int> {
        // Chabad custom: 3 chapters daily from 1 Elul until Yom Kippur (10 Tishrei)
        if (month == "אלול" && day in 1..29) {
            val start = (day - 1) * 3 + 1
            return listOf(start, start + 1, start + 2).filter { it <= 150 }
        }
        if (month == "תשרי") {
            when (day) {
                1 -> return listOf(88, 89, 90) // Rosh Hashanah 1
                2 -> return listOf(91, 92, 93) // Rosh Hashanah 2
                3 -> return listOf(94, 95, 96) // Fast of Gedaliah
                4 -> return listOf(97, 98, 99)
                5 -> return listOf(100, 101, 102) // Today!
                6 -> return listOf(103, 104, 105)
                7 -> return listOf(106, 107, 108)
                8 -> return listOf(109, 110, 111)
                9 -> return listOf(112, 113, 114) // Erev Yom Kippur
                10 -> return (115..150).toList() // Yom Kippur (36 chapters)
            }
        }
        return emptyList()
    }

    private fun removeCantillation(text: String): String {
        return text.replace(Regex("[\u0591-\u05AF]"), "")
    }

    private fun cleanTehillimText(text: String): String {
        var s = text
        // Replace common HTML tags and their raw/broken versions
        s = s.replace(Regex("<[^>]*>"), "")
        s = s.replace(Regex("\\{פ\\}"), "")
        s = s.replace(Regex("\\{ס\\}"), "")
        s = s.replace(Regex("(?i)class=\"[^\"]*\""), "")
        s = s.replace(Regex("(?i)class=[a-zA-Z0-9_-]+"), "")
        s = s.replace(Regex("(?i)span>?"), "")
        s = s.replace(Regex("(?i)<span"), "")
        s = s.replace(Regex("(?i)thinsp;?"), "")
        s = s.replace(Regex("(?i)nbsp;?"), "")
        s = s.replace("b&", "")
        s = s.replace("b/", "")
        s = s.replace("/|", "")
        s = s.replace("<", "")
        s = s.replace(">", "")
        s = s.replace("&", "")
        s = s.replace("\"", "")
        s = s.replace("'", "")
        
        // Remove leftover English letters/numbers and equations
        s = s.replace(Regex("[a-zA-Z0-9_-]+=[a-zA-Z0-9_-]+"), "")
        s = s.replace(Regex("[a-zA-Z]+"), "")
        
        // Replace multiple spaces with a single space
        s = s.replace(Regex("\\s+"), " ")
        
        // Replace tetragrammaton
        s = replaceTetragrammaton(s)
        
        return s.trim()
    }

    private fun replaceTetragrammaton(text: String): String {
        val regex = Regex("\u05d9[\u0591-\u05C7]*\u05d4[\u0591-\u05C7]*\u05d5[\u0591-\u05C7]*\u05d4[\u0591-\u05C7]*")
        return text.replace(regex, "ה׳")
    }
}
