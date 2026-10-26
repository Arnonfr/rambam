package com.example.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import android.text.TextUtils
import android.text.SpannableString
import android.text.Spanned
import android.text.style.MetricAffectingSpan
import com.example.ui.util.HebrewTextNormalizer
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.ui.theme.FontStyleOption

/** Launcher RemoteViews use a restricted context which ignores resource font families. */
internal object VilnaBitmapText {
    fun render(context: Context, text: CharSequence, widthDp: Float, sizeSp: Float, color: Int,
        maxLines: Int, centered: Boolean, bold: Boolean, font: FontStyleOption = FontStyleOption.SERIF): Bitmap {
        val layout = layout(context, text, widthDp, sizeSp, color, maxLines, centered, bold, font)
        return Bitmap.createBitmap(layout.width, layout.height.coerceAtLeast(1), Bitmap.Config.ARGB_8888).apply {
            density = context.resources.displayMetrics.densityDpi
            layout.draw(Canvas(this))
        }
    }

    internal fun layout(context: Context, text: CharSequence, widthDp: Float, sizeSp: Float,
        color: Int = android.graphics.Color.BLACK, maxLines: Int = Int.MAX_VALUE,
        centered: Boolean = false, bold: Boolean = false, font: FontStyleOption = FontStyleOption.SERIF): StaticLayout {
        val metrics = context.resources.displayMetrics
        val width = (widthDp * metrics.density).toInt().coerceAtLeast(1)
        val resource = when (font) {
            FontStyleOption.SERIF -> R.font.romm_vilna_regular
            FontStyleOption.SANS -> if (bold) R.font.noto_sans_hebrew_semibold else R.font.noto_sans_hebrew
            FontStyleOption.LIBERTINUS -> if (bold) R.font.libertinus_serif_bold else R.font.libertinus_serif_regular
            FontStyleOption.BONA_NOVA -> if (bold) R.font.bona_nova_bold else R.font.bona_nova_regular
        }
        val typeface = requireNotNull(ResourcesCompat.getFont(context, resource))
        val paint = TextPaint(TextPaint.ANTI_ALIAS_FLAG).apply {
            textSize = android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_SP, sizeSp, metrics)
            this.color = color
            this.typeface = if (bold && font == FontStyleOption.SERIF) Typeface.create(typeface, Typeface.BOLD) else typeface
        }
        val displayed = SpannableString(HebrewTextNormalizer.plainQuotes(text.toString()))
        val punctuationFont = requireNotNull(ResourcesCompat.getFont(context, R.font.noto_sans_hebrew))
        displayed.forEachIndexed { index, char ->
            if (char == '\'' || char == '"') displayed.setSpan(object : MetricAffectingSpan() {
                override fun updateDrawState(tp: TextPaint) { tp.typeface = punctuationFont }
                override fun updateMeasureState(tp: TextPaint) { tp.typeface = punctuationFont }
            }, index, index + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        return StaticLayout.Builder.obtain(displayed, 0, displayed.length, paint, width)
            .setAlignment(if (centered) Layout.Alignment.ALIGN_CENTER else Layout.Alignment.ALIGN_NORMAL)
            .setTextDirection(TextDirectionHeuristics.RTL)
            .setIncludePad(false)
            .setLineSpacing(0f, 1.08f)
            .setMaxLines(maxLines)
            .setEllipsize(TextUtils.TruncateAt.END)
            .build()
    }

    /** Measure with the selected face, so wider fonts cannot push text into the footer. */
    fun plan(context: Context, blocks: List<WidgetBlock>, widthDp: Float, heightDp: Float,
        font: FontStyleOption): WidgetTextFit.Plan {
        if (blocks.isEmpty()) return WidgetTextFit.Plan(WidgetTextFit.Mode.FULL, emptyList(), Int.MAX_VALUE)
        val density = context.resources.displayMetrics.density
        fun measure(block: WidgetBlock) = layout(context, block.text, widthDp,
            if (block.isHeading) DailyStudyWidget.HEADING_FONT_SP else DailyStudyWidget.BODY_FONT_SP,
            bold = block.isHeading, font = font)
        val layouts = blocks.map(::measure)
        val gap = 6f * density
        val availableHeight = heightDp * density
        val fullHeight = layouts.sumOf { it.height } + gap * (blocks.size - 1)
        if (fullHeight <= availableHeight) return WidgetTextFit.Plan(WidgetTextFit.Mode.FULL, blocks, Int.MAX_VALUE)

        var remaining = availableHeight - layout(context, "הצג עוד", widthDp,
            DailyStudyWidget.HEADING_FONT_SP, bold = true, font = font).height - 2f * density
        val visible = mutableListOf<WidgetBlock>()
        var lastMaxLines = Int.MAX_VALUE
        for ((index, block) in blocks.withIndex()) {
            val layout = layouts[index]
            val blockGap = if (index > 0) gap else 0f
            if (blockGap + layout.height <= remaining) {
                visible += block
                remaining -= blockGap + layout.height
                continue
            }
            val lines = (0 until layout.lineCount).count { layout.getLineBottom(it) <= remaining - blockGap }
            if (lines > 0 && !block.isHeading) {
                visible += block.copy(text = WidgetTextFit.truncate(block.text, layout.getLineEnd(lines - 1)))
                lastMaxLines = lines
            }
            break
        }
        if (visible.isEmpty()) {
            visible += blocks.first().copy(text = WidgetTextFit.truncate(blocks.first().text, layouts.first().getLineEnd(0)))
            lastMaxLines = 1
        }
        return WidgetTextFit.Plan(WidgetTextFit.Mode.TRUNCATED, visible, lastMaxLines)
    }
}
