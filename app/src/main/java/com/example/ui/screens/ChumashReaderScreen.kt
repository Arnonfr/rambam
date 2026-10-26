@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CheckCircleOutline
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
import com.example.data.local.UserPreferences
import com.example.domain.chumash.ChumashAliya
import com.example.domain.chumash.ChumashVerse
import com.example.domain.chumash.DailyChumashLesson
import com.example.ui.components.FloatingReaderBar
import com.example.ui.components.CompactReaderHeader
import com.example.ui.components.EndOfLessonPullIndicator
import com.example.ui.components.ReaderTypographySheet
import com.example.ui.components.StudyTextBlock
import com.example.ui.components.endOfLessonPull
import com.example.ui.components.rememberReaderViewportState
import com.example.ui.components.rememberEndOfLessonPullState
import com.example.ui.theme.*
import com.example.ui.util.HebrewNumberFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

sealed interface ChumashUiRow {
    val key: String

    data class AliyaHeader(
        val aliya: ChumashAliya,
        val parashaName: String,
        val aliyaIndexInSelection: Int,
        val totalAliyotInSelection: Int
    ) : ChumashUiRow {
        override val key: String = "header_${aliya.aliyaIndex}"
    }

    data class VerseRow(
        val aliya: ChumashAliya,
        val verse: ChumashVerse,
        val indexInAliya: Int,
        val totalInAliya: Int,
        val aliyaIndexInSelection: Int,
        val totalAliyotInSelection: Int
    ) : ChumashUiRow {
        override val key: String = "${aliya.aliyaIndex}_${verse.positionId}"
    }

