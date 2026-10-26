package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.tehillim.*
import com.example.ui.util.HebrewNumberFormatter
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.abs

private val PsalmInk = Color(0xFF171714)
private val PsalmMuted = Color(0xFF525B60)
private val PsalmRule = Color(0xFFDDE7EA)

internal fun psalmYearLabel(year: Int): String {
    val raw = HebrewNumberFormatter.toHebrewNumeral(year % 1000)
    return if (raw.length > 1) raw.dropLast(1) + "״" + raw.last() else raw
}
internal fun psalmDateLabel(date: HebrewBirthday): String {
    val month = when (date.month) { 6 -> if (PersonalPsalmCalendar.isLeap(date.year)) "אדר ב׳" else "אדר"; else -> PersonalPsalmCalendar.monthNames[date.month] }
    return "${HebrewNumberFormatter.toHebrewNumeral(date.day, true)} $month ${psalmYearLabel(date.year)}"
}

@Composable
internal fun PersonalPsalmEditor(entry: PersonalTehillimEntry?, date: LocalDate, onDismiss: () -> Unit, onSave: (PersonalTehillimEntry) -> Unit) {
    BackHandler(onBack = onDismiss)
    val today = remember(date) { PersonalPsalmCalendar.fromCivil(date) }
    val id = rememberSaveable { entry?.id ?: java.util.UUID.randomUUID().toString() }
    var label by rememberSaveable { mutableStateOf(entry?.label.orEmpty()) }
    var kind by rememberSaveable { mutableStateOf(if (entry?.birthday != null) "birthday" else "chapter") }
    var calendarKind by rememberSaveable { mutableStateOf("hebrew") }
    var number by rememberSaveable { mutableIntStateOf(entry?.fixedChapter ?: 1) }
    var day by rememberSaveable { mutableIntStateOf(entry?.birthday?.day ?: today.day) }
    var month by rememberSaveable { mutableIntStateOf(entry?.birthday?.month ?: today.month) }
    var year by rememberSaveable { mutableIntStateOf(entry?.birthday?.year ?: today.year) }
    val initialCivil = remember(entry?.id, date) { entry?.birthday?.let(PersonalPsalmCalendar::toCivil) ?: date }
    var civilDay by rememberSaveable { mutableIntStateOf(initialCivil.dayOfMonth) }
    var civilMonth by rememberSaveable { mutableIntStateOf(initialCivil.monthValue) }
    var civilYear by rememberSaveable { mutableIntStateOf(initialCivil.year) }
    var sunset by rememberSaveable { mutableStateOf(false) }
    var adar by rememberSaveable { mutableStateOf(entry?.adar ?: AdarBirthday.SECOND) }
    var missing by rememberSaveable { mutableStateOf(entry?.missingDay ?: MissingBirthdayDay.NEXT_MONTH) }
    val keyboard = LocalSoftwareKeyboardController.current
    val birthday = remember(year, month, day, calendarKind, civilYear, civilMonth, civilDay, sunset) {
        if (calendarKind == "civil") PersonalPsalmCalendar.fromCivil(LocalDate.of(civilYear, civilMonth, civilDay), sunset)
        else HebrewBirthday(year, month, day)
    }
    val candidate = runCatching {
        if (kind == "chapter") PersonalTehillimEntry(id, label.trim(), fixedChapter = number)
        else PersonalTehillimEntry(id, label.trim(), birthday = birthday, adar = adar, missingDay = missing)
            .also { require(PersonalPsalmCalendar.toCivil(birthday) <= date) }
    }.getOrNull()
    Column(Modifier.fillMaxSize().background(Color.White).imePadding().testTag("personal_psalm_editor")) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDismiss) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "חזרה", tint = PsalmInk) }
            Text(if (entry == null) "הוספת פרק" else "עריכת פרק", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = PsalmInk)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("שם או כינוי · לבחירתך", color = PsalmMuted, fontSize = 13.sp)
                BasicTextField(label, { label = it }, singleLine = true,
                    textStyle = TextStyle(color = PsalmInk, fontSize = 20.sp), cursorBrush = SolidColor(PsalmInk),
                    modifier = Modifier.fillMaxWidth().testTag("personal_label"), decorationBox = { input ->
                        Box(Modifier.fillMaxWidth().background(Color(0xFFF3F8F9), RoundedCornerShape(16.dp))
                            .border(1.dp, PsalmRule, RoundedCornerShape(16.dp)).padding(16.dp)) {
                            if (label.isEmpty()) Text("למשל: שלי, אמא, הילדים", color = PsalmMuted, fontSize = 18.sp)
                            input()
                        }
                    })
            }
            PsalmToggle(listOf("chapter" to "לפי פרק", "birthday" to "לפי תאריך לידה"), kind,
                { keyboard?.hide(); kind = it }, "personal_kind")
            if (kind == "chapter") {
                PsalmScrollPicker("הפרק לקריאה", (1..150).toList(), number, { number = it },
                    { HebrewNumberFormatter.toHebrewNumeral(it) }, "personal_chapter", itemWidth = 64)
            } else {
                PsalmToggle(listOf("hebrew" to "תאריך עברי", "civil" to "תאריך אזרחי"), calendarKind,
                    { keyboard?.hide(); calendarKind = it }, "personal_calendar")
                if (calendarKind == "hebrew") {
                    PsalmScrollPicker("יום בחודש", (1..PersonalPsalmCalendar.daysInMonth(year, month)).toList(), day,
                        { day = it }, { HebrewNumberFormatter.toHebrewNumeral(it) }, "personal_day", itemWidth = 56)
                    val months = (0..12).filter { it != 5 || PersonalPsalmCalendar.isLeap(year) }
                    PsalmScrollPicker("חודש", months, month, {
                        month = it; day = day.coerceAtMost(PersonalPsalmCalendar.daysInMonth(year, it))
                    }, { if (it == 6) { if (PersonalPsalmCalendar.isLeap(year)) "אדר ב׳" else "אדר" } else PersonalPsalmCalendar.monthNames[it] }, "personal_month", itemWidth = 104)
                    PsalmScrollPicker("שנת לידה", (today.year downTo 5000).toList(), year, { selected ->
                        year = selected
                        if (month == 5 && !PersonalPsalmCalendar.isLeap(selected)) month = 6
                        day = day.coerceAtMost(PersonalPsalmCalendar.daysInMonth(selected, month))
                    }, ::psalmYearLabel, "personal_year", itemWidth = 104, secondary = { it.toString() })
                } else {
                    val civil = LocalDate.of(civilYear, civilMonth, 1)
                    PsalmScrollPicker("יום בחודש", (1..civil.lengthOfMonth()).toList(), civilDay, { civilDay = it }, { it.toString() }, "personal_civil_day", itemWidth = 56)
                    val monthNames = listOf("ינואר", "פברואר", "מרץ", "אפריל", "מאי", "יוני", "יולי", "אוגוסט", "ספטמבר", "אוקטובר", "נובמבר", "דצמבר")
                    PsalmScrollPicker("חודש", (1..12).toList(), civilMonth, {
                        civilMonth = it; civilDay = civilDay.coerceAtMost(LocalDate.of(civilYear, it, 1).lengthOfMonth())
                    }, { monthNames[it - 1] }, "personal_civil_month", itemWidth = 104)
                    PsalmScrollPicker("שנת לידה", (date.year downTo 1800).toList(), civilYear, {
                        civilYear = it; civilDay = civilDay.coerceAtMost(LocalDate.of(it, civilMonth, 1).lengthOfMonth())
                    }, { it.toString() }, "personal_civil_year", itemWidth = 92)
                    PsalmToggle(listOf("before" to "לפני השקיעה", "after" to "אחרי השקיעה"), if (sunset) "after" else "before",
                        { sunset = it == "after" }, "personal_sunset")
                    Text("התאריך העברי: ${psalmDateLabel(birthday)}", color = PsalmMuted, fontSize = 14.sp)
                }
                if (birthday.month == 6 && !PersonalPsalmCalendar.isLeap(birthday.year)) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("החלפת הפרק בשנה מעוברת", color = PsalmMuted, fontSize = 13.sp)
                        PsalmToggle(listOf("FIRST" to "באדר א׳", "SECOND" to "באדר ב׳"), adar.name,
                            { adar = AdarBirthday.valueOf(it) }, "personal_adar")
                    }
                }
                if (birthday.day == 30) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("אם ל׳ בחודש אינו קיים באותה שנה", color = PsalmMuted, fontSize = 13.sp)
                        PsalmToggle(listOf("NEXT_MONTH" to "בא׳ בחודש הבא", "LAST_DAY" to "ביום האחרון"), missing.name,
                            { missing = MissingBirthdayDay.valueOf(it) }, "personal_missing_day")
                    }
                }
            }
            Box(Modifier.fillMaxWidth().background(PsalmBlue.copy(alpha = .34f), RoundedCornerShape(20.dp)).padding(20.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("הפרק לתאריך הנבחר", color = PsalmMuted, fontSize = 13.sp)
                    Text(candidate?.chapter(date)?.let(::psalmName) ?: if (candidate == null) "בחרו תאריך שאינו בעתיד" else "יש לבחור פרק קבוע לגיל הזה",
                        color = PsalmInk, fontSize = 25.sp, fontWeight = FontWeight.Bold, modifier = Modifier.testTag("personal_preview"))
                    candidate?.nextBirthday(date)?.let { Text("החלפה הבאה: ${psalmDateLabel(PersonalPsalmCalendar.fromCivil(it))}", color = PsalmMuted, fontSize = 13.sp) }
                }
            }
            if (kind == "birthday") Text("הפרק מחושב לפי הגיל העברי המלא ועוד אחת, ומתעדכן ביום ההולדת בשקיעה.", color = PsalmMuted, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PsalmAction("שמירת הפרק", { candidate?.let(onSave) }, Modifier.weight(1f).testTag("save_personal_psalm"), enabled = candidate != null)
            PsalmAction("ביטול", onDismiss, Modifier.width(92.dp), filled = false)
        }
    }
}

