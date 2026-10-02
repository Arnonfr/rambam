@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.example.ui.screens

import android.app.Activity
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.data.local.UserPreferences
import com.example.data.local.ContentReadingAnchor
import com.example.data.local.ContentReadingAnchorManager
import com.example.domain.tanya.DailyTanyaLesson
import com.example.domain.tanya.TanyaSection
import com.example.ui.components.FloatingReaderBar
import com.example.ui.components.CompactReaderHeader
import com.example.ui.components.EndOfLessonPullIndicator
import com.example.ui.components.ReaderTypographySheet
import com.example.ui.components.StudyTextBlock
import com.example.ui.components.TanyaBookText
import com.example.ui.components.endOfLessonPull
import com.example.ui.components.rememberEndOfLessonPullState
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

@Composable
fun TanyaReaderScreen(
    lesson: DailyTanyaLesson?,
    isLoading: Boolean,
    onBack: () -> Unit,
    preferences: UserPreferences,
    onBookViewChange: (Boolean) -> Unit = {},
    onBookNikudChange: (Boolean) -> Unit = {},
    onFontSizeChange: (Float) -> Unit = {},
    onLineSpacingChange: (Float) -> Unit = {},
    onFontFamilyChange: (String) -> Unit = {},
    onNikudToggle: (Boolean) -> Unit = {},
    onThemeChange: (String) -> Unit = {},
    onKeepScreenOnChange: (Boolean) -> Unit = {},
    hebrewDateText: String = "",
    onNextDay: (() -> Unit)? = null,
    onPrevDay: (() -> Unit)? = null,
    onOpenDatePicker: () -> Unit = {},
    onToggleCompletion: () -> Unit = {},
    onSavePosition: (TanyaSection, DailyTanyaLesson) -> Unit = { _, _ -> },
    savedPosition: com.example.data.local.ReadingPositionEntity? = null,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    var showTypographySheet by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val resumePosition = remember(lesson?.date) { savedPosition }

    // Keep screen on during study
    val context = LocalContext.current
    val anchorManager = remember { ContentReadingAnchorManager(context) }
    val printLines = remember(lesson?.fullRef) {
        com.example.domain.tanya.VerifiedTanyaPrint.fullPagesForReference(lesson?.fullRef.orEmpty())
    }
    val mappedPrint = preferences.tanyaBookView && printLines.isNotEmpty()
    val anchorContentId = if (mappedPrint) "tanya_print" else "tanya"
    val entryAnchor = remember(lesson?.date, anchorContentId) {
        lesson?.let { current ->
            anchorManager.get(anchorContentId, current.date)?.let { anchor ->
                if (mappedPrint) anchor.copy(blockIndex = com.example.domain.tanya.VerifiedTanyaPrint
                    .restoredIndex(current.fullRef, anchor.blockIndex, anchor.blockId)) else anchor
            }
        }
    }
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

    if (isLoading || lesson == null) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl,
        androidx.compose.foundation.LocalOverscrollConfiguration provides null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        when (preferences.readerTheme) {
                            "dark" -> Color(0xFF121214)
                            "sepia" -> Color(0xFFF4ECD8)
                            else -> MaterialTheme.colorScheme.background
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = when (preferences.readerTheme) {
                        "dark" -> GoldAccent
                        "sepia" -> Color(0xFF8C6D4F)
                        else -> MaterialTheme.colorScheme.primary
                    }
                )
            }
        }
        return
    }

    val completionPullState = rememberEndOfLessonPullState(
        key = lesson.date,
        canScrollForward = { listState.canScrollForward },
        onCompleted = { if (!lesson.isCompleted) onToggleCompletion() },
        onExitAfterCompletion = onBack
    )

    val sections = lesson.sections

    // Identify current active section from scroll index (offset by 1 for intro card)
    val displayCount = if (mappedPrint) printLines.size else sections.size
    val currentSectionText = remember(sections, mappedPrint) {
        derivedStateOf {
            val visibleIdx = listState.firstVisibleItemIndex
            if (visibleIdx == 0) {
                ""
            } else {
                val secIdx = if (mappedPrint) (printLines.getOrNull(visibleIdx - 1)?.section ?: 1) - 1
                    else (visibleIdx - 1).coerceIn(0, (sections.size - 1).coerceAtLeast(0))
                val sec = sections.getOrNull(secIdx)
                if (sec != null) "סעיף ${sec.sectionHebrew}" else ""
            }
        }
    }

    // Scroll progress along the sections list (0.0 to 1.0)
    val scrollProgress by remember(sections, mappedPrint) {
        derivedStateOf {
            if (displayCount <= 1) 0f
            else {
                val progress = listState.firstVisibleItemIndex.toFloat() / displayCount.toFloat()
                progress.coerceIn(0f, 1f)
            }
        }
    }

    // Scroll to saved position on first load
    var hasScrolledToSavedPosition by remember(lesson.date, mappedPrint) { mutableStateOf(false) }
    LaunchedEffect(sections, resumePosition, mappedPrint) {
        if (!hasScrolledToSavedPosition && sections.isNotEmpty()) {
            if (entryAnchor != null) {
                listState.scrollToItem(entryAnchor.blockIndex.coerceIn(0, displayCount), entryAnchor.textOffset)
            } else if (resumePosition != null) {
                val targetIdx = if (mappedPrint) printLines.indexOfFirst { it.section == resumePosition.halachaIndex + 1 && com.example.domain.tanya.VerifiedTanyaPrint.hasDailyWords(it) }.coerceAtLeast(0) + 1
                    else resumePosition.halachaIndex + 1
                if (targetIdx in 0..displayCount) {
                    listState.scrollToItem(targetIdx)
                }
            } else if (mappedPrint) {
                val firstDaily = printLines.indexOfFirst { com.example.domain.tanya.VerifiedTanyaPrint.hasDailyWords(it) }
                if (firstDaily >= 0) listState.scrollToItem(firstDaily + 1)
            }
            hasScrolledToSavedPosition = true
        }
    }

    fun activeSectionAt(visibleIndex: Int): TanyaSection? =
        sections.getOrNull(if (mappedPrint) (printLines.getOrNull((visibleIndex - 1).coerceAtLeast(0))?.section ?: 1) - 1
            else (visibleIndex - 1).coerceAtLeast(0))

    fun saveAnchor() {
        if (!hasScrolledToSavedPosition) return
        val sourceLine = if (mappedPrint) printLines.getOrNull(listState.firstVisibleItemIndex - 1) else null
        anchorManager.save(ContentReadingAnchor(anchorContentId, lesson.date, lesson.fullRef,
            sourceLine?.let { com.example.domain.tanya.VerifiedTanyaPrint.stableId(it) }
                ?: if (mappedPrint) "print_intro" else "section_${listState.firstVisibleItemIndex}", listState.firstVisibleItemIndex,
            textOffset = listState.firstVisibleItemScrollOffset))
    }

    LaunchedEffect(listState, sections, lesson, hasScrolledToSavedPosition, mappedPrint) {
        if (!hasScrolledToSavedPosition) return@LaunchedEffect
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collectLatest { (visibleIndex, _) ->
                delay(250)
                saveAnchor()
                activeSectionAt(visibleIndex)?.let { onSavePosition(it, lesson) }
            }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, sections, lesson, hasScrolledToSavedPosition, mappedPrint) {
        fun saveVisibleSection() {
            if (!hasScrolledToSavedPosition) return
            saveAnchor()
            activeSectionAt(listState.firstVisibleItemIndex)?.let { onSavePosition(it, lesson) }
        }
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                saveVisibleSection()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            saveVisibleSection()
        }
    }

    val activeFontFamily = FontStyleOption.fromId(preferences.fontFamily).fontFamily

    val backgroundColor = when (preferences.readerTheme) {
        "dark" -> Color(0xFF121214)
        "sepia" -> Color(0xFFF5EEDA)
        else -> MaterialTheme.colorScheme.background
    }

    val textColor = when (preferences.readerTheme) {
        "dark" -> Color(0xFFE2E2E6)
        "sepia" -> Color(0xFF3C3020)
        else -> MaterialTheme.colorScheme.onBackground
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl,
        androidx.compose.foundation.LocalOverscrollConfiguration provides null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (mappedPrint) textColor.copy(alpha = .045f).compositeOver(backgroundColor) else backgroundColor)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .endOfLessonPull(completionPullState)
                    .padding(horizontal = if (mappedPrint) 6.dp else 20.dp),
                contentPadding = PaddingValues(top = 40.dp, bottom = 118.dp)
            ) {
                // 1. Header Card (Same structure as Tehillim and Chumash)
                // Retain the zero-height slot so existing saved paragraph indices stay valid.
                item(key = "tanya_intro_card") { }

                // 2. Sections / Paragraphs
                if (mappedPrint) {
                    itemsIndexed(printLines, key = { idx, line -> "print_${line.page}_$idx" }) { idx, line ->
                        com.example.ui.components.TanyaPageLine(
                            line = line, textColor = textColor, pageColor = backgroundColor,
                            first = idx == 0 || printLines[idx - 1].page != line.page,
                            last = idx == printLines.lastIndex || printLines[idx + 1].page != line.page,
                            showAnchor = idx + 1 == entryAnchor?.blockIndex
                        )
                    }
                } else {
                itemsIndexed(
                    items = sections,
                    key = { idx, sec -> "tanya_sec_${sec.sectionIndex}_$idx" }
                ) { _, section ->
                    StudyTextBlock(
                        indexLetter = section.sectionHebrew,
                        textWithNikud = section.textWithNikud,
                        textPlain = section.textPlain,
                        preferences = preferences,
                        fontFamily = activeFontFamily,
                        textColor = textColor,
                        fontSizeSp = preferences.chumashFontSizeSp,
                        isReadingAnchor = section.sectionIndex == (entryAnchor?.blockIndex ?: resumePosition?.halachaIndex?.plus(1)),
                        modifier = Modifier.padding(bottom = 18.dp),
                        testTag = "tanya_section_${section.sectionIndex}"
                    )
                }
                }

            }

            CompactReaderHeader(
                title = "תניא",
                location = listOf(lesson.chapterTitle.removePrefix("תניא, "), currentSectionText.value)
                    .filter { it.isNotBlank() }.joinToString(" · "),
                accentColor = Color(0xFFEA78D5),
                scrollProgress = scrollProgress,
                readerTheme = preferences.readerTheme,
                modifier = Modifier.align(Alignment.TopCenter)
            )

            EndOfLessonPullIndicator(
                state = completionPullState,
                accentColor = Color(0xFFEA78D5),
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 92.dp)
            )

            // Floating date navigation.
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
            ) {
                val displayDate = if (hebrewDateText.isNotBlank()) {
                    hebrewDateText
                } else {
                    lesson.hebrewDate
                }
                FloatingReaderBar(
                    currentHalachaText = "",
                    hebrewDateText = displayDate,
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
                preferences = if (preferences.tanyaBookView) preferences.copy(showNikud = preferences.tanyaBookShowNikud) else preferences,
                fontSizeSp = preferences.chumashFontSizeSp,
                onDismiss = { showTypographySheet = false },
                onFontSizeChange = onFontSizeChange,
                onLineSpacingChange = onLineSpacingChange,
                onFontFamilyChange = onFontFamilyChange,
                onNikudToggle = if (preferences.tanyaBookView) onBookNikudChange else onNikudToggle,
                onThemeChange = onThemeChange,
                onKeepScreenOnChange = onKeepScreenOnChange,
                fixedPrintLayout = preferences.tanyaBookView,
                extraControls = {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("צורת הדף", fontWeight = FontWeight.Bold)
                            Text("טקסט חי · גופן ושורות לפי המקור", fontSize = 12.sp)
                        }
                        Switch(checked = preferences.tanyaBookView, onCheckedChange = onBookViewChange,
                            colors = com.example.ui.components.readerSwitchColors())
                    }
                    if (preferences.tanyaBookView) Text(
                        "הרוחב והגופן קבועים לפי המקור, ללא ניקוד וללא גלילה לצדדים. מיפוי השורות הורחב לעמודים 270–295. העמודים מוצגים במלואם; הטקסט שמחוץ לשיעור באפור. הפתיחה מובילה למיקום השמור או לתחילת השיעור. בשיעור שחורג מהעמודים שמופו מוצג הטקסט הרגיל במלואו.",
                        fontSize = 11.sp, modifier = Modifier.padding(bottom = 12.dp))
                }
            )
        }
    }
}
