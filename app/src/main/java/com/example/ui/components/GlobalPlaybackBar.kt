package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.PlaybackBarState

@Composable
internal fun BoxScope.FloatingPlaybackOverlay(
    state: PlaybackBarState,
    onToggle: () -> Unit,
    onStop: () -> Unit,
    extraBottomClearance: Dp = 0.dp
) {
    if (!state.visible) return
    // The navigation stays in the screen's layout. Leave room above its floating bar.
    Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 72.dp + extraBottomClearance)) {
        GlobalPlaybackBar(state, onToggle, onStop)
    }
}

/** The mini player: a floating pill, as the reference's bottom bar, with the lesson colour on the play button. */
@Composable
fun GlobalPlaybackBar(state: PlaybackBarState, onToggle: () -> Unit, onStop: () -> Unit) {
    if (!state.visible) return
    val lesson = LessonColors.forUsage(state.usageType)
    val colors = playerColors(lesson ?: Color(0xFF111111))
    val onPlay = if (lesson != null) Color(0xFF111111) else colors.onInk
    Surface(color = colors.sheet, shape = RoundedCornerShape(50), shadowElevation = 8.dp,
        border = BorderStroke(1.dp, colors.outline),
        modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("global_playback_bar")) {
        Row(Modifier.padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            IconButton(onClick = onToggle, modifier = Modifier.size(48.dp).clip(CircleShape).background(colors.lesson)
                .testTag("global_playback_toggle")) {
                if (state.loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = onPlay)
                else Icon(if (state.playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                    if (state.playing) "השהיה" else "המשך ניגון", tint = onPlay)
            }
            Column(Modifier.weight(1f)) {
                Text(state.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 14.sp,
                    fontWeight = FontWeight.Medium, color = colors.ink)
                val time = if (state.duration > 0) " · ⁦${audioTime(state.position)} / ${audioTime(state.duration)}⁩" else ""
                Text(if (state.loading) "טוען…" else if (state.playing) "${state.subtitle}$time" else "מושהה · ${state.subtitle}",
                    maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 11.sp, color = colors.muted)
                if (state.duration > 0) CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    LinearProgressIndicator(progress = { (state.position.toFloat() / state.duration).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().padding(top = 5.dp).height(3.dp).clip(RoundedCornerShape(50)),
                        color = lesson ?: colors.ink, trackColor = colors.track, drawStopIndicator = {})
                }
            }
            PlayerCircleButton(Icons.Default.Close, "עצירת השיעור", colors.track, colors.ink, onStop,
                Modifier.testTag("global_playback_stop"), size = 40)
        }
    }
}
