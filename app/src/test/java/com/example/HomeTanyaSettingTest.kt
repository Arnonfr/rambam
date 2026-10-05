package com.example

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsOff
import com.example.data.local.UserPreferences
import com.example.ui.components.SettingsDialog
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class HomeTanyaSettingTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun homeSwitchUsesTheExistingReaderPreferenceAndDoesNotHideTanya() {
        val preferences = mutableStateOf(UserPreferences())
        composeRule.setContent {
            SettingsDialog(preferences.value, onDismiss = {}, onTrackChange = {},
                onDayBoundaryChange = {}, onCityChange = {}, onOpenAttribution = {},
                onTanyaBookViewChange = { preferences.value = preferences.value.copy(tanyaBookView = it) })
        }
        val toggle = composeRule.onNodeWithTag("home_tanya_book_view")
        toggle.performScrollTo().assertIsOff().performClick().assertIsOn()
        composeRule.runOnIdle {
            assertTrue(preferences.value.tanyaBookView)
            assertTrue("tanya" in preferences.value.visibleStudies)
            // Simulate the reader changing the very same preference.
            preferences.value = preferences.value.copy(tanyaBookView = false)
        }
        toggle.assertIsOff()
    }
}
