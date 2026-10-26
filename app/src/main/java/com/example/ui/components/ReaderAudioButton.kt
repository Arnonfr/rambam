package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp

/** Playback remains visible even when the controls are hidden or scrolled away. */
@Composable
fun ReaderAudioButton(shown: Boolean, playing: Boolean, loading: Boolean = false,
    accent: Color, readerTheme: String, tag: String, showLabel: String, hideLabel: String,
    onClick: () -> Unit) {
    val dark = readerTheme in setOf("dark", "oled")
    val ink = if (dark) Color(0xFFF4F5F5) else Color(0xFF171714)
    val moving = playing || loading
    IconButton(onClick = onClick,
        modifier = Modifier.size(40.dp).testTag(tag).semantics {
            contentDescription = if (shown) hideLabel else showLabel
            stateDescription = listOf(if (shown) "מוצג" else "מוסתר",
                if (playing) "מנגן" else if (loading) "מכין שמע" else "ללא ניגון").joinToString(" · ")
        }, colors = IconButtonDefaults.iconButtonColors(
            containerColor = if (shown) accent.copy(alpha = if (dark) .20f else .28f) else Color.Transparent,
            contentColor = ink)) {
        if (moving) {
            PlaybackWaveform(ink, Modifier.size(26.dp).testTag("${tag}_playing"), playing)
        }
        if (!moving) Icon(Icons.Default.Headphones, null, Modifier.size(22.dp))
    }
}

@Composable
fun PlaybackWaveform(color: Color, modifier: Modifier = Modifier, playing: Boolean = true) {
    val transition = rememberInfiniteTransition(label = "audioPlayback")
    val phase by transition.animateFloat(0f, 6.283185f,
        infiniteRepeatable(tween(if (playing) 950 else 1500, easing = LinearEasing)), label = "audioWave")
    Canvas(modifier) {
        repeat(5) { index ->
            val amplitude = .3f + .7f * ((kotlin.math.sin(phase + index * 1.3f) + 1f) / 2f)
            val height = size.height * amplitude
            val x = size.width * (index + .5f) / 5f
            drawLine(color, androidx.compose.ui.geometry.Offset(x, (size.height - height) / 2f),
                androidx.compose.ui.geometry.Offset(x, (size.height + height) / 2f),
                strokeWidth = 2.5.dp.toPx(), cap = StrokeCap.Round)
        }
    }
}
