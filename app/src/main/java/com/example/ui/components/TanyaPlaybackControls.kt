package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.tanya.TanyaPlaybackState

/**
 * Timeline and transport, drawn on the lesson card: [ink] is the card's text colour, [accent] the card itself.
 * Media transport keeps the universal direction even in Hebrew: back 10s on the left, ▶ pointing right,
 * time runs left to right.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TanyaPlaybackControls(
    playback: TanyaPlaybackState, onPlay: () -> Unit, onSeek: (Long) -> Unit,
    ink: Color, paper: Color, accent: Color
) {
    var scrubPosition by remember { mutableStateOf<Float?>(null) }
    val duration = playback.duration.coerceAtLeast(0)
    val position = (scrubPosition?.toLong() ?: playback.position).coerceIn(0, duration.coerceAtLeast(playback.position))
    val canSeek = playback.active && duration > 0
    val fraction = if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(Modifier.fillMaxWidth().padding(top = 8.dp).testTag("tanya_playback")) {
            Slider(value = if (duration > 0) position.toFloat().coerceIn(0f, duration.toFloat()) else 0f, enabled = canSeek,
                onValueChange = { scrubPosition = it }, onValueChangeFinished = {
                    scrubPosition?.let { onSeek(it.toLong()) }; scrubPosition = null
                }, valueRange = 0f..duration.coerceAtLeast(1).toFloat(),
                modifier = Modifier.fillMaxWidth().height(32.dp).testTag("tanya_audio_timeline")
                    .semantics { contentDescription = "מיקום בהקלטה" },
                thumb = { Box(Modifier.size(14.dp).clip(CircleShape)
                    .background(if (canSeek) ink else ink.copy(alpha = .3f)).border(3.dp, accent, CircleShape)) },
                track = {
                    Canvas(Modifier.fillMaxWidth().height(4.dp)) {
                        drawLine(ink.copy(alpha = .18f), Offset(0f, size.height / 2), Offset(size.width, size.height / 2),
                            strokeWidth = size.height, cap = StrokeCap.Round)
                        if (fraction > 0f) drawLine(ink, Offset(0f, size.height / 2), Offset(size.width * fraction, size.height / 2),
                            strokeWidth = size.height, cap = StrokeCap.Round)
                    }
                })
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(audioTime(position), fontSize = 11.sp, fontWeight = FontWeight.Medium, color = ink,
                    modifier = Modifier.testTag("tanya_audio_elapsed"))
                Text(if (duration > 0) audioTime(duration) else "--:--", fontSize = 11.sp,
                    color = ink.copy(alpha = .66f), modifier = Modifier.testTag("tanya_audio_duration"))
            }
            // The white pill with the black play button, as in the reference's "Start ▶".
            Row(Modifier.fillMaxWidth().padding(top = 10.dp).height(64.dp).clip(RoundedCornerShape(50)).background(paper)
                .padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                SkipButton(Icons.Default.Replay10, "אחורה 10 שניות", "אחורה", canSeek, ink) {
                    onSeek((playback.position - 10_000).coerceAtLeast(0))
                }
                IconButton(onClick = onPlay, modifier = Modifier.size(52.dp).clip(CircleShape)
                    .background(Color(0xFF111111)).testTag("play_tanya_audio")) {
                    if (playback.loading) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = Color.White)
                    else Icon(if (playback.playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                        if (playback.playing) "השהיה" else "ניגון שיעור", Modifier.size(28.dp), tint = Color.White)
                }
                SkipButton(Icons.Default.Forward10, "קדימה 10 שניות", "קדימה", canSeek, ink) {
                    onSeek((playback.position + 10_000).coerceAtMost(duration))
                }
            }
        }
    }
}

@Composable
private fun SkipButton(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, label: String,
    enabled: Boolean, ink: Color, onClick: () -> Unit) {
    val tint = if (enabled) ink else ink.copy(alpha = .3f)
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(width = 64.dp, height = 56.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, description, Modifier.size(24.dp), tint = tint)
            Text(label, fontSize = 9.sp, color = tint.copy(alpha = if (enabled) .66f else .3f))
        }
    }
}

internal fun audioTime(ms: Long) = "${ms / 60000}:${(ms / 1000 % 60).toString().padStart(2, '0')}"
