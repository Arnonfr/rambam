package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.ReadingStateManager
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ReadingStateManagerTest {
    private lateinit var manager: ReadingStateManager

    @Before
    fun setUp() {
        manager = ReadingStateManager(ApplicationProvider.getApplicationContext<Context>())
        manager.clear()
    }

    @After
    fun tearDown() = manager.clear()

    @Test
    fun `each lesson keeps an independent reading anchor`() {
        save("one", "rambam_4", 4, 7)
        save("chumash", "וזאת הברכה", 3, 11)
        save("tehillim", "chapter_27", 27, 5)
        save("tanya", "Iggeret 21", 0, 8)

        assertAnchor("one", "rambam_4", 4, 7)
        assertAnchor("chumash", "וזאת הברכה", 3, 11)
        assertAnchor("tehillim", "chapter_27", 27, 5)
        assertAnchor("tanya", "Iggeret 21", 0, 8)
    }

    @Test
    fun `latest global anchor does not erase a track anchor`() {
        save("one", "rambam_2", 2, 3)
        save("chumash", "האזינו", 6, 12)

        assertEquals("chumash", manager.getSavedAnchor()!!.track)
        assertEquals("rambam_2", manager.getSavedAnchor("one")!!.chapterId)
    }

    @Test
    fun `clearing removes global and per lesson anchors`() {
        save("one", "rambam_1", 1, 1)
        manager.clear()

        assertNull(manager.getSavedAnchor())
        assertNull(manager.getSavedAnchor("one"))
    }

    private fun save(track: String, chapterId: String, chapterNumber: Int, index: Int) {
        manager.saveAnchor(
            isReaderActive = true,
            track = track,
            chapterId = chapterId,
            sectionId = "section_$track",
            chapterNumber = chapterNumber,
            halachaId = "item_$index",
            halachaIndex = index,
            scrollOffsetFraction = 0.42f,
            studyDate = "2026-09-26"
        )
    }

    @Test fun `previous date survives reopening storage until that track is explicitly replaced`() {
        save("tanya", "Iggeret 23", 0, 6)
        save("tehillim", "chapter_104", 104, 12)
        val reopened = ReadingStateManager(ApplicationProvider.getApplicationContext<Context>())
        assertEquals("2026-09-26", reopened.getSavedAnchor("tehillim")!!.studyDate)
        org.junit.Assert.assertFalse(reopened.isStudyDateCurrent("tehillim", "2026-09-28"))
        assertEquals(12, reopened.getSavedAnchor("tehillim")!!.halachaIndex)
        reopened.clearTrack("tehillim")
        assertNull(reopened.getSavedAnchor("tehillim"))
        assertNull(reopened.getSavedAnchor())
        assertEquals(6, reopened.getSavedAnchor("tanya")!!.halachaIndex)
    }

    @Test fun `completion belongs to track and date and is cleared for a new lesson`() {
        save("one", "rambam_2", 2, 3)
        manager.setCompleted("one", "2026-09-26")
        org.junit.Assert.assertTrue(manager.isCompleted("one", "2026-09-26"))
        org.junit.Assert.assertFalse(manager.isCompleted("one", "2026-09-27"))
        manager.clearTrack("one")
        org.junit.Assert.assertFalse(manager.isCompleted("one", "2026-09-26"))
    }

    private fun assertAnchor(track: String, chapterId: String, chapterNumber: Int, index: Int) {
        val anchor = manager.getSavedAnchor(track)!!
        assertEquals(track, anchor.track)
        assertEquals(chapterId, anchor.chapterId)
        assertEquals(chapterNumber, anchor.chapterNumber)
        assertEquals(index, anchor.halachaIndex)
        assertEquals("2026-09-26", anchor.studyDate)
        assertEquals(0.42f, anchor.scrollOffsetFraction)
    }
}
