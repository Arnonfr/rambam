@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.view.WindowManager
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.UserPreferences
import com.example.data.tehillim.PersonalTehillimStore
import com.example.data.tehillim.TehillimRepository
import com.example.domain.tehillim.PersonalTehillimEntry
import com.example.domain.tehillim.TehillimChapter
import com.example.ui.components.EndOfLessonPullIndicator
import com.example.ui.components.ReaderTypographySheet
import com.example.ui.components.StudyTextBlock
import com.example.ui.components.endOfLessonPull
import com.example.ui.components.rememberEndOfLessonPullState
import com.example.ui.util.HebrewTextNormalizer
import kotlinx.coroutines.flow.distinctUntilChanged
import java.security.MessageDigest
import java.time.LocalDate

private data class PersonalReading(val entry: PersonalTehillimEntry, val chapter: TehillimChapter, val headingIndex: Int)

/** One list and one local preload for the whole selection, including repeated chapters for different people. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PersonalPsalmSequenceReader(
    entries: List<PersonalTehillimEntry>, date: LocalDate, preferences: UserPreferences, completed: Set<String>,
    background: Color, foreground: Color, onBack: () -> Unit, onManage: () -> Unit,
    onComplete: (PersonalTehillimEntry) -> Unit,
    onFontSizeChange: (Float) -> Unit, onLineSpacingChange: (Float) -> Unit, onFontFamilyChange: (String) -> Unit,
    onNikudToggle: (Boolean) -> Unit, onThemeChange: (String) -> Unit, onKeepScreenOnChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val repository = remember(context) { TehillimRepository(context.applicationContext) }
    val selection = entries.mapNotNull { entry -> entry.chapter(date)?.let { entry to it } }
    val sequenceKey = remember(selection, date) {
        val digest = MessageDigest.getInstance("SHA-256").digest(selection.joinToString("|") { "${it.first.id}:${it.second}" }.toByteArray())
            .joinToString("") { "%02x".format(it) }
        "$date:$digest"
    }
    var failed by remember(sequenceKey) { mutableStateOf(false) }
    val readings by produceState<List<PersonalReading>?>(null, sequenceKey) {
        try {
            val chapters = selection.map { it.second }.distinct().associateWith { repository.getChapter(it) }
            var heading = 0
            value = selection.map { (entry, number) ->
                PersonalReading(entry, chapters.getValue(number), heading).also { heading += 1 + it.chapter.verses.size }
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            failed = true
        }
    }
    val saved = remember { context.getSharedPreferences("personal_psalm_positions", Context.MODE_PRIVATE) }
    val positionKey = "sequence:$sequenceKey"
    val list = rememberLazyListState()
    var restored by remember(sequenceKey) { mutableStateOf(false) }
    var typography by remember { mutableStateOf(false) }
    val marked = remember(sequenceKey) { mutableSetOf<String>() }
    val latestComplete by rememberUpdatedState(onComplete)
    val latestCompleted by rememberUpdatedState(completed)
    fun mark(reading: PersonalReading) {
        val completionKey = PersonalTehillimStore.completionKey(reading.entry.id, date.toString(), reading.chapter.chapterNumber)
        if (marked.add(completionKey) && completionKey !in latestCompleted) latestComplete(reading.entry)
    }
    val completionPull = rememberEndOfLessonPullState(sequenceKey,
        canScrollForward = { readings == null || !restored || list.canScrollForward },
        onCompleted = {
            readings.orEmpty().forEach(::mark)
            saved.edit().remove("$positionKey:entry").remove("$positionKey:verse").apply()
            readings.orEmpty().forEach { saved.edit().remove("${it.entry.id}:${it.chapter.chapterNumber}:$date").apply() }
        }, onExitAfterCompletion = onBack)
    val activeReading by remember(readings) { derivedStateOf {
        readings?.lastOrNull { it.headingIndex <= list.firstVisibleItemIndex } ?: readings?.firstOrNull()
    } }

    LaunchedEffect(readings, sequenceKey) {
        val rows = readings ?: return@LaunchedEffect
        val savedEntry = saved.getString("$positionKey:entry", null)
        val previous = rows.find { it.entry.id == savedEntry }
        val legacy = rows.firstOrNull { saved.getInt("${it.entry.id}:${it.chapter.chapterNumber}:$date", 0) > 0 }
        val target = previous ?: legacy
        if (target != null) {
            val verse = if (previous != null) saved.getInt("$positionKey:verse", 0)
                else saved.getInt("${target.entry.id}:${target.chapter.chapterNumber}:$date", 0) + 1
            list.scrollToItem(target.headingIndex + verse.coerceIn(0, target.chapter.verses.size))
        }
        restored = true
    }
    LaunchedEffect(readings, restored, sequenceKey) {
        val rows = readings ?: return@LaunchedEffect
        if (!restored) return@LaunchedEffect
        snapshotFlow { list.firstVisibleItemIndex }.distinctUntilChanged().collect { index ->
            if (!completionPull.isConfirmed) {
                val current = rows.lastOrNull { it.headingIndex <= index } ?: return@collect
                saved.edit().putString("$positionKey:entry", current.entry.id)
                    .putInt("$positionKey:verse", index - current.headingIndex).apply()
                // Crossing the next heading completes preceding entries without stopping the scroll.
                rows.takeWhile { it.headingIndex < current.headingIndex }.forEach(::mark)
            }
        }
    }
    DisposableEffect(preferences.keepScreenOn) {
        val window = (context as? Activity)?.window
        if (preferences.keepScreenOn) window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    Column(Modifier.fillMaxSize().background(background).testTag("personal_psalm_reader")) {
        TopAppBar(title = { Column {
            Text(activeReading?.entry?.label?.ifBlank { "הפרקים שלי" } ?: "הפרקים שלי", color = foreground)
            activeReading?.let { Text(psalmName(it.chapter.chapterNumber), style = MaterialTheme.typography.labelMedium, color = foreground) }
        } }, navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "חזרה לפרקים שלי", tint = foreground) }
        }, actions = {
            IconButton(onClick = { typography = true }) { Icon(Icons.Default.TextFields, "העדפות קריאה", tint = foreground) }
            TextButton(onClick = onManage, modifier = Modifier.testTag("personal_reader_edit")) { Text("עריכה", color = foreground) }
        }, colors = TopAppBarDefaults.topAppBarColors(containerColor = background))
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (readings == null && !failed) CircularProgressIndicator(Modifier.align(Alignment.Center), color = foreground)
            if (failed) Text("לא ניתן לפתוח את הפרקים. חזרו לרשימה ונסו שוב.", Modifier.padding(20.dp), color = foreground)
            CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
                LazyColumn(state = list, modifier = Modifier.fillMaxSize().alpha(if (restored) 1f else 0f)
                    .endOfLessonPull(completionPull).testTag("personal_psalm_text"),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 110.dp)) {
                    readings.orEmpty().forEach { reading ->
                        val entry = reading.entry
                        val chapter = reading.chapter
                        item(key = "${entry.id}:heading") {
                            Row(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 20.dp).testTag("personal_heading_${entry.id}"),
                                verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    if (entry.label.isNotBlank()) Text(entry.label, color = foreground.copy(alpha = .65f), style = MaterialTheme.typography.labelMedium)
                                    Text(psalmName(chapter.chapterNumber), color = foreground, fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleLarge)
                                }
                                if (PersonalTehillimStore.completionKey(entry.id, date.toString(), chapter.chapterNumber) in completed)
                                    Icon(Icons.Default.Check, "הפרק הושלם", tint = foreground, modifier = Modifier.size(20.dp))
                            }
                        }
                        itemsIndexed(chapter.verses, key = { _, verse -> "${entry.id}:${verse.verseNumber}" }) { _, verse ->
                            StudyTextBlock(verse.verseHebrew, verse.textPlainNikud, HebrewTextNormalizer.stripMarks(verse.textPlainNikud),
                                preferences, textColor = foreground, modifier = Modifier.padding(bottom = 20.dp),
                                testTag = "personal_verse_${entry.id}_${verse.verseNumber}")
                        }
                    }
                }
            }
            EndOfLessonPullIndicator(completionPull, PsalmBlue,
                Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 24.dp))
        }
    }
    if (typography) ReaderTypographySheet(preferences, onDismiss = { typography = false },
        onFontSizeChange = onFontSizeChange, onLineSpacingChange = onLineSpacingChange, onFontFamilyChange = onFontFamilyChange,
        onNikudToggle = onNikudToggle, onThemeChange = onThemeChange, onKeepScreenOnChange = onKeepScreenOnChange)
}
