package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.UserPreferences
import com.example.ui.theme.*

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
    modifier: Modifier = Modifier
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Surface(
            modifier = modifier
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .shadow(12.dp, RoundedCornerShape(28.dp))
                .clip(RoundedCornerShape(28.dp)),
            color = when (readerTheme) {
                "dark" -> Color(0xFF1E222A)
                "sepia" -> Color(0xFF4A3828)
                else -> DeepNavy
            },
            contentColor = Color.White
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Scroll progress indicator along top edge
                LinearProgressIndicator(
                    progress = { scrollProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = GoldAccent,
                    trackColor = Color.White.copy(alpha = 0.15f)
                )

                Row(
                    modifier = Modifier
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Back Button (חץ חזרה) on the bar, pointing right (RTL Back)
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("reader_bottom_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "חזרה למסך הבית",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Middle Section with Day Navigation Arrows and Hebrew Date
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (onPrevDay != null) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.20f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .clickable(onClick = onPrevDay)
                                    .testTag("reader_prev_day_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowRight,
                                        contentDescription = "יום קודם",
                                        tint = Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        // Hebrew Date Button
                        if (hebrewDateText.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White.copy(alpha = 0.18f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable(onClick = onOpenDatePicker)
                                    .testTag("reader_bottom_date_picker_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = "מעבר ליום אחר",
                                        tint = GoldWarm,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = hebrewDateText,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.8f),
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }

                        // Specific Halacha Indicator Capsule (only if provided and non-empty)
                        if (currentHalachaText.isNotBlank()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color.White.copy(alpha = 0.10f)
                            ) {
                                Text(
                                    text = currentHalachaText,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White.copy(alpha = 0.9f),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        if (onNextDay != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.20f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .clickable(onClick = onNextDay)
                                    .testTag("reader_next_day_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowLeft,
                                        contentDescription = "יום הבא",
                                        tint = Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Typography settings button (TT)
                    IconButton(
                        onClick = onTypographyClick,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("reader_typography_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatSize,
                            contentDescription = "הגדרות גופן ולימוד",
                            tint = GoldWarm,
                            modifier = Modifier.size(20.dp)
                        )
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
    onDismiss: () -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onLineSpacingChange: (Float) -> Unit,
    onFontFamilyChange: (String) -> Unit,
    onNikudToggle: (Boolean) -> Unit,
    onThemeChange: (String) -> Unit,
    onKeepScreenOnChange: (Boolean) -> Unit
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
                            FontStyleOption.SERIF -> "תורני"
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
                                    text = "Aa",
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
                                    onClick = { if (preferences.fontSizeSp > 15f) onFontSizeChange(preferences.fontSizeSp - 1f) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "הקטן", modifier = Modifier.size(16.dp))
                                }
                                Text(
                                    text = "${preferences.fontSizeSp.toInt()}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                FilledTonalIconButton(
                                    onClick = { if (preferences.fontSizeSp < 32f) onFontSizeChange(preferences.fontSizeSp + 1f) },
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
                                onCheckedChange = onKeepScreenOnChange
                            )
                        }

                        HorizontalDivider(color = Color(0xFFF1F5F9))

                        // Nikud Switch Row
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
                                onCheckedChange = onNikudToggle
                            )
                        }
                    }
                }
            }
        }
    }
}

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
