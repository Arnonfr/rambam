package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.prayers.audio.PrayerAudioState

@Composable
fun PrayerAudioControls(state: PrayerAudioState, theme: String,
    onStart: () -> Unit, onToggle: () -> Unit, onStop: () -> Unit,
    onSeek: (Long) -> Unit, onSpeed: () -> Unit, onSections: () -> Unit,
    modifier: Modifier = Modifier) {
    val dark = theme == "dark"
    val fg = if (dark) Color.White else Color.Black
    val bg = when (theme) { "dark" -> Color(0xFF18191D); "sepia" -> Color(0xFFF5EEDA); else -> Color(0xFFFFFDF8) }
    val aqua = Color(0xFF70D1E1)
    Column(modifier.fillMaxWidth().background(bg).testTag("prayer_audio_controls")) {
        if (!state.active) {
            Row(Modifier.fillMaxWidth().height(44.dp).padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = onSections, modifier = Modifier.testTag("prayer_audio_sections")) {
                    Text("אודיו · מקטעים", fontSize = 11.sp, color = fg.copy(alpha = .6f))
                }
                TextButton(onClick = onStart, modifier = Modifier.testTag("prayer_audio_start")) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = fg, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp)); Text("האזנה", color = fg, fontSize = 13.sp)
                }
            }
        } else {
            Column(Modifier.fillMaxWidth().background(aqua).padding(horizontal = 12.dp, vertical = 4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onSections, modifier = Modifier.weight(1f)) {
                        Text(state.title.ifBlank { "תפילה ברצף" }, color = Color.Black, maxLines = 1)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "בחירת מקטע", tint = Color.Black)
                    }
                    TextButton(onClick = onSpeed) { Text("${state.speed}×", color = Color.Black, fontSize = 12.sp) }
                    IconButton(onClick = onToggle, enabled = !state.loading && !state.finished,
                        modifier = Modifier.background(Color.Black, RoundedCornerShape(24.dp)).testTag("prayer_audio_toggle")) {
                        if (state.loading) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        else Icon(if (state.playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (state.playing) "השהיית האודיו" else "המשך האודיו", tint = Color.White)
                    }
                    IconButton(onClick = onStop, modifier = Modifier.testTag("prayer_audio_close")) {
                        Icon(Icons.Default.Close, "סגירת האודיו וחזרה לקריאה", tint = Color.Black)
                    }
                }
                androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Slider(value = state.positionMs.toFloat().coerceIn(0f, state.durationMs.coerceAtLeast(1).toFloat()),
                        onValueChange = { onSeek(it.toLong()) }, valueRange = 0f..state.durationMs.coerceAtLeast(1).toFloat(),
                        enabled = state.durationMs > 0 && !state.loading,
                        modifier = Modifier.fillMaxWidth().height(24.dp).testTag("prayer_audio_seek"),
                        colors = SliderDefaults.colors(thumbColor = Color.Black, activeTrackColor = Color.Black,
                            inactiveTrackColor = Color.Black.copy(alpha = .18f)))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(if (state.draft) "אודיו ניסיוני · כיסוי חלקי" else if (state.finished) "הרצף הסתיים" else "מנגן ברצף",
                        color = Color.Black, fontSize = 10.sp)
                    Text("${prayerAudioTime(state.positionMs)} / ${prayerAudioTime(state.durationMs)}", color = Color.Black, fontSize = 10.sp)
                }
            }
        }
    }
}

fun prayerAudioTime(ms: Long): String = "%d:%02d".format(ms.coerceAtLeast(0) / 60000, ms.coerceAtLeast(0) / 1000 % 60)
