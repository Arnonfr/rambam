package com.example

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.example.domain.RambamDiagrams
import com.example.ui.components.RambamDiagramBlock
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RambamDiagramRenderingTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun allEighteenDiagramsRenderIncludingTheBundledOriginal() {
        val selected = mutableStateOf(RambamDiagrams.all.first())
        composeRule.setContent { MaterialTheme { RambamDiagramBlock(selected.value) } }
        RambamDiagrams.all.forEach { diagram ->
            composeRule.runOnIdle { selected.value = diagram }
            composeRule.onNodeWithTag("rambam_diagram_${diagram.id}").assertExists()
        }
    }
}
