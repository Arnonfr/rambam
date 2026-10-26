package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.tanya.*
import com.example.domain.tanya.DailyTanyaLesson

@Composable
fun TanyaAudioControls(lesson: DailyTanyaLesson, selectedDate: String, player: TanyaAudioPlayer) {
    val context = LocalContext.current
    val dark = MaterialTheme.colorScheme.background.luminance() < .3f
    val paper = if (dark) Color(0xFF1E222A) else Color.White
    val ink = if (dark) Color(0xFFF4F5F5) else Color(0xFF171714)
    val accent = Color(0xFFF0A0DD)
    val prefs = remember { context.applicationContext.getSharedPreferences("tanya_audio_preferences", android.content.Context.MODE_PRIVATE) }
    var teacher by remember { mutableStateOf(prefs.getString("teacher", "nadav")
        ?.takeIf { it in setOf("nadav", "ashkenazi", "wilhelm", "teib") } ?: "nadav") }
    val ashkenazi = remember(context) { AshkenaziTanyaRepository.get(context) }
    val catalog = remember(ashkenazi) { ashkenazi.lessons }
    val repository = remember(context) { TanyaAvailableLessons.get(context) }
    var available by remember(teacher) { mutableStateOf(repository.cached(teacher.takeUnless { it == "ashkenazi" } ?: "nadav")) }
    val matchingChapter = AshkenaziTanyaRepository.matchingChapter(lesson)
    var ashkenaziId by rememberSaveable(lesson.fullRef) {
        mutableStateOf(catalog.firstOrNull { matchingChapter in it.chapters }?.id)
    }
    var availableId by rememberSaveable(lesson.fullRef, teacher) {
        mutableStateOf(available.firstOrNull { it.matches(lesson) }?.id)
    }
    val selected = available.firstOrNull { it.id == availableId }
    var browsingAvailable by rememberSaveable(lesson.fullRef, teacher) { mutableStateOf(false) }
    var browsingAshkenazi by rememberSaveable(lesson.fullRef) { mutableStateOf(false) }
    var clip by remember(lesson.date, lesson.fullRef, teacher, availableId, ashkenaziId) { mutableStateOf<TanyaAudioClip?>(null) }
    var loading by remember(lesson.date, teacher, availableId, ashkenaziId) { mutableStateOf(true) }
    var retry by remember { mutableIntStateOf(0) }
    var sourceError by remember { mutableStateOf(false) }
    val playback by player.state.collectAsState()
    LaunchedEffect(teacher, browsingAvailable) {
        if (teacher != "ashkenazi" && browsingAvailable) available = repository.refresh(teacher)
    }
    LaunchedEffect(lesson.date, lesson.fullRef, selectedDate, teacher, ashkenaziId, availableId, retry) {
        clip = null
        loading = true
        clip = when {
            teacher == "ashkenazi" -> ashkenaziId?.let { ashkenazi.resolve(it, refresh = retry > 0) }
            selected != null -> repository.resolve(selected)
            teacher == "nadav" -> TanyaAudioRepository.get(context).resolve(lesson, selectedDate, refresh = retry > 0)
            else -> null
        }
        loading = false
    }
    fun source(url: String) {
        player.stop()
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }.onFailure { sourceError = true }
    }
    val record = catalog.firstOrNull { it.id == ashkenaziId }
    val sourceUrl = when (teacher) {
        "ashkenazi" -> record?.website ?: AshkenaziTanyaRepository.SOURCE
        "wilhelm" -> selected?.source ?: "https://rbaruchwilhelm.captivate.fm"
        "teib" -> selected?.source ?: "https://www.youtube.com/@YAHADUTTV"
        else -> selected?.source ?: "https://tanyayomi.com/yy/ClientTania/Home"
    }
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).testTag("tanya_audio_controls")
        .border(1.dp, ink.copy(alpha = .15f), RoundedCornerShape(20.dp)), shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = paper, contentColor = ink)) {
        Column(Modifier.padding(12.dp)) {
            Text("האזנה לשיעור תניא", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                .background(ink.copy(alpha = .04f)).border(1.dp, ink.copy(alpha = .15f), RoundedCornerShape(16.dp))
                .horizontalScroll(rememberScrollState()).padding(4.dp).selectableGroup()
                .testTag("tanya_teacher_picker")) {
                listOf("nadav", "ashkenazi", "wilhelm", "teib").forEach { key ->
                    val active = teacher == key
                    Box(Modifier.widthIn(min = 124.dp).heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp))
                        .background(if (active) accent else Color.Transparent)
                        .selectable(active, role = Role.RadioButton, onClick = {
                            if (teacher != key) player.stop()
                            teacher = key; prefs.edit().putString("teacher", key).apply()
                        }).padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                        Text(TanyaAvailableLessons.teacherName(key), modifier = Modifier.padding(vertical = 6.dp),
                            fontSize = 12.sp, lineHeight = 16.sp, textAlign = TextAlign.Center,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                            color = if (active) Color(0xFF171714) else ink)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            if (teacher == "ashkenazi") {
                AshkenaziLessonSelector(catalog, matchingChapter, ashkenaziId, ink, accent,
                    browsingAshkenazi, { browsingAshkenazi = it }) { id ->
                    if (ashkenaziId != id) player.stop()
                    ashkenaziId = id
                }
            } else {
                if (selected != null || clip != null || browsingAvailable) TextButton(
                    onClick = { browsingAvailable = !browsingAvailable },
                    modifier = Modifier.align(Alignment.CenterHorizontally).testTag("tanya_browse_available"),
                    colors = ButtonDefaults.textButtonColors(contentColor = ink)) {
                    Text(if (browsingAvailable) "סגירת רשימת השיעורים" else "בחירת שיעור אחר")
                }
                if (browsingAvailable) TanyaAvailableLessonPicker(available, availableId, lesson, ink, accent) {
                    player.stop()
                    availableId = it.id
                }
            }
            when {
                selected?.kind == "youtube" && teacher != "ashkenazi" -> {
                    Text(selected.title, style = MaterialTheme.typography.bodySmall)
                    Text("הצפייה בשיעור נפתחת באתר המקור, מחוץ לאפליקציה.",
                        style = MaterialTheme.typography.bodySmall, color = ink.copy(alpha = .75f),
                        modifier = Modifier.padding(vertical = 8.dp))
                    TanyaAudioActions("פתיחת השיעור באתר המקור", { source(selected.source) }, ink, accent)
                }
                clip != null -> {
                    val current = clip!!
                    Text(when {
                        selected != null -> "מהשיעורים הזמינים · ${current.title}"
                        teacher == "ashkenazi" -> current.title
                        current.chapterLesson -> "שיעור על הפרק · ${current.title}"
                        else -> "השיעור היומי · ${current.title}"
                    }, style = MaterialTheme.typography.bodySmall)
                    TanyaPlaybackControls(playback, onPlay = { player.play(current) }, onSeek = player::seek,
                        onSpeed = player::speed, ink = ink, paper = paper, accent = accent)
                    playback.error?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        TanyaAudioActions("רענון ההקלטה", { player.stop(); clip = null; retry++ }, ink, accent,
                            "למקור השיעור", { source(sourceUrl) })
                    }
                    if (teacher == "ashkenazi") TanyaAudioActions("ניגון השיעור באתר הרב",
                        { source(sourceUrl) }, ink, accent)
                }
                loading -> Text("מאתר הקלטה מתאימה…", style = MaterialTheme.typography.bodySmall)
                (browsingAvailable && selected == null) || (browsingAshkenazi && ashkenaziId == null && teacher == "ashkenazi") ->
                    Text("בחרו שיעור מהרשימה להאזנה", style = MaterialTheme.typography.bodySmall)
                else -> {
                    val noChapter = teacher == "ashkenazi" && ashkenaziId == null
                    val failedRecording = selected != null || (teacher == "ashkenazi" && ashkenaziId != null)
                    TanyaAudioEmptyState(ink, paper, accent,
                        primaryLabel = if (noChapter) "בחירת פרק אחר" else "השיעורים הזמינים",
                        onPrimary = { if (teacher == "ashkenazi") browsingAshkenazi = true else browsingAvailable = true },
                        title = when {
                            failedRecording -> "ההקלטה אינה זמינה כרגע"
                            teacher == "nadav" -> "לא נמצאה הקלטה תואמת ליום הזה"
                            else -> "עדיין אין הקלטה לפרק הזה"
                        },
                        detail = when {
                            noChapter -> "הסדרה של הרב שניאור אשכנזי כוללת ליקוטי אמרים; הלימוד הנוכחי בחלק אחר"
                            failedRecording -> "אפשר לנסות שוב או לפתוח את השיעור במקור"
                            else -> "לא נמצאה התאמה אצל ${TanyaAvailableLessons.teacherName(teacher)}; אפשר לבחור רב אחר"
                        },
                        onSource = { source(sourceUrl) })
                    if (failedRecording || teacher == "nadav") TextButton(onClick = { retry++ },
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        colors = ButtonDefaults.textButtonColors(contentColor = ink)) { Text("ניסיון נוסף") }
                }
            }
            if (sourceError) Text("אין יישום זמין לפתיחת הקישור.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun AshkenaziLessonSelector(catalog: List<AshkenaziTanyaLesson>, matchingChapter: Int?, selectedId: String?,
    ink: Color, accent: Color, browsing: Boolean, onBrowsing: (Boolean) -> Unit, onSelect: (String) -> Unit) {
    var chapter by rememberSaveable(matchingChapter) { mutableIntStateOf(matchingChapter ?: 1) }
    val chapters = remember(catalog) { listOf(0) + catalog.flatMap { it.chapters }.distinct().sorted() }
    if (matchingChapter != null) Text("ליקוטי אמרים · פרק ${com.example.ui.util.HebrewNumberFormatter.toHebrewNumeral(matchingChapter)}",
        style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
    if (matchingChapter != null || browsing) TextButton(onClick = { onBrowsing(!browsing) }, modifier = Modifier.testTag("ashkenazi_browse")) {
        Text(if (browsing) "סגירת בחירת הפרק" else "בחירת פרק אחר", color = ink)
    }
    if (browsing) {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            chapters.forEach { number ->
                Box(Modifier.clip(RoundedCornerShape(12.dp))
                    .background(if (chapter == number) accent else ink.copy(alpha = .05f))
                    .selectable(chapter == number, role = Role.RadioButton, onClick = { chapter = number })
                    .padding(horizontal = 14.dp, vertical = 10.dp).testTag("ashkenazi_chapter_$number")) {
                    Text(if (number == 0) "מבוא" else com.example.ui.util.HebrewNumberFormatter.toHebrewNumeral(number), color = ink)
                }
            }
        }
    }
    if (matchingChapter != null || browsing) {
        catalog.filter { if (chapter == 0) it.chapters.isEmpty() else chapter in it.chapters }.forEach { record ->
            val selected = record.id == selectedId
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(RoundedCornerShape(12.dp))
                .background(if (selected) accent.copy(alpha = .25f) else ink.copy(alpha = .035f))
                .selectable(selected, role = Role.RadioButton, onClick = { onSelect(record.id) })
                .padding(10.dp).testTag("ashkenazi_lesson_${record.order}"), verticalAlignment = Alignment.CenterVertically) {
                Icon(if (selected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null, tint = ink, modifier = Modifier.size(18.dp))
                Column(Modifier.weight(1f).padding(start = 8.dp)) {
                    Text(record.title, fontSize = 13.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, color = ink)
                    Text(record.subtitle, fontSize = 11.sp, color = ink.copy(alpha = .7f))
                }
            }
        }
    }
}
