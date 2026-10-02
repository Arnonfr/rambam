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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.schedule.DailyLessonResult
import com.example.ui.DailyRambamUiState
import com.example.ui.HomeTab
import com.example.ui.util.HebrewDateHelper
import com.example.ui.util.HebrewNumberFormatter
import com.example.domain.mitzvot.MitzvahAssignmentType
import com.example.data.prayers.PrayerSection
import java.time.LocalDate
import com.example.ui.components.StudyLineIcon
import androidx.compose.ui.text.style.TextOverflow

@Composable
fun MainContentList(
    uiState: DailyRambamUiState,
    onOpenChapter: (String) -> Unit,
    onOpenDailyLesson: () -> Unit,
    onResumeReading: () -> Unit,
    onOpenChumash: () -> Unit,
    onOpenTehillim: () -> Unit,
    onOpenTanya: () -> Unit,
    onOpenMitzvah: () -> Unit,
    onOpenSettings: () -> Unit,
    prayerSections: List<PrayerSection> = emptyList(),
    onOpenPrayer: (String) -> Unit = {},
    onGetRambamLesson: (String) -> DailyLessonResult? = { uiState.dailyLesson },
    onOpenRambamTrack: (String) -> Unit = { onOpenDailyLesson() },
    onResumeStudy: (String) -> Unit = {},
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

    val mitzvahSubtitle = remember(uiState.dailyMitzvahAssignment) {
        val assignment = uiState.dailyMitzvahAssignment
        if (assignment == null) {
            "הלוח אינו זמין לתאריך זה"
        } else {
            val first = assignment.items.firstOrNull()
            val itemTitle = when (first?.type) {
                MitzvahAssignmentType.POSITIVE -> "מצוות עשה ${HebrewNumberFormatter.toHebrewNumeral(first.number ?: 0, true)}"
                MitzvahAssignmentType.NEGATIVE -> "מצוות לא תעשה ${HebrewNumberFormatter.toHebrewNumeral(first.number ?: 0, true)}"
                MitzvahAssignmentType.SPECIAL -> first.title.orEmpty()
                null -> ""
            }
            "שיעור ${assignment.lessonNumber} · $itemTitle"
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
                    listOf("one", "three").forEach { track ->
                    if (visibleStudies.contains("rambam_$track")) {
                        val lesson = onGetRambamLesson(track)
                        val total = lesson?.chapters?.size?.coerceAtLeast(1) ?: 1
                        val completed = if (track == uiState.preferences.selectedTrack) lesson?.chapters?.count {
                            uiState.completedChapterIds.contains("${it.sectionId}_${it.chapterNumber}")
                        } ?: 0 else 0
                        add(
                            StudyDashboardCard(
                                track = track,
                                eyebrow = if (track == "three") "שלושה פרקים" else "פרק יומי",
                                title = "רמב״ם",
                                subtitle = (if (track == "three") "ג׳ פרקים · " else "") +
                                    (lesson?.chapters?.joinToString(" · ") { "${it.sectionNameHebrew} ${it.chapterHebrew}" } ?: "טוען..."),
                                metric = lesson?.chapters
                                    ?.joinToString("–") { it.chapterHebrew.removePrefix("פרק ") }
                                    ?: "–",
                                progress = completed.toFloat() / total,
                                color = Color(0xFFFF873F),
                                icon = Icons.Outlined.LibraryBooks,
                                onClick = { onOpenRambamTrack(track) }
                            )
                        )
                    }
                    }
                    if (visibleStudies.contains("mitzvot")) {
                        val assignment = uiState.dailyMitzvahAssignment
                        val total = uiState.dailyMitzvahEntries.sumOf { entry ->
                            entry.paragraphs.size + 1
                        }.coerceAtLeast(1)
                        val current = uiState.savedMitzvahAnchor?.blockIndex?.plus(1) ?: 0
                        val metric = assignment?.items?.firstOrNull()?.let { item ->
                            when (item.type) {
                                MitzvahAssignmentType.POSITIVE,
                                MitzvahAssignmentType.NEGATIVE -> HebrewNumberFormatter.toHebrewNumeral(item.number ?: 0, true)
                                MitzvahAssignmentType.SPECIAL -> "–"
                            }
                        } ?: "–"
                        add(
                            StudyDashboardCard(
                                track = "mitzvot",
                                eyebrow = "רמב״ם יומי",
                                title = "ספר המצוות",
                                subtitle = mitzvahSubtitle,
                                metric = metric,
                                progress = current.toFloat() / total,
                                color = Color(0xFF72D7E8),
                                icon = Icons.Outlined.MenuBook,
                                onClick = onOpenMitzvah
                            )
                        )
                    }
                    if (visibleStudies.contains("chumash")) {
                        val total = uiState.dailyChumash?.allAliyot?.size?.coerceAtLeast(1) ?: 7
                        val current = uiState.dailyChumash?.currentAliyaIndex ?: 1
                        add(
                            StudyDashboardCard(
                                track = "chumash",
                                eyebrow = "חת״ת",
                                title = "חומש",
                                subtitle = chumashSubtitle,
                                metric = "$current/$total",
                                progress = current.toFloat() / total,
                                color = Color(0xFFF4F66A),
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
                                track = "tanya",
                                eyebrow = "חת״ת",
                                title = "תניא",
                                subtitle = "${uiState.dailyTanya?.bookTitle?.removePrefix("תניא, ") ?: "תניא"} · פרק $tanyaChapter",
                                metric = tanyaChapter,
                                progress = if (completed) 1f else 0f,
                                color = Color(0xFFF0A0DD),
                                icon = Icons.Outlined.MenuBook,
                                onClick = onOpenTanya
                            )
                        )
                    }
                    if (visibleStudies.contains("tehillim")) {
                        val day = uiState.dailyTehillim?.dayOfMonth ?: 1
                        add(
                            StudyDashboardCard(
                                track = "tehillim",
                                eyebrow = "תהילים יומי",
                                title = "תהילים",
                                subtitle = tehillimSubtitle,
                                metric = "${HebrewNumberFormatter.toHebrewNumeral(day, true)}\n${uiState.dailyLesson?.hebrewDate?.let(HebrewDateHelper::extractHebrewMonth) ?: "לחודש"}",
                                progress = day / 30f,
                                color = Color(0xFF9FDEEF),
                                icon = Icons.Outlined.MusicNote,
                                onClick = onOpenTehillim
                            )
                        )
                    }
                }

            items(cards) { card ->
                val savedDate = uiState.bookmarkDates[card.track]
                    ?.takeUnless { card.track in uiState.completedBookmarks }
                StudyStrip(card, savedDate, uiState.selectedStudyDate.toString(),
                    onResume = { onResumeStudy(card.track) })
            }
        } else {
            item {
                Text("נוסח האר״י · סידור תורה אור", fontSize = 16.sp, color = Color.Black,
                    modifier = Modifier.padding(20.dp))
                Text("מהדורת בדיקה — חלק מהתמלולים חלקיים. טרם הוגה מול תהלת השם.",
                    fontSize = 12.sp, color = Color.DarkGray, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
            }
            items(prayerSections, key = { it.id }) { section ->
                ListItem(
                    headlineContent = { Text(section.title, fontWeight = FontWeight.Bold) },
                    supportingContent = { Text("נוסח חב״ד · התאמת טקסט ושמירת מיקום") },
                    colors = ListItemDefaults.colors(containerColor = Color(0xFFB3E9F2)),
                    modifier = Modifier.clickable { onOpenPrayer(section.id) }.padding(bottom = 2.dp)
                )
            }
        }
    }
}

