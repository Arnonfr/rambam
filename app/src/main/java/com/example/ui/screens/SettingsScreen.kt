package com.example.ui.screens

import androidx.compose.foundation.clickable

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.Feedback
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.UserPreferences
import com.example.domain.sunset.SolarSunsetCalculator
import com.example.domain.sunset.SupportedCities
import com.example.ui.components.StudyLineIcon
import com.example.ui.theme.FontStyleOption
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val SettingsInk = Color(0xFF171714)
private val SettingsPaper = Color.White
private val SettingsMuted = Color(0xFF66645C)
private val SettingsLine = Color(0xFFE1DDD3)

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun SettingsScreen(
    preferences: UserPreferences,
    onBack: () -> Unit,
    onDayBoundaryChange: (String) -> Unit,
    onCityChange: (String) -> Unit,
    onFontFamilyChange: (String) -> Unit,
    onToggleStudy: (String) -> Unit,
    onTanyaBookViewChange: (Boolean) -> Unit,
    onOpenAttribution: () -> Unit,
    onFontSizeChange: (Float) -> Unit = {},
    onLineSpacingChange: (Float) -> Unit = {},
    onLetterSpacingChange: (Float) -> Unit = {},
    onNikudToggle: (Boolean) -> Unit = {},
    onThemeChange: (String) -> Unit = {},
    onKeepScreenOnChange: (Boolean) -> Unit = {},
    personalPsalmCount: Int = 0,
    onOpenPersonalPsalms: () -> Unit = {},
    zmanimPreferences: com.example.domain.zmanim.ZmanimPreferences = com.example.domain.zmanim.ZmanimPreferences(),
    onZmanimChange: (com.example.domain.zmanim.ZmanimPreferences) -> Unit = {},
    onZmanimLocate: () -> Unit = {},
    zmanimLocating: Boolean = false,
    zmanimMessage: String? = null,
    expandZmanim: Boolean = false,
    onRateInStore: () -> Unit = {}
) {
    val analytics = com.example.data.analytics.AudioAnalytics.get(androidx.compose.ui.platform.LocalContext.current)
    val measurementEnabled by analytics.enabled.collectAsState()
    var showFeedback by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    var showZmanimPreferences by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(expandZmanim) }
    fun navigateBack() { if (showZmanimPreferences) showZmanimPreferences = false else onBack() }
    BackHandler { navigateBack() }
    val pageScroll = key(showZmanimPreferences) { rememberScrollState() }
    var expandedCityDropdown by remember { mutableStateOf(false) }
    val selectedCity = remember(preferences.selectedCity) {
        SupportedCities.findCityByName(preferences.selectedCity)
    }
    val today = LocalDate.now()
    val zone = ZoneId.systemDefault()
    val sunsetTime = remember(selectedCity, today, zone) {
        SolarSunsetCalculator.calculateSunset(today, selectedCity, zone)
            ?.atZone(zone)?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "לא זמין"
    }
    val studies = listOf(
        Triple("chumash", "חומש", Color(0xFFF4F66A)),
        Triple("tehillim", "תהילים", Color(0xFF9FDEEF)),
        Triple("tanya", "תניא", Color(0xFFF0A0DD)),
        Triple("rambam_one", "רמב״ם — פרק אחד", Color(0xFFFFB27C)),
        Triple("rambam_three", "רמב״ם — ג׳ פרקים", Color(0xFFFFB27C)),
        Triple("mitzvot", "ספר המצוות", Color(0xFF9DE0E8)),
        Triple("hayom_yom", "היום יום", Color(0xFFD1C4F1))
    )

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        if (showFeedback) {
            com.example.ui.components.FeedbackScreen { showFeedback = false }
            return@CompositionLocalProvider
        }
        Column(Modifier.fillMaxSize().background(SettingsPaper).testTag("settings_screen")) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navigateBack() }, modifier = Modifier.testTag("close_settings_button")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "חזרה", tint = SettingsInk)
                }
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(if (showZmanimPreferences) "זמני היום" else "העדפות", fontSize = 25.sp, fontWeight = FontWeight.Bold, color = SettingsInk)
                    Text(if (showZmanimPreferences) "העדפות" else "לימוד יומי", fontSize = 11.sp, color = SettingsMuted)
                }
            }
            HorizontalDivider(color = SettingsLine)
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(pageScroll)
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                if (showZmanimPreferences) {
                    Column(Modifier.testTag("zmanim_settings_page")) {
                        com.example.ui.components.ZmanimSettingsSection(zmanimPreferences,zmanimLocating,zmanimMessage,
                            true,onZmanimChange,onZmanimLocate,standalone = true)
                        SettingsDivider()
                        com.example.ui.components.ZmanimToggle("הצגת תאריך לועזי",zmanimPreferences.showCivilDate,"settings_civil_date") {
                            onZmanimChange(zmanimPreferences.copy(showCivilDate=it))
                        }
                    }
                } else {
                SettingsHeading("השיעורים שלי")
                Text("השיעורים שיופיעו במסך הבית", fontSize = 11.sp, color = SettingsMuted)
                Spacer(Modifier.height(12.dp))
                studies.forEachIndexed { index, (key, name, color) ->
                    if (index > 0) HorizontalDivider(color = SettingsLine.copy(alpha = 0.5f))
                    val checked = key in preferences.visibleStudies
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 52.dp)
                            .toggleable(checked, role = Role.Switch, onValueChange = { onToggleStudy(key) })
                            .testTag("settings_study_$key"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.size(30.dp).background(color, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            StudyLineIcon(key.removePrefix("rambam_"), Modifier.size(23.dp))
                        }
                        Spacer(Modifier.width(11.dp))
                        Text(name, Modifier.weight(1f), fontSize = 14.sp,
                            fontWeight = FontWeight.Normal, color = SettingsInk)
                        SettingsSwitch(checked)
                    }
                }

                SettingsDivider()
                SettingsHeading("הפרקים שלי")
                Text("פרקי תהילים אישיים ומשפחתיים", fontSize = 11.sp, color = SettingsMuted)
                OutlinedButton(onClick = onOpenPersonalPsalms, modifier = Modifier.fillMaxWidth().testTag("settings_personal_psalms")) {
                    Text(if (personalPsalmCount == 0) "הוספת פרקים אישיים" else "ניהול הפרקים שלי · $personalPsalmCount")
                }
                SettingsDivider()
                SettingsHeading("תצוגה וקריאה")
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 64.dp)
                        .toggleable(preferences.tanyaBookView, role = Role.Switch,
                            onValueChange = onTanyaBookViewChange)
                        .testTag("home_tanya_book_view"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("צורת הדף בתניא", fontSize = 14.sp,
                            fontWeight = FontWeight.Normal, color = SettingsInk)
                        Text("הצגת העמוד המקורי של הספר", fontSize = 11.sp, color = SettingsMuted)
                    }
                    SettingsSwitch(preferences.tanyaBookView)
                }
                Spacer(Modifier.height(12.dp))
                Text("גופן הלימוד", fontSize = 13.sp, color = SettingsInk)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FontStyleOption.entries.forEach { option ->
                        val selected = FontStyleOption.fromId(preferences.fontFamily) == option
                        Column(
                            Modifier.weight(1f).height(92.dp).clip(RoundedCornerShape(9.dp))
                                .background(if (selected) SettingsInk else Color.Transparent)
                                .selectable(selected, role = Role.RadioButton,
                                    onClick = { onFontFamilyChange(option.id) })
                                .testTag("settings_font_${option.id}")
                                .padding(horizontal = 2.dp, vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val textColor = if (selected) SettingsPaper else SettingsInk
                            Layout(
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                content = {
                                    Text("אב", fontFamily = option.fontFamily, fontSize = 36.sp,
                                        lineHeight = 44.sp,
                                        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
                                        fontWeight = FontWeight.Normal, color = textColor,
                                        modifier = Modifier.testTag("settings_font_sample_${option.id}"))
                                }
                            ) { measurables, constraints ->
                                val sample = measurables.single().measure(constraints.copy(minWidth = 0, minHeight = 0))
                                layout(constraints.maxWidth, constraints.maxHeight) {
                                    sample.placeRelative((constraints.maxWidth - sample.width) / 2,
                                        constraints.maxHeight - 10.dp.roundToPx() - sample[FirstBaseline])
                                }
                            }
                            Text(option.title, modifier = Modifier.height(18.dp).wrapContentHeight(),
                                fontSize = 9.sp, maxLines = 1,
                                overflow = TextOverflow.Ellipsis, color = textColor)
                        }
                    }
                }
                if (preferences.tanyaBookView) {
                    Spacer(Modifier.height(10.dp))
                    Text("בצורת הדף בתניא נשמר גופן הספר.", fontSize = 10.sp, color = SettingsMuted)
                }

                Spacer(Modifier.height(20.dp))
                SettingsStepControl(
                    title = "גודל טקסט", value = preferences.fontSizeSp.toInt().toString(), tag = "font_size",
                    canDecrease = preferences.fontSizeSp > 15f, canIncrease = preferences.fontSizeSp < 32f,
                    onDecrease = { onFontSizeChange((preferences.fontSizeSp - 1f).coerceIn(15f, 32f)) },
                    onIncrease = { onFontSizeChange((preferences.fontSizeSp + 1f).coerceIn(15f, 32f)) }
                )
                HorizontalDivider(color = SettingsLine.copy(alpha = .5f))
                SettingsStepControl(
                    title = "מרווח שורות", value = String.format(Locale.ROOT, "%.1f", preferences.lineSpacingMultiplier),
                    tag = "line_spacing", canDecrease = preferences.lineSpacingMultiplier > 1.3f,
                    canIncrease = preferences.lineSpacingMultiplier < 2.2f,
                    onDecrease = { onLineSpacingChange((preferences.lineSpacingMultiplier - .1f).coerceIn(1.3f, 2.2f)) },
                    onIncrease = { onLineSpacingChange((preferences.lineSpacingMultiplier + .1f).coerceIn(1.3f, 2.2f)) }
                )
                Spacer(Modifier.height(8.dp))
                Text("ריווח תווים", fontSize = 14.sp, color = SettingsInk)
                Slider(
                    value = preferences.letterSpacingEm.coerceIn(0f, .12f),
                    onValueChange = onLetterSpacingChange, valueRange = 0f..0.12f, steps = 11,
                    colors = SliderDefaults.colors(thumbColor = SettingsInk, activeTrackColor = SettingsInk,
                        inactiveTrackColor = SettingsLine),
                    modifier = Modifier.fillMaxWidth().testTag("settings_letter_spacing")
                )

                Spacer(Modifier.height(12.dp))
                Text("צבע רקע לקריאה", fontSize = 13.sp, color = SettingsInk)
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        Triple("light", "בהיר", Color.White),
                        Triple("sepia", "ספיה", Color(0xFFF7EEDD)),
                        Triple("dark", "אפור", Color(0xFF475569)),
                        Triple("oled", "כהה", Color(0xFF0F172A))
                    ).forEach { (id, label, color) ->
                        val selected = preferences.readerTheme == id
                        Column(
                            Modifier.weight(1f).clip(RoundedCornerShape(9.dp))
                                .border(if (selected) 2.dp else 1.dp,
                                    if (selected) SettingsInk else SettingsLine, RoundedCornerShape(9.dp))
                                .selectable(selected, role = Role.RadioButton, onClick = { onThemeChange(id) })
                                .testTag("settings_theme_$id").padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(Modifier.size(28.dp).background(color, RoundedCornerShape(14.dp))
                                .border(1.dp, SettingsLine, RoundedCornerShape(14.dp)))
                            Spacer(Modifier.height(6.dp))
                            Text(label, fontSize = 11.sp, color = SettingsInk)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                SettingsToggleRow("הצג ניקוד וטעמים", preferences.showNikud, "settings_nikud", onNikudToggle)
                HorizontalDivider(color = SettingsLine.copy(alpha = .5f))
                SettingsToggleRow("השאר מסך דולק", preferences.keepScreenOn, "settings_keep_screen_on", onKeepScreenOnChange)

                SettingsDivider()
                Row(Modifier.fillMaxWidth().heightIn(min = 64.dp)
                    .clickable { showZmanimPreferences = true }.testTag("settings_zmanim"),
                    verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("זמני היום", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = SettingsInk)
                        Text(if (zmanimPreferences.enabled) "${zmanimPreferences.location?.name.orEmpty()} · בחירת זמנים, שיטה ותצוגה" else "זמני היום מוסתרים",
                            fontSize = 12.sp, color = SettingsMuted)
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = SettingsMuted, modifier = Modifier.size(18.dp))
                }
                SettingsDivider()
                SettingsHeading("מועד החלפת היום")
                Spacer(Modifier.height(6.dp))
                Row(
                    Modifier.fillMaxWidth().border(1.dp, Color(0xFFC9C4B9), RoundedCornerShape(11.dp))
                        .padding(3.dp).selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("midnight" to "חצות", "sunset" to "שקיעה").forEach { (value, label) ->
                        val selected = preferences.dayBoundary == value
                        Box(
                            Modifier.weight(1f).heightIn(min = 48.dp).clip(RoundedCornerShape(8.dp))
                                .background(if (selected) SettingsInk else Color.Transparent)
                                .selectable(selected, role = Role.RadioButton,
                                    onClick = { onDayBoundaryChange(value) })
                                .testTag("settings_day_$value")
                                .padding(horizontal = 8.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, fontSize = 14.sp, color = if (selected) SettingsPaper else SettingsInk)
                        }
                    }
                }
                if (preferences.dayBoundary == "sunset") {
                    Spacer(Modifier.height(14.dp))
                    ExposedDropdownMenuBox(
                        expanded = expandedCityDropdown,
                        onExpandedChange = { expandedCityDropdown = it }
                    ) {
                        OutlinedTextField(
                            value = "${selectedCity.nameHebrew} · $sunsetTime",
                            onValueChange = {}, readOnly = true, singleLine = true,
                            label = { Text("עיר לחישוב השקיעה") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedCityDropdown) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SettingsInk, unfocusedBorderColor = SettingsLine,
                                focusedTextColor = SettingsInk, unfocusedTextColor = SettingsInk,
                                focusedLabelColor = SettingsInk, unfocusedLabelColor = SettingsMuted
                            ),
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth().testTag("settings_city")
                        )
                        ExposedDropdownMenu(
                            expanded = expandedCityDropdown,
                            onDismissRequest = { expandedCityDropdown = false }
                        ) {
                            SupportedCities.CITIES.forEach { city ->
                                DropdownMenuItem(text = { Text(city.nameHebrew) }, onClick = {
                                    onCityChange(city.nameHebrew)
                                    expandedCityDropdown = false
                                })
                            }
                        }
                    }
                }

                SettingsDivider()
                SettingsHeading("פרטיות")
                SettingsToggleRow("שיתוף נתוני שימוש לשיפור האפליקציה", measurementEnabled, "settings_measurement", analytics::setEnabled)
                Text("נמדדים פתיחת הנגן, זמן האזנה ותקלות באמצעות Google Analytics. מזהה התקנה מאפשר לזהות שימוש חוזר; לא נשלחים שם, דוא״ל או טקסט הלימוד. אפשר לכבות בכל עת.", fontSize=11.sp, color=SettingsMuted)
                SettingsDivider()
                TextButton(
                    onClick = { showFeedback = true }, modifier = Modifier.fillMaxWidth().testTag("settings_feedback"),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = SettingsInk)
                ) {
                    Icon(Icons.Outlined.Feedback, null, Modifier.size(17.dp))
                    Spacer(Modifier.width(9.dp))
                    Text("שליחת משוב", Modifier.weight(1f), fontSize = 12.sp)
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = SettingsMuted, modifier = Modifier.size(18.dp))
                }
                TextButton(
                    onClick=onRateInStore,modifier=Modifier.fillMaxWidth().testTag("settings_rate_in_store"),
                    contentPadding=PaddingValues(vertical=8.dp),colors=ButtonDefaults.textButtonColors(contentColor=SettingsInk)
                ) {
                    Icon(Icons.Outlined.StarOutline,null,Modifier.size(17.dp))
                    Spacer(Modifier.width(9.dp))
                    Text("דירוג בחנות",Modifier.weight(1f),fontSize=12.sp)
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight,null,tint=SettingsMuted,modifier=Modifier.size(18.dp))
                }
                TextButton(
                    onClick = onOpenAttribution, modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = SettingsInk)
                ) {
                    Icon(Icons.Outlined.Info, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(9.dp))
                    Text("אודות, מקורות ורישוי", Modifier.weight(1f), fontSize = 12.sp)
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null,
                        tint = SettingsMuted, modifier = Modifier.size(18.dp))
                }
                }
            }
        }
    }
}

