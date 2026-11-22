package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * One visual language for every lesson player: a light sheet, the lesson's own colour
 * for what is playing, a white pill holding a black play button, round grey secondary
 * buttons. Text stays regular or medium weight.
 */
@Immutable
data class PlayerColors(
    val sheet: Color, val raised: Color, val track: Color, val muted: Color,
    val ink: Color, val onInk: Color, val outline: Color,
    /** The lesson's colour, and the ink used on top of it. */
    val lesson: Color, val onLesson: Color = Color(0xFF111111), val onLessonMuted: Color = Color(0xFF111111).copy(alpha = .66f)
)

@Composable
fun playerColors(lesson: Color, dark: Boolean = MaterialTheme.colorScheme.background.luminance() < .3f) =
    if (dark) PlayerColors(sheet = Color(0xFF1E222A), raised = Color(0xFF2A2F38), track = Color(0xFF363C46),
        muted = Color(0xFFA9AEB6), ink = Color(0xFFF4F5F5), onInk = Color(0xFF111111), outline = Color(0xFF363C46), lesson = lesson)
    else PlayerColors(sheet = Color.White, raised = Color(0xFFF4F2EE), track = Color(0xFFE8E8E8),
        muted = Color(0xFF7A746C), ink = Color(0xFF111111), onInk = Color.White, outline = Color(0xFFEEE8DE), lesson = lesson)

object LessonColors {
    val Rambam = Color(0xFFFF873F)
    val Tanya = Color(0xFFF0A0DD)
    val Prayer = Color(0xFF70D1E1)
    val Podcast = Color(0xFF9FDEEF)

    fun forUsage(type: String?): Color? = when {
        type == null -> null
        type.startsWith("rambam") || type == "daily_speech" -> Rambam
        type == "tanya" -> Tanya
        type == "prayer" -> Prayer
        type == "podcast" -> Podcast
        else -> null
    }
}

/** The reader overlay supplies its close action here, so each player draws it in its own header. */
val LocalPlayerClose = staticCompositionLocalOf<(() -> Unit)?> { null }

@Composable
fun PlayerHeader(eyebrow: String, title: String, colors: PlayerColors, onBack: (() -> Unit)? = null,
    backLabel: String = "חזרה") {
    val onClose = LocalPlayerClose.current
    Row(Modifier.fillMaxWidth().padding(bottom = 14.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        // Auto-mirrored: "back" points to the start, which is the right in Hebrew.
        if (onBack != null) PlayerCircleButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft, backLabel,
            colors.track, colors.ink, onBack, Modifier.testTag("player_back"))
        Column(Modifier.weight(1f)) {
            if (eyebrow.isNotBlank()) Text(eyebrow, fontSize = 12.sp, color = colors.muted, maxLines = 1,
                overflow = TextOverflow.Ellipsis)
            Text(title, fontSize = 21.sp, lineHeight = 26.sp, fontWeight = FontWeight.Medium, color = colors.ink)
        }
        if (onClose != null) PlayerCircleButton(Icons.Default.Close, "סגירת חלונית השמע", colors.track, colors.ink, onClose)
    }
}

@Composable
fun PlayerCircleButton(icon: ImageVector, label: String, container: Color, content: Color, onClick: () -> Unit,
    modifier: Modifier = Modifier, size: Int = 44, enabled: Boolean = true) {
    Box(modifier.size(size.dp).clip(CircleShape).background(container)
        .clickable(enabled = enabled, role = Role.Button, onClickLabel = label, onClick = onClick)
        .semantics { contentDescription = label }, contentAlignment = Alignment.Center) {
        Icon(icon, null, Modifier.size((size * .5f).dp), tint = if (enabled) content else content.copy(alpha = .35f))
    }
}

/** Segmented choice, as the reference's Day / Week switch: the chosen option is a white pill. */
@Composable
fun PlayerSegmented(options: List<Pair<String, String>>, selected: String, colors: PlayerColors,
    onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().clip(RoundedCornerShape(50)).background(colors.track).padding(4.dp).selectableGroup()) {
        options.forEach { (key, label) ->
            val active = key == selected
            Box(Modifier.weight(1f).heightIn(min = 44.dp)
                .then(if (active) Modifier.shadow(1.dp, RoundedCornerShape(50)) else Modifier)
                .clip(RoundedCornerShape(50)).background(if (active) colors.sheet else Color.Transparent)
                .selectable(active, role = Role.RadioButton, onClick = { onSelect(key) }).padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center) {
                Text(label, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    fontWeight = if (active) FontWeight.Medium else FontWeight.Normal,
                    color = if (active) colors.ink else colors.muted)
            }
        }
    }
}

/** The lesson card: what plays now, in the lesson's colour. [corner] sits in the top end corner. */
@Composable
fun PlayerLessonCard(colors: PlayerColors, eyebrow: String, title: String, subtitle: String = "",
    modifier: Modifier = Modifier, corner: (@Composable () -> Unit)? = null, content: @Composable ColumnScope.() -> Unit = {}) {
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(colors.lesson).padding(16.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                if (eyebrow.isNotBlank()) Text(eyebrow, fontSize = 12.sp, color = colors.onLessonMuted)
                Text(title, fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium, color = colors.onLesson)
                if (subtitle.isNotBlank()) Text(subtitle, fontSize = 13.sp, color = colors.onLessonMuted)
            }
            corner?.invoke()
        }
        content()
    }
}

