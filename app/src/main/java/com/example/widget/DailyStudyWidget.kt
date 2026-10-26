package com.example.widget

import android.content.Context
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.TypedValue
import android.widget.RemoteViews
import androidx.glance.appwidget.AndroidRemoteViews
import android.view.View
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import com.example.ui.theme.FontStyleOption
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.layout.wrapContentHeight
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.unit.ColorProvider
import com.example.R

/**
 * Home-screen widget: today's Hayom Yom (or another chosen lesson) on a black or white
 * card, with fixed text-only shortcuts to חומש | תהילים | תניא | רמב״ם underneath.
 */
class DailyStudyWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val loader = WidgetContentLoader(context)
        val initialConfig = WidgetConfigStore.load(context, id)
        val initialContent = loader.load(initialConfig)
        provideContent {
            val state = currentState<Preferences>()
            val config = WidgetConfigStore.read(state)
            val token = WidgetConfigStore.refreshToken(state)
            val initialToken = remember { token }
            val content by produceState(initialContent, config, token) {
                if (config != initialConfig || token != initialToken) value = loader.load(config)
            }
            DailyStudyWidgetContent(content, config.theme, config.font)
        }
    }

    companion object {
        const val BODY_FONT_SP = 16f
        const val HEADING_FONT_SP = 14f
        const val BUTTON_FONT_SP = 16f
        val HORIZONTAL_PADDING = 18.dp
        val TOP_PADDING = 14.dp
        val HEADER_HEIGHT = 20.dp
        val HEADER_GAP = 8.dp
        val BODY_BOTTOM_GAP = 6.dp
        val FOOTER_HEIGHT = 46.dp
        /** Glance supports up to 10 children per container; keep the body well under it. */
        const val MAX_BODY_TEXTS = 6
    }
}

private val LocalWidgetFont = staticCompositionLocalOf { FontStyleOption.SERIF }

@Composable
fun DailyStudyWidgetContent(content: WidgetContent, theme: WidgetTheme, font: FontStyleOption = FontStyleOption.SERIF) {
    CompositionLocalProvider(LocalWidgetFont provides font) {
        WidgetCard(content, theme)
    }
}

@Composable
private fun WidgetCard(content: WidgetContent, theme: WidgetTheme) {
    val context = LocalContext.current
    val size = LocalSize.current
    val rtl = context.resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL
    val accent = WidgetPalette.accent(content.main, theme)
    val openMain = actionStartActivity(WidgetLaunch.intent(context, content.main))
    val background = if (theme == WidgetTheme.DARK) R.drawable.widget_bg_dark else R.drawable.widget_bg_light

    // The background RemoteViews opts out of launcher corner rounding.
    Box(modifier = GlanceModifier.fillMaxSize()) {
        val flatBackground = RemoteViews(context.packageName, R.layout.widget_flat_background).apply {
            setInt(android.R.id.background, "setBackgroundResource", background)
        }
        AndroidRemoteViews(flatBackground, modifier = GlanceModifier.fillMaxSize())
        Column(modifier = GlanceModifier.fillMaxSize()) {
            // The whole text area is one tap target: it opens the main lesson.
            Column(
                modifier = GlanceModifier.defaultWeight().fillMaxWidth()
                    .clickable(openMain)
                    .padding(
                        start = DailyStudyWidget.HORIZONTAL_PADDING,
                        end = DailyStudyWidget.HORIZONTAL_PADDING,
                        top = DailyStudyWidget.TOP_PADDING,
                        bottom = DailyStudyWidget.BODY_BOTTOM_GAP
                    )
            ) {
                Header(content, theme, accent)
                Spacer(GlanceModifier.height(DailyStudyWidget.HEADER_GAP))
                Body(content, theme, accent, size.width.value, size.height.value, context)
            }
            Box(GlanceModifier.fillMaxWidth().height(1.dp).background(ColorProvider(WidgetPalette.divider(theme)))) {}
            ButtonRow(content.buttons, theme, rtl)
        }
    }
}

