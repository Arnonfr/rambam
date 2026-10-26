@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.example.ui.screens

import com.example.ui.components.ReaderAudioOverlay

import com.example.data.local.CompletionText
import com.example.data.local.SavedStudyCompletion
import com.example.domain.content.CompletionReaderRow
import com.example.ui.components.ManualStudyCompletionControls
import com.example.ui.components.rememberManualStudyCompletionState

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
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import kotlinx.coroutines.launch
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
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
import com.example.ui.components.PrintedPageZoomState
import com.example.ui.components.printedPageZoom
import com.example.ui.components.printedPageZoomLayer
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

@Composable
fun TanyaReaderScreen(
    onSaveCompletion: (SavedStudyCompletion) -> Unit = {},
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
    readerTitle: String = "תניא",
    contentId: String = "tanya",
    allowPrintLayout: Boolean = true,
    audioStudyDate: String? = null,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    var showTypographySheet by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    var showAudio by rememberSaveable(lesson?.date) { mutableStateOf(false) }
    val readerScope = rememberCoroutineScope()
    val audioUiState = androidx.compose.runtime.saveable.rememberSaveableStateHolder()
    val resumePosition = remember(lesson?.date) { savedPosition }

    // Keep screen on during study
    val context = LocalContext.current
    val usageAnalytics = remember(context) { com.example.data.analytics.AudioAnalytics.get(context) }
    val usageType = "tanya"
    androidx.lifecycle.compose.LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
        if(contentId == "tanya" && audioStudyDate != null) usageAnalytics.available(usageType)
    }
    val audioPlayer = if (contentId == "tanya" && audioStudyDate != null) remember(lesson?.date, lesson?.fullRef) {
        com.example.data.tanya.TanyaAudioPlayer(context)
    } else null
    DisposableEffect(audioPlayer) { onDispose { audioPlayer?.close() } }
    val audioPlayback = audioPlayer?.state?.collectAsState()?.value
    val anchorManager = remember { ContentReadingAnchorManager(context) }
    val printLines = remember(lesson?.fullRef) {
        com.example.domain.tanya.VerifiedTanyaPrint.fullPagesForReference(lesson?.fullRef.orEmpty())
    }
    val mappedPrint = allowPrintLayout && preferences.tanyaBookView && printLines.isNotEmpty()
    val pageZoom = remember(lesson?.date, mappedPrint) { PrintedPageZoomState() }
    val anchorContentId = if (mappedPrint) "tanya_print" else contentId
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
                            "dark", "oled" -> Color(0xFF121214)
                            "sepia" -> Color(0xFFF4ECD8)
                            else -> MaterialTheme.colorScheme.background
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = when (preferences.readerTheme) {
                        "dark", "oled" -> GoldAccent
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

    val manualCompletion = rememberManualStudyCompletionState(lesson.date to mappedPrint)
    val completionRows = remember(sections, mappedPrint, printLines) {
        if (mappedPrint) printLines.mapIndexedNotNull { index, line ->
            if (!com.example.domain.tanya.VerifiedTanyaPrint.hasDailyWords(line) || line.isCatchword) null else {
                val words = line.text.split(' ')
                val text = words.subList((line.greyBeforeWord ?: 0).coerceIn(0, words.size),
                    (line.greyFromWord ?: words.size).coerceIn(0, words.size)).joinToString(" ")
                CompletionReaderRow(index + 1, CompletionText("print_${line.page}_$index", "עמוד ${line.page}",
                    text, com.example.ui.util.HebrewTextNormalizer.stripMarks(text)))
            }
        } else sections.mapIndexed { index, section -> CompletionReaderRow(index + 1, CompletionText(
            "section_${section.sectionIndex}", lesson.chapterTitle,
            com.example.ui.util.HebrewTextNormalizer.forDisplay(section.textWithNikud),
            com.example.ui.util.HebrewTextNormalizer.forDisplay(section.textPlain))) }
    }

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
                if (sec != null && sec.sectionHebrew.isNotBlank()) "סעיף ${sec.sectionHebrew}" else ""
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
            } else {
                listState.scrollToItem(0)
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
        "dark", "oled" -> Color(0xFF121214)
        "sepia" -> Color(0xFFF5EEDA)
        else -> MaterialTheme.colorScheme.background
    }

    val textColor = when (preferences.readerTheme) {
        "dark", "oled" -> Color(0xFFE2E2E6)
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
            Box(Modifier.fillMaxSize().clipToBounds()
                .then(if (mappedPrint) Modifier.printedPageZoom(pageZoom) else Modifier)) {
            LazyColumn(
                state = listState,
                userScrollEnabled = !mappedPrint || !pageZoom.isZoomed,
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (mappedPrint) Modifier.printedPageZoomLayer(pageZoom) else Modifier)
                    .then(if (!pageZoom.isZoomed) Modifier.endOfLessonPull(completionPullState) else Modifier)
                    .padding(horizontal = if (mappedPrint) 6.dp else 20.dp),
                contentPadding = PaddingValues(top = 64.dp, bottom = 220.dp)
            ) {
                // 1. Header Card (Same structure as Tehillim and Chumash)
                // Retain the zero-height slot so existing saved paragraph indices stay valid.
                item(key = "tanya_intro_card") {
                    lesson.originalStudies?.let { original ->
                        com.example.ui.components.OriginalHayomYomHeading(original, preferences, textColor)
                    }
                    lesson.additionalOriginalStudies.forEach { original ->
                        com.example.ui.components.OriginalHayomYomHeading(original, preferences, textColor)
                    }
                }

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
                ) { sectionIndex, section ->
                    StudyTextBlock(
                        onTextLayout = { manualCompletion.measure(sectionIndex + 1, it) },
                        onTextTop = { manualCompletion.textTop(sectionIndex + 1, it) },
                        indexLetter = "",
                        textWithNikud = section.textWithNikud,
                        textPlain = section.textPlain,
                        preferences = preferences,
                        fontFamily = activeFontFamily,
                        textColor = textColor,
                        fontSizeSp = preferences.fontSizeSp,
                        isReadingAnchor = section.sectionIndex == (entryAnchor?.blockIndex ?: resumePosition?.halachaIndex?.plus(1)),
                        modifier = Modifier.padding(bottom = 18.dp),
                        testTag = "tanya_section_${section.sectionIndex}"
                    )
                }
                }

            }
            }

            CompactReaderHeader(
                title = readerTitle,
                location = listOf(lesson.chapterTitle.removePrefix("תניא, "), currentSectionText.value)
                    .filter { it.isNotBlank() }.joinToString(" · "),
                accentColor = if (allowPrintLayout) Color(0xFFEA78D5) else Color(0xFFB8ACF4),
                scrollProgress = scrollProgress,
                readerTheme = preferences.readerTheme,
                modifier = Modifier.align(Alignment.TopCenter),
                trailingAction = if (audioPlayer == null) null else ({
                    com.example.ui.components.ReaderAudioButton(showAudio,
                        playing = audioPlayback?.playing == true, loading = audioPlayback?.loading == true,
                        accent = Color(0xFFEA78D5), readerTheme = preferences.readerTheme,
                        tag = "tanya_audio_toggle", showLabel = "הצגת השיעור המוקלט", hideLabel = "הסתרת השיעור המוקלט",
                        onClick = {
                            showAudio = !showAudio
                            if (showAudio) usageAnalytics.panelEvent("audio_panel_open", usageType)
                        })
                })
            )

            if (audioPlayer != null && showAudio) {
                ReaderAudioOverlay(onClose = { showAudio = false }) {
                    audioUiState.SaveableStateProvider(lesson.date + lesson.fullRef) {
                        com.example.ui.components.TanyaAudioControls(lesson, audioStudyDate ?: lesson.date, audioPlayer)
                    }
                }
            }

            ManualStudyCompletionControls(manualCompletion, listState, completionRows,
                track = contentId, studyDate = lesson.date, dateLabel = lesson.hebrewDate, title = readerTitle,
                cutAtMiddle = true, enabled = !pageZoom.isZoomed, onSave = onSaveCompletion)

            EndOfLessonPullIndicator(
                state = completionPullState,
                accentColor = if (allowPrintLayout) Color(0xFFEA78D5) else Color(0xFFB8ACF4),
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
                    studyDate = lesson.date,
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
                preferences = if (allowPrintLayout && preferences.tanyaBookView) preferences.copy(showNikud = preferences.tanyaBookShowNikud) else preferences,
                fontSizeSp = preferences.fontSizeSp,
                onDismiss = { showTypographySheet = false },
                onFontSizeChange = onFontSizeChange,
                onLineSpacingChange = onLineSpacingChange,
                onFontFamilyChange = onFontFamilyChange,
                onNikudToggle = if (allowPrintLayout && preferences.tanyaBookView) onBookNikudChange else onNikudToggle,
                onThemeChange = onThemeChange,
                onKeepScreenOnChange = onKeepScreenOnChange,
                fixedPrintLayout = allowPrintLayout && preferences.tanyaBookView,
                extraControls = {
                    if (allowPrintLayout) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("צורת הדף", fontWeight = FontWeight.Bold)
                            Text("טקסט חי · גופן ושורות לפי המקור", fontSize = 12.sp)
                        }
                        Switch(checked = preferences.tanyaBookView, onCheckedChange = onBookViewChange,
                            colors = com.example.ui.components.readerSwitchColors())
                    }
                    if (preferences.tanyaBookView) Text(
                        "הרוחב והגופן קבועים לפי המקור, ללא ניקוד. אפשר להתקרב בשתי אצבעות ולהזיז את הדף המוגדל בלי לשנות שורות; כיווץ בחזרה מחזיר לגלילה רגילה. העמודים מוצגים במלואם; הטקסט שמחוץ לשיעור באפור. הפתיחה מובילה למיקום השמור או לתחילת השיעור. בשיעור שחורג מהעמודים שמופו מוצג הטקסט הרגיל במלואו.",
                        fontSize = 11.sp, modifier = Modifier.padding(bottom = 12.dp))
                    }
                }
            )
        }
    }
}