    data class AliyaTransition(
        val completedAliya: ChumashAliya,
        val nextAliya: ChumashAliya,
        val isCompleted: Boolean,
        val aliyaIndexInSelection: Int,
        val totalAliyotInSelection: Int
    ) : ChumashUiRow {
        override val key: String = "transition_${completedAliya.aliyaIndex}"
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChumashReaderScreen(
    onSaveCompletion: (SavedStudyCompletion) -> Unit = {},
    chumashLesson: DailyChumashLesson?,
    selectedAliyaIndex: Int,
    preferences: UserPreferences,
    isLoading: Boolean,
    onSelectAliya: (Int) -> Unit,
    onToggleShowTeamim: () -> Unit,
    onNikudToggle: (Boolean) -> Unit = {},
    onChangeFontSize: (Float) -> Unit,
    onToggleAliyaCompletion: (String, Int) -> Unit,
    onBack: () -> Unit,
    onSavePosition: (ChumashVerse, ChumashAliya) -> Unit = { _, _ -> },
    onOpenDatePicker: () -> Unit = {},
    onLineSpacingChange: (Float) -> Unit = {},
    onFontFamilyChange: (String) -> Unit = {},
    onThemeChange: (String) -> Unit = {},
    onKeepScreenOnChange: (Boolean) -> Unit = {},
    savedPosition: com.example.data.local.ReadingPositionEntity? = null,
    hebrewDateText: String = "",
    onNextDay: (() -> Unit)? = null,
    onPrevDay: (() -> Unit)? = null
) {
    BackHandler(onBack = onBack)

    var showTypographySheet by remember { mutableStateOf(false) }
    val resumePosition = remember(chumashLesson?.parashaName, selectedAliyaIndex) { savedPosition }

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

    val aliyotToDisplay = remember(chumashLesson, selectedAliyaIndex) {
        if (chumashLesson == null) emptyList()
        else if (selectedAliyaIndex == 0) chumashLesson.allAliyot
        else listOfNotNull(chumashLesson.allAliyot.find { it.aliyaIndex == selectedAliyaIndex })
    }

    val flatRows = remember(aliyotToDisplay, chumashLesson) {
        val rows = mutableListOf<ChumashUiRow>()
        val totalAliyot = aliyotToDisplay.size
        
        if (chumashLesson != null) {
            aliyotToDisplay.forEachIndexed { aIdx, aliya ->
                rows.add(
                    ChumashUiRow.AliyaHeader(
                        aliya = aliya,
                        parashaName = chumashLesson.parashaName,
                        aliyaIndexInSelection = aIdx + 1,
                        totalAliyotInSelection = totalAliyot
                    )
                )

                val vCount = aliya.verses.size
                aliya.verses.forEachIndexed { vIdx, verse ->
                    rows.add(
                        ChumashUiRow.VerseRow(
                            aliya = aliya,
                            verse = verse,
                            indexInAliya = vIdx,
                            totalInAliya = vCount,
                            aliyaIndexInSelection = aIdx + 1,
                            totalAliyotInSelection = totalAliyot
                        )
                    )
                }

                if (aIdx < aliyotToDisplay.lastIndex) {
                    val nextAliya = aliyotToDisplay[aIdx + 1]
                    rows.add(
                        ChumashUiRow.AliyaTransition(
                            completedAliya = aliya,
                            nextAliya = nextAliya,
                            isCompleted = aliya.isCompleted,
                            aliyaIndexInSelection = aIdx + 1,
                            totalAliyotInSelection = totalAliyot
                        )
                    )
                }
            }

        }
        rows
    }

    val fallbackIndex = flatRows.indexOfFirst { row ->
            row is ChumashUiRow.VerseRow && resumePosition != null &&
                row.aliya.aliyaIndex == resumePosition.chapterNumber &&
                (row.verse.positionId == resumePosition.halachaId ||
                    (resumePosition.halachaId.matches(Regex("verse_\\d+")) &&
                        row.indexInAliya == resumePosition.halachaIndex))
            }
    val listState = rememberReaderViewportState("chumash_$selectedAliyaIndex", chumashLesson?.date.toString(),
        flatRows.map { it.key }, fallbackIndex.coerceAtLeast(0))
    val completionPullState = rememberEndOfLessonPullState(
        key = chumashLesson?.date?.toString() to selectedAliyaIndex,
        canScrollForward = { listState.canScrollForward },
        onCompleted = {
            chumashLesson?.let { daily ->
                aliyotToDisplay.filterNot { it.isCompleted }.forEach { aliya ->
                    onToggleAliyaCompletion(daily.parashaName, aliya.aliyaIndex)
                }
            }
        },
        onExitAfterCompletion = onBack
    )

    val manualCompletion = rememberManualStudyCompletionState(chumashLesson?.date to selectedAliyaIndex)
    val completionRows = remember(flatRows) { flatRows.mapIndexedNotNull { index, row ->
        (row as? ChumashUiRow.VerseRow)?.let { CompletionReaderRow(index, CompletionText(
            it.key, "${it.verse.bookName} ${it.verse.chapterNumber}:${it.verse.verseNumber}",
            it.verse.textWithTeamim, it.verse.textPlainNikud, it.verse.rashi)) }
    } }

    val hasScrolledToSavedPosition = flatRows.isNotEmpty()

    fun getActiveVerseInfo(visibleIndex: Int): Pair<ChumashUiRow.VerseRow?, ChumashUiRow.AliyaHeader?> {
        val direct = flatRows.getOrNull(visibleIndex)
        if (direct is ChumashUiRow.VerseRow) return Pair(direct, null)
        if (direct is ChumashUiRow.AliyaHeader) return Pair(null, direct)

        for (i in visibleIndex downTo 0) {
            val r = flatRows.getOrNull(i)
            if (r is ChumashUiRow.VerseRow) return Pair(r, null)
            if (r is ChumashUiRow.AliyaHeader) return Pair(null, r)
        }
        for (i in visibleIndex until flatRows.size) {
            val r = flatRows.getOrNull(i)
            if (r is ChumashUiRow.VerseRow) return Pair(r, null)
            if (r is ChumashUiRow.AliyaHeader) return Pair(null, r)
        }
        return Pair(null, null)
    }

    val currentVerseText = remember(flatRows) {
        derivedStateOf {
            val (activeVerse, activeHeader) = getActiveVerseInfo(listState.firstVisibleItemIndex)
            val verse = activeVerse?.verse ?: activeHeader?.aliya?.verses?.firstOrNull()
            val aliya = activeVerse?.aliya ?: activeHeader?.aliya
            if (verse != null && aliya != null) {
                val chapter = HebrewNumberFormatter.toHebrewNumeral(verse.chapterNumber, withGershayim = true)
                "${aliya.aliyaName} · $chapter:${verse.verseHebrew}"
            } else {
                "טוען..."
            }
        }
    }

    val scrollProgress by remember(flatRows) {
        derivedStateOf {
            if (flatRows.size <= 1) 0f
            else {
                val progress = listState.firstVisibleItemIndex.toFloat() / (flatRows.size - 1).toFloat()
                progress.coerceIn(0f, 1f)
            }
        }
    }

    // Persist the final settled row. collectLatest cancels an older pending save,
    // so a fast fling cannot leave the anchor at the first row of the fling.
    LaunchedEffect(listState, flatRows, hasScrolledToSavedPosition) {
        if (!hasScrolledToSavedPosition) return@LaunchedEffect
        snapshotFlow { listState.firstVisibleItemIndex }
            .collectLatest { visibleIndex ->
                delay(250)
                getActiveVerseInfo(visibleIndex).first?.let { activeVerse ->
                    onSavePosition(activeVerse.verse, activeVerse.aliya)
                }
            }
    }

    // A back press, app switch or process stop must save without waiting for
    // the settling delay above.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, flatRows, hasScrolledToSavedPosition) {
        fun saveVisibleRow() {
            if (!hasScrolledToSavedPosition) return
            getActiveVerseInfo(listState.firstVisibleItemIndex).first?.let { activeVerse ->
                onSavePosition(activeVerse.verse, activeVerse.aliya)
            }
        }
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                saveVisibleRow()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            saveVisibleRow()
        }
    }

