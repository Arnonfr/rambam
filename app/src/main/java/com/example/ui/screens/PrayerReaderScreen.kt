package com.example.ui.screens

import android.content.Intent
import android.app.Activity
import android.view.WindowManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.local.ContentReadingAnchor
import com.example.data.local.ContentReadingAnchorManager
import com.example.data.local.UserPreferences
import com.example.data.prayers.PrayerSection
import com.example.data.prayers.PrayerService
import com.example.data.prayers.PrayerDayRules
import com.example.data.prayers.audio.*
import com.example.BuildConfig
import java.time.LocalDate
import kotlinx.coroutines.launch
import com.example.ui.components.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerReaderScreen(
    service: PrayerService,
    preferences: UserPreferences,
    onBack: () -> Unit,
    onNextPrayer: (() -> Unit)?,
    onPrevPrayer: (() -> Unit)?,
    onFontSizeChange: (Float) -> Unit,
    onLineSpacingChange: (Float) -> Unit,
    onFontFamilyChange: (String) -> Unit,
    onNikudToggle: (Boolean) -> Unit,
    onThemeChange: (String) -> Unit,
    onKeepScreenOnChange: (Boolean) -> Unit,
    date: LocalDate = LocalDate.now()
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val blocks = remember(service) { service.sections.flatMap { part ->
        part.paragraphs.mapIndexed { index, text -> Triple(part, index, text) }
    } }
    val section = service.sections.first()
    val scope = rememberCoroutineScope()
    var sectionPicker by remember { mutableStateOf(false) }
    var audioPicker by remember { mutableStateOf(false) }
    val audio = remember(service.id, date) { PrayerAudioPlayer(context, service.id, date, BuildConfig.DEBUG) }
    val audioState by audio.state.collectAsState()
    // Weekday Shacharit conditions for the date; the reader text itself is not filtered by them.
    val prayerDay = remember(date) { PrayerDayRules.of(date) }
    val audioCatalog by produceState(Pair(emptyMap<String, PrayerAudioClip>(), emptyList<PrayerAudioOrderStep>()), date) {
        value = withContext(Dispatchers.IO) {
            Pair(runCatching { PrayerAudioCatalog.load(context, date.dayOfWeek, BuildConfig.DEBUG) }.getOrDefault(emptyMap()),
                runCatching { PrayerAudioOrder.load(context) }.getOrDefault(emptyList()))
        }
    }
    val audioSteps = remember(service, audioCatalog, prayerDay) {
        if (prayerDay.outOfScope != null || service.id != "shacharit") emptyList()
        else PrayerAudioPlan.forService(service, audioCatalog.first, audioCatalog.second, prayerDay.flags(minyan = false))
    }
    var audioNotice by remember { mutableStateOf<String?>(null) }
    DisposableEffect(audio) { onDispose { audio.close() } }
    val density = LocalDensity.current
    var headerHeight by remember { mutableStateOf(78.dp) }
    val manager = remember { ContentReadingAnchorManager(context) }
    DisposableEffect(preferences.keepScreenOn) {
        val window = (context as? Activity)?.window
        if (preferences.keepScreenOn) window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
    // Versioned cleaned paragraphs must not reuse indices from the old raw import.
    val contentId = "prayer_service_${service.id}"
    val studyDate = "prayer_service_v1"
    val entryAnchor = remember(contentId, studyDate) {
        manager.get(contentId, studyDate) ?: service.sections.mapNotNull { part ->
            manager.get("prayer_clean_${part.id}", part.bookmarkKey)?.let { old ->
                val start = blocks.indexOfFirst { it.first.id == part.id }
                old.copy(contentId = contentId, assignmentDate = studyDate,
                    blockIndex = start + old.blockIndex.coerceIn(0, part.paragraphs.lastIndex))
            }
        }.maxByOrNull { it.updatedAt }
    }
    val listState = rememberLazyListState()
    val anchorIndex = remember(entryAnchor, blocks) {
        blocks.indexOfFirst { "${it.first.id}_${it.second}" == entryAnchor?.blockId }
            .takeIf { it >= 0 } ?: entryAnchor?.blockIndex?.coerceIn(0, blocks.lastIndex)
    }
    var restored by remember(contentId, studyDate) { mutableStateOf(false) }
    var typography by remember { mutableStateOf(false) }
    val foreground = if (preferences.readerTheme in setOf("dark", "oled")) Color.White else Color.Black
    val background = when (preferences.readerTheme) {
        "dark", "oled" -> Color(0xFF121214)
        "sepia" -> Color(0xFFF5EEDA)
        else -> Color.White
    }
    val currentSection by remember(service) { derivedStateOf {
        blocks.getOrNull(listState.firstVisibleItemIndex)?.first ?: section
    } }
    fun audioStepAtReadingPosition(): PrayerAudioStep? {
        val block = blocks.getOrNull(listState.firstVisibleItemIndex) ?: return audioSteps.firstOrNull()
        return audioSteps.lastOrNull { it.textSectionId == block.first.id && it.paragraphIndex <= block.second }
            ?: audioSteps.firstOrNull { it.textSectionId == block.first.id }
    }
    fun jumpToAudioStep(step: PrayerAudioStep) {
        val index = blocks.indexOfFirst { it.first.id == step.textSectionId && it.second == step.paragraphIndex }
        if (index >= 0) scope.launch { listState.scrollToItem(index) }
    }
    LaunchedEffect(audioState.stepId) {
        if (audioState.active) audioSteps.firstOrNull { it.id == audioState.stepId }?.let { jumpToAudioStep(it) }
    }
    val progress by remember(service) { derivedStateOf {
        listState.firstVisibleItemIndex.toFloat() / blocks.size.coerceAtLeast(1)
    } }
    LaunchedEffect(contentId, studyDate) {
        listState.scrollToItem(anchorIndex ?: 0,
            entryAnchor?.textOffset?.coerceAtLeast(0) ?: 0)
        restored = true
    }
    fun save() {
        if (!restored) return
        val block = blocks.getOrNull(listState.firstVisibleItemIndex) ?: return
        manager.save(ContentReadingAnchor(contentId, studyDate, service.id,
            "${block.first.id}_${block.second}", listState.firstVisibleItemIndex,
            textOffset = listState.firstVisibleItemScrollOffset))
    }
    LaunchedEffect(contentId, studyDate, restored) {
        if (!restored) return@LaunchedEffect
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collectLatest { delay(250); withContext(Dispatchers.IO) { save() } }
    }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner, contentId, studyDate, restored) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) save()
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer); save() }
    }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(Modifier.fillMaxSize().background(background)) {
            LazyColumn(state = listState, contentPadding = PaddingValues(top = headerHeight + 12.dp, bottom = 128.dp),
                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
                itemsIndexed(blocks, key = { _, block -> "${block.first.id}_${block.second}" }) { index, block ->
                    if (block.second == 0 && index > 0) Text(block.first.title,
                        color = foreground, style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 16.dp, bottom = 12.dp))
                    StudyTextBlock(indexLetter = "", textWithNikud = block.third,
                        textPlain = com.example.ui.util.HebrewTextNormalizer.stripMarks(block.third),
                        preferences = preferences, textColor = foreground,
                        isReadingAnchor = anchorIndex == index,
                        modifier = Modifier.padding(bottom = 18.dp))
                }
                item(key = "attribution") {
                    var expanded by remember { mutableStateOf(false) }
                    TextButton(onClick = { expanded = !expanded }) { Text("אודות הנוסח ומקורות") }
                    if (expanded) {
                    Text("נוסח האר״י · תורה אור. התמלול טרם הוגה במלואו מול תהלת השם. תוספות עונתיות אינן נבחרות אוטומטית.",
                        style = MaterialTheme.typography.bodySmall, color = foreground.copy(alpha = .65f))
                    Text("מקור: תורמי ויקיטקסט העברי · CC BY-SA 4.0. שינויים: חלוקה לפסקאות, נרמול תווים והסרת הוראות והלכות. מזמורי שיר של יום: מאגר התהלים של האפליקציה.",
                        style = MaterialTheme.typography.bodySmall, color = foreground.copy(alpha = .65f))
                    TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(section.source))) }) {
                        Text("המקור והיסטוריית העריכות")
                    }
                    TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://creativecommons.org/licenses/by-sa/4.0/"))) }) {
                        Text("רישיון התוכן")
                    }
                    }
                }
            }
            Column(Modifier.align(Alignment.TopCenter).onSizeChanged { headerHeight = with(density) { it.height.toDp() } }) {
                CompactReaderHeader(service.title, currentSection.title, Color(0xFF73D4ED), progress, preferences.readerTheme)
                PrayerAudioControls(audioState, preferences.readerTheme,
                    onStart = {
                        prayerDay.outOfScope?.let { audioNotice = "האודיו מסודר לימי חול וחול המועד. היום: $it. אפשר להמשיך לקרוא." }
                        audioStepAtReadingPosition()?.let { audio.start(audioSteps, it.id, resume = true) }
                    },
                    onToggle = { audio.toggle() }, onStop = { audio.stop() }, onSeek = { audio.seek(it) },
                    onSpeed = { audio.cycleSpeed() }, onSections = { audioPicker = true })
            }
            val current = service.sections.indexOfFirst { it.id == currentSection.id }
            fun jumpTo(index: Int) {
                val part = service.sections.getOrNull(index) ?: return
                scope.launch { listState.scrollToItem(blocks.indexOfFirst { it.first.id == part.id }) }
            }
            FloatingReaderBar("", currentSection.title, progress, onBack, {}, { typography = true },
                preferences.readerTheme,
                if (audioState.active) ({
                    val next = audioSteps.indexOfFirst { it.id == audioState.stepId } + 1
                    audioSteps.getOrNull(next)?.let { audio.start(audioSteps, it.id); jumpToAudioStep(it) }
                    Unit
                }) else if (current < service.sections.lastIndex) ({ jumpTo(current + 1) }) else onNextPrayer,
                if (audioState.active) ({
                    val prev = audioSteps.indexOfFirst { it.id == audioState.stepId } - 1
                    audioSteps.getOrNull(prev)?.let { audio.start(audioSteps, it.id); jumpToAudioStep(it) }
                    Unit
                }) else if (current > 0) ({ jumpTo(current - 1) }) else onPrevPrayer,
                Modifier.align(Alignment.BottomCenter).navigationBarsPadding(), prayerNavigation = true,
                onOpenPrayerSections = { if (audioState.active) audioPicker = true else sectionPicker = true })
        }
        if (audioState.error != null) AlertDialog(onDismissRequest = { audio.clearError() },
            containerColor = background, titleContentColor = foreground, textContentColor = foreground,
            title = { Text("אודיו לתפילה") }, text = { Text(audioState.error!!) },
            confirmButton = { Row {
                TextButton(onClick = { audio.clearError(); audioPicker = true }) { Text("בחירת מקטע") }
                val nextId = audioState.nextAvailableId
                if (nextId != null) TextButton(onClick = {
                    audio.clearError()
                    audioSteps.firstOrNull { it.id == nextId }?.let { audio.start(audioSteps, it.id); jumpToAudioStep(it) }
                }, modifier = Modifier.testTag("prayer_audio_continue")) { Text("המשך מ־${audioState.nextAvailableTitle}") }
            } },
            dismissButton = { TextButton(onClick = { audio.clearError() }) { Text("המשך קריאה") } })
        if (audioNotice != null) AlertDialog(onDismissRequest = { audioNotice = null },
            containerColor = background, titleContentColor = foreground, textContentColor = foreground,
            title = { Text("אודיו לתפילה") }, text = { Text(audioNotice!!) },
            confirmButton = { TextButton(onClick = { audioNotice = null }) { Text("המשך קריאה") } })
        if (audioPicker) ModalBottomSheet(onDismissRequest = { audioPicker = false },
            containerColor = background, contentColor = foreground) {
            Text("${service.title} · מקטעי האודיו", style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(20.dp))
            Text(PrayerDayRules.describe(prayerDay), color = foreground.copy(alpha = .8f),
                style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 20.dp)
                    .testTag("prayer_audio_day"))
            Text(if (BuildConfig.DEBUG) "הקלטות ניסיוניות, חלקיות וטרם הוגהות. בחירה מפעילה אודיו; מקטע חסר לא ידולג אוטומטית."
                else "בחירה מפעילה אודיו מהמקטע הזה. הקריאה זמינה גם ללא הקלטה.",
                color = foreground.copy(alpha = .65f), style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 400.dp).testTag("prayer_audio_picker")) {
                items(audioSteps, key = { it.id }) { step ->
                    val ready = PrayerAudioPlan.playable(step.clip, date.dayOfWeek, BuildConfig.DEBUG)
                    ListItem(headlineContent = { Text(step.title) },
                        supportingContent = { Text(if (ready) "הקלטה זמינה" else "לקריאה · ההקלטה עדיין חסרה",
                            color = if (step.id == audioState.stepId) Color.Black else foreground.copy(alpha = .65f)) },
                        trailingContent = {
                            TextButton(onClick = {
                                audioPicker = false
                                jumpToAudioStep(step)
                                if (ready) audio.start(audioSteps, step.id) else audio.stop()
                            }, modifier = Modifier.testTag("prayer_audio_play_${step.id}"),
                                colors = ButtonDefaults.textButtonColors(contentColor = foreground)) {
                                Text(if (ready) "הפעלה" else "קריאה")
                            }
                        }, colors = ListItemDefaults.colors(
                            containerColor = if (step.id == audioState.stepId) Color(0xFF70D1E1) else background,
                            headlineColor = if (step.id == audioState.stepId) Color.Black else foreground))
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
        if (sectionPicker) ModalBottomSheet(onDismissRequest = { sectionPicker = false },
            containerColor = background, contentColor = foreground) {
            Text("${service.title} · מעבר למקטע", style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(20.dp))
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 500.dp)) {
            items(service.sections, key = { it.id }) { part ->
                TextButton(onClick = {
                    sectionPicker = false
                    scope.launch { listState.scrollToItem(blocks.indexOfFirst { it.first.id == part.id }) }
                }, modifier = Modifier.fillMaxWidth()) { Text(part.title, color = foreground) }
            }
            item { Spacer(Modifier.height(24.dp)) }
            }
        }
        if (typography) ReaderTypographySheet(preferences = preferences,
            onDismiss = { typography = false }, onFontSizeChange = onFontSizeChange,
            onLineSpacingChange = onLineSpacingChange, onFontFamilyChange = onFontFamilyChange,
            onNikudToggle = onNikudToggle, onThemeChange = onThemeChange,
            onKeepScreenOnChange = onKeepScreenOnChange)
    }
}