/** Small speed chip: icon and "1×". The spoken label carries the word. */
@Composable
fun PlayerSpeedChip(speed: Float, onClick: () -> Unit, colors: PlayerColors, modifier: Modifier = Modifier) {
    val label = "${speed.toString().removeSuffix(".0")}×"
    Row(modifier.padding(start = 8.dp).height(24.dp).clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = .5f))
        .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 7.dp)
        .semantics(mergeDescendants = true) { contentDescription = "מהירות $label" },
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        Icon(Icons.Default.Speed, null, Modifier.size(13.dp), tint = colors.onLesson)
        Text("⁦$label⁩", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = colors.onLesson)
    }
}

@Composable
fun PlayerSectionHeader(title: String, colors: PlayerColors, actionLabel: String? = null, onAction: () -> Unit = {},
    actionTag: String = "") {
    Row(Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 10.dp, start = 4.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colors.ink, modifier = Modifier.weight(1f))
        if (actionLabel != null) Row(Modifier.clip(RoundedCornerShape(50)).background(colors.raised)
            .clickable(role = Role.Button, onClick = onAction).padding(horizontal = 12.dp, vertical = 7.dp)
            .then(if (actionTag.isNotBlank()) Modifier.testTag(actionTag) else Modifier),
            verticalAlignment = Alignment.CenterVertically) {
            Text(actionLabel, fontSize = 12.sp, color = colors.ink)
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, Modifier.size(16.dp), tint = colors.ink)
        }
    }
}

enum class LessonRowAction { PLAY, PAUSE, EXTERNAL }

/** One lesson in a list: a round play button, the name, one short line. Playing rows take the lesson colour. */
@Composable
fun PlayerLessonRow(title: String, meta: String, colors: PlayerColors, action: LessonRowAction, onClick: () -> Unit,
    modifier: Modifier = Modifier, highlighted: Boolean = false, badge: String? = null, selected: Boolean = highlighted) {
    val fill = if (highlighted) colors.lesson else colors.raised
    val text = if (highlighted) colors.onLesson else colors.ink
    Row(modifier.fillMaxWidth().padding(bottom = 8.dp).clip(RoundedCornerShape(24.dp)).background(fill)
        .selectable(selected, role = Role.Button, onClick = onClick).padding(start = 10.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        val external = action == LessonRowAction.EXTERNAL
        Box(Modifier.size(40.dp).clip(CircleShape).background(when {
            external -> colors.track; highlighted -> colors.onLesson; else -> colors.ink }), contentAlignment = Alignment.Center) {
            Icon(when (action) {
                LessonRowAction.PLAY -> Icons.Default.PlayArrow
                LessonRowAction.PAUSE -> Icons.Default.Pause
                LessonRowAction.EXTERNAL -> Icons.Default.OpenInNew
            }, null, Modifier.size(22.dp), tint = when {
                external -> colors.ink; highlighted -> colors.lesson; else -> colors.onInk })
        }
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium, color = text)
            if (meta.isNotBlank()) Text(meta, fontSize = 12.sp, lineHeight = 16.sp,
                color = if (highlighted) colors.onLessonMuted else colors.muted)
        }
        if (badge != null) Text(badge, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = colors.onLesson,
            modifier = Modifier.clip(RoundedCornerShape(50)).background(if (highlighted) Color.White.copy(alpha = .5f) else colors.lesson)
                .padding(horizontal = 8.dp, vertical = 3.dp))
    }
}

/** Round chips for chapters, as the reference's weekday circles. The chosen one is black. */
@Composable
fun PlayerChips(options: List<Pair<Int, String>>, selected: Int, colors: PlayerColors, onSelect: (Int) -> Unit,
    tagPrefix: String) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 12.dp).selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (key, label) ->
            val active = key == selected
            Box(Modifier.heightIn(min = 44.dp).widthIn(min = 44.dp).clip(RoundedCornerShape(50))
                .background(if (active) colors.ink else colors.raised)
                .selectable(active, role = Role.RadioButton, onClick = { onSelect(key) })
                .padding(horizontal = 12.dp).testTag("$tagPrefix$key"), contentAlignment = Alignment.Center) {
                Text(label, fontSize = 15.sp, color = if (active) colors.onInk else colors.ink)
            }
        }
    }
}

/** Black pill for the one main action; light pill for the second. Both 52dp so they read as a pair. */
@Composable
fun PlayerPrimaryButton(label: String, onClick: () -> Unit, colors: PlayerColors, modifier: Modifier = Modifier,
    icon: ImageVector? = null) {
    Row(modifier.heightIn(min = 52.dp).clip(RoundedCornerShape(50)).background(colors.ink)
        .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        if (icon != null) { Icon(icon, null, Modifier.size(18.dp), tint = colors.onInk); Spacer(Modifier.width(6.dp)) }
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colors.onInk, maxLines = 2)
    }
}

@Composable
fun PlayerSecondaryButton(label: String, onClick: () -> Unit, colors: PlayerColors, modifier: Modifier = Modifier,
    icon: ImageVector? = null, container: Color = Color.White.copy(alpha = .55f), content: Color = colors.onLesson) {
    Row(modifier.heightIn(min = 52.dp).clip(RoundedCornerShape(50)).background(container)
        .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        if (icon != null) { Icon(icon, null, Modifier.size(18.dp), tint = content); Spacer(Modifier.width(6.dp)) }
        Text(label, fontSize = 13.sp, color = content, maxLines = 2)
    }
}

/** Splits archive titles such as "סימן כו - ה חשוון" into a name and its Hebrew date. */
internal fun splitLessonTitle(title: String): Pair<String, String> {
    val parts = title.split(" - ", " – ", limit = 2)
    return if (parts.size == 2) parts[0].trim() to parts[1].trim() else title to ""
}
