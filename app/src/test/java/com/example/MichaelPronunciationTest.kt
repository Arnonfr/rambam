package com.example

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import com.example.data.speech.MichaelPronunciation
import com.example.data.speech.PiperHebrewSpeech
import com.example.domain.speech.DailySpeechText
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
class MichaelPronunciationTest {
    private val context get()=ApplicationProvider.getApplicationContext<Context>()

    @Test fun selectedMichaelSampleKeepsItsApprovedPhonemeSequence() {
        val source=checkNotNull(javaClass.getResource("/michael-reference-text.txt")).readText()
        val expected=checkNotNull(javaClass.getResource("/michael-reference-phonemes.txt")).readText()
        assertEquals(expected.trim(),MichaelPronunciation(context).phonemes(DailySpeechText.prepare(source)))
    }

    @Test fun divineNameIsReadAsHashemAndSourceReferencesAreNotRead() {
        assertEquals("haʃˈem",MichaelPronunciation.divineName("ה'"))
        assertEquals("laʃˈem",MichaelPronunciation.divineName("לַה'"))
        assertEquals("haʃˈem",MichaelPronunciation.divineName("יְיָ"))
        assertEquals("vehaʃˈem",MichaelPronunciation.divineName("וַיְיָ'"))
        listOf("לָהּ'","משֶׁה'","שֶׂה'","שָׁמָּה'","הֵ'","בָּמָה'","תּוֹרָה").forEach { assertNull(it,MichaelPronunciation.divineName(it)) }
        assertEquals("שֶׁנֶּאֱמַר 'אֲנִי ה''. הַנּוֹפוֹת זֶה",DailySpeechText.speak("שֶׁנֶּאֱמַר (ויקרא יט-יב) 'אֲנִי ה''. (הַנּוֹפוֹת) זֶה"))
        val ipa=MichaelPronunciation(context).phonemes(DailySpeechText.speak("יַד ה'. שֶׁנֶּאֱמַר (שמות לא-יח) אֲנִי ה'."))
        assertTrue(ipa,ipa.contains("haʃˈem"));assertFalse(ipa,ipa.contains("ʃmˈot"))
    }

    @Test fun upcomingLessonsUseSentenceContextAndHalachaNumbersAreNotRead() {
        val file=File(context.cacheDir,"michael-context.db")
        context.assets.open("database/rambam.db").use { input -> file.outputStream().use {input.copyTo(it)} }
        val halachot=SQLiteDatabase.openDatabase(file.absolutePath,null,SQLiteDatabase.OPEN_READONLY).use { db ->
            db.rawQuery("SELECT textWithNikud FROM halachot WHERE sectionId='mechirah' AND chapterNumber=1 ORDER BY halachaNumber",null).use { c ->
                generateSequence { if(c.moveToNext()) c.getString(0) else null }.toList() }
        }
        val plan=DailySpeechText.lesson("2026-10-23","three","",listOf(Triple("mechirah_1","הלכות מכירה, פרק 1",halachot)))
        assertTrue(plan.parts.none { it.text.contains(Regex("(^|\\. )יא ")) })
        val ipa=plan.parts.joinToString(" ") { MichaelPronunciation(context).phonemes(PiperHebrewSpeech.speechWords(DailySpeechText.speak(it.text))) }
        // In sentence context: vocal shva in בְּכֶסֶף; the word index alone says "bχˈesef".
        assertTrue(ipa.contains("beχˈesef"));assertFalse(ipa.contains("bχˈesef"))
        assertTrue(ipa.contains("kanˈa"))
    }

    @Test fun phonemesAreSynthesisedOneSentenceAtATimeWithEvenPauses() {
        val parts=PiperHebrewSpeech.sentences("kˈol haʔovˈeʁ midaʔtˈo belˈo ʔonˈes. jˈad haʃˈem. ʔenˈej haʃˈem. veχaʔajotsˈe bidvaʁˈim haʔˈelu; ʔˈela")
        assertEquals(listOf("kˈol haʔovˈeʁ midaʔtˈo belˈo ʔonˈes.","jˈad haʃˈem. ʔenˈej haʃˈem.","veχaʔajotsˈe bidvaʁˈim haʔˈelu; ʔˈela"),parts)
        assertEquals(.45f,PiperHebrewSpeech.pauseAfter(parts[0],false))
        assertEquals(.32f,PiperHebrewSpeech.pauseAfter("a b c;",false))
        assertEquals(.7f,PiperHebrewSpeech.pauseAfter(parts[2],true))
        val trimmed=PiperHebrewSpeech.trim(FloatArray(5000).also { for(i in 2000 until 3000) it[i]=.5f })
        assertEquals(220+1000+879,trimmed.size)
    }

    @Test fun everyBundledDailyRambamPartHasPreparedPronunciationIncludingChapterIntroductions() {
        val file=File(context.cacheDir,"michael-corpus.db")
        context.assets.open("database/rambam.db").use { input -> file.outputStream().use {input.copyTo(it)} }
        val pronunciation=MichaelPronunciation(context)
        val alphabet=context.assets.open("speech/michael.config.json").bufferedReader().use {
            JSONObject(it.readText()).getJSONObject("phoneme_id_map")
        }
        val chapters=linkedMapOf<String,Pair<String,MutableList<String>>>()
        SQLiteDatabase.openDatabase(file.absolutePath,null,SQLiteDatabase.OPEN_READONLY).use { db ->
            db.rawQuery("SELECT h.chapterId,s.titleHebrew,h.chapterNumber,h.textWithNikud FROM halachot h JOIN content_sections s ON s.sectionId=h.sectionId ORDER BY s.orderIndex,h.chapterNumber,h.halachaNumber",null).use { cursor ->
                while(cursor.moveToNext()) {
                    chapters.getOrPut(cursor.getString(0)) { "${cursor.getString(1)}, פרק ${cursor.getInt(2)}" to mutableListOf() }
                        .second.add(cursor.getString(3))
                }
            }
        }
        var checked=0
        chapters.forEach { (id,data) ->
            val plan=DailySpeechText.lesson("coverage","three","",listOf(Triple(id,data.first,data.second)))
            plan.parts.forEach { part ->
                val ipa=pronunciation.phonemes(PiperHebrewSpeech.speechWords(DailySpeechText.speak(part.text)))
                assertTrue("$id has no speech",ipa.isNotBlank())
                assertTrue("$id has unsupported phonemes",ipa.all {alphabet.has(it.toString())})
                checked++
            }
        }
        assertTrue(chapters.size>1000)
        assertTrue(checked>15000)
        println("Prepared Michael coverage: ${chapters.size} chapters, $checked speech parts")
    }
}
