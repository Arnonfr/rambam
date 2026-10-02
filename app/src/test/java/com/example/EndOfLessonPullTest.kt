@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.example

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.example.ui.components.endOfLessonPull
import com.example.ui.components.rememberEndOfLessonPullState
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w400dp-h900dp")
class EndOfLessonPullTest {
    @get:Rule val compose = createComposeRule()

    private fun content(long: Boolean, onExit: () -> Unit = {}, onCompleted: () -> Unit) {
        compose.setContent {
            val list = rememberLazyListState()
            val pull = rememberEndOfLessonPullState("test", { list.canScrollForward }, onCompleted, onExit)
            CompositionLocalProvider(androidx.compose.foundation.LocalOverscrollConfiguration provides null) {
            LazyColumn(state = list,
                modifier = Modifier.size(320.dp, 500.dp).endOfLessonPull(pull).testTag("reader")) {
                items(if (long) 30 else 1) { Text("שיעור $it", Modifier.height(100.dp)) }
            }
            }
        }
    }

    @Test fun `upward finger pull at end actually completes`() {
        var completions = 0
        content(false) { completions++ }
        compose.onNodeWithTag("reader").performTouchInput { swipeUp() }
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(1, completions) }
    }

    @Test fun `ordinary scrolling before end never completes`() {
        var completions = 0
        content(true) { completions++ }
        compose.onNodeWithTag("reader").performTouchInput { swipeUp() }
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(0, completions) }
    }
    @Test fun `completion saves before returning home and exits only once`() {
        val events = mutableListOf<String>()
        content(false, onExit = { events.add("home") }) { events.add("complete") }
        compose.onNodeWithTag("reader").performTouchInput { swipeUp() }
        compose.waitUntil(timeoutMillis = 5_000) { events.contains("home") }
        compose.runOnIdle { assertEquals(listOf("complete", "home"), events) }
    }
}
