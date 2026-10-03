package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.data.local.ReadingStateManager
import com.example.ui.screens.StudyCompletionHint
import com.example.ui.theme.DailyRambamTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class StudyCompletionHintTest {
    @get:Rule val compose = createComposeRule()
    @Test fun `completion is date scoped independent of reading anchors`() {
        val manager = ReadingStateManager(ApplicationProvider.getApplicationContext<Context>())
        manager.setCompleted("mitzvot", "2026-10-03")
        assertEquals("2026-10-03", manager.completedDate("mitzvot"))
        assertTrue(manager.isCompleted("mitzvot", "2026-10-03"))
        assertFalse(manager.isCompleted("mitzvot", "2026-10-04"))
        assertNull(manager.getSavedAnchor("mitzvot"))
        manager.setCompleted("mitzvot", "2026-10-03", false)
        assertNull(manager.completedDate("mitzvot"))
    }
    @Test fun `completed hint is small and not an additional action button`() {
        compose.setContent { DailyRambamTheme { StudyCompletionHint() } }
        compose.onNodeWithText("נלמד").assertExists().assertHasNoClickAction()
    }
}
