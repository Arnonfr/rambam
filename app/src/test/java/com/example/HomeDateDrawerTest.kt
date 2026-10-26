package com.example

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.ui.DateDrawerState
import com.example.ui.screens.DateDrawer
import com.example.ui.theme.DailyRambamTheme
import com.example.ui.util.HebrewDateHelper
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h640dp")
class HomeDateDrawerTest {
    val compose = createComposeRule()
    @get:Rule val hostAndCompose = withComposeHost(compose)
    @Test fun weekDatesRemainInteractiveWithoutADuplicateSettingsEntry() {
        val today = LocalDate.of(2026, 10, 7)
        var selected by mutableStateOf(today)
        var expanded by mutableStateOf(false)
        compose.setContent { DailyRambamTheme {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                DateDrawer(selected, today, if (expanded) DateDrawerState.EXPANDED else DateDrawerState.COLLAPSED,
                    null, { date -> "${HebrewDateHelper.toHebrewNumeral(26 + ChronoUnit.DAYS.between(today, date).toInt())} בתשרי תשפ״ז" },
                    { selected = it }, { selected = selected.plusDays(it) }, { expanded = !expanded })
            }
        } }
        compose.onNodeWithText("רביעי כ״ו תשרי").assertIsDisplayed()
        compose.onNodeWithText("תשפ״ז", substring = true).assertDoesNotExist()
        compose.onNodeWithTag("home_settings_button").assertDoesNotExist()
        compose.onNodeWithTag("home_date_expand").performClick()
        compose.onNodeWithTag("home_settings_button").assertDoesNotExist()
        compose.onRoot().captureRoboImage(filePath = "build/review/home-date-week.png")
        compose.onNodeWithTag("home_day_${today.plusDays(1)}").performClick()
        compose.runOnIdle { assertEquals(today.plusDays(1), selected) }
        compose.onNodeWithTag("home_date_expand").performClick()
        compose.onNodeWithTag("home_settings_button").assertDoesNotExist()
    }
}
