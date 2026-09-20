package com.example.ui.screens

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
import com.example.ui.components.ReaderTypographySheet
import com.example.ui.components.StudyTextBlock
import com.example.ui.theme.*
import com.example.ui.util.HebrewNumberFormatter

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
        override val key: String = "verse_${aliya.aliyaIndex}_${verse.verseNumber}"
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

    data class LessonCompletedCard(
        val totalAliyot: Int,
        val allCompleted: Boolean
    ) : ChumashUiRow {
        override val key: String = "lesson_completion"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChumashReaderScreen(
    chumashLesson: DailyChumashLesson?,
    selectedAliyaIndex: Int,
    preferences: UserPreferences,
    isLoading: Boolean,
    onSelectAliya: (Int) -> Unit,
    onToggleShowTeamim: () -> Unit,
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

            if (totalAliyot > 0) {
                rows.add(
                    ChumashUiRow.LessonCompletedCard(
                        totalAliyot = totalAliyot,
                        allCompleted = aliyotToDisplay.all { it.isCompleted }
                    )
                )
            }
        }
        rows
    }

    val listState = rememberLazyListState()

    // Scroll to saved position on first load
    var hasScrolledToSavedPosition by remember { mutableStateOf(false) }
    LaunchedEffect(flatRows, savedPosition) {
        if (!hasScrolledToSavedPosition && savedPosition != null && flatRows.isNotEmpty()) {
            val targetIdx = flatRows.indexOfFirst { row ->
                row is ChumashUiRow.VerseRow &&
                    row.aliya.aliyaIndex == savedPosition.chapterNumber &&
                    row.verse.verseNumber == (savedPosition.halachaIndex + 1)
            }
            if (targetIdx >= 0) {
                listState.scrollToItem(targetIdx)
            }
            hasScrolledToSavedPosition = true
        }
    }

    LaunchedEffect(selectedAliyaIndex, chumashLesson?.date) {
        if (hasScrolledToSavedPosition) {
            listState.scrollToItem(0)
        }
    }

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
            if (activeVerse != null) {
                val currentLetter = HebrewNumberFormatter.toHebrewNumeral(activeVerse.indexInAliya + 1)
                val totalLetter = HebrewNumberFormatter.toHebrewNumeral(activeVerse.totalInAliya)
                if (activeVerse.totalAliyotInSelection > 1) {
                    "עליה ${activeVerse.aliya.aliyaName} · פסוק $currentLetter מתוך $totalLetter"
                } else {
                    "פסוק $currentLetter מתוך $totalLetter"
                }
            } else if (activeHeader != null) {
                "עליה ${activeHeader.aliya.aliyaName} · פסוק א׳"
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

    // Save reading position when scrolling
    var lastSaveTime by remember { mutableStateOf(0L) }
    LaunchedEffect(listState.firstVisibleItemIndex) {
        val (activeVerse, _) = getActiveVerseInfo(listState.firstVisibleItemIndex)
        if (activeVerse != null) {
            val now = System.currentTimeMillis()
            if (now - lastSaveTime > 1000) {
                onSavePosition(activeVerse.verse, activeVerse.aliya)
                lastSaveTime = now
            }
        }
    }

    val activeFontFamily = FontStyleOption.fromId(preferences.fontFamily).fontFamily
    val backgroundColor = when (preferences.readerTheme) {
        "dark" -> Color(0xFF121214)
        "sepia" -> Color(0xFFF5EEDA)
        else -> MaterialTheme.colorScheme.background
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(top = 24.dp, bottom = 170.dp)
            ) {
                items(
                    items = flatRows,
                    key = { it.key }
                ) { row ->
                    when (row) {
                        is ChumashUiRow.AliyaHeader -> {
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

                        is ChumashUiRow.VerseRow -> {
                            val cleanTextWithNikud = if (preferences.chumashShowTeamim) {
                                row.verse.textWithTeamim
                            } else {
                                row.verse.textWithTeamim.replace(Regex("[\\u0591-\\u05AF]"), "")
                            }
                            StudyTextBlock(
                                indexLetter = HebrewNumberFormatter.toHebrewNumeral(row.indexInAliya + 1) + "׳",
                                textWithNikud = cleanTextWithNikud,
                                textPlain = row.verse.textPlainNikud,
                                preferences = preferences,
                                rashi = row.verse.rashi,
                                fontFamily = activeFontFamily,
                                textColor = MaterialTheme.colorScheme.onBackground,
                                fontSizeSp = preferences.chumashFontSizeSp,
                                modifier = Modifier.padding(bottom = 20.dp),
                                testTag = "verse_item_${row.verse.verseNumber}"
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

                        is ChumashUiRow.LessonCompletedCard -> {
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
                                        text = if (row.totalAliyot > 1) "סיימת את השיעור היומי!" else "סיימת את העליה היומית!",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "כל הכבוד על ההתמדה בלימוד החומש היומי",
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
                                                aliyotToDisplay.forEach { aliya ->
                                                    onToggleAliyaCompletion(chumashLesson!!.parashaName, aliya.aliyaIndex)
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
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = if (row.allCompleted) "הושלם ✓" else "סמן כהושלם",
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
                    onDismiss = { showTypographySheet = false },
                    onFontSizeChange = onChangeFontSize,
                    onLineSpacingChange = onLineSpacingChange,
                    onFontFamilyChange = onFontFamilyChange,
                    onNikudToggle = { onToggleShowTeamim() }, // We reuse Nikud for Teamim
                    onThemeChange = onThemeChange,
                    onKeepScreenOnChange = onKeepScreenOnChange
                )
            }
        }
    }
}
