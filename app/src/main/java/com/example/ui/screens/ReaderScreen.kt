package com.example.ui.screens

import android.app.Activity
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.local.ChapterEntity
import com.example.data.local.ContentSectionEntity
import com.example.data.local.HalachaEntity
import com.example.data.local.ReadingPositionEntity
import com.example.data.local.UserPreferences
import com.example.domain.schedule.DailyLessonResult
import com.example.ui.ReaderChapterData
import com.example.ui.components.FloatingReaderBar
import com.example.ui.components.ReaderTypographySheet
import com.example.ui.components.StudyTextBlock
import com.example.ui.theme.*
import com.example.ui.util.HebrewNumberFormatter

sealed interface ReaderUiRow {
    val key: String

    data class ChapterHeader(
        val chapter: ChapterEntity,
        val section: ContentSectionEntity?,
        val chapterIndexInLesson: Int,
        val totalChaptersInLesson: Int
    ) : ReaderUiRow {
        override val key: String = "header_${chapter.id}"
    }

    data class HalachaRow(
        val chapter: ChapterEntity,
        val halacha: HalachaEntity,
        val indexInChapter: Int,
        val totalInChapter: Int,
        val chapterIndexInLesson: Int,
        val totalChaptersInLesson: Int
    ) : ReaderUiRow {
        override val key: String = "halacha_${halacha.id}"
    }

    data class ChapterTransition(
        val completedChapter: ChapterEntity,
        val nextChapter: ChapterEntity,
        val isCompleted: Boolean,
        val chapterIndexInLesson: Int,
        val totalChaptersInLesson: Int
    ) : ReaderUiRow {
        override val key: String = "transition_${completedChapter.id}"
    }

