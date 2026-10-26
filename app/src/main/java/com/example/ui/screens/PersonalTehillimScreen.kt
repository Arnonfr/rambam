@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.data.local.UserPreferences
import com.example.data.tehillim.PersonalTehillimState
import com.example.data.tehillim.PersonalTehillimStore
import com.example.domain.tehillim.*
import com.example.ui.util.HebrewNumberFormatter
import java.time.LocalDate

internal val PsalmBlue = Color(0xFF9FDEEF)
internal fun psalmName(number: Int) = "פרק ${HebrewNumberFormatter.toHebrewNumeral(number)}"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalTehillimScreen(
    state: PersonalTehillimState,
    date: LocalDate,
    preferences: UserPreferences,
    onBack: () -> Unit,
    onSave: (PersonalTehillimEntry) -> Unit,
    onDelete: (String) -> Unit,
    onMove: (String, Int) -> Unit,
    onComplete: (PersonalTehillimEntry, Boolean) -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onLineSpacingChange: (Float) -> Unit,
    onFontFamilyChange: (String) -> Unit,
    onNikudToggle: (Boolean) -> Unit,
    onThemeChange: (String) -> Unit,
    onKeepScreenOnChange: (Boolean) -> Unit,
    error: String? = null,
    startWithManagement: Boolean = false
) {
    var managing by rememberSaveable { mutableStateOf(startWithManagement) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var adding by rememberSaveable { mutableStateOf(false) }
    var deleteId by rememberSaveable { mutableStateOf<String?>(null) }
    var readingIds by rememberSaveable { mutableStateOf<ArrayList<String>?>(null) }
    val availableIds = state.entries.filter { it.chapter(date) != null }.map { it.id }
    val sequence = (readingIds ?: availableIds).filter { it in availableIds }
    val readingEntries = if (managing) emptyList() else sequence.mapNotNull { id -> state.entries.find { it.id == id } }
    val background = when (preferences.readerTheme) {
        "dark", "oled" -> Color(0xFF121214)
        "sepia" -> Color(0xFFF5EEDA)
        else -> Color.White
    }
    val foreground = if (preferences.readerTheme in setOf("dark", "oled")) Color(0xFFE2E2E6) else Color(0xFF171714)
    fun backFromManagement() {
        if (!startWithManagement && state.entries.any { it.chapter(date) != null }) managing = false else onBack()
    }
    BackHandler {
        when {
            adding || editingId != null -> { adding = false; editingId = null }
            managing -> backFromManagement()
            else -> onBack()
        }
    }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        if (adding || editingId != null) {
            PersonalPsalmEditor(state.entries.find { it.id == editingId }, date,
                onDismiss = { adding = false; editingId = null },
                onSave = { onSave(it); adding = false; editingId = null })
        } else if (readingEntries.isNotEmpty()) {
            key(date, readingEntries.map { it.id to it.chapter(date) }) {
            PersonalPsalmSequenceReader(readingEntries, date, preferences, state.completed, background, foreground,
                onBack = onBack, onManage = { managing = true }, onComplete = { onComplete(it, true) },
                onFontSizeChange = onFontSizeChange, onLineSpacingChange = onLineSpacingChange,
                onFontFamilyChange = onFontFamilyChange, onNikudToggle = onNikudToggle,
                onThemeChange = onThemeChange, onKeepScreenOnChange = onKeepScreenOnChange)
            }
        } else {
            Column(Modifier.fillMaxSize().background(Color.White).testTag("personal_tehillim_screen")) {
                TopAppBar(title = { Text(if (state.entries.isEmpty()) "הפרקים שלי" else "ניהול הפרקים") }, navigationIcon = {
                    IconButton(onClick = ::backFromManagement) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "חזרה") }
                }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White, titleContentColor = Color.Black, navigationIconContentColor = Color.Black))
                LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        Text("פרקים אישיים ומשפחתיים", fontWeight = FontWeight.Bold, color = Color.Black)
                        Text("לפי פרק קבוע או לפי גיל עברי. הפרק מתעדכן ביום ההולדת בשקיעה.",
                            style = MaterialTheme.typography.bodyMedium, color = Color.DarkGray)
                        Text(psalmDateLabel(PersonalPsalmCalendar.fromCivil(date)),
                            style = MaterialTheme.typography.labelMedium, color = Color.DarkGray)
                        if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
                    }
                    if (state.entries.isEmpty()) item {
                        Text("אפשר להוסיף פרקים עבורך, עבור בני המשפחה, וכל פרק שרוצים לקרוא.", Modifier.padding(vertical = 24.dp), color = Color.DarkGray)
                    }
                    itemsIndexed(state.entries, key = { _, e -> e.id }) { index, entry ->
                        val number = entry.chapter(date)
                        val completed = number != null && PersonalTehillimStore.completionKey(entry.id, date.toString(), number) in state.completed
                        Card(colors = CardDefaults.cardColors(containerColor = PsalmBlue.copy(alpha = .32f), contentColor = Color.Black), modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f).clickable(enabled = number != null) {
                                        readingIds = ArrayList(availableIds.drop(availableIds.indexOf(entry.id).coerceAtLeast(0))); managing = false
                                    }.padding(vertical = 8.dp).testTag("personal_open_${entry.id}")) {
                                        Text(entry.label.ifBlank { number?.let(::psalmName) ?: "פרק לפי גיל" }, fontWeight = FontWeight.Bold, color = Color.Black)
                                        Text(if (entry.label.isBlank() && number != null) {
                                            if (entry.fixedChapter != null) "פרק קבוע" else "לפי גיל עברי"
                                        } else number?.let(::psalmName) ?: "בחרו פרק ידני לתאריך הזה", color = Color.DarkGray)
                                        entry.nextBirthday(date)?.let {
                                            Text("החלפה: ${psalmDateLabel(PersonalPsalmCalendar.fromCivil(it))}", style = MaterialTheme.typography.labelSmall, color = Color.DarkGray)
                                        }
                                    }
                                    Checkbox(completed, onCheckedChange = { onComplete(entry, it) }, enabled = number != null,
                                        colors = CheckboxDefaults.colors(checkedColor = Color.Black, uncheckedColor = Color.DarkGray, checkmarkColor = Color.White),
                                        modifier = Modifier.testTag("personal_done_${entry.id}").semantics {
                                            contentDescription = "סימון קריאה: ${entry.label.ifBlank { number?.let(::psalmName).orEmpty() }}"
                                        })
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TextButton(onClick = { editingId = entry.id }) { Text("עריכה", color = Color.Black) }
                                    TextButton(onClick = { deleteId = entry.id }) { Text("מחיקה", color = Color.Black) }
                                    Spacer(Modifier.weight(1f))
                                    IconButton(onClick = { onMove(entry.id, -1) }, enabled = index > 0) { Icon(Icons.Default.ArrowUpward, "העברה למעלה") }
                                    IconButton(onClick = { onMove(entry.id, 1) }, enabled = index < state.entries.lastIndex) { Icon(Icons.Default.ArrowDownward, "העברה למטה") }
                                }
                            }
                        }
                    }
                    item {
                        PsalmAction("הוספת פרק", { adding = true }, Modifier.fillMaxWidth().testTag("add_personal_psalm"))
                        if (state.entries.any { it.chapter(date) != null }) PsalmAction("קריאת הכול ברצף", onClick = {
                            readingIds = ArrayList(state.entries.filter { it.chapter(date) != null }.map { it.id }); managing = false
                        }, modifier = Modifier.fillMaxWidth().padding(top = 10.dp).testTag("read_all_personal_psalms"), filled = false)
                    }
                }
            }
        }
        deleteId?.let { id -> AlertDialog(containerColor = Color.White, titleContentColor = Color.Black,
            textContentColor = Color.Black, onDismissRequest = { deleteId = null }, title = { Text("מחיקת הפרק מהרשימה?") },
            confirmButton = { TextButton(onClick = { onDelete(id); deleteId = null }) { Text("מחיקה") } },
            dismissButton = { TextButton(onClick = { deleteId = null }) { Text("ביטול") } }) }
    }
}
