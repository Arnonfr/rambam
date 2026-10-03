package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Velocity
import com.example.data.local.UserPreferences
import com.example.ui.theme.*
import kotlinx.coroutines.delay

private val ReaderBarBrown = Color(0xFF4A3828)

@Stable
class EndOfLessonPullState internal constructor(
    private val thresholdPx: Float,
    private val canScrollForward: () -> Boolean,
    private val onCompleted: () -> Unit,
    private val performHaptic: () -> Unit,
    private val onExitAfterCompletion: () -> Unit = {}
) {
    var pullDistancePx by mutableFloatStateOf(0f)
        private set
    var isConfirmed by mutableStateOf(false)
        private set
    private var settling = false

    val progress: Float
        get() = (pullDistancePx / thresholdPx).coerceIn(0f, 1f)

    val connection = object : NestedScrollConnection {
        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            if (source == NestedScrollSource.UserInput && !canScrollForward() && available.y < 0f && !settling) {
                pullDistancePx = (pullDistancePx + (-available.y * 0.75f)).coerceAtMost(thresholdPx * 1.25f)
                return available
            }
            return Offset.Zero
        }

        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (source == NestedScrollSource.UserInput && available.y > 0f && pullDistancePx > 0f && !settling) {
                pullDistancePx = (pullDistancePx - available.y).coerceAtLeast(0f)
            }
            return Offset.Zero
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            settle()
            return Velocity.Zero
        }

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            settle()
            return Velocity.Zero
        }
    }

    private suspend fun settle() {
        if (settling || pullDistancePx <= 0f) return
        settling = true
        if (progress >= 1f && !isConfirmed) {
            isConfirmed = true
            performHaptic()
            onCompleted()
            delay(350)
            onExitAfterCompletion()
            delay(300)
        }
        pullDistancePx = 0f
        delay(120)
        isConfirmed = false
        settling = false
    }
}

@Composable
fun rememberEndOfLessonPullState(
    key: Any?,
    canScrollForward: () -> Boolean,
    onCompleted: () -> Unit,
    onExitAfterCompletion: () -> Unit = {}
): EndOfLessonPullState {
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val latestOnCompleted by rememberUpdatedState(onCompleted)
    val latestExit by rememberUpdatedState(onExitAfterCompletion)
    val latestCanScrollForward by rememberUpdatedState(canScrollForward)
    return remember(key, density) {
        EndOfLessonPullState(
            thresholdPx = with(density) { 56.dp.toPx() },
            canScrollForward = { latestCanScrollForward() },
            onCompleted = { latestOnCompleted() },
            onExitAfterCompletion = { latestExit() },
            performHaptic = { haptics.performHapticFeedback(HapticFeedbackType.LongPress) }
        )
    }
}

fun Modifier.endOfLessonPull(state: EndOfLessonPullState): Modifier = nestedScroll(state.connection)

@Composable
fun EndOfLessonPullIndicator(
    state: EndOfLessonPullState,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val visible = state.pullDistancePx > 1f || state.isConfirmed
    val checkScale by animateFloatAsState(
        targetValue = if (state.isConfirmed) 1f else 0.55f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 420f),
        label = "lessonCompleteCheckScale"
    )
    AnimatedVisibility(visible = visible, modifier = modifier) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(58.dp)) {
                CircularProgressIndicator(
                    progress = { if (state.isConfirmed) 1f else state.progress },
                    modifier = Modifier.fillMaxSize(),
                    color = accentColor,
                    trackColor = Color.Black.copy(alpha = 0.10f),
                    strokeWidth = 4.dp
                )
                if (state.isConfirmed || state.progress >= 0.78f) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "השיעור הושלם",
                        tint = Color.Black,
                        modifier = Modifier.size(30.dp).scale(checkScale)
                    )
                }
            }
            if (!state.isConfirmed) {
                Text(
                    text = "משכו לסיום",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black.copy(alpha = 0.48f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun CompactReaderHeader(
    title: String,
    location: String,
    accentColor: Color,
    scrollProgress: Float,
    readerTheme: String,
    modifier: Modifier = Modifier
) {
    val surface = when (readerTheme) {
        "dark" -> Color(0xFF18191D)
        "sepia" -> Color(0xFFF5EEDA)
        else -> Color(0xFFFFFDF8)
    }
    val foreground = if (readerTheme == "dark") Color.White else Color.Black
    Surface(
        color = surface.copy(alpha = 0.98f),
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 0.dp,
        shadowElevation = 1.dp
    ) {
        // MainActivity's Scaffold already reserves the system status-bar inset.
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().height(32.dp).padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier.size(20.dp).clip(RoundedCornerShape(3.dp)).background(accentColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.MenuBook, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                }
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = foreground)
                Text("•", fontSize = 12.sp, color = foreground.copy(alpha = 0.42f))
                Text(
                    text = location,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = foreground.copy(alpha = 0.68f),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    lineHeight = 13.sp,
                    modifier = Modifier.weight(1f)
                )
            }
            LinearProgressIndicator(
                progress = { scrollProgress },
                modifier = Modifier.fillMaxWidth().height(2.dp),
                color = accentColor,
                trackColor = foreground.copy(alpha = 0.07f)
            )
        }
    }
}

