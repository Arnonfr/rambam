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
import com.example.data.tanya.TanyaAvailableLesson
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
        ?.takeIf { it in setOf("nadav", "ashkenazi") } ?: "nadav") }
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
    val colors = playerColors(accent, dark)
    val teacherName = TanyaAvailableLessons.teacherName(teacher)
    // Choosing from a list plays at once; the clip is resolved first.
    var playWhenReady by remember { mutableStateOf(false) }
    LaunchedEffect(clip, playWhenReady) {
        val ready = clip
        if (playWhenReady && ready != null) { playWhenReady = false; player.play(ready) }
    }
    val browsing = if (teacher == "ashkenazi") browsingAshkenazi else browsingAvailable
    Column(Modifier.fillMaxWidth().testTag("tanya_audio_controls")) {
        if (browsing) {
            PlayerHeader(teacherName, if (teacher == "ashkenazi") "בחירת פרק" else "כל השיעורים", colors,
                onBack = { if (teacher == "ashkenazi") browsingAshkenazi = false else browsingAvailable = false },
                backLabel = "חזרה לנגן")
            if (teacher == "ashkenazi") AshkenaziLessonSelector(catalog, matchingChapter, ashkenaziId, colors,
                playingId = if (playback.active) ashkenaziId else null, playing = playback.playing) { id ->
                if (ashkenaziId == id) { if (clip != null) player.play(clip!!) } else {
                    player.stop(); ashkenaziId = id; playWhenReady = true
                }
            } else TanyaAvailableLessonPicker(available, availableId, lesson, colors,
                playingId = if (playback.active) availableId else null, playing = playback.playing) { record ->
                if (record.kind == "youtube") source(record.source)
                else if (availableId == record.id) { if (clip != null) player.play(clip!!) } else {
                    player.stop(); availableId = record.id; playWhenReady = true
                }
            }
            if (sourceError) Text("אין יישום זמין לפתיחת הקישור.", style = MaterialTheme.typography.bodySmall, color = colors.ink)
            return@Column
        }
        PlayerHeader("האזנה", "תניא · ${tanyaPlace(lesson)}", colors)
        PlayerSegmented(listOf("nadav", "ashkenazi").map { it to TanyaAvailableLessons.teacherName(it) }, teacher, colors,
            onSelect = { key ->
                if (teacher != key) player.stop()
                teacher = key; prefs.edit().putString("teacher", key).apply()
            }, modifier = Modifier.padding(bottom = 14.dp).testTag("tanya_teacher_picker"))
        val current = clip
        when {
            selected?.kind == "youtube" && teacher != "ashkenazi" ->
                PlayerLessonCard(colors, "מהשיעורים הזמינים", selected.title, "הצפייה נפתחת באתר המקור, מחוץ לאפליקציה") {
                    Spacer(Modifier.height(14.dp))
                    TanyaAudioActions("פתיחת השיעור באתר המקור", { source(selected.source) }, ink, accent)
                }
            current != null -> PlayerLessonCard(colors,
                eyebrow = when {
                    selected != null -> "מהשיעורים הזמינים"
                    teacher == "ashkenazi" -> matchingChapter?.let { "ליקוטי אמרים · פרק ${com.example.ui.util.HebrewNumberFormatter.toHebrewNumeral(it)}" } ?: "ליקוטי אמרים"
                    current.chapterLesson -> "שיעור על הפרק"
                    else -> "השיעור של היום · ${lesson.hebrewDate.substringBeforeLast(' ')}"
                },
                // Archive titles carry the day after a dash: "קונטרס אחרון - קטע רביעי יב כסלו".
                title = splitLessonTitle(current.title).first,
                subtitle = listOf(splitLessonTitle(current.title).second, teacherName).filter { it.isNotBlank() }.joinToString(" · "),
                corner = { PlayerSpeedChip(playback.speed, player::speed, colors, Modifier.testTag("tanya_audio_speed")) }) {
                TanyaPlaybackControls(playback, onPlay = { player.play(current) }, onSeek = player::seek,
                    ink = colors.onLesson, paper = Color.White, accent = colors.lesson)
                playback.error?.let {
                    Text(it, color = colors.onLesson, fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp))
                    Spacer(Modifier.height(10.dp))
                    TanyaAudioActions("רענון ההקלטה", { player.stop(); clip = null; retry++ }, ink, accent,
                        "למקור השיעור", { source(sourceUrl) })
                }
            }
            loading -> PlayerLessonCard(colors, "", "מאתר הקלטה מתאימה…")
            else -> {
                val noChapter = teacher == "ashkenazi" && ashkenaziId == null
                val failedRecording = selected != null || (teacher == "ashkenazi" && ashkenaziId != null)
                TanyaAudioEmptyState(ink, paper, accent,
                    primaryLabel = if (noChapter) "בחירת פרק אחר" else if (failedRecording || teacher == "nadav") "ניסיון נוסף" else "השיעורים הזמינים",
                    onPrimary = {
                        if (failedRecording || (teacher == "nadav" && !noChapter)) retry++
                        else if (teacher == "ashkenazi") browsingAshkenazi = true else browsingAvailable = true
                    },
                    title = when {
                        failedRecording -> "ההקלטה אינה זמינה כרגע"
                        teacher == "nadav" -> "לא נמצאה הקלטה ליום הזה"
                        else -> "אין הקלטה לפרק הזה"
                    },
                    detail = when {
                        noChapter -> "הסדרה של הרב שניאור אשכנזי היא על ליקוטי אמרים. אפשר לשמוע פרק אחר."
                        failedRecording -> "אפשר לנסות שוב או לפתוח את השיעור במקור."
                        else -> "אפשר לנסות שוב, או לבחור מהשיעורים הזמינים למטה."
                    },
                    onSource = { source(sourceUrl) })
            }
        }
        if (teacher == "ashkenazi" && current != null) {
            Spacer(Modifier.height(8.dp))
            PlayerSecondaryButton("ניגון השיעור באתר הרב", { source(sourceUrl) }, colors, Modifier.fillMaxWidth(),
                icon = Icons.Default.OpenInNew, container = colors.raised, content = colors.ink)
        }
        // The list continues below the card; the whole list is one step away, not a nested scroll.
        // Without a recording the card above already offers the chapter choice.
        if (teacher == "ashkenazi") {
            if (current != null) PlayerSectionHeader("ליקוטי אמרים · פרקים", colors, "בחירת פרק", { browsingAshkenazi = true }, "ashkenazi_browse")
        } else if (available.isNotEmpty()) {
            PlayerSectionHeader("שיעורים אחרים", colors, "כל ${available.size} השיעורים", { browsingAvailable = true },
                "tanya_browse_available")
            sortedLessons(available, lesson).filter { it.id != availableId }.take(2).forEach { record ->
                AvailableLessonRow(record, lesson, colors, playing = false, current = false) {
                    if (record.kind == "youtube") source(record.source) else {
                        player.stop(); availableId = record.id; playWhenReady = true
                    }
                }
            }
        }
        if (sourceError) Text("אין יישום זמין לפתיחת הקישור.", style = MaterialTheme.typography.bodySmall, color = colors.ink)
    }
}