/** "היום יום · כ״ט תשרי" as one line: lesson name in its colour, date muted. */
@Composable
private fun Header(content: WidgetContent, theme: WidgetTheme, accent: Color) {
    val title = SpannableStringBuilder().apply {
        append(content.title, ForegroundColorSpan(accent.toArgb()), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        setSpan(StyleSpan(Typeface.BOLD), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        if (content.subtitle.isNotBlank()) {
            append("  ·  ${content.subtitle}", ForegroundColorSpan(WidgetPalette.muted(theme).toArgb()), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }
    VilnaText(
        text = title,
        sizeSp = 14f,
        color = WidgetPalette.muted(theme),
        style = VilnaStyle.PARAGRAPH,
        maxLines = 1,
        modifier = GlanceModifier.fillMaxWidth().height(DailyStudyWidget.HEADER_HEIGHT)
    )
}

@Composable
private fun Body(
    content: WidgetContent,
    theme: WidgetTheme,
    accent: Color,
    widgetWidthDp: Float,
    widgetHeightDp: Float,
    context: Context
) {
    val textColor = WidgetPalette.text(theme)

    @Composable
    fun Block(block: WidgetBlock, maxLines: Int, modifier: GlanceModifier) = VilnaText(
        text = block.text,
        sizeSp = if (block.isHeading) DailyStudyWidget.HEADING_FONT_SP else DailyStudyWidget.BODY_FONT_SP,
        color = if (block.isHeading) accent else textColor,
        style = if (block.isHeading) VilnaStyle.LINE else VilnaStyle.PARAGRAPH,
        maxLines = maxLines,
        modifier = modifier
    )

    val message = content.unavailableMessage
    if (message != null || content.blocks.isEmpty()) {
        VilnaText(message.orEmpty(), DailyStudyWidget.BODY_FONT_SP, WidgetPalette.muted(theme), VilnaStyle.PARAGRAPH)
        return
    }

    val bodyWidth = widgetWidthDp - 2 * DailyStudyWidget.HORIZONTAL_PADDING.value
    val bodyHeight = widgetHeightDp - DailyStudyWidget.TOP_PADDING.value - DailyStudyWidget.HEADER_HEIGHT.value -
        DailyStudyWidget.HEADER_GAP.value - DailyStudyWidget.BODY_BOTTOM_GAP.value - 1f -
        DailyStudyWidget.FOOTER_HEIGHT.value
    val plan = VilnaBitmapText.plan(context, content.blocks, bodyWidth, bodyHeight, LocalWidgetFont.current)

    Column(modifier = GlanceModifier.fillMaxSize()) {
        val texts = mergeForGlance(plan.blocks)
        texts.forEachIndexed { index, block ->
            Block(
                block,
                maxLines = if (index == texts.lastIndex) plan.lastBlockMaxLines else Int.MAX_VALUE,
                modifier = GlanceModifier.fillMaxWidth().wrapContentHeight().padding(top = if (index == 0) 0.dp else 6.dp)
            )
        }
        if (plan.mode == WidgetTextFit.Mode.TRUNCATED) {
            VilnaText(
                text = "הצג עוד",
                sizeSp = DailyStudyWidget.HEADING_FONT_SP,
                color = accent,
                style = VilnaStyle.LINE,
                maxLines = 1,
                modifier = GlanceModifier.fillMaxWidth().wrapContentHeight().padding(top = 2.dp)
            )
        }
    }
}

/** Keeps the number of text children within Glance's per-container limit. */
private fun mergeForGlance(blocks: List<WidgetBlock>): List<WidgetBlock> {
    if (blocks.size <= DailyStudyWidget.MAX_BODY_TEXTS) return blocks
    val head = blocks.take(DailyStudyWidget.MAX_BODY_TEXTS - 1)
    val tail = blocks.drop(DailyStudyWidget.MAX_BODY_TEXTS - 1)
    return head + WidgetBlock(tail.joinToString("\n") { it.text })
}

@Composable
private fun ButtonRow(buttons: List<WidgetButton>, theme: WidgetTheme, rtl: Boolean) {
    val context = LocalContext.current
    // Row follows the launcher's layout direction; keep חומש on the right either way.
    val ordered = if (rtl) buttons else buttons.reversed()
    Row(
        modifier = GlanceModifier.fillMaxWidth().height(DailyStudyWidget.FOOTER_HEIGHT).padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ordered.forEachIndexed { index, button ->
            if (index > 0) {
                Box(GlanceModifier.width(1.dp).height(14.dp).background(ColorProvider(WidgetPalette.divider(theme)))) {}
            }
            Box(
                modifier = GlanceModifier.defaultWeight().fillMaxHeight()
                    .clickable(actionStartActivity(WidgetLaunch.intent(context, button.study)))
                    .semantics { contentDescription = "פתיחת שיעור ${button.label}" },
                contentAlignment = Alignment.Center
            ) {
                VilnaText(
                    text = button.label,
                    sizeSp = DailyStudyWidget.BUTTON_FONT_SP,
                    color = WidgetPalette.accent(button.study, theme),
                    style = VilnaStyle.BUTTON,
                    maxLines = 1,
                    widthDp = (LocalSize.current.width.value - 12f - (buttons.size - 1)) / buttons.size
                )
            }
        }
    }
}

private enum class VilnaStyle(val layout: Int) {
    PARAGRAPH(R.layout.widget_vilna_text_regular),
    LINE(R.layout.widget_vilna_text_bold),
    BUTTON(R.layout.widget_vilna_text_center)
}

/**
 * Text in the font selected for this widget. Glance's Text cannot use
 * an app font, so text is drawn with the app typeface and embedded in a RemoteViews ImageView.
 */
@Composable
private fun VilnaText(
    text: CharSequence,
    sizeSp: Float,
    color: Color,
    style: VilnaStyle,
    maxLines: Int = Int.MAX_VALUE,
    widthDp: Float? = null,
    // AndroidRemoteViews fills the parent unless told otherwise; wrap by default so blocks stack.
    modifier: GlanceModifier = GlanceModifier.fillMaxWidth().wrapContentHeight()
) {
    val context = LocalContext.current
    val size = LocalSize.current
    val textWidthDp = widthDp ?: (size.width.value - 2 * DailyStudyWidget.HORIZONTAL_PADDING.value)
    val font = LocalWidgetFont.current
    val bitmap = remember(font, text.toString(), textWidthDp, sizeSp, color, style, maxLines,
        context.resources.configuration.fontScale) {
        VilnaBitmapText.render(context, text, textWidthDp, sizeSp, color.toArgb(),
            maxLines, centered = style == VilnaStyle.BUTTON, bold = style != VilnaStyle.PARAGRAPH, font = font)
    }
    val views = RemoteViews(context.packageName, R.layout.widget_vilna_bitmap).apply {
        setImageViewBitmap(R.id.widget_vilna_bitmap, bitmap)
        setContentDescription(R.id.widget_vilna_bitmap, text.toString())
    }
    AndroidRemoteViews(remoteViews = views, modifier = modifier)
}
