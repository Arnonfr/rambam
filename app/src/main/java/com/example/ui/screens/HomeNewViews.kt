package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.semantics.Role
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
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
    playback: com.example.data.prayers.audio.StudyPlaybackState? = null,
    onOpenCompletion: (com.example.data.local.SavedStudyCompletion) -> Unit = {},
    onRemoveCompletion: (String) -> Unit = {},
    personalPsalmCount: Int = 0,
    onOpenPersonalPsalms: () -> Unit = {},
    uiState: DailyRambamUiState,
    onOpenChapter: (String) -> Unit,
    onOpenDailyLesson: () -> Unit,
    onResumeReading: () -> Unit,
    onOpenChumash: () -> Unit,
    onOpenTehillim: () -> Unit,
    onOpenTanya: () -> Unit,
    onOpenMitzvah: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHayomYom: () -> Unit = {},
    prayerSections: List<PrayerSection> = emptyList(),
    onOpenPrayer: (String) -> Unit = {},
    onGetRambamLesson: (String) -> DailyLessonResult? = { uiState.dailyLesson },
    onOpenRambamTrack: (String) -> Unit = { onOpenDailyLesson() },
    onResumeStudy: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val servicePlayback by com.example.data.prayers.audio.StudyPlaybackStatus.state.collectAsState()
    val activePlayback = playback ?: servicePlayback
    if (uiState.selectedTab in setOf(HomeTab.PODCASTS, HomeTab.PRAYERS)) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("בבניה", fontSize = 22.sp, color = Color.Black)
        }
        return
    }
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

    val tehillimDate = remember(uiState.dailyLesson) {
        val hebDate = uiState.dailyLesson?.hebrewDate
        if (!hebDate.isNullOrBlank()) {
            val dayNum = HebrewDateHelper.extractHebrewDayNumeral(hebDate)
            dayNum to HebrewDateHelper.extractHebrewMonth(hebDate)
        } else {
            null
        }
    }
    val tehillimSubtitle = tehillimDate?.let { "יום ${it.first} בחודש" } ?: "טוען..."

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
        modifier = modifier.fillMaxWidth().background(homeStudyBackground(uiState.preferences.visibleStudies)),
        verticalArrangement = Arrangement.spacedBy(0.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        if (uiState.selectedTab == HomeTab.STUDY) {
            item(key = "study_preferences_header") {
                Row(
                    modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "השיעורים שלי",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF575757)
                    )
                    TextButton(
                        onClick = onOpenSettings,
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF686868)),
                        modifier = Modifier.testTag("home_study_preferences")
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("העדפות", fontSize = 12.sp, fontWeight = FontWeight.Normal)
                    }
                }
            }
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
                                progress = 0f,
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
                                progress = 0f,
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
                    if (visibleStudies.contains("hayom_yom")) {
                        val lesson = uiState.dailyHayomYom
                        add(StudyDashboardCard(
                            track = "hayom_yom", eyebrow = "פתגם יומי", title = "היום יום",
                            subtitle = "תורת הרבי · לפי ימי השנה",
                            metric = lesson?.hebrewDate?.replaceFirst(" ", "\n") ?: "–",
                            progress = if (lesson?.isCompleted == true) 1f else 0f,
                            color = Color(0xFFC5B8F5), icon = Icons.Outlined.MenuBook,
                            onClick = onOpenHayomYom))
                    }
                    if (visibleStudies.contains("tehillim")) {
                        add(
                            StudyDashboardCard(
                                track = "tehillim",
                                eyebrow = "תהילים יומי",
                                title = "תהילים",
                                subtitle = tehillimSubtitle,
                                metric = tehillimDate?.let { "${it.first}\n${it.second}" } ?: "–",
                                progress = 0f,
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
                    playing = activePlayback.playing && activePlayback.track == card.track,
                    completed = uiState.completedStudyDates[card.track] == uiState.selectedStudyDate.toString() || card.progress >= 1f || (uiState.studyProgress[card.track] ?: 0f) >= 1f,
                    uiProgress = uiState.studyProgress[card.track],
                    onResume = { onResumeStudy(card.track) },
                    onOpenPersonalPsalms = onOpenPersonalPsalms.takeIf { card.track == "tehillim" && personalPsalmCount > 0 })
                val completions = uiState.savedCompletions.filter { it.track == card.track }
                if (completions.isNotEmpty()) SavedCompletionsStrip(card.color, completions, onOpenCompletion, onRemoveCompletion)
            }
        } else if (uiState.selectedTab == HomeTab.PODCASTS) {
            item { PodcastPanel() }
        } else {
            item {
                Text("נוסח האר״י · סידור תורה אור", fontSize = 16.sp, color = Color.Black,
                    modifier = Modifier.padding(20.dp))
                Text("מהדורת בדיקה — חלק מהתמלולים חלקיים. טרם הוגה מול תהלת השם.",
                    fontSize = 12.sp, color = Color.DarkGray, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
            }
            items(com.example.data.prayers.PrayerServices.group(prayerSections), key = { it.id }) { section ->
                ListItem(
                    headlineContent = { Text(section.title, fontWeight = FontWeight.Bold) },
                    supportingContent = { Text(section.sections.joinToString(" · ") { it.title }, maxLines = 2) },
                    colors = ListItemDefaults.colors(containerColor = Color(0xFFB3E9F2)),
                    modifier = Modifier.clickable { onOpenPrayer(section.id) }.padding(bottom = 2.dp)
                )
            }
        }
    }
}