@Composable
private fun AshkenaziLessonSelector(catalog: List<AshkenaziTanyaLesson>, matchingChapter: Int?, selectedId: String?,
    colors: PlayerColors, playingId: String?, playing: Boolean, onSelect: (String) -> Unit) {
    var chapter by rememberSaveable(matchingChapter) { mutableIntStateOf(matchingChapter ?: 1) }
    val chapters = remember(catalog) { listOf(0) + catalog.flatMap { it.chapters }.distinct().sorted() }
    PlayerChips(chapters.map { it to if (it == 0) "מבוא" else com.example.ui.util.HebrewNumberFormatter.toHebrewNumeral(it) },
        chapter, colors, { chapter = it }, "ashkenazi_chapter_")
    catalog.filter { if (chapter == 0) it.chapters.isEmpty() else chapter in it.chapters }.forEach { record ->
        val active = record.id == playingId
        PlayerLessonRow(record.title.substringAfter(": ", record.title), record.subtitle, colors,
            if (active && playing) LessonRowAction.PAUSE else LessonRowAction.PLAY, { onSelect(record.id) },
            Modifier.testTag("ashkenazi_lesson_${record.order}"), highlighted = active, selected = record.id == selectedId)
    }
}

/** "תניא, קונטרס אחרון 4" → "קונטרס אחרון ד׳": no repeated book name, Hebrew numerals. */
internal fun tanyaPlace(lesson: DailyTanyaLesson): String {
    val place = lesson.heRef.ifBlank { lesson.chapterTitle }.removePrefix("תניא, ").removePrefix("תניא ")
    // Only a plain trailing chapter number; "12:3" style references stay as they are.
    return place.replace(Regex(" (\\d+)$")) {
        " " + com.example.ui.util.HebrewNumberFormatter.toHebrewNumeral(it.groupValues[1].toInt(), withGershayim = true)
    }
}
