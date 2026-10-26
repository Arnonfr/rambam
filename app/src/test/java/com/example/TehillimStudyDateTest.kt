package com.example

import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.UserPreferences
import com.example.data.tehillim.TehillimRepository
import com.example.domain.schedule.DailyScheduleEngine
import com.example.domain.tehillim.DailyTehillimLesson
import com.example.ui.DailyRambamUiState
import com.example.ui.screens.MainContentList
import com.example.ui.theme.DailyRambamTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "iw-rIL-w360dp-h640dp")
class TehillimStudyDateTest {
    @get:Rule val compose = createComposeRule()
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test fun firstOfCheshvanLoadsFirstDaysChaptersWithoutAnyRambamState() = runBlocking {
        val repository = TehillimRepository(context)
        val engine = DailyScheduleEngine(context)
        val first = repository.getDailyTehillimLesson(LocalDate.of(2026, 10, 12), engine)
        assertEquals(1, first.dayOfMonth)
        assertEquals("א׳", first.dayOfMonthHebrew)
        assertEquals("2026-10-12", first.date)
        assertEquals((1..9).toList(), first.chapters.map { it.chapterNumber })
        val fifth = repository.getDailyTehillimLesson(LocalDate.of(2026, 10, 16), engine)
        assertEquals(5, fifth.dayOfMonth)
        assertEquals("2026-10-16", fifth.date)
        assertEquals((29..34).toList(), fifth.chapters.map { it.chapterNumber })
    }

    private fun show(state: DailyRambamUiState) {
        compose.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                DailyRambamTheme {
                    MainContentList(uiState = state, onOpenChapter = {}, onOpenDailyLesson = {},
                        onResumeReading = {}, onOpenChumash = {}, onOpenTehillim = {}, onOpenTanya = {},
                        onOpenMitzvah = {}, onOpenSettings = {})
                }
            }
        }
    }

    @Test fun dashboardCannotCombineStaleFifthDayWithTheCurrentMonth() {
        val date = LocalDate.of(2026, 10, 12)
        val current = DailyScheduleEngine(context).getDailyLesson(
            date.atTime(12, 0).atZone(ZoneId.of("Asia/Jerusalem")).toInstant(),
            zoneId = ZoneId.of("Asia/Jerusalem"), dayBoundary = "midnight")
        show(DailyRambamUiState(selectedStudyDate = date, dailyLesson = current,
            preferences = UserPreferences(visibleStudies = setOf("tehillim")),
            dailyTehillim = DailyTehillimLesson(date.toString(), 5, "ה׳", emptyList())))
        compose.onNodeWithText("יום א׳ בחודש").assertExists()
        compose.onNodeWithTag("study_metric_tehillim_0").assertDoesNotExist()
        compose.onNodeWithTag("study_metric_tehillim_1").assertDoesNotExist()
        compose.onNodeWithText("ה׳").assertDoesNotExist()
    }

    @Test fun dashboardDoesNotInventADayWhileTheCalendarIsLoading() {
        show(DailyRambamUiState(preferences = UserPreferences(visibleStudies = setOf("tehillim"))))
        compose.onNodeWithText("טוען...").assertExists()
        compose.onNodeWithText("יום ב׳ בחודש").assertDoesNotExist()
        compose.onNodeWithTag("study_metric_tehillim_0").assertDoesNotExist()
    }
}
