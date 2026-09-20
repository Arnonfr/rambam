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

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = Color(0xFFF8FAFC), // Light surface matching home screen
            shape = RoundedCornerShape(24.dp), // More rounded corners
            title = {
                Text(
                    text = "הגדרות האפליקציה",
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = Color(0xFF2D3748), // TextDark from theme
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    // Track selection
                    Text(
                        text = "מסלול לימוד ראשי",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF2D3748),
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Modern styled segmented pill for track with sharp contrast
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFE2E8F0),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // 1 Chapter
                            val isOneSelected = preferences.selectedTrack == "one"
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isOneSelected) Color.White else Color.Transparent,
                                border = if (isOneSelected) BorderStroke(1.5.dp, Color(0xFF4C51C6)) else null,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onTrackChange("one") }
                                    .then(
                                        if (isOneSelected) Modifier.shadow(2.dp, RoundedCornerShape(8.dp)) else Modifier
                                    )
                            ) {
                                Text(
                                    text = "פרק אחד ליום",
                                    fontSize = 13.sp,
                                    fontWeight = if (isOneSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isOneSelected) Color(0xFF4C51C6) else Color(0xFF4A5568),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 9.dp)
                                )
                            }

                            // 3 Chapters
                            val isThreeSelected = preferences.selectedTrack == "three"
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isThreeSelected) Color.White else Color.Transparent,
                                border = if (isThreeSelected) BorderStroke(1.5.dp, Color(0xFF4C51C6)) else null,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onTrackChange("three") }
                                    .then(
                                        if (isThreeSelected) Modifier.shadow(2.dp, RoundedCornerShape(8.dp)) else Modifier
                                    )
                            ) {
                                Text(
                                    text = "שלושה פרקים ליום",
                                    fontSize = 13.sp,
                                    fontWeight = if (isThreeSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isThreeSelected) Color(0xFF4C51C6) else Color(0xFF4A5568),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 9.dp)
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = Color(0xFFE2E8F0))

                    // Font Family selection
                    Text(
                        text = "סגנון גופן הלימוד",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF2D3748),
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FontStyleOption.entries.forEach { option ->
                            val isSelected = preferences.fontFamily == option.id
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onFontFamilyChange(option.id) }
                                    .border(
                                        width = if (isSelected) 2.dp else 1.5.dp,
                                        color = if (isSelected) Color(0xFF4C51C6) else Color(0xFFE2E8F0),
                                        shape = RoundedCornerShape(8.dp)
                                    ),
                                color = if (isSelected) Color(0xFFF3F4F6) else Color.White
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "רמב״ם",
                                        fontFamily = option.fontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isSelected) Color(0xFF4C51C6) else Color(0xFF2D3748)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = option.title,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color(0xFF4C51C6) else Color(0xFF718096)
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = Color(0xFFE2E8F0))

                    // Day boundary mode
                    Text(
                        text = "מועד החלפת יום הלימוד",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF2D3748),
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "קובע מתי לוח הלימוד עובר לשיעור של היום הבא",
                        fontSize = 12.sp,
                        color = Color(0xFF718096),
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, if (preferences.dayBoundary == "midnight") Color(0xFF4C51C6) else Color(0xFFCBD5E1)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onDayBoundaryChange("midnight") }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = preferences.dayBoundary == "midnight",
                                    onClick = { onDayBoundaryChange("midnight") },
                                    modifier = Modifier.testTag("day_boundary_midnight"),
                                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF4C51C6))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(horizontalAlignment = Alignment.Start) {
                                    Text(
                                        text = "חצות מקומי (ברירת מחדל)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color(0xFF2D3748),
                                        textAlign = TextAlign.Start
                                    )
                                    Text(
                                        text = "מתאים לסדר יום קבוע לפי שעון המכשיר",
                                        fontSize = 12.sp,
                                        color = Color(0xFF718096),
                                        textAlign = TextAlign.Start
                                    )
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, if (preferences.dayBoundary == "sunset") Color(0xFF4C51C6) else Color(0xFFCBD5E1)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onDayBoundaryChange("sunset") }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = preferences.dayBoundary == "sunset",
                                    onClick = { onDayBoundaryChange("sunset") },
                                    modifier = Modifier.testTag("day_boundary_sunset"),
                                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF4C51C6))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(horizontalAlignment = Alignment.Start) {
                                    Text(
                                        text = "שקיעת החמה (הלכתי)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color(0xFF2D3748),
                                        textAlign = TextAlign.Start
                                    )
                                    Text(
                                        text = "היום העברי מתחלף עם שקיעת השמש",
                                        fontSize = 12.sp,
                                        color = Color(0xFF718096),
                                        textAlign = TextAlign.Start
                                    )
                                }
                            }
                        }
                    }

                    // City selector for sunset calculation
                    if (preferences.dayBoundary == "sunset") {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF3F4F6),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.Start) {
                                Text(
                                    text = "עיר לחישוב זמן שקיעה:",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2D3748),
                                    textAlign = TextAlign.Start
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                ExposedDropdownMenuBox(
                                    expanded = expandedCityDropdown,
                                    onExpandedChange = { expandedCityDropdown = it }
                                ) {
                                    OutlinedTextField(
                                        value = "${selectedCity.nameHebrew} (שקיעה היום: $sunsetTimeStr)",
                                        onValueChange = {},
                                        readOnly = true,
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCityDropdown) },
                                        modifier = Modifier
                                            .menuAnchor()
                                            .fillMaxWidth()
                                    )

                                    ExposedDropdownMenu(
                                        expanded = expandedCityDropdown,
                                        onDismissRequest = { expandedCityDropdown = false }
                                    ) {
                                        SupportedCities.CITIES.forEach { city ->
                                            DropdownMenuItem(
                                                text = { Text("${city.nameHebrew} (${city.nameEnglish})") },
                                                onClick = {
                                                    onCityChange(city.nameHebrew)
                                                    expandedCityDropdown = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = Color(0xFFE2E8F0))

                    // About & Attribution Button
                    TextButton(
                        onClick = {
                            onDismiss()
                            onOpenAttribution()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color(0xFF4C51C6))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("אודות התוכן, מקורות ורישוי", color = Color(0xFF4C51C6), fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4C51C6)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("close_settings_button")
                ) {
                    Text("סגור", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun AttributionDialog(
    onDismiss: () -> Unit
) {
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
