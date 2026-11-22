package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.prayers.audio.PrayerAudioState

/** The prayer reader's audio, in the same language as the lesson player: a card in the prayer colour. */
@Composable
fun PrayerAudioControls(state: PrayerAudioState, theme: String,
    onStart: () -> Unit, onToggle: () -> Unit, onStop: () -> Unit,
    onSeek: (Long) -> Unit, onSpeed: () -> Unit, onSections: () -> Unit,
    modifier: Modifier = Modifier) {
    val dark = theme == "dark" || theme == "oled"
    val bg = when (theme) { "dark", "oled" -> Color(0xFF18191D); "sepia" -> Color(0xFFF5EEDA); else -> Color(0xFFFFFDF8) }
    val colors = playerColors(LessonColors.Prayer, dark)
    Column(modifier.fillMaxWidth().background(bg).padding(horizontal = 8.dp, vertical = 6.dp).testTag("prayer_audio_controls")) {
        if (!state.active) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(50)).background(colors.raised)
                    .clickable(role = Role.Button, onClick = onSections).padding(horizontal = 14.dp)
                    .testTag("prayer_audio_sections"), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FormatListBulleted, null, Modifier.size(16.dp), tint = colors.muted)
                    Spacer(Modifier.width(6.dp))
                    Text("מקטעי האודיו", fontSize = 12.sp, color = colors.ink)
                }
                Row(Modifier.height(40.dp).clip(RoundedCornerShape(50)).background(colors.ink)
                    .clickable(role = Role.Button, onClick = onStart).padding(horizontal = 16.dp)
                    .testTag("prayer_audio_start"), verticalAlignment = Alignment.CenterVertically) {
                    Text("האזנה", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.onInk)
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Default.PlayArrow, null, Modifier.size(18.dp), tint = colors.onInk)
                }
            }
        } else {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(colors.lesson)
                .padding(start = 14.dp, end = 10.dp, top = 8.dp, bottom = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable(role = Role.Button, onClick = onSections),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(state.title.ifBlank { "תפילה ברצף" }, color = colors.onLesson, fontSize = 15.sp,
                            fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false))
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "בחירת מקטע", tint = colors.onLesson)
                    }
                    PlayerSpeedChip(state.speed, onSpeed, colors)
                    IconButton(onClick = onToggle, enabled = !state.loading && !state.finished,
                        modifier = Modifier.size(44.dp).clip(CircleShape).background(Color(0xFF111111)).testTag("prayer_audio_toggle")) {
                        if (state.loading) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        else Icon(if (state.playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (state.playing) "השהיית האודיו" else "המשך האודיו", tint = Color.White)
                    }
                    PlayerCircleButton(Icons.Default.Close, "סגירת האודיו וחזרה לקריאה", Color.White.copy(alpha = .55f),
                        colors.onLesson, onStop, Modifier.testTag("prayer_audio_close"), size = 36)
                }
                androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Slider(value = state.positionMs.toFloat().coerceIn(0f, state.durationMs.coerceAtLeast(1).toFloat()),
                        onValueChange = { onSeek(it.toLong()) }, valueRange = 0f..state.durationMs.coerceAtLeast(1).toFloat(),
                        enabled = state.durationMs > 0 && !state.loading,
                        modifier = Modifier.fillMaxWidth().height(24.dp).testTag("prayer_audio_seek"),
                        colors = SliderDefaults.colors(thumbColor = colors.onLesson, activeTrackColor = colors.onLesson,
                            inactiveTrackColor = colors.onLesson.copy(alpha = .18f)))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(if (state.draft) "אודיו ניסיוני · כיסוי חלקי" else if (state.finished) "הרצף הסתיים" else "מנגן ברצף",
                        color = colors.onLessonMuted, fontSize = 11.sp)
                    Text("⁦${prayerAudioTime(state.positionMs)} / ${prayerAudioTime(state.durationMs)}⁩",
                        color = colors.onLessonMuted, fontSize = 11.sp)
                }
            }
        }
    }
}

fun prayerAudioTime(ms: Long): String = "%d:%02d".format(ms.coerceAtLeast(0) / 60000, ms.coerceAtLeast(0) / 1000 % 60)
