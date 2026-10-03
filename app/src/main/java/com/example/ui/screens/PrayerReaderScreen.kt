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
import androidx.compose.ui.platform.LocalLayoutDirection
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
    onKeepScreenOnChange: (Boolean) -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val blocks = remember(service) { service.sections.flatMap { part ->
        part.paragraphs.mapIndexed { index, text -> Triple(part, index, text) }
    } }
    val section = service.sections.first()
    val scope = rememberCoroutineScope()
    var sectionPicker by remember { mutableStateOf(false) }
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
    val foreground = if (preferences.readerTheme == "dark") Color.White else Color.Black
    val background = when (preferences.readerTheme) {
        "dark" -> Color(0xFF121214)
        "sepia" -> Color(0xFFF5EEDA)
        else -> Color.White
    }
    val currentSection by remember(service) { derivedStateOf {
        blocks.getOrNull(listState.firstVisibleItemIndex)?.first ?: section
    } }
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
            LazyColumn(state = listState, contentPadding = PaddingValues(top = 40.dp, bottom = 128.dp),
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
            CompactReaderHeader(service.title, currentSection.title, Color(0xFF73D4ED), progress, preferences.readerTheme,
                Modifier.align(Alignment.TopCenter))
            val current = service.sections.indexOfFirst { it.id == currentSection.id }
            fun jumpTo(index: Int) {
                val part = service.sections.getOrNull(index) ?: return
                scope.launch { listState.scrollToItem(blocks.indexOfFirst { it.first.id == part.id }) }
            }
            FloatingReaderBar("", currentSection.title, progress, onBack, {}, { typography = true },
                preferences.readerTheme,
                if (current < service.sections.lastIndex) ({ jumpTo(current + 1) }) else onNextPrayer,
                if (current > 0) ({ jumpTo(current - 1) }) else onPrevPrayer,
                Modifier.align(Alignment.BottomCenter).navigationBarsPadding(), prayerNavigation = true,
                onOpenPrayerSections = { sectionPicker = true })
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