internal fun homeStudyBackground(visibleStudies: Set<String>): Color = when {
    "tehillim" in visibleStudies -> Color(0xFF9FDEEF)
    "hayom_yom" in visibleStudies -> Color(0xFFC5B8F5)
    "tanya" in visibleStudies -> Color(0xFFF0A0DD)
    "chumash" in visibleStudies -> Color(0xFFF4F66A)
    "mitzvot" in visibleStudies -> Color(0xFF72D7E8)
    visibleStudies.any { it == "rambam_one" || it == "rambam_three" } -> Color(0xFFFF873F)
    else -> Color.White
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
    completed: Boolean,
    playing: Boolean = false,
    uiProgress: Float? = null,
    onResume: () -> Unit,
    onOpenPersonalPsalms: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val resumeToday = savedDate == selectedDate
    Column(modifier.fillMaxWidth().height(148.dp).background(card.color)
        .padding(horizontal = 18.dp, vertical = 12.dp)) {
    Column(
        modifier = Modifier.fillMaxWidth().weight(1f)
            .clickable(onClick = if (resumeToday) onResume else card.onClick),
        verticalArrangement = Arrangement.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StudyLineIcon(card.track, Modifier.size(36.dp).testTag("study_icon_${card.track}"))
            Spacer(Modifier.width(12.dp))
            Text(card.title, fontSize = 25.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold,
                color = Color.Black, modifier = Modifier.testTag("study_title_${card.track}"))
            if (playing) {
                Spacer(Modifier.width(14.dp))
                com.example.ui.components.PlaybackWaveform(Color.Black,
                    Modifier.size(24.dp).testTag("study_audio_${card.track}").semantics {
                        contentDescription = "שיעור מתנגן"
                    })
            }
        }
        Text(card.subtitle, fontSize = 14.sp, lineHeight = 18.sp, color = Color.Black.copy(alpha = .82f),
            maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 48.dp))
    }
    Row(Modifier.fillMaxWidth().height(40.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f).padding(end = 16.dp)) {
            LinearProgressIndicator(
                progress = { if (completed) 1f else (uiProgress ?: card.progress).coerceIn(0f, .99f) },
                modifier = Modifier
                    .widthIn(max = 140.dp).fillMaxWidth()
                    .height(5.dp)
                    .testTag("study_progress_${card.track}")
                    .clip(CircleShape),
                color = Color.Black,
                trackColor = Color.White.copy(alpha = 0.58f)
            )
        }
        if (completed) {
            Icon(Icons.Default.Check, contentDescription = "השיעור הושלם", tint = Color.Black,
                modifier = Modifier.size(22.dp).testTag("completed_${card.track}"))
            Spacer(Modifier.width(8.dp))
        }
        Row(
            modifier = Modifier.height(40.dp).clip(RoundedCornerShape(20.dp))
                .border(1.5.dp, Color.Black, RoundedCornerShape(20.dp))
                .testTag("study_actions_${card.track}"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            onOpenPersonalPsalms?.let { open ->
                Box(Modifier.fillMaxHeight().clickable(role = Role.Button, onClick = open)
                    .testTag("home_personal_psalms").padding(horizontal = 10.dp), contentAlignment = Alignment.Center) {
                    Text("הפרקים שלי", fontSize = 12.sp, color = Color.Black, maxLines = 1)
                }
                VerticalDivider(Modifier.fillMaxHeight().width(1.dp), color = Color.Black)
            }
            if (savedDate != null) {
                Box(
                    modifier = Modifier.fillMaxHeight().clickable(role = Role.Button, onClick = onResume)
                        .testTag("resume_${card.track}").padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("מיקום אחרון", fontSize = 12.sp, color = Color.Black, maxLines = 1)
                }
                VerticalDivider(Modifier.fillMaxHeight().width(1.dp), color = Color.Black)
            }
            Row(
                modifier = Modifier.fillMaxHeight().clickable(role = Role.Button, onClick = card.onClick)
                    .testTag("open_${card.track}").padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("השיעור היומי", fontSize = 12.sp, color = Color.Black, maxLines = 1)
                Icon(Icons.Default.KeyboardArrowLeft, contentDescription = null,
                    tint = Color.Black, modifier = Modifier.size(16.dp))
            }
        }
    }
    }
}

@Composable
fun StudyCompletionHint(modifier: Modifier = Modifier) {
    Row(modifier.padding(top = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black.copy(alpha = .72f),
            modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(4.dp))
        Text("נלמד", fontSize = 11.sp, color = Color.Black.copy(alpha = .72f))
    }
}

@Composable
fun FloatingDarkNavBar(
    selectedTab: HomeTab,
    onSelectTab: (HomeTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val constructionToast = remember(context) { Toast.makeText(context, "בבניה", Toast.LENGTH_SHORT) }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Surface(
            modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)
                .height(58.dp).testTag("home_bottom_menu"),
            shape = RoundedCornerShape(28.dp), color = Color(0xFFF2F2F2), shadowElevation = 3.dp
        ) {
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                listOf(HomeTab.STUDY to "שיעורים", HomeTab.PRAYERS to "תפילות", HomeTab.PODCASTS to "פודקאסטים").forEach { (tab, label) ->
                    val active = selectedTab == tab
                    Box(Modifier.weight(1f).fillMaxHeight().padding(4.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(if (active) Color.White else Color.Transparent)
                        .clickable { if (tab == HomeTab.STUDY) onSelectTab(tab) else constructionToast.show() }
                        .testTag("home_tab_${tab.name}"), contentAlignment = Alignment.Center) {
                        Text(label,
                            textAlign = TextAlign.Center, fontSize = 14.sp, lineHeight = 16.sp,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                            color = if (active) Color.Black else Color(0xFF777777))
                    }
                }
            }
        }
    }
}
