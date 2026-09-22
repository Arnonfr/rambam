package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.schedule.DailyLessonResult
import com.example.domain.schedule.ScheduledChapter
import com.example.domain.sunset.SupportedCities
import com.example.ui.DailyRambamUiState
import com.example.ui.DateDrawerState
import com.example.ui.HomeTab
import com.example.ui.theme.*
import com.example.ui.util.HebrewDateHelper
import com.example.ui.util.HebrewNumberFormatter
import java.time.DayOfWeek
import java.time.LocalDate

// Design Colors from PRD & Mock
private val DarkNavy = Color(0xFF1E293B)
private val PeachAccent = Color(0xFFFB923C)
private val TextDark = Color(0xFF0F172A)
private val TextMuted = Color(0xFF64748B)
private val CardBorderColor = Color(0xFFE2E8F0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: DailyRambamUiState,
    onGetHebrewDate: (LocalDate) -> String = { _ -> "" },
    onOpenChapter: (String) -> Unit,
    onOpenDailyLesson: () -> Unit,
    onResumeReading: () -> Unit,
    onTrackSelect: (String) -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onStepDate: (Long) -> Unit,
    onToggleDateDrawer: () -> Unit,
    onSelectTab: (HomeTab) -> Unit,
    onOpenEditSheet: () -> Unit,
    onCloseEditSheet: () -> Unit,
    onToggleStudyVisibility: (String) -> Unit,
    onDayBoundaryChange: (String) -> Unit,
    onCityChange: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAttribution: () -> Unit,
    onOpenChumash: () -> Unit = {},
    onOpenTehillim: () -> Unit = {},
    onOpenTanya: () -> Unit = {}
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        val backgroundGradient = androidx.compose.ui.graphics.Brush.verticalGradient(
            colors = listOf(Color(0xFFF8EAE2), Color(0xFFE2EFD8))
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundGradient)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // 1. Top App Bar
                TopHomeBar(
                    onOpenSettings = onOpenSettings,
                    onOpenAttribution = onOpenAttribution
                )
                
                // 2. Date Drawer
                DateDrawer(
                    selectedDate = uiState.selectedStudyDate,
                    effectiveToday = uiState.effectiveToday,
                    drawerState = uiState.dateDrawerState,
                    dailyLesson = uiState.dailyLesson,
                    onGetHebrewDate = onGetHebrewDate,
                    onSelectDate = onSelectDate,
                    onStepDate = onStepDate,
                    onToggleDrawer = onToggleDateDrawer
                )

                // 3. Main Content
                MainContentList(
                    uiState = uiState,
                    onOpenChapter = onOpenChapter,
                    onOpenDailyLesson = onOpenDailyLesson,
                    onResumeReading = onResumeReading,
                    onOpenChumash = onOpenChumash,
                    onOpenTehillim = onOpenTehillim,
                    onOpenTanya = onOpenTanya,
                    onOpenSettings = onOpenSettings,
                    modifier = Modifier.weight(1f)
                )
            }

            // 4. Floating Capsule Navigation at Bottom
            FloatingDarkNavBar(
                selectedTab = uiState.selectedTab,
                onSelectTab = onSelectTab,
                onOpenEditSheet = onOpenEditSheet,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            )

            // 5. Edit Daily Study Bottom Sheet ("התאמת הלימוד היומי")
            if (uiState.showEditStudySheet) {
                EditDailyStudySheet(
                    selectedTrack = uiState.preferences.selectedTrack,
                    visibleStudies = uiState.preferences.visibleStudies,
                    dayBoundary = uiState.preferences.dayBoundary,
                    selectedCity = uiState.preferences.selectedCity,
                    onTrackChange = onTrackSelect,
                    onToggleStudy = onToggleStudyVisibility,
                    onDayBoundaryChange = onDayBoundaryChange,
                    onCityChange = onCityChange,
                    onDismiss = onCloseEditSheet
                )
            }
        }
    }
}

/**
 * Top App Bar with Title and Action Icons
 */
