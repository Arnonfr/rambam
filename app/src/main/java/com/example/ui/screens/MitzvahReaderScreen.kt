@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.example.ui.screens

import android.app.Activity
import android.view.WindowManager
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ContentReadingAnchor
import com.example.data.local.UserPreferences
import com.example.data.mitzvot.MitzvahLessonEntry
import com.example.domain.mitzvot.DailyMitzvahAssignment
import com.example.domain.mitzvot.MitzvahAssignmentType
import com.example.ui.components.*
import com.example.ui.theme.FontStyleOption
import com.example.ui.util.HebrewNumberFormatter
import com.example.ui.util.HebrewTextNormalizer
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

private data class MitzvahReaderBlock(
    val id: String,
    val sectionId: String,
    val title: String,
    val text: String,
    val isHeading: Boolean
)

@Composable
fun MitzvahReaderScreen(
    assignment: DailyMitzvahAssignment,
    entries: List<MitzvahLessonEntry>,
    preferences: UserPreferences,
    hebrewDateText: String,
    savedAnchor: ContentReadingAnchor?,
    onBack: () -> Unit,
    onCompleted: () -> Unit = {},
    onSavePosition: (blockIndex: Int, blockId: String, sectionId: String) -> Unit,
    onNextDay: () -> Unit,
    onPrevDay: () -> Unit,
    onOpenDatePicker: () -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onLineSpacingChange: (Float) -> Unit,
    onFontFamilyChange: (String) -> Unit,
    onNikudToggle: (Boolean) -> Unit,
    onThemeChange: (String) -> Unit,
    onKeepScreenOnChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    val resumeAnchor = remember(assignment.studyDate) { savedAnchor }
    val context = LocalContext.current
    DisposableEffect(preferences.keepScreenOn) {
        val window = (context as? Activity)?.window
        if (preferences.keepScreenOn) window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    val blocks = remember(assignment, entries) {
        buildList {
            entries.forEachIndexed { entryIndex, entry ->
                val sectionId = when (entry.item.type) {
                    MitzvahAssignmentType.POSITIVE -> "positive_${entry.item.number}"
                    MitzvahAssignmentType.NEGATIVE -> "negative_${entry.item.number}"
                    MitzvahAssignmentType.SPECIAL -> "special_$entryIndex"
                }
                val title = when (entry.item.type) {
                    MitzvahAssignmentType.POSITIVE -> "מצוות עשה ${HebrewNumberFormatter.toHebrewNumeral(entry.item.number ?: 0, true)}"
                    MitzvahAssignmentType.NEGATIVE -> "מצוות לא תעשה ${HebrewNumberFormatter.toHebrewNumeral(entry.item.number ?: 0, true)}"
                    MitzvahAssignmentType.SPECIAL -> entry.item.title.orEmpty()
                }
                add(MitzvahReaderBlock("${sectionId}_heading", sectionId, title, "", true))
                entry.paragraphs.forEachIndexed { paragraphIndex, paragraph ->
                    add(
                        MitzvahReaderBlock(
                            id = "${sectionId}_$paragraphIndex",
                            sectionId = sectionId,
                            title = title,
                            text = HebrewTextNormalizer.forDisplay(paragraph),
                            isHeading = false
                        )
                    )
                }
            }
        }
    }

    val listState = rememberLazyListState()
    var showTypographySheet by remember { mutableStateOf(false) }
    var restored by remember(assignment.studyDate) { mutableStateOf(false) }
    LaunchedEffect(blocks, resumeAnchor) {
        if (!restored && blocks.isNotEmpty()) {
            val target = resumeAnchor?.blockIndex?.coerceIn(0, blocks.lastIndex)
            listState.scrollToItem(if (target == null) 0 else target + 1)
            restored = true
        }
    }

    fun readableBlockIndex(visibleItemIndex: Int): Int? {
        if (blocks.isEmpty()) return null
        val rawIndex = (visibleItemIndex - 1).coerceIn(0, blocks.lastIndex)
        if (!blocks[rawIndex].isHeading) return rawIndex
        return (rawIndex..blocks.lastIndex).firstOrNull { !blocks[it].isHeading }
            ?: (rawIndex downTo 0).firstOrNull { !blocks[it].isHeading }
            ?: rawIndex
    }

    fun saveVisibleBlock() {
        if (!restored) return
        val index = readableBlockIndex(listState.firstVisibleItemIndex) ?: return
        val block = blocks[index]
        onSavePosition(index, block.id, block.sectionId)
    }

    LaunchedEffect(listState, restored, blocks) {
        if (!restored) return@LaunchedEffect
        snapshotFlow { listState.firstVisibleItemIndex }
            .collectLatest { visible ->
                delay(250)
                val index = readableBlockIndex(visible) ?: return@collectLatest
                blocks.getOrNull(index)?.let { onSavePosition(index, it.id, it.sectionId) }
            }
    }

    DisposableEffect(assignment.studyDate, blocks, restored) {
        onDispose { saveVisibleBlock() }
    }

    val completionPullState = rememberEndOfLessonPullState(
        key = assignment.studyDate,
        canScrollForward = { listState.canScrollForward },
        onCompleted = onCompleted,
        onExitAfterCompletion = onBack
    )
    val scrollProgress by remember(blocks) {
        derivedStateOf {
            if (blocks.size <= 1) 0f
            else ((listState.firstVisibleItemIndex - 1).coerceAtLeast(0).toFloat() / blocks.lastIndex).coerceIn(0f, 1f)
        }
    }
    val currentLocation by remember(blocks) {
        derivedStateOf {
            val index = (listState.firstVisibleItemIndex - 1).coerceAtLeast(0)
            blocks.getOrNull(index)?.title ?: "שיעור ${assignment.lessonNumber}"
        }
    }
    val background = when (preferences.readerTheme) {
        "dark" -> Color(0xFF121214)
        "sepia" -> Color(0xFFF5EEDA)
        else -> Color(0xFFFFFDF8)
    }
    val foreground = if (preferences.readerTheme == "dark") Color(0xFFE8E8EC) else Color(0xFF17120F)
    val accent = Color(0xFF72D7E8)
    val activeFont = FontStyleOption.fromId(preferences.fontFamily).fontFamily

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl,
        androidx.compose.foundation.LocalOverscrollConfiguration provides null) {
        Box(modifier.fillMaxSize().background(background)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().endOfLessonPull(completionPullState).padding(horizontal = 20.dp),
                contentPadding = PaddingValues(top = 40.dp, bottom = 220.dp)
            ) {
                // Retain the zero-height slot so existing saved paragraph indices stay valid.
                item(key = "mitzvah_intro") { }

                itemsIndexed(blocks, key = { _, block -> block.id }) { index, block ->
                    if (block.isHeading) {
                        Text(
                            text = block.title,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = foreground,
                            modifier = Modifier.fillMaxWidth().padding(top = if (index == 0) 0.dp else 20.dp, bottom = 12.dp)
                        )
                    } else {
                        StudyTextBlock(
                            indexLetter = "",
                            textWithNikud = block.text,
                            textPlain = HebrewTextNormalizer.stripMarks(block.text),
                            preferences = preferences,
                            fontFamily = activeFont,
                            textColor = foreground,
                            isReadingAnchor = index == resumeAnchor?.blockIndex,
                            modifier = Modifier.padding(bottom = 18.dp),
                            testTag = "mitzvah_block_$index"
                        )
                    }
                }
            }

            CompactReaderHeader(
                title = "ספר המצוות",
                location = currentLocation,
                accentColor = accent,
                scrollProgress = scrollProgress,
                readerTheme = preferences.readerTheme,
                modifier = Modifier.align(Alignment.TopCenter)
            )
            EndOfLessonPullIndicator(
                state = completionPullState,
                accentColor = accent,
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 92.dp)
            )
            FloatingReaderBar(
                currentHalachaText = currentLocation,
                hebrewDateText = hebrewDateText,
                scrollProgress = scrollProgress,
                onBack = onBack,
                onOpenDatePicker = onOpenDatePicker,
                onTypographyClick = { showTypographySheet = true },
                readerTheme = preferences.readerTheme,
                onNextDay = onNextDay,
                onPrevDay = onPrevDay,
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding()
            )
        }

        if (showTypographySheet) {
            ReaderTypographySheet(
                preferences = preferences,
                onDismiss = { showTypographySheet = false },
                onFontSizeChange = onFontSizeChange,
                onLineSpacingChange = onLineSpacingChange,
                onFontFamilyChange = onFontFamilyChange,
                onNikudToggle = onNikudToggle,
                onThemeChange = onThemeChange,
                onKeepScreenOnChange = onKeepScreenOnChange
            )
        }
    }
}
