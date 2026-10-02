package com.example

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.data.local.UserPreferences
import com.example.ui.DailyRambamUiState
import com.example.ui.screens.MainContentList
import com.example.ui.theme.DailyRambamTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h640dp")
class HomeResumeActionsTest {
    @get:Rule val compose = createComposeRule()
    private var opened = 0
    private var resumed: String? = null

    private fun show(savedDate: String? = null, completed: Boolean = false) {
        val state = DailyRambamUiState(
            selectedStudyDate = LocalDate.parse("2026-10-01"),
            preferences = UserPreferences(visibleStudies = setOf("tehillim")),
            bookmarkDates = savedDate?.let { mapOf("tehillim" to it) }.orEmpty(),
            completedBookmarks = if (completed) setOf("tehillim") else emptySet())
        compose.setContent {
            DailyRambamTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    MainContentList(state, {}, {}, {}, {}, { opened++ }, {}, {}, {},
                        onResumeStudy = { resumed = it })
                }
            }
        }
    }

    @Test fun `unfinished today replaces the arrow with resume`() {
        show("2026-10-01")
        compose.onNodeWithTag("open_tehillim").assertDoesNotExist()
        compose.onNodeWithTag("resume_tehillim").performClick()
        assertEquals("tehillim", resumed)
        assertEquals(0, opened)
    }

    @Test fun `previous lesson offers two independent actions with resume to the right`() {
        show("2026-09-29")
        val arrow = compose.onNodeWithTag("open_tehillim")
        val resume = compose.onNodeWithTag("resume_tehillim")
        org.junit.Assert.assertTrue(resume.fetchSemanticsNode().boundsInRoot.left > arrow.fetchSemanticsNode().boundsInRoot.right)
        resume.performClick()
        assertEquals("tehillim", resumed)
        assertEquals(0, opened)
        arrow.performClick()
        assertEquals(1, opened)
    }

    @Test fun `fresh lesson shows only arrow`() {
        show()
        compose.onNodeWithTag("resume_tehillim").assertDoesNotExist()
        compose.onNodeWithTag("open_tehillim").performClick()
        assertEquals(1, opened)
    }

    @Test fun `completed lesson does not offer unfinished resume`() {
        show("2026-10-01", completed = true)
        compose.onNodeWithTag("resume_tehillim").assertDoesNotExist()
        compose.onNodeWithTag("open_tehillim").assertExists()
    }
}
