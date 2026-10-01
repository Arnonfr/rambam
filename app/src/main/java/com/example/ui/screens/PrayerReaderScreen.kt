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
import com.example.ui.components.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext

@Composable
fun PrayerReaderScreen(
    section: PrayerSection,
    studyDate: String,
    hebrewDate: String,
    preferences: UserPreferences,
    onBack: () -> Unit,
    onNextDay: () -> Unit,
    onPrevDay: () -> Unit,
    onOpenDatePicker: () -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onLineSpacingChange: (Float) -> Unit,
    onFontFamilyChange: (String) -> Unit,
    onNikudToggle: (Boolean) -> Unit,
    onThemeChange: (String) -> Unit,
    onKeepScreenOnChange: (Boolean) -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val manager = remember { ContentReadingAnchorManager(context) }
    DisposableEffect(preferences.keepScreenOn) {
        val window = (context as? Activity)?.window
        if (preferences.keepScreenOn) window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
    val contentId = "prayer_${section.id}"
    val entryAnchor = remember(contentId, studyDate) { manager.get(contentId, studyDate) }
    val listState = rememberLazyListState()
    var restored by remember(contentId, studyDate) { mutableStateOf(false) }
    var typography by remember { mutableStateOf(false) }
    val foreground = if (preferences.readerTheme == "dark") Color.White else Color.Black
    val background = when (preferences.readerTheme) {
        "dark" -> Color(0xFF121214)
        "sepia" -> Color(0xFFF5EEDA)
        else -> Color.White
    }
    val progress by remember(section) { derivedStateOf {
        listState.firstVisibleItemIndex.toFloat() / (section.paragraphs.size + 1).coerceAtLeast(1)
    } }
    LaunchedEffect(contentId, studyDate) {
        listState.scrollToItem(entryAnchor?.blockIndex?.coerceIn(0, section.paragraphs.lastIndex + 1) ?: 0,
            entryAnchor?.textOffset?.coerceAtLeast(0) ?: 0)
        restored = true
    }
    fun save() {
        if (!restored) return
        manager.save(ContentReadingAnchor(contentId, studyDate, section.id,
            "${section.id}_${listState.firstVisibleItemIndex}", listState.firstVisibleItemIndex,
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
            LazyColumn(state = listState, contentPadding = PaddingValues(top = 76.dp, bottom = 128.dp),
                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
                item(key = "source_notice") {
                    Text("נוסח האר״י — סידור תורה אור. מהדורת בדיקה; התמלול טרם הוגה מול תהלת השם. תוספות לימים מיוחדים מוצגות כהוראות המקור, ולא נבחרות אוטומטית.",
                        style = MaterialTheme.typography.bodySmall, color = foreground.copy(alpha = .65f))
                    Spacer(Modifier.height(16.dp))
                }
                itemsIndexed(section.paragraphs, key = { index, _ -> "${section.id}_$index" }) { index, text ->
                    StudyTextBlock(indexLetter = "", textWithNikud = text,
                        textPlain = text.replace(Regex("[\\u0591-\\u05BD\\u05BF-\\u05C2\\u05C4-\\u05C5\\u05C7]"), ""),
                        preferences = preferences, textColor = foreground,
                        isReadingAnchor = entryAnchor?.blockIndex == index + 1,
                        modifier = Modifier.padding(bottom = 18.dp))
                }
                item(key = "attribution") {
                    Text("מקור: תורמי ויקיטקסט העברי · CC BY-SA 4.0. שינויים: חלוקה לפסקאות ונרמול תווים.",
                        style = MaterialTheme.typography.bodySmall, color = foreground.copy(alpha = .65f))
                    TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(section.source))) }) {
                        Text("המקור והיסטוריית העריכות")
                    }
                    TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://creativecommons.org/licenses/by-sa/4.0/"))) }) {
                        Text("רישיון התוכן")
                    }
                }
            }
            CompactReaderHeader("תפילות", section.title, Color(0xFF73D4ED), progress, preferences.readerTheme,
                Modifier.align(Alignment.TopCenter))
            FloatingReaderBar("", hebrewDate, progress, onBack, onOpenDatePicker, { typography = true },
                preferences.readerTheme, onNextDay, onPrevDay,
                Modifier.align(Alignment.BottomCenter).navigationBarsPadding())
        }
        if (typography) ReaderTypographySheet(preferences = preferences,
            onDismiss = { typography = false }, onFontSizeChange = onFontSizeChange,
            onLineSpacingChange = onLineSpacingChange, onFontFamilyChange = onFontFamilyChange,
            onNikudToggle = onNikudToggle, onThemeChange = onThemeChange,
            onKeepScreenOnChange = onKeepScreenOnChange)
    }
}