@Composable
internal fun PsalmToggle(options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit, tag: String) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color(0xFFF1F6F7))
        .border(1.dp, PsalmRule, RoundedCornerShape(18.dp)).padding(4.dp).selectableGroup()) {
        options.forEach { (key, label) ->
            val checked = key == selected
            Box(Modifier.weight(1f).heightIn(min = 46.dp).clip(RoundedCornerShape(14.dp))
                .background(if (checked) PsalmBlue else Color.Transparent)
                .selectable(checked, role = Role.RadioButton, onClick = { onSelect(key) })
                .testTag("${tag}_$key").padding(horizontal = 6.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
                Text(label, color = PsalmInk, fontSize = 14.sp, fontWeight = if (checked) FontWeight.Bold else FontWeight.Medium)
            }
        }
    }
}

@Composable
internal fun PsalmAction(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, filled: Boolean = true, enabled: Boolean = true) {
    Box(modifier.heightIn(min = 50.dp).clip(RoundedCornerShape(18.dp))
        .background(if (filled) { if (enabled) PsalmBlue else Color(0xFFE1E6E8) } else Color.White)
        .border(1.dp, if (filled) Color.Transparent else PsalmInk, RoundedCornerShape(18.dp))
        .clickable(enabled = enabled, role = Role.Button, onClick = onClick).padding(horizontal = 12.dp, vertical = 13.dp), contentAlignment = Alignment.Center) {
        Text(label, color = PsalmInk, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun PsalmScrollPicker(label: String, values: List<Int>, selected: Int, onSelect: (Int) -> Unit,
    text: (Int) -> String, tag: String, itemWidth: Int, secondary: ((Int) -> String)? = null) {
    val index = values.indexOf(selected).coerceAtLeast(0)
    val state = rememberLazyListState(initialFirstVisibleItemIndex = index)
    val scope = rememberCoroutineScope()
    val select by rememberUpdatedState(onSelect)
    fun closest(): Int? {
        val info = state.layoutInfo
        if (info.totalItemsCount != values.size || info.visibleItemsInfo.isEmpty()) return null
        val center = (info.viewportStartOffset + info.viewportEndOffset) / 2
        return info.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2 - center) }?.index
    }
    LaunchedEffect(values) {
        snapshotFlow { if (state.isScrollInProgress) null else closest() }.distinctUntilChanged().collect { i ->
            i?.let { values.getOrNull(it) }?.let(select)
        }
    }
    LaunchedEffect(selected, values) {
        if (!state.isScrollInProgress && closest() != index) state.scrollToItem(index)
    }
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(label, color = PsalmMuted, fontSize = 13.sp)
        BoxWithConstraints(Modifier.fillMaxWidth().height(68.dp).clip(RoundedCornerShape(18.dp)).background(Color(0xFFF7FAFA))) {
            Box(Modifier.align(Alignment.Center).width(itemWidth.dp).fillMaxHeight().padding(vertical = 4.dp)
                .background(PsalmBlue, RoundedCornerShape(14.dp)))
            LazyRow(state = state, flingBehavior = rememberSnapFlingBehavior(state),
                contentPadding = PaddingValues(horizontal = ((maxWidth - itemWidth.dp) / 2).coerceAtLeast(0.dp)),
                modifier = Modifier.fillMaxSize().testTag("${tag}_picker")) {
                items(values, key = { it }) { value ->
                    Column(Modifier.width(itemWidth.dp).fillMaxHeight()
                        .selectable(value == selected, role = Role.RadioButton, onClick = {
                            select(value); scope.launch { state.animateScrollToItem(values.indexOf(value)) }
                        }).testTag("${tag}_$value"),
                        verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text(value), color = PsalmInk, fontSize = 19.sp, fontWeight = if (value == selected) FontWeight.Bold else FontWeight.Normal)
                        secondary?.let { Text(it(value), color = PsalmMuted, fontSize = 11.sp) }
                    }
                }
            }
        }
    }
}