private data class StudyDashboardCard(
    val track: String,
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
    savedDate: String?,
    selectedDate: String,
    onResume: () -> Unit,
    modifier: Modifier = Modifier
) {
    val resumeToday = savedDate == selectedDate
    Column(modifier.fillMaxWidth().height(148.dp).background(card.color)
        .padding(horizontal = 18.dp, vertical = 12.dp)) {
    Row(
        modifier = Modifier.fillMaxWidth().weight(1f)
            .clickable(onClick = if (resumeToday) onResume else card.onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StudyLineIcon(card.track, Modifier.size(48.dp))
        Spacer(Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = card.title,
                fontSize = 25.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Text(
                text = card.subtitle,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                color = Color.Black.copy(alpha = 0.82f),
                maxLines = 2, overflow = TextOverflow.Ellipsis
            )
        }
        Column(
            modifier = Modifier.width(76.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            card.metric.split('\n').forEach { metricLine ->
            Text(
                text = metricLine,
                fontSize = if (card.track == "tehillim") 19.sp else 28.sp,
                lineHeight = 25.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Normal,
                color = Color.Black
            )
            }
        }
    }
    Row(Modifier.fillMaxWidth().height(40.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f).padding(end = 16.dp)) {
            LinearProgressIndicator(
                progress = { card.progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .widthIn(max = 140.dp).fillMaxWidth()
                    .height(5.dp)
                    .clip(CircleShape),
                color = Color.Black,
                trackColor = Color.White.copy(alpha = 0.58f)
            )
        }
        if (savedDate != null) {
            Surface(onClick = onResume, color = Color(card.color.red * 0.32f,
                card.color.green * 0.32f, card.color.blue * 0.32f),
                shape = RoundedCornerShape(20.dp), modifier = Modifier.testTag("resume_${card.track}")) {
                Text("מיקום אחרון", fontSize = 12.sp, color = Color.White,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp))
            }
            Spacer(Modifier.width(8.dp))
        }
        if (!resumeToday) {
        Surface(
            onClick = card.onClick,
            shape = CircleShape,
            color = Color.Transparent,
            border = BorderStroke(1.5.dp, Color.Black),
            modifier = Modifier.size(38.dp).testTag("open_${card.track}")
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
    }
}

@Composable
fun FloatingDarkNavBar(
    selectedTab: HomeTab,
    onSelectTab: (HomeTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = Color(0xFFF2F2F2),
            shadowElevation = 8.dp,
            modifier = Modifier
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
}