@Composable
private fun TopHomeBar(
    onOpenSettings: () -> Unit,
    onOpenAttribution: () -> Unit
) {
    Surface(
        color = Color(0xFFF8FAFC),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // App Title Right
            Text(
                text = "לימוד יומי",
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark,
                textAlign = TextAlign.Start
            )

            // Minimal Action Icons Left
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("home_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "הגדרות",
                        tint = TextMuted,
                        modifier = Modifier.size(22.dp)
                    )
                }

                IconButton(
                    onClick = onOpenAttribution,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("home_info_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "אודות",
                        tint = TextMuted,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

/**
 * Date Drawer: Month/Year header, arrows, 7 days week carousel, and bottom navy handle
 */
@Composable
private fun DateDrawer(
    selectedDate: LocalDate,
    effectiveToday: LocalDate,
    drawerState: DateDrawerState,
    dailyLesson: DailyLessonResult?,
    onGetHebrewDate: (LocalDate) -> String,
    onSelectDate: (LocalDate) -> Unit,
    onStepDate: (Long) -> Unit,
    onToggleDrawer: () -> Unit
) {
    val fullHebrewDate = dailyLesson?.hebrewDate ?: onGetHebrewDate(selectedDate).ifBlank { "ב׳ בתשרי תשפ״ז" }
    val monthYearTitle = HebrewDateHelper.extractHebrewMonthYear(fullHebrewDate)
    val hebrewDay = HebrewDateHelper.extractHebrewDay(fullHebrewDate)
    val civilFormatted = HebrewDateHelper.formatCivilDateHebrew(selectedDate)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8FAFC))
    ) {
        // Top Month & Navigation Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Right Arrow: Previous day
            Surface(
                shape = CircleShape,
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier
                    .size(38.dp)
                    .clickable { onStepDate(-1) }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = "יום קודם",
                        tint = TextDark,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Centered Month & Date Title (Clickable to expand/collapse)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onToggleDrawer() }
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = fullHebrewDate,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = civilFormatted,
                        fontSize = 13.sp,
                        color = TextMuted,
                        fontWeight = FontWeight.Normal
                    )
                    if (selectedDate != effectiveToday) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "•  חזור להיום",
                            fontSize = 13.sp,
                            color = PurplePrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { onSelectDate(effectiveToday) }
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            // Left Arrow: Next day
            Surface(
                shape = CircleShape,
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier
                    .size(38.dp)
                    .clickable { onStepDate(1) }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowLeft,
                        contentDescription = "יום הבא",
                        tint = TextDark,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // 7-day week row (Animated based on expanded/collapsed)
        AnimatedVisibility(
            visible = drawerState == DateDrawerState.EXPANDED
        ) {
            val sunday = selectedDate.minusDays((selectedDate.dayOfWeek.value % 7).toLong())
            val weekDays = remember(selectedDate) {
                (0..6).map { sunday.plusDays(it.toLong()) }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                weekDays.forEach { date ->
                    val isSelected = date == selectedDate
                    val isToday = date == effectiveToday
                    val isShabbat = date.dayOfWeek == DayOfWeek.SATURDAY
                    val dateHebrewStr = onGetHebrewDate(date)
                    val hebDayNumeral = if (dateHebrewStr.isNotBlank()) {
                        HebrewDateHelper.extractHebrewDayNumeral(dateHebrewStr)
                    } else {
                        HebrewDateHelper.toHebrewNumeral(date.dayOfMonth)
                    }

                    DayCell(
                        date = date,
                        hebrewDayNumeral = hebDayNumeral,
                        isSelected = isSelected,
                        isToday = isToday,
                        isShabbat = isShabbat,
                        onClick = { onSelectDate(date) }
                    )
                }
            }
        }

        // Subtle bottom handle bar (32dp)
        Surface(
            color = Color(0xFFF1F5F9), // Very light gray instead of Dark Navy
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .clickable { onToggleDrawer() }
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Small drag chevron
                    Icon(
                        imageVector = if (drawerState == DateDrawerState.EXPANDED)
                            Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "פתיחה / סגירת מגירת תאריך",
                        tint = Color(0xFF94A3B8), // Muted gray
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Single day cell in the week carousel - Hebrew date prominent, civil date small below
 */
@Composable
private fun DayCell(
    date: LocalDate,
    hebrewDayNumeral: String,
    isSelected: Boolean,
    isToday: Boolean,
    isShabbat: Boolean,
    onClick: () -> Unit
) {
    val dayOfWeekHebrew = HebrewDateHelper.getHebrewDayOfWeekShort(date.dayOfWeek)
    val civilDay = date.dayOfMonth.toString()

    if (isSelected) {
        // Vertical purple capsule (50dp x 74dp)
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = PurplePrimary,
            modifier = Modifier
                .width(50.dp)
                .height(74.dp)
                .clickable(onClick = onClick)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 5.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = dayOfWeekHebrew,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = hebrewDayNumeral,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = civilDay,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    if (isToday) {
                        Box(
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(GoldAccent)
                        )
                    }
                }
            }
        }
    } else {
        // Normal cell with optional peach indicator for effective today
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color.Transparent,
            modifier = Modifier
                .width(46.dp)
                .height(72.dp)
                .clickable(onClick = onClick)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 5.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = dayOfWeekHebrew,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isShabbat) PurplePrimary else TextMuted
                )
                Text(
                    text = hebrewDayNumeral,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextDark
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = civilDay,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF94A3B8)
                    )
                    if (isToday) {
                        Box(
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(PurplePrimary)
                        )
                    }
                }
            }
        }
    }
}

