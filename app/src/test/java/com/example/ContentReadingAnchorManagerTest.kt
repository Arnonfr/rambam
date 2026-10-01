package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.ContentReadingAnchor
import com.example.data.local.ContentReadingAnchorManager
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
class ContentReadingAnchorManagerTest {
    private lateinit var manager: ContentReadingAnchorManager

    @Before
    fun setUp() {
        manager = ContentReadingAnchorManager(ApplicationProvider.getApplicationContext<Context>())
        manager.clearAll()
    }

    @After
    fun tearDown() = manager.clearAll()

    @Test
    fun `returns an anchor only for its study date`() {
        manager.save(anchor("sefer_hamitzvot", "2026-09-27", 4))

        assertEquals(4, manager.get("sefer_hamitzvot", "2026-09-27")?.blockIndex)
        assertNull(manager.get("sefer_hamitzvot", "2026-09-28"))
    }

    @Test
    fun `keeps independent anchors for prayer and study`() {
        manager.save(anchor("sefer_hamitzvot", "2026-09-27", 2))
        manager.save(anchor("prayer_shacharit", "2026-09-27", 11))

        assertEquals(2, manager.get("sefer_hamitzvot", "2026-09-27")?.blockIndex)
        assertEquals(11, manager.get("prayer_shacharit", "2026-09-27")?.blockIndex)
    }

    private fun anchor(contentId: String, date: String, index: Int) = ContentReadingAnchor(
        contentId = contentId,
        assignmentDate = date,
        sectionId = "section",
        blockId = "block_$index",
        blockIndex = index,
        pageNumber = 12,
        normalizedYOffset = 0.37f
    )
}