    data class LessonCompletedCard(
        val totalChapters: Int,
        val allCompleted: Boolean
    ) : ReaderUiRow {
        override val key: String = "lesson_completion"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    chapter: ChapterEntity,
    section: ContentSectionEntity?,
    halachot: List<HalachaEntity>,
    activeChaptersList: List<ReaderChapterData> = emptyList(),
    savedPosition: ReadingPositionEntity?,
    isCompleted: Boolean,
    preferences: UserPreferences,
    dailyLesson: DailyLessonResult?,
    completedChapterIds: Set<String> = emptySet(),
    onOpenChapter: (String) -> Unit = {},
    onSaveImmediate: (chapterId: String, halachaIndex: Int, scrollOffsetFraction: Float) -> Unit = { _, _, _ -> },
    onBack: () -> Unit,
    onScrollChanged: (chapterId: String, halachaIndex: Int, scrollOffsetFraction: Float) -> Unit,
    onMarkCompleted: (String) -> Unit,
    onNextChapter: () -> Unit,
    onPreviousChapter: () -> Unit,
    onJumpToToday: () -> Unit,
    onOpenDatePicker: () -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onLineSpacingChange: (Float) -> Unit,
    onFontFamilyChange: (String) -> Unit,
    onNikudToggle: (Boolean) -> Unit,
    onThemeChange: (String) -> Unit,
    onKeepScreenOnChange: (Boolean) -> Unit,
    onNextDay: (() -> Unit)? = null,
    onPrevDay: (() -> Unit)? = null
) {
    BackHandler(onBack = onBack)

    var showTypographySheet by remember { mutableStateOf(false) }

    // Keep screen on during study
    val context = LocalContext.current
    DisposableEffect(preferences.keepScreenOn) {
        val window = (context as? Activity)?.window
        if (preferences.keepScreenOn) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Build the continuous list of chapters
    val continuousChapters = remember(activeChaptersList, chapter.id, halachot) {
        if (activeChaptersList.isNotEmpty()) {
            activeChaptersList
        } else {
            listOf(
                ReaderChapterData(
                    chapter = chapter,
                    section = section,
                    halachot = halachot,
                    isCompleted = isCompleted
                )
            )
        }
    }

    // Flatten all chapters into continuous UI rows
    val flatRows = remember(continuousChapters, completedChapterIds) {
        val rows = mutableListOf<ReaderUiRow>()
        val totalChapters = continuousChapters.size

        continuousChapters.forEachIndexed { chIdx, chData ->
            // Chapter Title Header
            rows.add(
                ReaderUiRow.ChapterHeader(
                    chapter = chData.chapter,
                    section = chData.section,
                    chapterIndexInLesson = chIdx + 1,
                    totalChaptersInLesson = totalChapters
                )
            )

            // All Halachot in this chapter
            val hCount = chData.halachot.size
            chData.halachot.forEachIndexed { hIdx, hItem ->
                rows.add(
                    ReaderUiRow.HalachaRow(
                        chapter = chData.chapter,
                        halacha = hItem,
                        indexInChapter = hIdx,
                        totalInChapter = hCount,
                        chapterIndexInLesson = chIdx + 1,
                        totalChaptersInLesson = totalChapters
                    )
                )
            }

            // Continuous transition between chapters in a multi-chapter lesson
            if (chIdx < continuousChapters.lastIndex) {
                val nextChData = continuousChapters[chIdx + 1]
                val isDone = completedChapterIds.contains(chData.chapter.id)
                rows.add(
                    ReaderUiRow.ChapterTransition(
                        completedChapter = chData.chapter,
                        nextChapter = nextChData.chapter,
                        isCompleted = isDone,
                        chapterIndexInLesson = chIdx + 1,
                        totalChaptersInLesson = totalChapters
                    )
                )
            }
        }

        // Completion card at the end of the entire lesson
        rows.add(
            ReaderUiRow.LessonCompletedCard(
                totalChapters = totalChapters,
                allCompleted = continuousChapters.all { completedChapterIds.contains(it.chapter.id) }
            )
        )

        rows
    }

    // Find initial row index to scroll to
    val initialListIndex = remember(flatRows) {
        if (savedPosition != null) {
            val halachaTarget = flatRows.indexOfFirst { row ->
                row is ReaderUiRow.HalachaRow &&
                row.chapter.id == savedPosition.chapterId &&
                row.indexInChapter == savedPosition.halachaIndex
            }
            if (halachaTarget >= 0) halachaTarget
            else {
                val chapterHeaderTarget = flatRows.indexOfFirst { row ->
                    row is ReaderUiRow.ChapterHeader && row.chapter.id == savedPosition.chapterId
                }
                if (chapterHeaderTarget >= 0) chapterHeaderTarget else 0
            }
        } else 0
    }

    val initialOffset = remember(flatRows) {
        if (savedPosition != null) {
            (savedPosition.scrollOffsetFraction.coerceIn(0f, 1f) * 200).toInt()
        } else 0
    }

    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = initialListIndex,
        initialFirstVisibleItemScrollOffset = initialOffset
    )

    // Helper to extract active halacha or chapter header from visible row
    fun getActiveHalachaInfo(visibleIndex: Int): Pair<ReaderUiRow.HalachaRow?, ReaderUiRow.ChapterHeader?> {
        val direct = flatRows.getOrNull(visibleIndex)
        if (direct is ReaderUiRow.HalachaRow) return Pair(direct, null)
        if (direct is ReaderUiRow.ChapterHeader) return Pair(null, direct)

        // Find nearest preceding Halacha or Header
        for (i in visibleIndex downTo 0) {
            val r = flatRows.getOrNull(i)
            if (r is ReaderUiRow.HalachaRow) return Pair(r, null)
            if (r is ReaderUiRow.ChapterHeader) return Pair(null, r)
        }
        for (i in visibleIndex until flatRows.size) {
            val r = flatRows.getOrNull(i)
            if (r is ReaderUiRow.HalachaRow) return Pair(r, null)
            if (r is ReaderUiRow.ChapterHeader) return Pair(null, r)
        }
        return Pair(null, null)
    }

    // Scroll tracking: updates position in ViewModel
    LaunchedEffect(listState, flatRows) {
        snapshotFlow {
            Triple(listState.isScrollInProgress, listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset)
        }.collect { (_, index, offset) ->
            val (activeHalacha, activeHeader) = getActiveHalachaInfo(index)
            val fraction = (offset.toFloat() / 300f).coerceIn(0f, 1f)
            if (activeHalacha != null) {
                onScrollChanged(activeHalacha.chapter.id, activeHalacha.indexInChapter, fraction)
            } else if (activeHeader != null) {
                onScrollChanged(activeHeader.chapter.id, 0, fraction)
            }
        }
    }

    // Lifecycle & Exit Safety: Instantly persist reading position on app pause, stop, or dispose
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, flatRows) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                val (activeHalacha, activeHeader) = getActiveHalachaInfo(listState.firstVisibleItemIndex)
                val off = (listState.firstVisibleItemScrollOffset.toFloat() / 300f).coerceIn(0f, 1f)
                if (activeHalacha != null) {
                    onSaveImmediate(activeHalacha.chapter.id, activeHalacha.indexInChapter, off)
                } else if (activeHeader != null) {
                    onSaveImmediate(activeHeader.chapter.id, 0, off)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            val (activeHalacha, activeHeader) = getActiveHalachaInfo(listState.firstVisibleItemIndex)
            val off = (listState.firstVisibleItemScrollOffset.toFloat() / 300f).coerceIn(0f, 1f)
            if (activeHalacha != null) {
                onSaveImmediate(activeHalacha.chapter.id, activeHalacha.indexInChapter, off)
            } else if (activeHeader != null) {
                onSaveImmediate(activeHeader.chapter.id, 0, off)
            }
        }
    }

    // Indicator text formatted as "פרק כ׳ · הלכה ד׳ מתוך י״ד"
    val currentHalachaText = remember(flatRows) {
        derivedStateOf {
            val (activeHalacha, activeHeader) = getActiveHalachaInfo(listState.firstVisibleItemIndex)
            if (activeHalacha != null) {
                val currentLetter = HebrewNumberFormatter.toHebrewNumeral(activeHalacha.indexInChapter + 1)
                val totalLetter = HebrewNumberFormatter.toHebrewNumeral(activeHalacha.totalInChapter)
                if (activeHalacha.totalChaptersInLesson > 1) {
                    "${activeHalacha.chapter.chapterHebrew} · הלכה $currentLetter מתוך $totalLetter"
                } else {
                    "הלכה $currentLetter מתוך $totalLetter"
                }
            } else if (activeHeader != null) {
                "${activeHeader.chapter.chapterHebrew} · הלכה א׳"
            } else {
                "טוען..."
            }
        }
    }

    // Scroll progress from 0f to 1f
    val scrollProgress by remember(flatRows) {
        derivedStateOf {
            if (flatRows.size <= 1) 0f
            else {
                val progress = listState.firstVisibleItemIndex.toFloat() / (flatRows.size - 1).toFloat()
                progress.coerceIn(0f, 1f)
            }
        }
    }

    // Hebrew typography font family
    val activeFontFamily = FontStyleOption.fromId(preferences.fontFamily).fontFamily

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Main Text Scroll Column (Continuous, uninterrupted scrolling)
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp)
            ) {
                items(
                    items = flatRows,
                    key = { it.key }
                ) { row ->
                    when (row) {
                        is ReaderUiRow.ChapterHeader -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = if (row.chapterIndexInLesson == 1) 16.dp else 36.dp, bottom = 14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (row.section != null) {
                                    Text(
                                        text = row.section.titleHebrew,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                }
                                Text(
                                    text = row.chapter.chapterHebrew,
                                    fontSize = 25.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                if (row.totalChaptersInLesson > 1) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "פרק ${row.chapterIndexInLesson} מתוך ${row.totalChaptersInLesson} בשיעור היומי",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider(
                                    modifier = Modifier.width(60.dp),
                                    thickness = 2.dp,
                                    color = GoldAccent
                                )
                            }
                        }

                        is ReaderUiRow.HalachaRow -> {
                            HalachaBlock(
                                halacha = row.halacha,
                                preferences = preferences,
                                fontFamily = activeFontFamily,
                                modifier = Modifier.padding(bottom = 20.dp)
                            )
                        }

                        is ReaderUiRow.ChapterTransition -> {
                            // Seamless transition banner between chapters
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp)
                                    .testTag("chapter_transition_${row.completedChapter.id}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = if (row.isCompleted) Icons.Default.CheckCircle else Icons.Default.CheckCircleOutline,
                                            contentDescription = null,
                                            tint = if (row.isCompleted) CompletedGreen else PurplePrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "סיום ${row.completedChapter.chapterHebrew}",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (!row.isCompleted) {
                                            Spacer(modifier = Modifier.width(12.dp))
                                            FilledTonalButton(
                                                onClick = { onMarkCompleted(row.completedChapter.id) },
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier.height(30.dp)
                                            ) {
                                                Text("סמן כהושלם", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    HorizontalDivider(
                                        modifier = Modifier.fillMaxWidth(0.5f),
                                        thickness = 1.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "ממשיכים ברצף ל${row.nextChapter.chapterHebrew}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = PurplePrimary
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = PurplePrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        is ReaderUiRow.LessonCompletedCard -> {
                            // Completion card at the very end of all chapters
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp, bottom = 28.dp)
                                    .clip(RoundedCornerShape(16.dp)),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(24.dp)
                                        .fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = if (row.allCompleted) Icons.Default.CheckCircle else Icons.Default.Stars,
                                        contentDescription = null,
                                        tint = if (row.allCompleted) CompletedGreen else GoldAccent,
                                        modifier = Modifier.size(44.dp)
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Text(
                                        text = if (row.totalChapters > 1) "סיימת את השיעור היומי (${row.totalChapters} פרקים)!" else "סיימת את השיעור היומי!",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = "כל הכבוד על ההתמדה בלימוד הרמב״ם היומי",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                continuousChapters.forEach { chData ->
                                                    onMarkCompleted(chData.chapter.id)
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = CompletedGreen,
                                                contentColor = Color.White
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(46.dp)
                                                .testTag("mark_all_lesson_completed_button")
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = if (row.allCompleted) "השיעור הושלם ✓" else "סמן הכל כהושלם",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        OutlinedButton(
                                            onClick = onBack,
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(46.dp)
                                                .testTag("back_to_home_button")
                                        ) {
                                            Text("חזרה לדף הבית", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Floating Bottom Bar with back arrow, halacha indicator, date picker, and typography
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
            ) {
                FloatingReaderBar(
                    currentHalachaText = currentHalachaText.value,
                    hebrewDateText = dailyLesson?.hebrewDate ?: "",
                    scrollProgress = scrollProgress,
                    onBack = onBack,
                    onOpenDatePicker = onOpenDatePicker,
                    onTypographyClick = { showTypographySheet = true },
                    readerTheme = preferences.readerTheme,
                    onNextDay = onNextDay,
                    onPrevDay = onPrevDay
                )
            }
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

@Composable
private fun HalachaBlock(
    halacha: HalachaEntity,
    preferences: UserPreferences,
    fontFamily: FontFamily,
    modifier: Modifier = Modifier
) {
    StudyTextBlock(
        indexLetter = halacha.letter,
        textWithNikud = halacha.textWithNikud,
        textPlain = halacha.textPlain,
        preferences = preferences,
        fontFamily = fontFamily,
        textColor = MaterialTheme.colorScheme.onBackground,
        modifier = modifier,
        testTag = "halacha_item_${halacha.halachaNumber}"
    )
}
