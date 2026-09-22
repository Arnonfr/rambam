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
import androidx.compose.ui.draw.shadow
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

        // Studies — compact 2×2 watch-style dashboard
        if (uiState.selectedTab == HomeTab.STUDY) {
            item {
                val visibleStudies = uiState.preferences.visibleStudies
                val cards = buildList {
                    if (visibleStudies.contains("rambam")) {
                        val total = uiState.dailyLesson?.chapters?.size?.coerceAtLeast(1) ?: 1
                        val completed = uiState.dailyLesson?.chapters?.count {
                            uiState.completedChapterIds.contains("${it.sectionId}_${it.chapterNumber}")
                        } ?: 0
                        add(
                            StudyDashboardCard(
                                eyebrow = if (uiState.preferences.selectedTrack == "three") "שלושה פרקים" else "פרק יומי",
                                title = "רמב״ם",
                                subtitle = rambamSubtitle,
                                metric = "$completed/$total",
                                progress = completed.toFloat() / total,
                                colors = listOf(Color(0xFFFFFBE8), Color(0xFFE6F4C9), Color(0xFFB8DED2), Color(0xFF77B6A5)),
                                onClick = onOpenDailyLesson
                            )
                        )
                    }
                    if (visibleStudies.contains("chumash")) {
                        val total = uiState.dailyChumash?.allAliyot?.size?.coerceAtLeast(1) ?: 7
                        val completed = uiState.dailyChumash?.completedCount ?: 0
                        add(
                            StudyDashboardCard(
                                eyebrow = "חת״ת",
                                title = "חומש",
                                subtitle = chumashSubtitle,
                                metric = "$completed/$total",
                                progress = completed.toFloat() / total,
                                colors = listOf(Color(0xFFFFF7FC), Color(0xFFF0E4F6), Color(0xFFD6D8F0), Color(0xFFA5B3DD)),
                                onClick = onOpenChumash
                            )
                        )
                    }
                    if (visibleStudies.contains("tehillim")) {
                        add(
                            StudyDashboardCard(
                                eyebrow = "תהילים יומי",
                                title = "תהילים",
                                subtitle = tehillimSubtitle,
                                metric = "12/30",
                                progress = 0.40f,
                                colors = listOf(Color(0xFFFFF7F2), Color(0xFFF7DED9), Color(0xFFE5B7C1), Color(0xFFB97691)),
                                onClick = onOpenTehillim
                            )
                        )
                    }
                    if (visibleStudies.contains("tanya")) {
                        val completed = uiState.dailyTanya?.isCompleted == true
                        add(
                            StudyDashboardCard(
                                eyebrow = "חת״ת",
                                title = "תניא",
                                subtitle = tanyaSubtitle,
                                metric = if (completed) "✓" else "0/1",
                                progress = if (completed) 1f else 0f,
                                colors = listOf(Color(0xFFFFFAE8), Color(0xFFF8E9C3), Color(0xFFECC99B), Color(0xFFD89D76)),
                                onClick = onOpenTanya
                            )
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    cards.chunked(2).forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            pair.forEach { card ->
                                VibrantDashboardCard(card, Modifier.weight(1f))
                            }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        } else {
            item {
                Text("תפילות", fontSize = 18.sp, color = Color.Black)
            }
        }
    }
}

private data class StudyDashboardCard(
    val eyebrow: String,
    val title: String,
    val subtitle: String,
    val metric: String,
    val progress: Float,
    val colors: List<Color>,
    val onClick: () -> Unit
)

@Composable
private fun VibrantDashboardCard(
    card: StudyDashboardCard,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(22.dp)
    Box(
        modifier = modifier
            .aspectRatio(1.02f)
            .shadow(
                elevation = 7.dp,
                shape = cardShape,
                ambientColor = card.colors.last().copy(alpha = 0.12f),
                spotColor = Color.Black.copy(alpha = 0.08f)
            )
            .clip(cardShape)
            .background(
                androidx.compose.ui.graphics.Brush.radialGradient(
                    colors = card.colors
                )
            )
            .border(
                width = 1.dp,
                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.76f),
                        Color.White.copy(alpha = 0.20f),
                        Color.Black.copy(alpha = 0.05f)
                    )
                ),
                shape = cardShape
            )
            .clickable(onClick = card.onClick)
    ) {
        // A soft central bloom keeps the color source in the middle.
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(132.dp)
                .background(
                    brush = androidx.compose.ui.graphics.Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.23f),
                            Color.White.copy(alpha = 0.07f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        // Restrained glass reflections: light catches the rim instead of forming side bands.
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(38.dp)
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.20f),
                            Color.White.copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(28.dp)
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.035f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 15.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = card.eyebrow,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontSize = 10.sp,
                fontWeight = FontWeight.Normal,
                color = Color.Black.copy(alpha = 0.45f)
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = card.title,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontSize = 20.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF201F1D)
            )

            Spacer(Modifier.weight(1f))

            Text(
                text = card.metric,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontSize = 28.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.Light,
                color = Color.Black.copy(alpha = 0.66f)
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = card.subtitle,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontSize = 10.sp,
                lineHeight = 13.sp,
                color = Color.Black.copy(alpha = 0.48f),
                maxLines = 2
            )
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { card.progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth(0.68f)
                    .height(2.dp)
                    .clip(CircleShape),
                color = Color.Black.copy(alpha = 0.30f),
                trackColor = Color.White.copy(alpha = 0.42f)
            )
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
