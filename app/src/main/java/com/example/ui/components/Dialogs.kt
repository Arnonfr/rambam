package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.UserPreferences
import com.example.domain.schedule.DailyLessonResult
import com.example.domain.sunset.CityLocation
import com.example.domain.sunset.SolarSunsetCalculator
import com.example.domain.sunset.SupportedCities
import com.example.ui.theme.FontStyleOption
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldWarm
import com.example.ui.util.HebrewNumberFormatter
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    preferences: UserPreferences,
    onDismiss: () -> Unit,
    onTrackChange: (String) -> Unit,
    onDayBoundaryChange: (String) -> Unit,
    onCityChange: (String) -> Unit,
    onFontFamilyChange: (String) -> Unit = {},
    onToggleStudy: (String) -> Unit = {},
    onOpenAttribution: () -> Unit
) {
    var expandedCityDropdown by remember { mutableStateOf(false) }

    val selectedCity = remember(preferences.selectedCity) {
        SupportedCities.findCityByName(preferences.selectedCity)
    }
    val sunsetInstant = remember(selectedCity) {
        SolarSunsetCalculator.calculateSunset(LocalDate.now(), selectedCity, ZoneId.systemDefault())
    }
    val sunsetTimeStr = remember(sunsetInstant) {
        if (sunsetInstant != null) {
            val zdt = sunsetInstant.atZone(ZoneId.systemDefault())
            String.format("%02d:%02d", zdt.hour, zdt.minute)
        } else {
            "לא זמין"
        }
    }

    val ink = Color(0xFF111111)
    val paper = Color(0xFFFFFCF6)
    val studies = listOf(
        Triple("rambam_one", "רמב״ם — פרק אחד", Color(0xFFFF873F)),
        Triple("rambam_three", "רמב״ם — ג׳ פרקים", Color(0xFFFF873F)),
        Triple("mitzvot", "ספר המצוות", Color(0xFF72D7E8)),
        Triple("chumash", "חומש", Color(0xFFF4F66A)),
        Triple("tanya", "תניא", Color(0xFFF0A0DD)),
        Triple("tehillim", "תהילים", Color(0xFF9FDEEF)),
        Triple("hayom_yom", "היום יום", Color(0xFFC5B8F5))
    )

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        AlertDialog(
            onDismissRequest = onDismiss,
            modifier = Modifier.fillMaxWidth(0.92f).fillMaxHeight(0.9f).heightIn(max = 760.dp).border(1.5.dp, ink, RoundedCornerShape(22.dp)),
            properties = DialogProperties(usePlatformDefaultWidth = false),
            containerColor = paper,
            shape = RoundedCornerShape(22.dp),
            title = {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("הגדרות האפליקציה", fontWeight = FontWeight.ExtraBold, fontSize = 23.sp, color = ink, modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "סגירה", tint = ink)
                    }
                }
            },
            text = {
                Column(
                    Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("השיעורים במסך הבית", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = ink)
                    Text("אפשר לבחור כמה מסלולי רמב״ם במקביל.", fontSize = 12.sp, color = ink)
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        studies.forEach { (key, name, color) ->
                            val subtitle = when (key) {
                                "rambam_one" -> "פרק אחד ליום"
                                "rambam_three" -> "שלושה פרקים ליום"
                                "mitzvot" -> "השיעור המקביל למסלול ג׳ פרקים"
                                "chumash" -> "העלייה היומית"
                                "tanya" -> "השיעור היומי"
                                "hayom_yom" -> "פתגם יומי לפי התאריך העברי"
                                else -> "לפי ימי החודש"
                            }
                            val icon = when (key) {
                                "rambam_one", "rambam_three" -> Icons.Default.MenuBook
                                "mitzvot" -> Icons.Default.FactCheck
                                "chumash" -> Icons.Default.AutoStories
                                "tanya" -> Icons.Default.Book
                                else -> Icons.Default.MusicNote
                            }
                            Row(
                                Modifier.fillMaxWidth().height(58.dp).background(color).clickable { onToggleStudy(key) }.padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                com.example.ui.components.StudyLineIcon(key.removePrefix("rambam_"), Modifier.size(27.dp))
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(name, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = ink)
                                    Text(subtitle, fontSize = 11.sp, color = ink.copy(alpha = 0.72f))
                                }
                                Switch(
                                    checked = preferences.visibleStudies.contains(key),
                                    onCheckedChange = { onToggleStudy(key) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = ink,
                                        uncheckedThumbColor = Color.White,
                                        uncheckedTrackColor = ink.copy(alpha = 0.35f),
                                        uncheckedBorderColor = Color.Transparent
                                    )
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = ink, thickness = 1.dp)
                    Text("גופן הלימוד", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = ink)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FontStyleOption.entries.forEach { option ->
                            val selected = preferences.fontFamily == option.id
                            Surface(
                                modifier = Modifier.weight(1f).clickable { onFontFamilyChange(option.id) },
                                color = if (selected) ink else Color.Transparent,
                                contentColor = if (selected) Color.White else ink,
                                border = BorderStroke(1.2.dp, ink),
                                shape = RoundedCornerShape(7.dp)
                            ) {
                                Column(Modifier.padding(vertical = 7.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("רמב״ם", fontFamily = option.fontFamily, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text(option.title, fontSize = 9.sp, maxLines = 1)
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = ink, thickness = 1.dp)
                    Text("מועד החלפת היום", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = ink)
                    listOf(
                        "midnight" to Pair("חצות מקומי (ברירת מחדל)", "לפי שעון המכשיר"),
                        "sunset" to Pair("שקיעת החמה (הלכתי)", "לפי שקיעת השמש")
                    ).forEach { (value, copy) ->
                        val selected = preferences.dayBoundary == value
                        Row(
                            Modifier.fillMaxWidth().border(1.2.dp, ink, RoundedCornerShape(8.dp)).clickable { onDayBoundaryChange(value) }.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = selected, onClick = { onDayBoundaryChange(value) }, colors = RadioButtonDefaults.colors(selectedColor = ink))
                            Column(Modifier.padding(start = 8.dp)) {
                                Text(copy.first, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ink)
                                Text(copy.second, fontSize = 10.sp, color = ink.copy(alpha = 0.55f))
                            }
                        }
                    }

                    if (preferences.dayBoundary == "sunset") {
                        ExposedDropdownMenuBox(expanded = expandedCityDropdown, onExpandedChange = { expandedCityDropdown = it }) {
                            OutlinedTextField(
                                value = "${selectedCity.nameHebrew} · $sunsetTimeStr",
                                onValueChange = {}, readOnly = true,
                                label = { Text("עיר לחישוב שקיעה") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedCityDropdown) },
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ink, unfocusedBorderColor = ink),
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(expanded = expandedCityDropdown, onDismissRequest = { expandedCityDropdown = false }) {
                                SupportedCities.CITIES.forEach { city ->
                                    DropdownMenuItem(text = { Text(city.nameHebrew) }, onClick = { onCityChange(city.nameHebrew); expandedCityDropdown = false })
                                }
                            }
                        }
                    }

                    TextButton(onClick = { onDismiss(); onOpenAttribution() }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = ink, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("אודות התוכן, מקורות ורישוי", color = ink, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = ink),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("close_settings_button")
                ) { Text("שמירה", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp) }
            }
        )
    }
}

@Composable
fun AttributionDialog(
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var showHayomLicense by remember { mutableStateOf(false) }
    val hayomLicense = remember {
        context.assets.open("GFDL-1.3.txt").bufferedReader().use { it.readText() }
    }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = "אודות התוכן והרישוי",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.Start) {
                            Text(
                                text = "טקסט משנה תורה — ספר נשים",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Start
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "הנוסח המנוקד של ספר נשים (הלכות אישות, גירושין, ייבום וחליצה, נערה בתולה וסוטה — 53 פרקים, 1,147 הלכות) מובא ממהדורת תורת אמת, בעריכת והזנת ר׳ פנחס ראובן ור.מ.",
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                textAlign = TextAlign.Start
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "רישיון: Creative Commons Attribution-NonCommercial-ShareAlike 2.5 (CC BY-NC-SA 2.5)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Start
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.Start) {
                            Text(
                                text = "לוח הלימוד היומי (חב״ד / Hebcal)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Start
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "מיפוי שיעורי הרמב״ם במסלול פרק אחד ושלושה פרקים מבוסס על Hebcal REST API ברישיון CC BY 4.0, ואומת באופן קפדני מול לוח חב״ד הרשמי.",
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                textAlign = TextAlign.Start
                            )
                        }
                    }

                    Text("היום יום", fontWeight = FontWeight.Bold)
                    Text("מלקט: רבי מנחם מענדל שניאורסון. הטקסט מתוך חב״דטקסט, מאת תורמי האתר, ברישיון GFDL 1.3 או מאוחר יותר. הוסרו ניווט ועיצוב; פירושי מונחים מוצגים בסוגריים. המקור השקוף ומספרי הגרסאות מצורפים לאפליקציה.", fontSize = 12.sp)
                    TextButton(onClick = { android.content.Intent(android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse("https://text.chabadpedia.com/index.php?title=היום_יום"))
                        .also { context.startActivity(it) } }) { Text("מקור הטקסט והתורמים") }
                    TextButton(onClick = { showHayomLicense = !showHayomLicense }) { Text("רישיון GFDL — הנוסח המלא") }
                    if (showHayomLicense) Text(hayomLicense, fontSize = 11.sp)
                    Text(
                        text = "הבהרה משפטית: המקורות ובעלי הזכויות אינם תומכים באפליקציה או מעניקים לה חסות רשמית. האפליקציה פותחה לשם שמיים, להפצה חינמית לחלוטין ולזיכוי הרבים.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp,
                        textAlign = TextAlign.Start
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("הבנתי", color = Color.White, fontWeight = FontWeight.Medium)
                }
            }
        )
    }
}

