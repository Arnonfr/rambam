package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.text.style.TextAlign
import com.example.data.local.SavedStudyCompletion
import com.example.data.local.UserPreferences
import com.example.domain.content.CompletionReaderRow
import com.example.ui.components.*

@Composable
fun StudyCompletionReaderScreen(
    entry: SavedStudyCompletion,
    preferences: UserPreferences,
    onBack: () -> Unit,
    onSave: (SavedStudyCompletion) -> Unit,
    onCompleted: () -> Unit
) {
    BackHandler(onBack = onBack)
    val listState = rememberLazyListState()
    val manual = rememberManualStudyCompletionState(entry.updatedAt)
    val rows = remember(entry) { entry.blocks.mapIndexed { index, block -> CompletionReaderRow(index, block) } }
    val background = when (preferences.readerTheme) {
        "dark", "oled" -> Color(0xFF121214)
        "sepia" -> Color(0xFFF5EEDA)
        else -> Color(0xFFFFFDF8)
    }
    val foreground = if (preferences.readerTheme in setOf("dark", "oled")) Color(0xFFE2E2E6) else Color(0xFF17120F)
    val pull = rememberEndOfLessonPullState(entry.updatedAt, { listState.canScrollForward }, onCompleted)
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(Modifier.fillMaxSize().background(background)) {
            LazyColumn(state = listState,
                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp).endOfLessonPull(pull),
                contentPadding = PaddingValues(top = 64.dp, bottom = 160.dp)) {
                itemsIndexed(entry.blocks, key = { _, block -> block.key }) { index, block ->
                    var blockTop by remember { mutableFloatStateOf(0f) }
                    var textTop by remember { mutableFloatStateOf(0f) }
                    Column(Modifier.fillMaxWidth().padding(bottom = 20.dp), horizontalAlignment = Alignment.Start) {
                        Text(block.label, color = foreground.copy(alpha = .65f), style = MaterialTheme.typography.labelMedium,
                            textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth())
                        StudyTextBlock(indexLetter = "", textWithNikud = block.textWithNikud, textPlain = block.textPlain,
                            modifier = Modifier.onGloballyPositioned {
                                blockTop = it.positionInParent().y
                                manual.textTop(index, blockTop + textTop)
                            },
                            preferences = preferences, rashi = block.rashi, textColor = foreground,
                            fontSizeSp = preferences.fontSizeSp,
                            onTextLayout = { manual.measure(index, it) },
                            onTextTop = { textTop = it; manual.textTop(index, blockTop + it) },
                            testTag = "completion_text_$index")
                    }
                }
            }
            CompactReaderHeader("${entry.title} · השלמה", entry.dateLabel, Color(0xFF6D52A0),
                if (entry.blocks.isEmpty()) 0f else listState.firstVisibleItemIndex.toFloat() / entry.blocks.size,
                preferences.readerTheme, Modifier.align(Alignment.TopCenter))
            ManualStudyCompletionControls(manual, listState, rows, entry.track, entry.studyDate,
                entry.dateLabel, entry.title, cutAtMiddle = entry.track in listOf("tanya", "hayom_yom"), onSave = onSave)
            Surface(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding(),
                color = background, shadowElevation = 4.dp) {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    Button(onClick = onCompleted, modifier = Modifier.fillMaxWidth().testTag("finish_saved_completion")) {
                        Text("השלמתי את השיעור")
                    }
                    TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth().testTag("back_saved_completion")) {
                        Text("חזרה לשיעורים")
                    }
                }
            }
        }
    }
}