@Composable
private fun SettingsStepControl(
    title: String,
    value: String,
    tag: String,
    canDecrease: Boolean,
    canIncrease: Boolean,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit
) {
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), fontSize = 14.sp, color = SettingsInk)
        IconButton(onClick = onDecrease, enabled = canDecrease, modifier = Modifier.testTag("settings_${tag}_decrease")) {
            Icon(Icons.Default.Remove, contentDescription = "הקטנת $title", tint = if (canDecrease) SettingsInk else SettingsLine)
        }
        Text(value, Modifier.width(36.dp).testTag("settings_${tag}_value"),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontSize = 14.sp, color = SettingsInk)
        IconButton(onClick = onIncrease, enabled = canIncrease, modifier = Modifier.testTag("settings_${tag}_increase")) {
            Icon(Icons.Default.Add, contentDescription = "הגדלת $title", tint = if (canIncrease) SettingsInk else SettingsLine)
        }
    }
}

@Composable
private fun SettingsToggleRow(title: String, checked: Boolean, tag: String, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp)
            .toggleable(checked, role = Role.Switch, onValueChange = onChange).testTag(tag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, Modifier.weight(1f), fontSize = 14.sp, color = SettingsInk)
        SettingsSwitch(checked)
    }
}

@Composable
private fun SettingsHeading(text: String) {
    Text(text, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = SettingsInk)
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun SettingsDivider() {
    Spacer(Modifier.height(22.dp))
    HorizontalDivider(color = SettingsLine)
    Spacer(Modifier.height(20.dp))
}

@Composable
private fun SettingsSwitch(checked: Boolean) {
    Switch(
        checked = checked, onCheckedChange = null,
        colors = SwitchDefaults.colors(
            checkedThumbColor = SettingsPaper, checkedTrackColor = MaterialTheme.colorScheme.primary,
            checkedBorderColor = Color.Transparent, uncheckedThumbColor = SettingsPaper,
            uncheckedTrackColor = Color(0xFFD6D2C8), uncheckedBorderColor = Color.Transparent
        )
    )
}
