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

/** Media transport keeps the universal direction even in Hebrew: back 10s on the left, ▶ pointing right, time runs left to right. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TanyaPlaybackControls(
    playback: TanyaPlaybackState, onPlay: () -> Unit, onSeek: (Long) -> Unit, onSpeed: () -> Unit,
    ink: Color, paper: Color, accent: Color
) {
    var scrubPosition by remember { mutableStateOf<Float?>(null) }
    val duration = playback.duration.coerceAtLeast(0)
    val position = (scrubPosition?.toLong() ?: playback.position).coerceIn(0, duration.coerceAtLeast(playback.position))
    val canSeek = playback.active && duration > 0
    val fraction = if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(Modifier.fillMaxWidth().padding(top = 10.dp).testTag("tanya_playback")) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { onSeek((playback.position - 10_000).coerceAtLeast(0)) }, enabled = canSeek,
                    modifier = Modifier.size(48.dp), colors = IconButtonDefaults.iconButtonColors(
                        contentColor = ink, disabledContentColor = ink.copy(alpha = .3f))) {
                    Icon(Icons.Default.Replay10, "אחורה 10 שניות", Modifier.size(26.dp))
                }
                IconButton(onClick = onPlay, modifier = Modifier.size(56.dp).clip(CircleShape)
                    .background(accent).testTag("play_tanya_audio")) {
                    if (playback.loading) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp, color = Color(0xFF171714))
                    else Icon(if (playback.playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                        if (playback.playing) "השהיה" else "ניגון שיעור", Modifier.size(30.dp), tint = Color(0xFF171714))
                }
                IconButton(onClick = { onSeek((playback.position + 10_000).coerceAtMost(duration)) }, enabled = canSeek,
                    modifier = Modifier.size(48.dp), colors = IconButtonDefaults.iconButtonColors(
                        contentColor = ink, disabledContentColor = ink.copy(alpha = .3f))) {
                    Icon(Icons.Default.Forward10, "קדימה 10 שניות", Modifier.size(26.dp))
                }
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                    TextButton(onClick = onSpeed, modifier = Modifier.height(36.dp)
                        .border(1.dp, ink.copy(alpha = .18f), RoundedCornerShape(12.dp)).testTag("tanya_audio_speed"),
                        contentPadding = PaddingValues(horizontal = 8.dp), colors = ButtonDefaults.textButtonColors(contentColor = ink)) {
                        Text("${playback.speed.toString().removeSuffix(".0")}×", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Slider(value = if (duration > 0) position.toFloat().coerceIn(0f, duration.toFloat()) else 0f, enabled = canSeek,
                onValueChange = { scrubPosition = it }, onValueChangeFinished = {
                    scrubPosition?.let { onSeek(it.toLong()) }; scrubPosition = null
                }, valueRange = 0f..duration.coerceAtLeast(1).toFloat(),
                modifier = Modifier.fillMaxWidth().height(40.dp).testTag("tanya_audio_timeline")
                    .semantics { contentDescription = "מיקום בהקלטה" },
                thumb = { Box(Modifier.size(14.dp).clip(CircleShape)
                    .background(if (canSeek) ink else ink.copy(alpha = .3f)).border(2.dp, paper, CircleShape)) },
                track = {
                    Canvas(Modifier.fillMaxWidth().height(4.dp)) {
                        drawLine(ink.copy(alpha = .12f), Offset(0f, size.height / 2), Offset(size.width, size.height / 2),
                            strokeWidth = size.height, cap = StrokeCap.Round)
                        if (fraction > 0f) drawLine(accent, Offset(0f, size.height / 2), Offset(size.width * fraction, size.height / 2),
                            strokeWidth = size.height, cap = StrokeCap.Round)
                    }
                })
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(audioTime(position), fontSize = 11.sp, fontWeight = FontWeight.Medium, color = ink,
                    modifier = Modifier.testTag("tanya_audio_elapsed"))
                Text(if (duration > 0) audioTime(duration) else "--:--", fontSize = 11.sp,
                    color = ink.copy(alpha = .65f), modifier = Modifier.testTag("tanya_audio_duration"))
            }
        }
    }
}

private fun audioTime(ms: Long) = "${ms / 60000}:${(ms / 1000 % 60).toString().padStart(2, '0')}"