@Composable
fun CalendarDatePickerDialog(
    initialDate: LocalDate,
    getLessonForDate: (LocalDate) -> DailyLessonResult?,
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate) -> Unit
) {
    var selectedDate by remember(initialDate) { mutableStateOf(initialDate) }
    val currentLesson = remember(selectedDate) { getLessonForDate(selectedDate) }
    val today = remember { LocalDate.now() }

    val daysStrip = remember(selectedDate) {
        (-5..5).map { offset -> selectedDate.plusDays(offset.toLong()) }
    }

    val civilFormatter = remember {
        DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale("he", "IL"))
    }

    val hebrewDateFormatted = currentLesson?.hebrewDate ?: "יום ${selectedDate.dayOfMonth}/${selectedDate.monthValue}"

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = Color.White,
            shape = RoundedCornerShape(8.dp),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = Color(0xFF0F172A),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "לוח שנה ושיעור יומי",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0F172A)
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Selected Date Header Card - Clean White / Glassmorphic
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = hebrewDateFormatted,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF0F172A),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = selectedDate.format(civilFormatter),
                                fontSize = 12.sp,
                                color = Color(0xFF64748B),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Stepper Navigation: Previous Day (Arrow Right ->) | Today | Next Day (Arrow Left <-)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // יום קודם - חץ לצד ימין
                        OutlinedButton(
                            onClick = { selectedDate = selectedDate.minusDays(1) },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0F172A)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("יום קודם", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF0F172A))
                        }

                        Button(
                            onClick = { selectedDate = today },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF1F5F9),
                                contentColor = Color(0xFF0F172A)
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text("היום", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
                        }

                        // יום הבא - חץ לצד שמאל
                        OutlinedButton(
                            onClick = { selectedDate = selectedDate.plusDays(1) },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0F172A)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text("יום הבא", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF0F172A))
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFF0F172A)
                            )
                        }
                    }

                    // Scrollable Horizontal Days Strip
                    Text(
                        text = "בחר יום מרצף הימים:",
                        fontSize = 12.sp,
                        color = Color(0xFF334155),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(daysStrip) { date ->
                            val isSelected = date == selectedDate
                            val isCurrentToday = date == today
                            val dayLetter = HebrewNumberFormatter.toHebrewNumeral(date.dayOfMonth)
                            val dayOfWeekStr = when (date.dayOfWeek) {
                                DayOfWeek.SUNDAY -> "א׳"
                                DayOfWeek.MONDAY -> "ב׳"
                                DayOfWeek.TUESDAY -> "ג׳"
                                DayOfWeek.WEDNESDAY -> "ד׳"
                                DayOfWeek.THURSDAY -> "ה׳"
                                DayOfWeek.FRIDAY -> "ו׳"
                                DayOfWeek.SATURDAY -> "ש׳"
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = when {
                                    isSelected -> Color(0xFF0F172A)
                                    isCurrentToday -> Color(0xFFF1F5F9)
                                    else -> Color.White
                                },
                                border = BorderStroke(
                                    1.dp,
                                    when {
                                        isSelected -> Color(0xFF0F172A)
                                        isCurrentToday -> Color(0xFF0F172A)
                                        else -> Color(0xFFE2E8F0)
                                    }
                                ),
                                modifier = Modifier
                                    .width(52.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { selectedDate = date }
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = dayOfWeekStr,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isSelected) Color.White.copy(alpha = 0.8f) else Color(0xFF64748B)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = dayLetter,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSelected) Color.White else Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "${date.dayOfMonth}",
                                        fontSize = 10.sp,
                                        color = if (isSelected) Color.White.copy(alpha = 0.7f) else Color(0xFF94A3B8)
                                    )
                                }
                            }
                        }
                    }

                    // Scheduled Lesson Card - Glassmorphic / White
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.MenuBook,
                                    contentDescription = null,
                                    tint = Color(0xFF0F172A),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "שיעור הרמב״ם ליום זה:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF0F172A)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))

                            if (currentLesson != null) {
                                val chaptersText = if (currentLesson.chapters.isNotEmpty()) {
                                    currentLesson.chapters.joinToString(separator = " · ") {
                                        "${it.sectionNameHebrew} ${it.chapterHebrew}"
                                    }
                                } else {
                                    "שיעור יומי ברמב״ם"
                                }
                                val trackLabel = if (currentLesson.track == "three") "מסלול שלושה פרקים" else "מסלול פרק אחד"

                                Text(
                                    text = chaptersText,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF0F172A)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = trackLabel,
                                    fontSize = 12.sp,
                                    color = Color(0xFF475569)
                                )

                                val hasBundled = currentLesson.chapters.any { it.isBundledInSeferNashim }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (hasBundled) "טקסט הפרק זמין לקריאה מיידית" else "שיעור מחוץ לספר נשים",
                                    fontSize = 11.sp,
                                    color = if (hasBundled) Color(0xFF166534) else Color(0xFF854D0E),
                                    fontWeight = FontWeight.Medium
                                )
                            } else {
                                Text(
                                    text = "טוען נתוני שיעור...",
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDateSelected(selectedDate)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("פתח שיעור ליום זה", color = Color.White, fontWeight = FontWeight.Medium)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("סגור", color = Color(0xFF475569), fontWeight = FontWeight.Medium)
                }
            }
        )
    }
}
