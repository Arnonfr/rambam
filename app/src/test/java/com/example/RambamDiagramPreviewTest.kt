package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.example.domain.RambamDiagrams
import com.example.ui.components.RambamDiagramBlock
import com.example.ui.theme.DailyRambamTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w760dp-h1280dp-mdpi")
class RambamDiagramPreviewTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun exportActualComposeDiagramsForReview() {
        val destination = System.getenv("DIAGRAM_PREVIEW_DIR") ?: return
        System.setProperty("roborazzi.test.record", "true")
        val groups = listOf(
            "temple" to listOf("altar_height", "altar_foundation", "lintels", "ephod"),
            "sanctuaries" to listOf("heichal", "courtyard"),
            "other" to listOf("tefillin_mold", "head_order", "hand_order", "shoham", "round_beam", "land_boundary"),
            "paragraphs" to listOf("open_1", "open_2", "closed_1", "closed_2", "closed_3")
        )
        val selected = mutableStateOf(0)
        composeRule.setContent {
            DailyRambamTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Column(Modifier.fillMaxSize().testTag("diagram_gallery").background(Color(0xFFFFFCF6)).padding(16.dp)) {
                        Text("תרשימי הרמב״ם — תצוגה מהקוד", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        groups[selected.value].second.chunked(2).forEach { pair ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                                pair.forEach { id ->
                                    Column(Modifier.weight(1f)) {
                                        RambamDiagramBlock(RambamDiagrams.all.single { it.id == id })
                                    }
                                }
                                if (pair.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
        groups.forEachIndexed { index, group ->
            composeRule.runOnIdle { selected.value = index }
            val output = File(destination, "${group.first}.png")
            output.parentFile?.mkdirs()
            composeRule.onNodeWithTag("diagram_gallery").captureRoboImage(filePath = output.path)
        }
        System.clearProperty("roborazzi.test.record")
    }
}