@Composable
fun FloatingReaderBar(
    currentHalachaText: String,
    hebrewDateText: String,
    scrollProgress: Float,
    onBack: () -> Unit,
    onOpenDatePicker: () -> Unit,
    onTypographyClick: () -> Unit,
    readerTheme: String,
    onNextDay: (() -> Unit)? = null,
    onPrevDay: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    prayerNavigation: Boolean = false,
    onOpenPrayerSections: (() -> Unit)? = null
) {
    val dark = readerTheme == "dark"
    val foreground = if (dark) Color.White else Color.Black
    val surface = if (dark) Color(0xFF1E222A) else Color.White
    val outline = foreground.copy(alpha = .13f)
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Surface(
            modifier = modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            shape = RoundedCornerShape(28.dp),
            color = surface,
            contentColor = foreground,
            border = BorderStroke(1.dp, outline),
            shadowElevation = 3.dp
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (onPrevDay != null) {
                    IconButton(onClick = onPrevDay,
                        modifier = Modifier.size(44.dp).testTag("reader_prev_day_button")) {
                        Icon(Icons.Default.KeyboardArrowRight, if (prayerNavigation) "תפילה קודמת" else "יום קודם", tint = foreground)
                    }
                }
                Surface(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp))
                        .clickable(enabled = !prayerNavigation || onOpenPrayerSections != null,
                            onClick = if (prayerNavigation) ({ onOpenPrayerSections?.invoke(); Unit }) else onOpenDatePicker)
                        .testTag("reader_bottom_date_picker_button"),
                    color = if (dark) Color(0xFF2C3038) else Color(0xFFF3F4F5),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center) {
                        if (!prayerNavigation) {
                            Icon(Icons.Default.CalendarMonth, "מעבר ליום אחר",
                                tint = foreground, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(hebrewDateText, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            color = foreground, maxLines = 1, modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center)
                        if (!prayerNavigation || onOpenPrayerSections != null) Icon(Icons.Default.ArrowDropDown, null,
                            tint = foreground, modifier = Modifier.size(15.dp))
                    }
                }
                if (onNextDay != null) {
                    IconButton(onClick = onNextDay,
                        modifier = Modifier.size(44.dp).testTag("reader_next_day_button")) {
                        Icon(Icons.Default.KeyboardArrowLeft, if (prayerNavigation) "תפילה הבאה" else "יום הבא", tint = foreground)
                    }
                }
                // Last in an RTL row = physical left edge, in every reader.
                Surface(
                    modifier = Modifier.size(44.dp).clip(CircleShape)
                        .clickable(onClick = onTypographyClick)
                        .testTag("reader_typography_button"),
                    shape = CircleShape, color = Color(0xFF73D4ED)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.FormatSize, "התאמת טקסט",
                            tint = Color.Black, modifier = Modifier.size(22.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderTypographySheet(
    preferences: UserPreferences,
    fontSizeSp: Float = preferences.fontSizeSp,
    onDismiss: () -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onLineSpacingChange: (Float) -> Unit,
    onFontFamilyChange: (String) -> Unit,
    onNikudToggle: (Boolean) -> Unit,
    onThemeChange: (String) -> Unit,
    onKeepScreenOnChange: (Boolean) -> Unit,
    extraControls: @Composable () -> Unit = {},
    fixedPrintLayout: Boolean = false
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFFF8F9FA),
        contentColor = Color(0xFF0F172A),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFCBD5E1))
            )
        }
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 36.dp),
                horizontalAlignment = Alignment.Start
            ) {
                // Title
                Text(
                    text = "הגדרות תצוגה",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    textAlign = TextAlign.Center
                )

                extraControls()

                // 1. ערכת נושא (Theme Selection)
                Text(
                    text = "צבע רקע",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    shadowElevation = 1.dp,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 18.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(
                            Triple("light", Color(0xFFFFFFFF), "בהיר"),
                            Triple("sepia", Color(0xFFF7EEDD), "ספיה"),
                            Triple("dark", Color(0xFF475569), "אפור"),
                            Triple("oled", Color(0xFF0F172A), "כהה")
                        ).forEach { (themeId, color, label) ->
                            val isSelected = preferences.readerTheme == themeId
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onThemeChange(themeId) }
                                    .padding(4.dp)
                            ) {
                                ThemeCircleSwatch(
                                    color = color,
                                    isSelected = isSelected,
                                    borderRing = Color(0xFF1E293B),
                                    innerBorder = if (themeId == "light") Color(0xFFE2E8F0) else Color.Transparent,
                                    onClick = { onThemeChange(themeId) }
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    color = if (isSelected) Color(0xFF0F172A) else Color(0xFF64748B),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                // 2. בחירת גופן (Select Font)
                if (!fixedPrintLayout) {
                Text(
                    text = "סוג גופן",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FontStyleOption.entries.forEach { option ->
                        val isSelected = preferences.fontFamily == option.id
                        val label = when (option) {
                            FontStyleOption.SANS -> "מודרני"
                            FontStyleOption.SERIF -> "דפוס תניא"
                            FontStyleOption.LIBERTINUS -> "סריף לטיני"
                            FontStyleOption.BONA_NOVA -> "בונה נובה"
                        }
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) Color(0xFFF1F5F9) else Color.White,
                            shadowElevation = if (isSelected) 1.dp else 0.dp,
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) Color(0xFF0F172A) else Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onFontFamilyChange(option.id) }
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "אב",
                                    fontFamily = option.fontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 18.sp,
                                    color = Color(0xFF0F172A)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color(0xFF0F172A) else Color(0xFF64748B),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // 3. גודל גופן ומרווח (Typography Controls)
                Text(
                    text = "גודל ומרווח",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 18.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        // Font Size Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.FormatSize, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("גודל טקסט", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF0F172A))
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                FilledTonalIconButton(
                                    onClick = { if (fontSizeSp > 15f) onFontSizeChange(fontSizeSp - 1f) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "הקטן", modifier = Modifier.size(16.dp))
                                }
                                Text(
                                    text = "${fontSizeSp.toInt()}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                FilledTonalIconButton(
                                    onClick = { if (fontSizeSp < 32f) onFontSizeChange(fontSizeSp + 1f) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "הגדל", modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        HorizontalDivider(color = Color(0xFFF1F5F9))

                        // Line Spacing Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.FormatLineSpacing, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("מרווח שורות", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF0F172A))
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                FilledTonalIconButton(
                                    onClick = { if (preferences.lineSpacingMultiplier > 1.3f) onLineSpacingChange(preferences.lineSpacingMultiplier - 0.1f) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "הקטן מרווח", modifier = Modifier.size(16.dp))
                                }
                                Text(
                                    text = String.format("%.1f", preferences.lineSpacingMultiplier),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                FilledTonalIconButton(
                                    onClick = { if (preferences.lineSpacingMultiplier < 2.2f) onLineSpacingChange(preferences.lineSpacingMultiplier + 0.1f) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "הגדל מרווח", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                // 4. הגדרות נוספות (Additional Options)
                }
                Text(
                    text = "הגדרות לימוד",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        // Keep Screen On Switch Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text("השאר מסך דולק", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF0F172A))
                                Text("מניעת כיבוי אוטומטי של המסך בזמן הלימוד", fontSize = 11.sp, color = Color(0xFF64748B))
                            }
                            Switch(
                                checked = preferences.keepScreenOn,
                                onCheckedChange = onKeepScreenOnChange,
                                colors = readerSwitchColors()
                            )
                        }

                        HorizontalDivider(color = Color(0xFFF1F5F9))

                        // Nikud Switch Row
                        if (!fixedPrintLayout) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text("הצג ניקוד וטעמים", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF0F172A))
                                Text("הצגת סימני הניקוד וטעמי המקרא בטקסט", fontSize = 11.sp, color = Color(0xFF64748B))
                            }
                            Switch(
                                checked = preferences.showNikud,
                                onCheckedChange = onNikudToggle,
                                colors = readerSwitchColors()
                            )
                        }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun readerSwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = Color.White,
    checkedTrackColor = Color(0xFF20262C),
    checkedBorderColor = Color.Transparent,
    uncheckedThumbColor = Color.White,
    uncheckedTrackColor = Color(0xFF9BA4AC),
    uncheckedBorderColor = Color.Transparent
)

@Composable
private fun ThemeCircleSwatch(
    color: Color,
    isSelected: Boolean,
    borderRing: Color,
    innerBorder: Color = Color.Transparent,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(2.dp, borderRing, CircleShape)
            )
        }
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(color)
                .border(if (innerBorder != Color.Transparent) 1.dp else 0.dp, innerBorder, CircleShape)
        )
    }
}

@Composable
private fun VerticalControlPill(
    valueText: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 1.dp,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier
            .width(46.dp)
            .height(96.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconButton(
                onClick = onIncrement,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF0F172A))
            }
            Text(
                text = valueText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
            IconButton(
                onClick = onDecrement,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF0F172A))
            }
        }
    }
}
