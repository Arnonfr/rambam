package com.example.ui.screens

import android.app.Activity
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
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
import com.example.domain.tehillim.DailyTehillimLesson
import com.example.domain.tehillim.TehillimChapter
import com.example.ui.components.FloatingReaderBar
import com.example.ui.components.ReaderTypographySheet
import com.example.ui.components.StudyTextBlock
import com.example.ui.theme.*

enum class TehillimTab {
    DAILY,
    ELUL
}

sealed interface TehillimUiRow {
    val key: String

    data class ChapterHeader(
        val chapter: TehillimChapter
    ) : TehillimUiRow {
        override val key: String = "hdr_${chapter.chapterNumber}"
    }

    data class VerseRow(
        val chapter: TehillimChapter,
        val verse: com.example.domain.tehillim.TehillimVerse,
        val indexInChapter: Int,
        val totalInChapter: Int
    ) : TehillimUiRow {
        override val key: String = "ch_${chapter.chapterNumber}_v_${verse.verseNumber}"
    }
}

@Composable
fun TehillimReaderScreen(
    lesson: DailyTehillimLesson?,
    isLoading: Boolean,
    onBack: () -> Unit,
    preferences: UserPreferences,
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
    onSavePosition: (com.example.domain.tehillim.TehillimVerse, TehillimChapter) -> Unit = { _, _ -> },
    savedPosition: com.example.data.local.ReadingPositionEntity? = null,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    var activeTab by remember { mutableStateOf(TehillimTab.DAILY) }
    var showTypographySheet by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

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

    // Reset list state when switching tabs
    LaunchedEffect(activeTab) {
        listState.scrollToItem(0)
    }

    if (isLoading || lesson == null) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
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

    // Content List for the active tab
    val activeChapters = if (activeTab == TehillimTab.DAILY) {
        lesson.chapters
    } else {
        lesson.elulChapters
    }

    // Flatten chapters and verses into UI rows
    val flatRows = remember(activeChapters) {
        val rows = mutableListOf<TehillimUiRow>()
        activeChapters.forEach { chapter ->
            rows.add(TehillimUiRow.ChapterHeader(chapter))
            val totalVerses = chapter.verses.size
            chapter.verses.forEachIndexed { idx, verse ->
                rows.add(
                    TehillimUiRow.VerseRow(
                        chapter = chapter,
                        verse = verse,
                        indexInChapter = idx,
                        totalInChapter = totalVerses
                    )
                )
            }
        }
        rows
    }

    // Identify current active chapter and verse from scroll index
    fun getActiveVerseInfo(visibleIndex: Int): Pair<TehillimUiRow.VerseRow?, TehillimUiRow.ChapterHeader?> {
        val direct = flatRows.getOrNull(visibleIndex)
        if (direct is TehillimUiRow.VerseRow) return Pair(direct, null)
        if (direct is TehillimUiRow.ChapterHeader) return Pair(null, direct)

        // Preceding lookup
        for (i in visibleIndex downTo 0) {
            val r = flatRows.getOrNull(i)
            if (r is TehillimUiRow.VerseRow) return Pair(r, null)
            if (r is TehillimUiRow.ChapterHeader) return Pair(null, r)
        }
        return Pair(null, null)
    }

    // Dynamic Text indicator for the bottom bar, formatted as "תהילים · פרק א׳ · פסוק ב׳"
    val currentVerseText = remember(flatRows, activeTab) {
        derivedStateOf {
            val (activeVerse, activeHeader) = getActiveVerseInfo(listState.firstVisibleItemIndex)
            val prefix = if (activeTab == TehillimTab.ELUL) "אלול" else "תהילים"
            if (activeVerse != null) {
                val chName = activeVerse.chapter.chapterHebrew
                val verseNum = activeVerse.verse.verseHebrew
                "$prefix · $chName · $verseNum"
            } else if (activeHeader != null) {
                "$prefix · ${activeHeader.chapter.chapterHebrew}"
            } else {
                if (activeTab == TehillimTab.ELUL) "פרקי אלול/תשרי" else "ספר התהילים"
            }
        }
    }

    // Scroll progress along the active chapters list (0.0 to 1.0)
    val scrollProgress by remember(flatRows) {
        derivedStateOf {
            if (flatRows.size <= 1) 0f
            else {
                val progress = listState.firstVisibleItemIndex.toFloat() / (flatRows.size - 1).toFloat()
                progress.coerceIn(0f, 1f)
            }
        }
    }

    // Scroll to saved position on first load
    var hasScrolledToSavedPosition by remember { mutableStateOf(false) }
    LaunchedEffect(flatRows, savedPosition) {
        if (!hasScrolledToSavedPosition && savedPosition != null && flatRows.isNotEmpty()) {
            val targetIdx = flatRows.indexOfFirst { row ->
                row is TehillimUiRow.VerseRow &&
                    row.chapter.chapterNumber == savedPosition.chapterNumber &&
                    row.verse.verseNumber == (savedPosition.halachaIndex + 1)
            }
            if (targetIdx >= 0) {
                listState.scrollToItem(targetIdx)
            }
            hasScrolledToSavedPosition = true
        }
    }

    // Save reading position when scrolling
    var lastSaveTime by remember { mutableStateOf(0L) }
    LaunchedEffect(listState.firstVisibleItemIndex) {
        val (activeVerse, _) = getActiveVerseInfo(listState.firstVisibleItemIndex)
        if (activeVerse != null) {
            val now = System.currentTimeMillis()
            if (now - lastSaveTime > 1000) {
                onSavePosition(activeVerse.verse, activeVerse.chapter)
                lastSaveTime = now
            }
        }
    }

    val activeFontFamily = FontStyleOption.fromId(preferences.fontFamily).fontFamily

    // Background color based on chosen theme preference
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

    val themeSurfaceColor = when (preferences.readerTheme) {
        "dark" -> Color(0xFF1E222A)
        "sepia" -> Color(0xFF4A3828)
        else -> DeepNavy
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
                // Header card explaining the standard day of the month portion
                item(key = "tehillim_intro_card") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "ספר התהילים היומי",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = when (preferences.readerTheme) {
                                "dark" -> GoldAccent
                                "sepia" -> Color(0xFF8C6D4F)
                                else -> PurplePrimary
                            },
                            fontFamily = activeFontFamily,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "יום ${lesson.dayOfMonthHebrew} בחודש",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = textColor.copy(alpha = 0.7f),
                            fontFamily = activeFontFamily,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        HorizontalDivider(
                            modifier = Modifier
                                .width(80.dp)
                                .padding(top = 12.dp),
                            thickness = 2.dp,
                            color = GoldAccent
                        )
                    }
                }

                // If on Elul/Tishrei tab, render the beautiful contextual banner
                if (activeTab == TehillimTab.ELUL && lesson.elulDaysDescription != null) {
                    item(key = "tehillim_elul_desc") {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = when (preferences.readerTheme) {
                                "dark" -> Color(0xFF23252E)
                                "sepia" -> Color(0xFFEADFCA)
                                else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            },
                            border = BorderStroke(
                                1.dp,
                                when (preferences.readerTheme) {
                                    "dark" -> Color(0xFF333742)
                                    "sepia" -> Color(0xFFD6C8B0)
                                    else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Stars,
                                    contentDescription = null,
                                    tint = GoldAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = lesson.elulDaysDescription,
                                    fontSize = 13.sp,
                                    color = when (preferences.readerTheme) {
                                        "dark" -> Color(0xFFE2E2E6)
                                        "sepia" -> Color(0xFF534230)
                                        else -> MaterialTheme.colorScheme.onPrimaryContainer
                                    },
                                    lineHeight = 19.sp,
                                    textAlign = TextAlign.Start,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                // Render the flat chapters and verses list
                itemsIndexed(
                    items = flatRows,
                    key = { _, row -> row.key }
                ) { _, row ->
                    when (row) {
                        is TehillimUiRow.ChapterHeader -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 32.dp, bottom = 18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = row.chapter.chapterHebrew,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (preferences.readerTheme) {
                                        "dark" -> GoldAccent
                                        "sepia" -> Color(0xFF8C6D4F)
                                        else -> PurplePrimary
                                    },
                                    fontFamily = activeFontFamily,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(
                                    modifier = Modifier.width(40.dp),
                                    thickness = 1.5.dp,
                                    color = GoldAccent.copy(alpha = 0.8f)
                                )
                            }
                        }

                        is TehillimUiRow.VerseRow -> {
                            val cleanText = if (preferences.chumashShowTeamim) {
                                row.verse.textWithTeamim
                            } else {
                                row.verse.textPlainNikud
                            }

                            StudyTextBlock(
                                indexLetter = row.verse.verseHebrew,
                                textWithNikud = cleanText,
                                textPlain = row.verse.textPlainNikud,
                                preferences = preferences,
                                fontFamily = activeFontFamily,
                                textColor = textColor,
                                fontSizeSp = preferences.chumashFontSizeSp,
                                modifier = Modifier.padding(bottom = 18.dp),
                                testTag = "tehillim_verse_${row.chapter.chapterNumber}_${row.verse.verseNumber}"
                            )
                        }
                    }
                }
            }

            // Unified bottom bar container
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Compact Segment Switcher for "יומי" / "ג״פ אלול"
                if (lesson.elulChapters.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = themeSurfaceColor.copy(alpha = 0.95f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                        modifier = Modifier.padding(bottom = 4.dp).shadow(4.dp, RoundedCornerShape(12.dp))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(2.dp)
                        ) {
                            // Tab 1: Daily ("יומי")
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (activeTab == TehillimTab.DAILY) Color.White.copy(alpha = 0.15f)
                                        else Color.Transparent
                                    )
                                    .clickable { activeTab = TehillimTab.DAILY }
                                    .padding(vertical = 5.dp, horizontal = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "יומי",
                                    fontSize = 11.sp,
                                    fontWeight = if (activeTab == TehillimTab.DAILY) FontWeight.Bold else FontWeight.Medium,
                                    color = if (activeTab == TehillimTab.DAILY) GoldAccent else Color.White.copy(alpha = 0.8f)
                                )
                            }

                            // Tab 2: Elul ("ג״פ אלול")
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (activeTab == TehillimTab.ELUL) Color.White.copy(alpha = 0.15f)
                                        else Color.Transparent
                                    )
                                    .clickable { activeTab = TehillimTab.ELUL }
                                    .padding(vertical = 5.dp, horizontal = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "ג״פ אלול",
                                    fontSize = 11.sp,
                                    fontWeight = if (activeTab == TehillimTab.ELUL) FontWeight.Bold else FontWeight.Medium,
                                    color = if (activeTab == TehillimTab.ELUL) GoldAccent else Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }

                val displayDate = if (hebrewDateText.isNotBlank()) hebrewDateText else "תהלים"
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
