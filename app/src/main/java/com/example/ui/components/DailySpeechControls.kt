package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.speech.*
import com.example.data.tanya.TanyaPlaybackState

@Composable
fun DailySpeechControls(state: DailySpeechState, dateLabel: String, track: String,
    onStart: () -> Unit, onAction: (String) -> Unit, onSeek: (Long) -> Unit = {},
    source: String = "recording", onSourceChange: ((String) -> Unit)? = null) {
    val colors = playerColors(LessonColors.Rambam)
    var restart by remember { mutableStateOf(false) }
    val scope = if (track == "three") "שלושה פרקים ברצף" else "פרק אחד"
    Column(Modifier.fillMaxWidth().testTag("daily_speech_controls")) {
        PlayerHeader("האזנה", "רמב״ם היומי", colors)
        PlayerLessonCard(colors,
            eyebrow = "$dateLabel · $scope",
            title = if (state.active) state.chapter else if (track == "three") "שלושת הפרקים של היום" else "הפרק של היום",
            subtitle = if (track == "three") "ר׳ אליהו פרץ" else "הרב נדב כהן",
            corner = { PlayerSpeedChip(state.speed, { onAction(DailySpeechStatus.SPEED) }, colors, Modifier.testTag("tanya_audio_speed")) }) {
            TanyaPlaybackControls(TanyaPlaybackState(active = state.active, playing = state.playing,
                loading = state.preparing && !state.paused, position = state.position, duration = state.duration,
                speed = state.speed, error = state.error),
                onPlay = { if (!state.active) { if (state.preparing) onAction(DailySpeechStatus.STOP) else onStart() } else onAction(DailySpeechStatus.TOGGLE) },
                onSeek = onSeek, ink = colors.onLesson, paper = Color.White, accent = colors.lesson)
            state.error?.let {
                Text(it, color = colors.onLesson, fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp).testTag("daily_speech_error"))
            }
        }
        if (state.active) {
            // Same direction as the transport: previous on the left, next on the right; "from the start" between them.
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    if (!state.recorded) HalachaButton("הלכה קודמת", Icons.Default.SkipPrevious, state.canPrevious, colors,
                        Modifier.weight(1f).testTag("daily_speech_previous"), iconFirst = true) { onAction(DailySpeechStatus.PREVIOUS) }
                    if (state.recorded) HalachaButton("מהתחלה", Icons.Default.Replay, true, colors,
                        Modifier.fillMaxWidth().testTag("daily_speech_restart"), iconFirst = true) { restart = true }
                    else PlayerCircleButton(Icons.Default.Replay, "מהתחלה", colors.raised, colors.ink, { restart = true },
                        Modifier.testTag("daily_speech_restart"))
                    if (!state.recorded) HalachaButton("הלכה הבאה", Icons.Default.SkipNext, state.canNext, colors,
                        Modifier.weight(1f).testTag("daily_speech_next"), iconFirst = false) { onAction(DailySpeechStatus.NEXT) }
                }
            }
        }
    }
    if (restart) AlertDialog(onDismissRequest = { restart = false }, title = { Text("להתחיל את השיעור מחדש?") },
        text = { Text("ההקראה תחזור לתחילת הרמב״ם היומי.") },
        confirmButton = { TextButton(onClick = { restart = false; onAction("daily_speech.restart") }) { Text("מהתחלה") } },
        dismissButton = { TextButton(onClick = { restart = false }) { Text("ביטול") } })
}

@Composable
private fun HalachaButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, enabled: Boolean,
    colors: PlayerColors, modifier: Modifier, iconFirst: Boolean, onClick: () -> Unit) {
    val tint = if (enabled) colors.ink else colors.ink.copy(alpha = .35f)
    Surface(onClick = onClick, enabled = enabled, shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
        color = colors.raised, modifier = modifier.heightIn(min = 44.dp)) {
        // Hebrew labels inside an LTR row: keep each label right-to-left.
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(Modifier.padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center) {
                if (iconFirst) Icon(icon, null, Modifier.size(18.dp), tint = tint)
                Text(label, fontSize = 12.sp, color = tint, modifier = Modifier.padding(horizontal = 4.dp), maxLines = 1)
                if (!iconFirst) Icon(icon, null, Modifier.size(18.dp), tint = tint)
            }
        }
    }
}
