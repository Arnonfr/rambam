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
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.ReceiptLong
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
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(0.dp),
        contentPadding = PaddingValues(bottom = 128.dp)
    ) {
        if (uiState.selectedTab == HomeTab.STUDY) {
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
                                metric = uiState.dailyLesson?.chapters
                                    ?.joinToString("–") { it.chapterHebrew.removePrefix("פרק ") }
                                    ?: "–",
                                progress = completed.toFloat() / total,
                                color = Color(0xFFFF681F),
                                icon = Icons.Outlined.LibraryBooks,
                                onClick = onOpenDailyLesson
                            )
                        )
                    }
                    if (visibleStudies.contains("chumash")) {
                        val total = uiState.dailyChumash?.allAliyot?.size?.coerceAtLeast(1) ?: 7
                        val current = uiState.dailyChumash?.currentAliyaIndex ?: 1
                        add(
                            StudyDashboardCard(
                                eyebrow = "חת״ת",
                                title = "חומש",
                                subtitle = chumashSubtitle,
                                metric = "$current/$total",
                                progress = current.toFloat() / total,
                                color = Color(0xFFF2FF38),
                                icon = Icons.Outlined.ReceiptLong,
                                onClick = onOpenChumash
                            )
                        )
                    }
                    if (visibleStudies.contains("tanya")) {
                        val completed = uiState.dailyTanya?.isCompleted == true
                        val tanyaChapter = uiState.dailyTanya?.heRef
                            ?.let { Regex("([א-ת׳״]+):").find(it)?.groupValues?.getOrNull(1) }
                            ?: "–"
                        add(
                            StudyDashboardCard(
                                eyebrow = "חת״ת",
                                title = "תניא",
                                subtitle = tanyaSubtitle,
                                metric = tanyaChapter,
                                progress = if (completed) 1f else 0f,
                                color = Color(0xFFE98DDE),
                                icon = Icons.Outlined.MenuBook,
                                onClick = onOpenTanya
                            )
                        )
                    }
                    if (visibleStudies.contains("tehillim")) {
                        val day = uiState.dailyTehillim?.dayOfMonth ?: 1
                        add(
                            StudyDashboardCard(
                                eyebrow = "תהילים יומי",
                                title = "תהילים",
                                subtitle = tehillimSubtitle,
                                metric = "$day/30",
                                progress = day / 30f,
                                color = Color(0xFFBCCB72),
                                icon = Icons.Outlined.MusicNote,
                                onClick = onOpenTehillim
                            )
                        )
                    }
                }

            items(cards) { card ->
                StudyStrip(card)
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
    val color: Color,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@Composable
private fun StudyStrip(
    card: StudyDashboardCard,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(132.dp)
            .background(card.color)
            .clickable(onClick = card.onClick)
            .padding(horizontal = 18.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = card.icon,
            contentDescription = null,
            tint = Color.Black,
            modifier = Modifier.size(52.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = card.title,
                fontSize = 28.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Text(
                text = card.subtitle,
                fontSize = 16.sp,
                lineHeight = 19.sp,
                color = Color.Black.copy(alpha = 0.82f),
                maxLines = 1
            )
        }
        Column(
            modifier = Modifier.width(105.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = card.metric,
                fontSize = 42.sp,
                lineHeight = 44.sp,
                fontWeight = FontWeight.Normal,
                color = Color.Black
            )
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { card.progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(CircleShape),
                color = Color.Black,
                trackColor = Color.White.copy(alpha = 0.58f)
            )
        }
        Spacer(Modifier.width(10.dp))
        Surface(
            shape = CircleShape,
            color = Color.Transparent,
            border = BorderStroke(1.5.dp, Color.Black),
            modifier = Modifier.size(34.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowLeft,
                    contentDescription = "פתיחת ${card.title}",
                    tint = Color.Black,
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
        color = Color(0xFFF2F2F2),
        shadowElevation = 8.dp,
        modifier = modifier
            .width(260.dp)
            .height(60.dp)
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
                    .fillMaxHeight()
                    .padding(4.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(if (selectedTab == HomeTab.PRAYERS) Color.White else Color.Transparent)
                    .clickable { onSelectTab(HomeTab.PRAYERS) },
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.NightlightRound,
                    contentDescription = null,
                    tint = if (selectedTab == HomeTab.PRAYERS) Color.Black else Color(0xFF9A9A9A),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "תפילות",
                    color = if (selectedTab == HomeTab.PRAYERS) Color.Black else Color(0xFF9A9A9A),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Divider
            Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color(0xFFD0D0D0)))

            // Study Tab (Right)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(4.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(if (selectedTab == HomeTab.STUDY) Color.White else Color.Transparent)
                    .clickable { onSelectTab(HomeTab.STUDY) },
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.MenuBook,
                    contentDescription = null,
                    tint = if (selectedTab == HomeTab.STUDY) Color.Black else Color(0xFF9A9A9A),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "לימוד",
                    color = if (selectedTab == HomeTab.STUDY) Color.Black else Color(0xFF9A9A9A),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
