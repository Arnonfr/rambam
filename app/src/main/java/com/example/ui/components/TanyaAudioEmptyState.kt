package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun TanyaAudioEmptyState(
    ink: Color, paper: Color, accent: Color,
    onPrimary: () -> Unit, onSource: () -> Unit,
    primaryLabel: String = "ניסיון נוסף",
    title: String = "עדיין אין הקלטה לפרק הזה",
    detail: String = "אפשר לבחור רב אחר או להמשיך בלימוד"
) {
    Column(Modifier.fillMaxWidth().testTag("tanya_audio_empty"),
        horizontalAlignment = Alignment.CenterHorizontally) {
        BookHeadphonesIllustration(ink, paper, accent)
        Text(title, color = ink, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 20.sp,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Text(detail, color = ink.copy(alpha = .75f), fontSize = 12.sp, lineHeight = 17.sp,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 2.dp))
        Spacer(Modifier.height(10.dp))
        TanyaAudioActions(primaryLabel, onPrimary, ink, accent, "למקור השיעור", onSource)
    }
}

/** Separate secondary actions: neither implies a selected playback mode. */
@Composable
internal fun TanyaAudioActions(
    primaryLabel: String, onPrimary: () -> Unit, ink: Color, accent: Color,
    secondaryLabel: String? = null, onSecondary: () -> Unit = {}
) {
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).testTag("tanya_audio_actions"),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(onClick = onPrimary,
            modifier = Modifier.weight(1f, fill = false).fillMaxHeight().heightIn(min = 48.dp).testTag("tanya_audio_primary"),
            shape = RoundedCornerShape(12.dp), border = androidx.compose.foundation.BorderStroke(1.dp, ink.copy(alpha = .25f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = ink),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
            Text(primaryLabel, fontSize = 12.sp, lineHeight = 17.sp, textAlign = TextAlign.Center)
        }
        if (secondaryLabel != null) OutlinedButton(onClick = onSecondary,
            modifier = Modifier.weight(1f, fill = false).fillMaxHeight().heightIn(min = 48.dp).testTag("tanya_audio_source"),
            shape = RoundedCornerShape(12.dp), border = androidx.compose.foundation.BorderStroke(1.dp, ink.copy(alpha = .25f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = ink),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
            Text(secondaryLabel, fontSize = 12.sp, lineHeight = 17.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun BookHeadphonesIllustration(ink: Color, paper: Color, accent: Color) {
    // Vector drawing stays crisp and adapts to dark mode; decorative, without redundant talkback text.
    Canvas(Modifier.size(width = 88.dp, height = 62.dp)) {
        withTransform({ scale(size.width / 120f, size.height / 88f, pivot = Offset.Zero) }) {
            val outline = Stroke(width = 2.6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            drawCircle(accent.copy(alpha = .3f), 35f, Offset(66f, 42f))
            val book = Path().apply {
                moveTo(25f, 41f); cubicTo(37f, 36f, 50f, 38f, 60f, 44f)
                cubicTo(70f, 38f, 83f, 36f, 95f, 41f)
                lineTo(98f, 78f); cubicTo(83f, 74f, 70f, 75f, 60f, 81f)
                cubicTo(50f, 75f, 37f, 74f, 22f, 78f); close()
            }
            drawPath(book, paper); drawPath(book, ink, style = outline)
            drawLine(ink, Offset(60f, 44f), Offset(60f, 80f), strokeWidth = 2.6f)
            for (y in listOf(50f, 59f, 68f)) {
                drawLine(ink, Offset(33f, y), Offset(49f, y + 2f), strokeWidth = 2.2f, cap = StrokeCap.Round)
                drawLine(ink, Offset(71f, y + 2f), Offset(87f, y), strokeWidth = 2.2f, cap = StrokeCap.Round)
            }
            val band = Path().apply {
                moveTo(19f, 53f); lineTo(19f, 39f)
                cubicTo(19f, -5f, 101f, -5f, 101f, 39f); lineTo(101f, 53f)
            }
            drawPath(band, ink, style = outline)
            drawRoundRect(accent, Offset(14f, 43f), Size(12f, 25f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(5f))
            drawRoundRect(ink, Offset(14f, 43f), Size(12f, 25f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(5f), style = outline)
            drawRoundRect(accent, Offset(94f, 43f), Size(12f, 25f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(5f))
            drawRoundRect(ink, Offset(94f, 43f), Size(12f, 25f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(5f), style = outline)
            drawCircle(Color(0xFF6BD1E1), 3f, Offset(7f, 32f), style = Stroke(2f))
            drawLine(Color(0xFFF3C64E), Offset(111f, 18f), Offset(111f, 26f), strokeWidth = 2f, cap = StrokeCap.Round)
            drawLine(Color(0xFFF3C64E), Offset(107f, 22f), Offset(115f, 22f), strokeWidth = 2f, cap = StrokeCap.Round)
        }
    }
}