/**
 * White Content Surface with Rounded Top Corners overlapping Navy bar
 */
@Composable
private fun WhiteContentSurface(
    uiState: DailyRambamUiState,
    onOpenChapter: (String) -> Unit,
    onOpenDailyLesson: () -> Unit,
    onResumeReading: () -> Unit,
    onOpenEditSheet: () -> Unit,
    onOpenChumash: () -> Unit = {},
    onOpenTehillim: () -> Unit = {},
    onOpenTanya: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        color = Color.White,
        modifier = modifier
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row: "השיעורים שלי" + Pencil Edit Icon + Resume reading anchor
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (uiState.selectedTab == HomeTab.STUDY) "השיעורים שלי" else "תפילות היום",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )
                            Text(
                                text = uiState.dailyLesson?.hebrewDate ?: "ב׳ בתשרי תשפ״ז",
                                fontSize = 13.sp,
                                color = TextMuted
                            )
                        }
                    }

                    // Prominent Reading Anchor Card: "המשך מהמקום האחרון שעצרת"
                    if (uiState.latestPosition != null && uiState.selectedTab == HomeTab.STUDY) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = PurplePrimary.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, PurplePrimary.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onResumeReading() }
                                .testTag("reading_anchor_banner")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(PurplePrimary.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Bookmark,
                                            contentDescription = null,
                                            tint = PurplePrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "המשך מהמקום האחרון שעצרת:",
                                            fontSize = 11.sp,
                                            color = TextMuted,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = if (uiState.latestChapter != null) {
                                                "${uiState.latestChapter.chapterHebrew} · ${uiState.latestHalachaTitle ?: "הלכה א׳"}"
                                            } else {
                                                uiState.latestHalachaTitle ?: ""
                                            },
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PurplePrimary
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = PurplePrimary,
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp)
                                    ) {
                                        Text(
                                            text = "המשך",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowLeft,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (uiState.selectedTab == HomeTab.STUDY) {
                item {
                    val studyCards = buildList {
                        if (uiState.preferences.visibleStudies.contains("rambam")) {
                            val chapters = uiState.dailyLesson?.chapters ?: emptyList()
                            val completed = chapters.count {
                                uiState.completedChapterIds.contains("${it.sectionId}_${it.chapterNumber}")
                            }
                            val total = chapters.size.coerceAtLeast(1)
                            add(
                                VibrantStudyCardModel(
                                    eyebrow = if (uiState.preferences.selectedTrack == "three") "שלושה פרקים" else "פרק יומי",
                                    title = "רמב״ם",
                                    subtitle = formatRambamChaptersTitle(chapters),
                                    progressText = "$completed / $total",
                                    progressFraction = completed.toFloat() / total,
                                    colors = listOf(Color(0xFF68F58B), Color(0xFFC7FA62), Color(0xFFFFE56C)),
                                    onClick = onOpenDailyLesson
                                )
                            )
                        }

                        if (uiState.preferences.visibleStudies.contains("chumash")) {
                            val chumash = uiState.dailyChumash
                            val aliya = chumash?.todayAliya
                            val completed = chumash?.completedCount ?: 0
                            val total = chumash?.allAliyot?.size?.coerceAtLeast(1) ?: 7
                            add(
                                VibrantStudyCardModel(
                                    eyebrow = "חת״ת",
                                    title = "חומש",
                                    subtitle = if (chumash != null && aliya != null) {
                                        "פרשת ${chumash.parashaName} · עלייה ${aliya.aliyaName}"
                                    } else {
                                        "השיעור היומי"
                                    },
                                    progressText = "$completed / $total",
                                    progressFraction = completed.toFloat() / total,
                                    colors = listOf(Color(0xFFF2A5FF), Color(0xFFCE78F0), Color(0xFF84A7FF)),
                                    onClick = onOpenChumash
                                )
                            )
                        }

                        if (uiState.preferences.visibleStudies.contains("tehillim")) {
                            add(
                                VibrantStudyCardModel(
                                    eyebrow = "תהילים יומי",
                                    title = "תהילים",
                                    subtitle = "יום ${HebrewDateHelper.extractHebrewDay(uiState.dailyLesson?.hebrewDate ?: "ב׳")} בחודש",
                                    progressText = "12 / 30",
                                    progressFraction = 0.40f,
                                    colors = listOf(Color(0xFFF7C4B4), Color(0xFFE7909D), Color(0xFFAE3A67)),
                                    onClick = onOpenTehillim
                                )
                            )
                        }

                        if (uiState.preferences.visibleStudies.contains("tanya")) {
                            val complete = uiState.dailyTanya?.isCompleted == true
                            add(
                                VibrantStudyCardModel(
                                    eyebrow = "חת״ת",
                                    title = "תניא",
                                    subtitle = uiState.dailyTanya?.chapterTitle ?: "השיעור היומי",
                                    progressText = if (complete) "הושלם" else "0 / 1",
                                    progressFraction = if (complete) 1f else 0f,
                                    colors = listOf(Color(0xFFFF6235), Color(0xFFFF9D31), Color(0xFFFFD52F)),
                                    onClick = onOpenTanya
                                )
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        studyCards.chunked(2).forEach { rowCards ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                rowCards.forEach { card ->
                                    VibrantStudyCard(
                                        model = card,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (rowCards.size == 1) Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            } else {
                // Prayers Tab ("תפילות")
                item {
                    PrayerCard(
                        title = "שחרית",
                        subtitle = "תפילת שחרית וקריאת התורה",
                        progressText = "הושלם",
                        isCompleted = true
                    )
                }
                item {
                    PrayerCard(
                        title = "מנחה",
                        subtitle = "תפילת מנחה",
                        progressText = "0/1",
                        isCompleted = false
                    )
                }
                item {
                    PrayerCard(
                        title = "ערבית",
                        subtitle = "תפילת ערבית וקריאת שמע",
                        progressText = "0/1",
                        isCompleted = false
                    )
                }
                item {
                    PrayerCard(
                        title = "ברכת המזון",
                        subtitle = "ברכת המזון וברכות הנהנין",
                        progressText = "0/1",
                        isCompleted = false
                    )
                }
                item {
                    PrayerCard(
                        title = "קריאת שמע שעל המיטה",
                        subtitle = "סדר קריאת שמע שעל המיטה",
                        progressText = "0/1",
                        isCompleted = false
                    )
                }
            }

            // Bottom space for floating nav bar
            item {
                Spacer(modifier = Modifier.height(76.dp))
            }
        }
    }
}

/**
 * Formats multi-chapter title for Rambam lesson.
 * If 1 chapter: e.g. "הלכות אישות · פרק י״ז"
 * If multiple chapters in same section: e.g. "הלכות שאר אבות הטומאות · פרקים י״ח–כ׳"
 * If cross-section: e.g. "שאר אבות הטומאות כ׳, טומאת אוכלין א׳–ב׳"
 */
private fun formatRambamChaptersTitle(chapters: List<ScheduledChapter>): String {
    if (chapters.isEmpty()) return "הלכות אישות · פרק י״ז"
    if (chapters.size == 1) {
        val ch = chapters.first()
        return "${ch.sectionNameHebrew} · ${ch.chapterHebrew}"
    }

    val bySection = chapters.groupBy { it.sectionNameHebrew }
    if (bySection.size == 1) {
        val sectionName = chapters.first().sectionNameHebrew
        val chapterNums = chapters.map { it.chapterNumber }
        val minNum = chapterNums.minOrNull() ?: 1
        val maxNum = chapterNums.maxOrNull() ?: 1
        val minHeb = HebrewNumberFormatter.toHebrewNumeral(minNum)
        val maxHeb = HebrewNumberFormatter.toHebrewNumeral(maxNum)
        return if (minNum == maxNum) {
            "$sectionName · פרק $minHeb"
        } else {
            "$sectionName · פרקים $minHeb–$maxHeb"
        }
    } else {
        return bySection.entries.joinToString(separator = ", ") { (secName, chList) ->
            val shortSecName = secName.removePrefix("הלכות ")
            val nums = chList.map { it.chapterNumber }
            val minNum = nums.minOrNull() ?: 1
            val maxNum = nums.maxOrNull() ?: 1
            val minHeb = HebrewNumberFormatter.toHebrewNumeral(minNum)
            val maxHeb = HebrewNumberFormatter.toHebrewNumeral(maxNum)
            if (minNum == maxNum) {
                "$shortSecName פרק $minHeb"
            } else {
                "$shortSecName פרקים $minHeb–$maxHeb"
            }
        }
    }
}

/**
 * Prominent Purple Rambam Card matching PRD & Mock
 */
@Composable
private fun RambamStudyCard(
    dailyLesson: DailyLessonResult?,
    selectedTrack: String,
    completedChapterIds: Set<String>,
    latestPosition: com.example.data.local.ReadingPositionEntity? = null,
    onOpenDailyLesson: () -> Unit,
    onOpenChapter: (String) -> Unit
) {
    val chapters = dailyLesson?.chapters ?: emptyList()
    val fullTitle = formatRambamChaptersTitle(chapters)
    val totalCount = chapters.size.coerceAtLeast(1)
    val completedCount = chapters.count { ch ->
        completedChapterIds.contains("${ch.sectionId}_${ch.chapterNumber}")
    }
    val progressFraction = (completedCount.toFloat() / totalCount.toFloat()).coerceIn(0f, 1f)

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = PurplePrimary,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("rambam_study_card")
            .clickable { onOpenDailyLesson() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Top Row: Title + Track badge + Left Arrow
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Right: White accent bar + Title "רמב״ם" + Track badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .width(3.5.dp)
                            .height(18.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "רמב״ם",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White.copy(alpha = 0.18f)
                    ) {
                        Text(
                            text = if (selectedTrack == "three") "שלושה פרקים ביום" else "פרק אחד ביום",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                // Left: White arrow
                Icon(
                    imageVector = Icons.Default.KeyboardArrowLeft,
                    contentDescription = "פתח שיעור",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Subtitle: e.g. "הלכות שאר אבות הטומאות · פרקים י״ח–כ׳"
            Text(
                text = fullTitle,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.95f),
                lineHeight = 22.sp
            )

            // If multi-chapter lesson: show individual chapter capsules
            if (chapters.size > 1) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    chapters.forEach { ch ->
                        val chId = "${ch.sectionId}_${ch.chapterNumber}"
                        val isDone = completedChapterIds.contains(chId)
                        val isCurrent = latestPosition?.chapterId == chId
                        val numHeb = HebrewNumberFormatter.toHebrewNumeral(ch.chapterNumber)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = when {
                                isCurrent -> Color.White
                                isDone -> Color.White.copy(alpha = 0.35f)
                                else -> Color.White.copy(alpha = 0.15f)
                            },
                            border = when {
                                isCurrent -> BorderStroke(1.5.dp, Color.White)
                                isDone -> BorderStroke(1.dp, Color.White.copy(alpha = 0.6f))
                                else -> null
                            },
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onOpenChapter(chId) }
                                .testTag("chapter_pill_${ch.chapterNumber}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                if (isDone) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "הושלם",
                                        tint = if (isCurrent) PurplePrimary else Color.White,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                } else if (isCurrent) {
                                    Icon(
                                        imageVector = Icons.Default.Bookmark,
                                        contentDescription = "מיקום נוכחי",
                                        tint = PurplePrimary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                }
                                Text(
                                    text = "פרק $numHeb",
                                    fontSize = 12.sp,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isCurrent) PurplePrimary else Color.White
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Progress text: e.g. "0 מתוך 3 הושלמו" or "1 מתוך 3 הושלמו"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (selectedTrack == "three") {
                        "$completedCount מתוך $totalCount הושלמו"
                    } else {
                        if (completedCount >= 1) "הושלם" else "0 מתוך 1 הושלמו"
                    },
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // White progress track
            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.25f),
            )
        }
    }
}

/**
 * Clean Secondary Study Card (Chumash, Tehillim, Tanya)
 */
@Composable
private fun StudyCard(
    title: String,
    subtitle: String,
    progressText: String,
    progressFraction: Float,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, CardBorderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Right: Dark indigo accent bar + Title
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .width(3.5.dp)
                            .height(18.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(DarkNavy)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                }

                // Left: Progress text + Arrow
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = progressText,
                        fontSize = 12.sp,
                        color = TextMuted,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowLeft,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = DarkNavy,
                trackColor = Color(0xFFF1F5F9),
            )
        }
    }
}

private data class VibrantStudyCardModel(
    val eyebrow: String,
    val title: String,
    val subtitle: String,
    val progressText: String,
    val progressFraction: Float,
    val colors: List<Color>,
    val onClick: () -> Unit
)

@Composable
private fun VibrantStudyCard(
    model: VibrantStudyCardModel,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(0.96f)
            .shadow(12.dp, RoundedCornerShape(24.dp), ambientColor = model.colors.first().copy(alpha = 0.32f))
            .clip(RoundedCornerShape(24.dp))
            .background(
                androidx.compose.ui.graphics.Brush.linearGradient(
                    colors = model.colors
                )
            )
            .clickable(onClick = model.onClick)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 32.dp, y = (-38).dp)
                .size(142.dp)
                .background(
                    androidx.compose.ui.graphics.Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.72f), Color.Transparent)
                    ),
                    CircleShape
                )
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-28).dp, y = 32.dp)
                .size(124.dp)
                .background(
                    androidx.compose.ui.graphics.Brush.radialGradient(
                        colors = listOf(Color(0xFF6B1E54).copy(alpha = 0.28f), Color.Transparent)
                    ),
                    CircleShape
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = model.eyebrow,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black.copy(alpha = 0.70f)
                    )
                    Text(
                        text = model.title,
                        fontSize = 23.sp,
                        lineHeight = 25.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111111)
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.50f)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "פתיחת ${model.title}",
                        tint = Color.Black,
                        modifier = Modifier
                            .padding(7.dp)
                            .size(17.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = model.progressText,
                fontSize = 30.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.Light,
                color = Color.Black.copy(alpha = 0.78f)
            )
            Text(
                text = model.subtitle,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                color = Color.Black.copy(alpha = 0.68f),
                maxLines = 2
            )
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { model.progressFraction.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(CircleShape),
                color = Color.Black.copy(alpha = 0.70f),
                trackColor = Color.White.copy(alpha = 0.38f)
            )
        }
    }
}

