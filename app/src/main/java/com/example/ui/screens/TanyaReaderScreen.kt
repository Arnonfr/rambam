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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.UserPreferences
import com.example.domain.tanya.DailyTanyaLesson
import com.example.domain.tanya.TanyaSection
import com.example.ui.components.FloatingReaderBar
import com.example.ui.components.ReaderTypographySheet
import com.example.ui.components.StudyTextBlock
import com.example.ui.theme.*

@Composable
fun TanyaReaderScreen(
    lesson: DailyTanyaLesson?,
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
    onToggleCompletion: () -> Unit = {},
    onSavePosition: (TanyaSection, DailyTanyaLesson) -> Unit = { _, _ -> },
    savedPosition: com.example.data.local.ReadingPositionEntity? = null,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

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

    val sections = lesson.sections

    // Identify current active section from scroll index (offset by 1 for intro card)
    val currentSectionText = remember(sections) {
        derivedStateOf {
            val visibleIdx = listState.firstVisibleItemIndex
            if (visibleIdx == 0) {
                "תניא · פתיחה"
            } else {
                val secIdx = (visibleIdx - 1).coerceIn(0, (sections.size - 1).coerceAtLeast(0))
                val sec = sections.getOrNull(secIdx)
                if (sec != null) "תניא · ${sec.sectionHebrew}" else "ספר התניא"
            }
        }
    }

    // Scroll progress along the sections list (0.0 to 1.0)
    val scrollProgress by remember(sections) {
        derivedStateOf {
            if (sections.size <= 1) 0f
            else {
                val progress = listState.firstVisibleItemIndex.toFloat() / sections.size.toFloat()
                progress.coerceIn(0f, 1f)
            }
        }
    }

    // Scroll to saved position on first load
    var hasScrolledToSavedPosition by remember { mutableStateOf(false) }
    LaunchedEffect(sections, savedPosition) {
        if (!hasScrolledToSavedPosition && savedPosition != null && sections.isNotEmpty()) {
            val targetIdx = savedPosition.halachaIndex + 1 // +1 for header item
            if (targetIdx in 0..sections.size) {
                listState.scrollToItem(targetIdx)
            }
            hasScrolledToSavedPosition = true
        }
    }

    // Save reading position when scrolling
    var lastSaveTime by remember { mutableStateOf(0L) }
    LaunchedEffect(listState.firstVisibleItemIndex) {
        val visibleIdx = listState.firstVisibleItemIndex
        if (visibleIdx > 0 && visibleIdx <= sections.size) {
            val activeSection = sections.getOrNull(visibleIdx - 1)
            if (activeSection != null) {
                val now = System.currentTimeMillis()
                if (now - lastSaveTime > 1000) {
                    onSavePosition(activeSection, lesson)
                    lastSaveTime = now
                }
            }
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
                // 1. Header Card (Same structure as Tehillim and Chumash)
                item(key = "tanya_intro_card") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "ספר התניא קדישא",
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
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "${lesson.bookTitle} · ${lesson.chapterTitle}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textColor.copy(alpha = 0.9f),
                            fontFamily = activeFontFamily,
                            textAlign = TextAlign.Center
                        )
                        if (lesson.hebrewDate.isNotBlank()) {
                            Text(
                                text = "שיעור יום ${lesson.hebrewDate}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Normal,
                                color = textColor.copy(alpha = 0.65f),
                                fontFamily = activeFontFamily,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        HorizontalDivider(
                            modifier = Modifier
                                .width(80.dp)
                                .padding(top = 12.dp),
                            thickness = 2.dp,
                            color = GoldAccent
                        )
                    }
                }

                // 2. Sections / Paragraphs
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
                        fontSizeSp = preferences.fontSizeSp,
                        modifier = Modifier.padding(bottom = 18.dp),
                        testTag = "tanya_section_${section.sectionIndex}"
                    )
                }

                // 3. Completion Card
                item(key = "tanya_completed_card") {
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
                                imageVector = if (lesson.isCompleted) Icons.Default.CheckCircle else Icons.Default.Stars,
                                contentDescription = null,
                                tint = if (lesson.isCompleted) CompletedGreen else GoldAccent,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "סיימת את שיעור התניא היומי!",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "אשריך שזכית לעסוק בפנימיות התורה",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = onToggleCompletion,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = CompletedGreen,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(46.dp)
                                        .testTag("tanya_toggle_complete_button")
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (lesson.isCompleted) "הושלם ✓" else "סמן כהושלם",
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
                                        .testTag("tanya_back_home_button")
                                ) {
                                    Text("חזרה לדף הבית", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }
            }

            // 4. Floating Reader Bar at Bottom
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
