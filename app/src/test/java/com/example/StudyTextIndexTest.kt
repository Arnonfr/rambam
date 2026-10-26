package com.example

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onChild
import com.example.data.local.UserPreferences
import com.example.ui.components.StudyTextBlock
import com.example.ui.theme.DailyRambamTheme
import com.example.ui.util.HebrewTextNormalizer
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class StudyTextIndexTest {
    @get:Rule val compose = createComposeRule()

    @Test fun `verse five retains the divine name with and without nikud`() {
        val pointed = "ה׳ צַדִּיק יִבְחָן"
        val plain = "ה׳ צדיק יבחן"
        compose.setContent {
            DailyRambamTheme {
                Column {
                    StudyTextBlock("ה׳", pointed, plain, UserPreferences(showNikud = true), testTag = "pointed")
                    StudyTextBlock("ה׳", pointed, plain, UserPreferences(showNikud = false), testTag = "plain")
                    // Other verses can begin with their index letter too.
                    StudyTextBlock("א", "אשרי האיש", "אשרי האיש", UserPreferences(), testTag = "first")
                }
            }
        }
        compose.onNodeWithTag("pointed").onChild().assertTextEquals(HebrewTextNormalizer.forDisplay("ה׳. $pointed"))
        compose.onNodeWithTag("plain").onChild().assertTextEquals("ה׳. $plain")
        compose.onNodeWithTag("first").onChild().assertTextEquals("א. אשרי האיש")
    }

    @Test fun `Rambam removes only an explicitly embedded index`() {
        compose.setContent {
            DailyRambamTheme {
                Column {
                    StudyTextBlock("א", "א קֹדֶם", "א קדם", UserPreferences(showNikud = false),
                        testTag = "indexed", textIncludesIndex = true)
                    StudyTextBlock("א", "א* קדם", "א* קדם", UserPreferences(showNikud = false),
                        testTag = "starred", textIncludesIndex = true)
                    StudyTextBlock("א", "אשרי האיש", "אשרי האיש", UserPreferences(showNikud = false),
                        testTag = "unindexed", textIncludesIndex = true)
                }
            }
        }
        compose.onNodeWithTag("indexed").onChild().assertTextEquals("א. קדם")
        compose.onNodeWithTag("starred").onChild().assertTextEquals("א. קדם")
        compose.onNodeWithTag("unindexed").onChild().assertTextEquals("א. אשרי האיש")
    }
}