/**
 * Prayer Card for Prayers Tab
 */
@Composable
private fun PrayerCard(
    title: String,
    subtitle: String,
    progressText: String,
    isCompleted: Boolean
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, CardBorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(3.5.dp)
                        .height(18.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (isCompleted) Color(0xFF10B981) else DarkNavy)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = title,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isCompleted) Color(0xFFDCFCE7) else Color(0xFFF1F5F9)
            ) {
                Text(
                    text = progressText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isCompleted) Color(0xFF15803D) else TextMuted,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                )
            }
        }
    }
}

/**
 * Floating Glass Pill Navigation Bar ("לימוד" / "תפילות")
 * Glassmorphic style with animated inner sliding capsule
 */
@Composable
private fun FloatingGlassNavBar(
    selectedTab: HomeTab,
    onSelectTab: (HomeTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = Color(0xF2FFFFFF),
        border = BorderStroke(1.dp, Color(0x1F000000)),
        shadowElevation = 8.dp,
        modifier = modifier
            .width(230.dp)
            .height(50.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
        ) {
            // Animated indicator position
            val isStudy = selectedTab == HomeTab.STUDY
            val indicatorAlignment = if (isStudy) Alignment.CenterEnd else Alignment.CenterStart

            // Sliding active capsule
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.5f)
                    .align(indicatorAlignment)
                    .clip(RoundedCornerShape(22.dp))
                    .background(PurpleSoft)
            )

            // Two clickable tab halves
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Prayers Tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(22.dp))
                        .clickable { onSelectTab(HomeTab.PRAYERS) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "תפילות",
                        fontSize = 14.sp,
                        fontWeight = if (!isStudy) FontWeight.Bold else FontWeight.Medium,
                        color = if (!isStudy) PurplePrimary else TextMuted
                    )
                }

                // Study Tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(22.dp))
                        .clickable { onSelectTab(HomeTab.STUDY) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "לימוד",
                        fontSize = 14.sp,
                        fontWeight = if (isStudy) FontWeight.Bold else FontWeight.Medium,
                        color = if (isStudy) PurplePrimary else TextMuted
                    )
                }
            }
        }
    }
}

