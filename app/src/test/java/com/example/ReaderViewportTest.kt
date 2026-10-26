package com.example

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ContentReadingAnchorManager
import com.example.ui.components.rememberReaderViewportState
import com.example.ui.theme.DailyRambamTheme
import kotlinx.coroutines.launch
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "iw-rIL-w360dp-h800dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReaderViewportTest {
    val compose = createComposeRule()
    @get:Rule val host = withComposeHost(compose)

    @Test fun longParagraphRestoresTheSameVisibleLineAfterReaderIsDestroyed() {
        var shown by mutableStateOf(true)
        var date by mutableStateOf("2026-10-24")
        var insertHeader by mutableStateOf(false)
        lateinit var list: LazyListState
        lateinit var scope: kotlinx.coroutines.CoroutineScope
        lateinit var manager: ContentReadingAnchorManager
        compose.setContent { DailyRambamTheme {
            manager = ContentReadingAnchorManager(LocalContext.current)
            if(shown) {
                val keys = (if(insertHeader) listOf("new_header") else emptyList()) + listOf("intro", "long", "tail")
                list = rememberReaderViewportState("viewport_test",date,keys)
                scope = rememberCoroutineScope()
                LazyColumn(state=list,modifier=Modifier.size(320.dp,500.dp),contentPadding=PaddingValues(top=64.dp)) {
                    items(keys,key={it}) { key ->
                        if(key == "long") Text("הלכה ארוכה מאוד שאותה קוראים שורה אחר שורה. ".repeat(100),
                            fontSize=24.sp,lineHeight=40.sp,modifier=Modifier.testTag("long_text"))
                        else Spacer(Modifier.height(80.dp))
                    }
                }
            }
        } }
        compose.runOnIdle { scope.launch { list.scrollToItem(1,1100) } }
        compose.waitForIdle()
        fun visibleLine(): Int {
            val node=compose.onNodeWithTag("long_text",useUnmergedTree=true)
            val layouts=mutableListOf<TextLayoutResult>()
            node.performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) { it(layouts) }
            return layouts.single().getLineForVerticalPosition((list.firstVisibleItemScrollOffset-64*compose.density.density).coerceAtLeast(0f))
        }
        val before=visibleLine()
        assertTrue(before>5)
        compose.runOnIdle { shown=false }
        compose.waitForIdle()
        compose.runOnIdle {
            val anchor=manager.get("viewport_viewport_test",date)!!
            assertEquals(1100,anchor.textOffset)
            assertEquals("long",anchor.blockId)
            shown=true
        }
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(1,list.firstVisibleItemIndex); assertEquals(1100,list.firstVisibleItemScrollOffset) }
        assertEquals(before,visibleLine())
        // A newly inserted header must not shift the saved paragraph or its line.
        compose.runOnIdle { shown=false }; compose.waitForIdle()
        compose.runOnIdle { insertHeader=true;shown=true };compose.waitForIdle()
        compose.runOnIdle { assertEquals(2,list.firstVisibleItemIndex);assertEquals(1100,list.firstVisibleItemScrollOffset) }
        assertEquals(before,visibleLine())
        // Another assignment date starts independently.
        compose.runOnIdle { shown=false };compose.waitForIdle()
        compose.runOnIdle { date="2026-10-25";shown=true };compose.waitForIdle()
        compose.runOnIdle { assertEquals(0,list.firstVisibleItemIndex);assertEquals(0,list.firstVisibleItemScrollOffset) }
    }
}
