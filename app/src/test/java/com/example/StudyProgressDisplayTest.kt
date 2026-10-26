package com.example
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.semantics.SemanticsProperties
import com.example.ui.DailyRambamUiState
import com.example.data.local.UserPreferences
import com.example.ui.screens.MainContentList
import com.example.ui.theme.DailyRambamTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h640dp")
class StudyProgressDisplayTest {
    @get:Rule val compose = createComposeRule()
    @Test fun progressReflectsTheDailyReadingAndFinishesWithACheck() {
        var completed by mutableStateOf(false)
        val date = LocalDate.of(2026, 10, 7)
        compose.setContent { DailyRambamTheme {
            MainContentList(uiState = DailyRambamUiState(selectedStudyDate = date,
                preferences = UserPreferences(visibleStudies = setOf("tehillim")),
                studyProgress = mapOf("tehillim" to .4f),
                completedStudyDates = if (completed) mapOf("tehillim" to date.toString()) else emptyMap()),
                onOpenChapter = {}, onOpenDailyLesson = {}, onResumeReading = {}, onOpenChumash = {},
                onOpenTehillim = {}, onOpenTanya = {}, onOpenMitzvah = {}, onOpenSettings = {})
        } }
        fun progress() = compose.onNodeWithTag("study_progress_tehillim").fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo].current
        assertEquals(.4f, progress(), .0001f)
        compose.runOnIdle { completed = true }
        assertEquals(1f, progress(), .0001f)
        compose.onNodeWithContentDescription("השיעור הושלם").assertIsDisplayed()
        compose.onNodeWithText("נלמד").assertDoesNotExist()
    }
}