/**
 * Edit Daily Study Bottom Sheet ("התאמת הלימוד היומי")
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditDailyStudySheet(
    selectedTrack: String,
    visibleStudies: Set<String>,
    dayBoundary: String,
    selectedCity: String,
    onTrackChange: (String) -> Unit,
    onToggleStudy: (String) -> Unit,
    onDayBoundaryChange: (String) -> Unit,
    onCityChange: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                // Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "התאמת הלימוד היומי",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "סגור",
                            tint = TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section 1: מסלול רמב״ם
                Text(
                    text = "מסלול רמב״ם",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
                Spacer(modifier = Modifier.height(10.dp))
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Option 1: פרק אחד ביום (ברירת המחדל)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (selectedTrack == "one") PurpleSoft else Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, if (selectedTrack == "one") PurplePrimary else Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTrackChange("one") }
                            .testTag("track_option_one")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedTrack == "one",
                                onClick = { onTrackChange("one") },
                                colors = RadioButtonDefaults.colors(selectedColor = PurplePrimary)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "פרק אחד ביום",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextDark
                                )
                                Text(
                                    text = "ברירת המחדל · סיום משנה תורה בכשלוש שנים",
                                    fontSize = 12.sp,
                                    color = TextMuted
                                )
                            }
                        }
                    }

                    // Option 2: שלושה פרקים ביום
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (selectedTrack == "three") PurpleSoft else Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, if (selectedTrack == "three") PurplePrimary else Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTrackChange("three") }
                            .testTag("track_option_three")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedTrack == "three",
                                onClick = { onTrackChange("three") },
                                colors = RadioButtonDefaults.colors(selectedColor = PurplePrimary)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "שלושה פרקים ביום",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextDark
                                )
                                Text(
                                    text = "מסלול מואץ · סיום משנה תורה בכל שנה",
                                    fontSize = 12.sp,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Section 2: השיעורים שלי
                Text(
                    text = "השיעורים המוצגים בבית",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
                Spacer(modifier = Modifier.height(8.dp))

                val studies = listOf(
                    "rambam" to "רמב״ם (משנה תורה)",
                    "chumash" to "חומש",
                    "tehillim" to "תהילים",
                    "tanya" to "תניא"
                )

                studies.forEach { (key, name) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = name,
                            fontSize = 14.sp,
                            color = TextDark
                        )
                        Switch(
                            checked = visibleStudies.contains(key),
                            onCheckedChange = { onToggleStudy(key) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = PurplePrimary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Section 3: מתי מתחיל יום חדש
                Text(
                    text = "מתי מתחיל יום חדש",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilterChip(
                        selected = dayBoundary == "midnight",
                        onClick = { onDayBoundaryChange("midnight") },
                        label = { Text("בחצות (ברירת מחדל)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PurpleSoft,
                            selectedLabelColor = PurplePrimary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = dayBoundary == "sunset",
                        onClick = { onDayBoundaryChange("sunset") },
                        label = { Text("בשקיעה") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PurpleSoft,
                            selectedLabelColor = PurplePrimary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                if (dayBoundary == "sunset") {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "עיר לחישוב שקיעה",
                        fontSize = 13.sp,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("ירושלים", "תל אביב", "בני ברק", "חיפה").forEach { city ->
                            FilterChip(
                                selected = selectedCity == city,
                                onClick = { onCityChange(city) },
                                label = { Text(city, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PurpleSoft,
                                    selectedLabelColor = PurplePrimary
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Save Button
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        text = "שמירה",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
