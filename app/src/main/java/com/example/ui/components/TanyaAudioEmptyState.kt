package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** No recording: a lesson card with one main action, and the source as the second. */
@Composable
internal fun TanyaAudioEmptyState(
    ink: Color, paper: Color, accent: Color,
    onPrimary: () -> Unit, onSource: () -> Unit,
    primaryLabel: String = "ניסיון נוסף",
    title: String = "עדיין אין הקלטה לפרק הזה",
    detail: String = "אפשר לבחור רב אחר או להמשיך בלימוד"
) {
    val colors = playerColors(accent, dark = ink.luminance() > .5f)
    PlayerLessonCard(colors, "", title, modifier = Modifier.testTag("tanya_audio_empty")) {
        Text(detail, color = colors.onLessonMuted, fontSize = 13.sp, lineHeight = 19.sp, modifier = Modifier.padding(top = 4.dp))
        Spacer(Modifier.height(14.dp))
        TanyaAudioActions(primaryLabel, onPrimary, ink, accent, "למקור השיעור", onSource)
    }
}

/** Separate actions: neither implies a selected playback mode. */
@Composable
internal fun TanyaAudioActions(
    primaryLabel: String, onPrimary: () -> Unit, ink: Color, accent: Color,
    secondaryLabel: String? = null, onSecondary: () -> Unit = {}
) {
    val colors = playerColors(accent, dark = ink.luminance() > .5f)
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).testTag("tanya_audio_actions"),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically) {
        // On the lesson card the main action is always black, in both reader themes.
        PlayerPrimaryButton(primaryLabel, onPrimary, colors.copy(ink = Color(0xFF111111), onInk = Color.White),
            Modifier.weight(1f, fill = false).fillMaxHeight().testTag("tanya_audio_primary"),
            icon = if (primaryLabel.startsWith("בחירת") || primaryLabel.startsWith("השיעורים")) Icons.Default.FormatListBulleted else null)
        if (secondaryLabel != null) PlayerSecondaryButton(secondaryLabel, onSecondary, colors,
            Modifier.weight(1f, fill = false).fillMaxHeight().testTag("tanya_audio_source"), icon = Icons.Default.OpenInNew)
    }
}
