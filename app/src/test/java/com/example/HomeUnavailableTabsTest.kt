package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.ui.HomeTab
import com.example.ui.screens.FloatingDarkNavBar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class HomeUnavailableTabsTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun unfinishedTabsShowToastWithoutNavigatingAndStudyStillWorks() {
        val selections = mutableListOf<HomeTab>()
        composeRule.setContent {
            FloatingDarkNavBar(HomeTab.STUDY, onSelectTab = { selections.add(it) })
        }

        for (label in listOf("תפילות", "פודקאסטים")) {
            composeRule.onNodeWithText(label).performClick()
            composeRule.runOnIdle {
                assertEquals("בבנייה", ShadowToast.getTextOfLatestToast())
                assertTrue(selections.isEmpty())
            }
        }

        composeRule.onNodeWithText("לימוד").performClick()
        composeRule.runOnIdle { assertEquals(listOf(HomeTab.STUDY), selections) }
    }
}