    val activeFontFamily = FontStyleOption.fromId(preferences.fontFamily).fontFamily
    val backgroundColor = when (preferences.readerTheme) {
        "dark", "oled" -> Color(0xFF121214)
        "sepia" -> Color(0xFFF5EEDA)
        else -> MaterialTheme.colorScheme.background
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl,
        androidx.compose.foundation.LocalOverscrollConfiguration provides null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .endOfLessonPull(completionPullState)
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(top = 64.dp, bottom = 220.dp)
            ) {
                items(
                    items = flatRows,
                    key = { it.key }
                ) { row ->
                    when (row) {
                        is ChumashUiRow.AliyaHeader -> {
                            if (row.aliyaIndexInSelection != 1) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = if (row.aliyaIndexInSelection == 1) 16.dp else 36.dp, bottom = 14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "פרשת ${row.parashaName}",
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "עליה ${row.aliya.aliyaName}",
                                    fontSize = 25.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                if (row.aliya.heRef.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = row.aliya.heRef,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (row.totalAliyotInSelection > 1) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "עליה ${row.aliyaIndexInSelection} מתוך ${row.totalAliyotInSelection} בשיעור היומי",
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
                        }

                        is ChumashUiRow.VerseRow -> {
                            val previous = row.aliya.verses.getOrNull(row.indexInAliya - 1)
                            if (row.verse.chapterNumber > 0 && (previous == null ||
                                previous.chapterNumber != row.verse.chapterNumber || previous.bookName != row.verse.bookName)) {
                                Text(
                                    text = "${row.verse.bookName} · פרק ${HebrewNumberFormatter.toHebrewNumeral(row.verse.chapterNumber)}",
                                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 16.dp),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            val cleanTextWithNikud = if (preferences.chumashShowTeamim) {
                                row.verse.textWithTeamim
                            } else {
                                com.example.ui.util.HebrewTextNormalizer.withoutBiblicalAnnotations(row.verse.textWithTeamim)
                            }
                            StudyTextBlock(
                                indexLetter = row.verse.verseHebrew,
                                textWithNikud = cleanTextWithNikud,
                                textPlain = row.verse.textPlainNikud,
                                preferences = preferences,
                                rashi = row.verse.rashi,
                                fontFamily = activeFontFamily,
                                textColor = MaterialTheme.colorScheme.onBackground,
                                fontSizeSp = preferences.fontSizeSp,
                                isReadingAnchor = false,
                                modifier = Modifier.padding(bottom = 20.dp),
                                testTag = "verse_item_${row.verse.chapterNumber}_${row.verse.verseNumber}"
                            )
                        }

                        is ChumashUiRow.AliyaTransition -> {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp)
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
                                            imageVector = if (row.isCompleted) Icons.Default.CheckCircle else Icons.Outlined.CheckCircleOutline,
                                            contentDescription = null,
                                            tint = if (row.isCompleted) CompletedGreen else PurplePrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "סיום עליה ${row.completedAliya.aliyaName}",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (!row.isCompleted) {
                                            Spacer(modifier = Modifier.width(12.dp))
                                            FilledTonalButton(
                                                onClick = { onToggleAliyaCompletion(chumashLesson!!.parashaName, row.completedAliya.aliyaIndex) },
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
                                            text = "ממשיכים ברצף לעליה ${row.nextAliya.aliyaName}",
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

                    }
                }
            }

            CompactReaderHeader(
                title = "חומש",
                location = "פרשת ${chumashLesson?.parashaName.orEmpty()} • ${currentVerseText.value}",
                accentColor = Color(0xFFEDFF24),
                scrollProgress = scrollProgress,
                readerTheme = preferences.readerTheme,
                modifier = Modifier.align(Alignment.TopCenter)
            )

            ManualStudyCompletionControls(manualCompletion, listState, completionRows,
                track = "chumash", studyDate = chumashLesson?.date.orEmpty(), dateLabel = hebrewDateText,
                title = "חומש · ${chumashLesson?.parashaName.orEmpty()}", onSave = onSaveCompletion)

            EndOfLessonPullIndicator(
                state = completionPullState,
                accentColor = Color(0xFFEDFF24),
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 92.dp)
            )

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
            ) {
                val displayDate = if (hebrewDateText.isNotBlank()) {
                    hebrewDateText
                } else {
                    chumashLesson?.parashaName ?: "חומש"
                }
                FloatingReaderBar(
                    currentHalachaText = "",
                    hebrewDateText = displayDate,
                    studyDate = chumashLesson?.date.orEmpty(),
                    scrollProgress = scrollProgress,
                    onBack = onBack,
                    onOpenDatePicker = onOpenDatePicker,
                    onTypographyClick = { showTypographySheet = true },
                    readerTheme = preferences.readerTheme,
                    onNextDay = onNextDay,
                    onPrevDay = onPrevDay
                )
            }

            if (showTypographySheet) {
                ReaderTypographySheet(
                    preferences = preferences,
                    fontSizeSp = preferences.fontSizeSp,
                    onDismiss = { showTypographySheet = false },
                    onFontSizeChange = onChangeFontSize,
                    onLineSpacingChange = onLineSpacingChange,
                    onFontFamilyChange = onFontFamilyChange,
                    onNikudToggle = onNikudToggle,
                    onThemeChange = onThemeChange,
                    onKeepScreenOnChange = onKeepScreenOnChange,
                    extraControls = {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("טעמים בחומש", modifier = Modifier.weight(1f))
                            Switch(checked = preferences.chumashShowTeamim, onCheckedChange = { onToggleShowTeamim() })
                        }
                    }
                )
            }
        }
    }
}
