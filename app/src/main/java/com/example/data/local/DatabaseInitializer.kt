package com.example.data.local

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.InputStreamReader

class DatabaseInitializer(
    private val context: Context,
    private val rambamDao: RambamDao
) {

    suspend fun initializeIfNeeded() = withContext(Dispatchers.IO) {
        val existingCount = rambamDao.getHalachotCount()
        if (existingCount > 0) {
            return@withContext
        }

        try {
            context.assets.open("sefer_nashim.json").use { inputStream ->
                InputStreamReader(inputStream, Charsets.UTF_8).use { reader ->
                    val jsonStr = reader.readText()
                    val root = JSONObject(jsonStr)
                    val sectionsArray = root.optJSONArray("sections") ?: return@use

                    val sectionsToInsert = mutableListOf<ContentSectionEntity>()
                    val chaptersToInsert = mutableListOf<ChapterEntity>()
                    val halachotToInsert = mutableListOf<HalachaEntity>()

                    for (sIdx in 0 until sectionsArray.length()) {
                        val sObj = sectionsArray.getJSONObject(sIdx)
                        val secId = sObj.getString("sectionId")
                        val secTitle = sObj.getString("titleHebrew")
                        val orderIdx = sObj.optInt("orderIndex", sIdx + 1)
                        val chaptersArray = sObj.optJSONArray("chapters") ?: continue

                        sectionsToInsert.add(
                            ContentSectionEntity(
                                sectionId = secId,
                                workId = "mishneh-torah",
                                titleHebrew = secTitle,
                                chapterCount = chaptersArray.length(),
                                orderIndex = orderIdx
                            )
                        )

                        for (cIdx in 0 until chaptersArray.length()) {
                            val cObj = chaptersArray.getJSONObject(cIdx)
                            val chNum = cObj.getInt("chapterNumber")
                            val chHeb = cObj.getString("chapterHebrew")
                            val chId = "${secId}_$chNum"
                            val source = cObj.optString("sourceCredit", "תורת אמת")
                            val lic = cObj.optString("license", "CC BY-NC-SA 2.5")
                            val halachotArray = cObj.optJSONArray("halachot") ?: continue

                            chaptersToInsert.add(
                                ChapterEntity(
                                    id = chId,
                                    sectionId = secId,
                                    chapterNumber = chNum,
                                    chapterHebrew = chHeb,
                                    halachotCount = halachotArray.length(),
                                    sourceCredit = source,
                                    license = lic
                                )
                            )

                            for (hIdx in 0 until halachotArray.length()) {
                                val hObj = halachotArray.getJSONObject(hIdx)
                                val hNum = hObj.getInt("halachaNumber")
                                val hHeb = hObj.getString("halachaHebrew")
                                val letter = hObj.optString("letter", "")
                                val nikud = hObj.getString("textWithNikud")
                                val plain = hObj.getString("textPlain")
                                val fp = hObj.optString("quoteFingerprint", "")
                                val hId = "${chId}_$hNum"

                                halachotToInsert.add(
                                    HalachaEntity(
                                        id = hId,
                                        chapterId = chId,
                                        sectionId = secId,
                                        chapterNumber = chNum,
                                        halachaNumber = hNum,
                                        halachaHebrew = hHeb,
                                        letter = letter,
                                        textWithNikud = nikud,
                                        textPlain = plain,
                                        quoteFingerprint = fp
                                    )
                                )
                            }
                        }
                    }

                    rambamDao.insertSections(sectionsToInsert)
                    rambamDao.insertChapters(chaptersToInsert)
                    rambamDao.insertHalachot(halachotToInsert)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
