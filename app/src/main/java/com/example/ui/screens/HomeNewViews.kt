package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.schedule.DailyLessonResult
import com.example.ui.DailyRambamUiState
import com.example.ui.HomeTab
import com.example.ui.util.HebrewDateHelper
import java.time.LocalDate

@Composable
fun MainContentList(
    uiState: DailyRambamUiState,
    onOpenChapter: (String) -> Unit,
    onOpenDailyLesson: () -> Unit,
    onResumeReading: () -> Unit,
    onOpenChumash: () -> Unit,
    onOpenTehillim: () -> Unit,
    onOpenTanya: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rambamSubtitle = remember(uiState.dailyLesson) {
        val lesson = uiState.dailyLesson
        if (lesson == null || lesson.chapters.isEmpty()) {
            "טוען..."
        } else {
            val chapters = lesson.chapters
            if (chapters.size == 1) {
                val ch = chapters[0]
                "${ch.sectionNameHebrew} · ${ch.chapterHebrew}"
            } else {
                val first = chapters.first()
                val last = chapters.last()
                if (first.sectionId == last.sectionId) {
                    "${first.sectionNameHebrew} · ${first.chapterHebrew} - ${last.chapterHebrew}"
                } else {
                    "${first.sectionNameHebrew} · ${first.chapterHebrew} עד ${last.sectionNameHebrew} · ${last.chapterHebrew}"
                }
            }
        }
    }

    val chumashSubtitle = remember(uiState.dailyChumash, uiState.selectedStudyDate) {
        val chumash = uiState.dailyChumash
        val aliya = chumash?.todayAliya
        if (chumash != null && aliya != null) {
            "פרשת ${chumash.parashaName} · עלייה ${aliya.aliyaName}"
        } else {
            val dayOfWeekHebrew = when (uiState.selectedStudyDate.dayOfWeek.value) {
                1 -> "שני"
                2 -> "שלישי"
                3 -> "רביעי"
                4 -> "חמישי"
                5 -> "שישי"
                6 -> "שבת"
                7 -> "ראשון"
                else -> "ראשון"
            }
            "שיעור יום $dayOfWeekHebrew"
        }
    }

    val tehillimSubtitle = remember(uiState.dailyLesson) {
        val hebDate = uiState.dailyLesson?.hebrewDate
        if (!hebDate.isNullOrBlank()) {
            val dayNum = HebrewDateHelper.extractHebrewDayNumeral(hebDate)
            "יום $dayNum בחודש"
        } else {
            "יום ב׳ בחודש"
        }
    }

    val tanyaSubtitle = remember(uiState.dailyTanya, uiState.selectedStudyDate) {
        val tanya = uiState.dailyTanya
        if (tanya != null && tanya.chapterTitle.isNotBlank()) {
            "${tanya.bookTitle} · ${tanya.chapterTitle}"
        } else {
            val dayName = HebrewDateHelper.getHebrewDayOfWeekName(uiState.selectedStudyDate.dayOfWeek)
            "שיעור יום $dayName"
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        // Continue Reading (Tiny Banner)
        if (uiState.latestPosition != null && uiState.selectedTab == HomeTab.STUDY) {
            item {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFBDDB9),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .clickable { onResumeReading() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Bookmark,
                                contentDescription = null,
                                tint = Color(0xFFE88130),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "להמשיך מהמקום האחרון: " + (if (uiState.latestChapter != null) {
                                    "${uiState.latestChapter.chapterHebrew} ${uiState.latestHalachaTitle ?: ""}"
                                } else {
                                    uiState.latestHalachaTitle ?: ""
                                }),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF352920)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowLeft,
                            contentDescription = "המשך",
                            tint = Color(0xFFE88130),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "הלימוד היומי", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2D241E))
                IconButton(onClick = onOpenSettings, modifier = Modifier.size(36.dp)) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = "הגדרות לימוד", tint = Color(0xFF8B6B4A), modifier = Modifier.size(20.dp))
                }
            }
        }

        // Studies
        if (uiState.selectedTab == HomeTab.STUDY) {
            val visibleStudies = uiState.preferences.visibleStudies
            if (visibleStudies.contains("rambam")) {
                item {
                    val isCompleted = uiState.isCurrentChapterCompleted
                    val trackTag = if (uiState.preferences.selectedTrack == "three") "3 פרקים" else "פרק 1"
                    StudyItemRow(
                        title = "רמב״ם",
                        subtitle = rambamSubtitle,
                        tag = trackTag,
                        iconBgColor = Color(0xFFF5DAC2),
                        iconTintColor = Color(0xFFD98A6C),
                        icon = Icons.Default.MenuBook,
                        isCompleted = isCompleted,
                        onClick = onOpenDailyLesson
                    )
                }
            }
            if (visibleStudies.contains("chumash")) {
                item {
                    val isCompleted = uiState.dailyChumash?.allAliyot?.all { it.isCompleted } == true
                    StudyItemRow(
                        title = "חומש",
                        subtitle = chumashSubtitle,
                        tag = null,
                        iconBgColor = Color(0xFFDFF0C2),
                        iconTintColor = Color(0xFF8DB264),
                        icon = Icons.AutoMirrored.Filled.List, // scroll like
                        isCompleted = isCompleted,
                        onClick = onOpenChumash
                    )
                }
            }
            if (visibleStudies.contains("tehillim")) {
                item {
                    StudyItemRow(
                        title = "תהילים",
                        subtitle = tehillimSubtitle,
                        tag = null,
                        iconBgColor = Color(0xFFE6D3F0),
                        iconTintColor = Color(0xFF9C77B7),
                        icon = Icons.Default.MusicNote,
                        isCompleted = false,
                        onClick = onOpenTehillim
                    )
                }
            }
            if (visibleStudies.contains("tanya")) {
                item {
                    val isCompleted = uiState.dailyTanya?.isCompleted == true
                    StudyItemRow(
                        title = "תניא",
                        subtitle = tanyaSubtitle,
                        tag = null,
                        iconBgColor = Color(0xFFFCE6C2),
                        iconTintColor = Color(0xFFD49D56),
                        icon = Icons.Default.Eco,
                        isCompleted = isCompleted,
                        isLast = true,
                        onClick = onOpenTanya
                    )
                }
            }
        } else {
            item {
                Text("תפילות", fontSize = 18.sp, color = Color.Black)
            }
        }
    }
}

