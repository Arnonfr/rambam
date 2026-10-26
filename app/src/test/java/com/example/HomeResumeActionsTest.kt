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
    val compose = createComposeRule()
    @get:Rule val hostAndCompose = withComposeHost(compose)
    private var opened = 0
    private var resumed: String? = null
    private var settingsOpened = false
    private var personalOpened = false

    private fun show(savedDate: String? = null, completed: Boolean = false, personalCount: Int = 0) {
        val state = DailyRambamUiState(
            selectedStudyDate = LocalDate.parse("2026-10-01"),
            preferences = UserPreferences(visibleStudies = setOf("tehillim")),
            bookmarkDates = savedDate?.let { mapOf("tehillim" to it) }.orEmpty(),
            completedBookmarks = if (completed) setOf("tehillim") else emptySet())
        compose.setContent {
            DailyRambamTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    MainContentList(personalPsalmCount = personalCount, onOpenPersonalPsalms = { personalOpened = true }, uiState = state, onOpenChapter = {}, onOpenDailyLesson = {},
                        onResumeReading = {}, onOpenChumash = {}, onOpenTehillim = { opened++ },
                        onOpenTanya = {}, onOpenMitzvah = {}, onOpenSettings = { settingsOpened = true },
                        onResumeStudy = { resumed = it })
                }
            }
        }
    }

    @Test fun `personal chapters appear alongside daily only after adding an entry`() {
        show("2026-10-01", personalCount = 2)
        val daily = compose.onNodeWithTag("open_tehillim").fetchSemanticsNode().boundsInRoot
        val personal = compose.onNodeWithTag("home_personal_psalms").fetchSemanticsNode().boundsInRoot
        val resume = compose.onNodeWithTag("resume_tehillim").fetchSemanticsNode().boundsInRoot
        assertEquals(daily.center.y, personal.center.y, 1f)
        org.junit.Assert.assertTrue(personal.left > resume.right)
        org.junit.Assert.assertTrue(resume.left > daily.right)
        compose.onNodeWithTag("home_personal_psalms").performClick()
        org.junit.Assert.assertTrue(personalOpened)
        assertEquals(0, opened)
        compose.onNodeWithTag("study_metric_tehillim_0").assertDoesNotExist()
    }

    @Test fun `personal stays on the right when resume is absent`() {
        show(personalCount = 2)
        compose.onNodeWithTag("resume_tehillim").assertDoesNotExist()
        val personal = compose.onNodeWithTag("home_personal_psalms").fetchSemanticsNode().boundsInRoot
        val daily = compose.onNodeWithTag("open_tehillim").fetchSemanticsNode().boundsInRoot
        org.junit.Assert.assertTrue(personal.left > daily.right)
        compose.onNodeWithTag("home_personal_psalms").performClick()
        org.junit.Assert.assertTrue(personalOpened)
    }

    @Test fun `completed bookmark hides only resume and preserves the personal entry`() {
        show("2026-10-01", completed = true, personalCount = 2)
        compose.onNodeWithTag("resume_tehillim").assertDoesNotExist()
        compose.onNodeWithTag("home_personal_psalms").assertExists()
        compose.onNodeWithTag("open_tehillim").assertExists()
    }

    @Test fun `empty personal list has no home action and title aligns to icon`() {
        show()
        compose.onNodeWithTag("home_personal_psalms").assertDoesNotExist()
        val title = compose.onNodeWithTag("study_title_tehillim", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val icon = compose.onNodeWithTag("study_icon_tehillim", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertEquals(title.center.y, icon.center.y, 1f)
    }

    @Test fun `unfinished today keeps daily lesson and resume independently available`() {
        show("2026-10-01")
        compose.onNodeWithTag("open_tehillim").assertExists()
        compose.onNodeWithTag("resume_tehillim").performClick()
        assertEquals("tehillim", resumed)
        assertEquals(0, opened)
        compose.onNodeWithTag("open_tehillim").performClick()
        assertEquals(1, opened)
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

    @Test fun `fresh lesson shows daily lesson and preferences opens existing settings`() {
        show()
        compose.onNodeWithTag("resume_tehillim").assertDoesNotExist()
        compose.onNodeWithTag("open_tehillim").performClick()
        assertEquals(1, opened)
        compose.onNodeWithTag("home_study_preferences").performClick()
        org.junit.Assert.assertTrue(settingsOpened)
    }

    @Test fun `completed lesson does not offer unfinished resume`() {
        show("2026-10-01", completed = true)
        compose.onNodeWithTag("resume_tehillim").assertDoesNotExist()
        compose.onNodeWithTag("open_tehillim").assertExists()
    }
}
