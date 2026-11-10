package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.PlaybackBarState

@Composable
fun GlobalPlaybackBar(state: PlaybackBarState, onToggle: () -> Unit, onStop: () -> Unit) {
    if (!state.visible) return
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, tonalElevation = 6.dp,
        modifier = Modifier.fillMaxWidth().navigationBarsPadding().testTag("global_playback_bar")) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            if (state.loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            else Icon(Icons.Default.Headphones, null, Modifier.size(22.dp))
            Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                Text(state.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelLarge)
                Text(if (state.loading) "טוען…" else if (state.playing) "מתנגן · ${state.subtitle}" else "מושהה · ${state.subtitle}",
                    maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelSmall)
            }
            IconButton(onClick = onToggle, modifier = Modifier.testTag("global_playback_toggle")) {
                Icon(if (state.playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                    if (state.playing) "השהיה" else "המשך ניגון")
            }
            IconButton(onClick = onStop, modifier = Modifier.testTag("global_playback_stop")) {
                Icon(Icons.Default.Close, "עצירת השיעור")
            }
        }
    }
}