@Composable
fun StudyItemRow(
    title: String,
    subtitle: String,
    tag: String?,
    iconBgColor: Color,
    iconTintColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isCompleted: Boolean,
    isLast: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Main Card
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            shadowElevation = 2.dp,
            modifier = Modifier
                .weight(1f)
                .clickable { onClick() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Icon Box
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = iconBgColor,
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = iconTintColor, modifier = Modifier.size(28.dp))
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(horizontalAlignment = Alignment.Start, modifier = Modifier.weight(1f)) {
                    Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2D241E))
                    Text(subtitle, fontSize = 14.sp, color = Color(0xFF5C5249))
                    if (tag != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF3E6DF),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(tag, fontSize = 10.sp, color = Color(0xFFD98A6C), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }
                
                Icon(
                    imageVector = Icons.Default.KeyboardArrowLeft,
                    contentDescription = null,
                    tint = Color.LightGray,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun FloatingDarkNavBar(
    selectedTab: HomeTab,
    onSelectTab: (HomeTab) -> Unit,
    onOpenEditSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(32.dp),
        color = Color(0xFF352920),
        modifier = modifier
            .width(260.dp)
            .height(56.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Prayers Tab (Left)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelectTab(HomeTab.PRAYERS) },
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.NightlightRound,
                    contentDescription = null,
                    tint = if (selectedTab == HomeTab.PRAYERS) Color(0xFFF6C879) else Color(0xFF8B827A),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "תפילות",
                    color = if (selectedTab == HomeTab.PRAYERS) Color.White else Color(0xFF8B827A),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Divider
            Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color(0xFF5C5249)))

            // Study Tab (Right)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelectTab(HomeTab.STUDY) },
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.MenuBook,
                    contentDescription = null,
                    tint = if (selectedTab == HomeTab.STUDY) Color(0xFFF6C879) else Color(0xFF8B827A),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "לימוד",
                    color = if (selectedTab == HomeTab.STUDY) Color.White else Color(0xFF8B827A),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
