package com.example.widget

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

/**
 * RemoteViews cannot measure text, so the widget estimates how much of the lesson
 * fits from its size in dp. The estimate errs on the side of fewer lines, so a
 * truncated text never runs into the buttons. Enlarging the widget shows more of the
 * lesson, and all of it once it fits; otherwise "הצג עוד" opens it in the app.
 */
object WidgetTextFit {
    enum class Mode {
        /** The whole text fits. */
        FULL,
        /** Show the beginning and a "show more" link. */
        TRUNCATED
    }

    data class Plan(val mode: Mode, val blocks: List<WidgetBlock>, val lastBlockMaxLines: Int)

    // Measured on rendered RemoteViews in Romm Vilna: lines are 1.08em tall and a Hebrew
    // character averages 0.37em; both are rounded up so text never runs into the buttons.
    private const val LINE_HEIGHT_EM = 1.10f
    private const val CHAR_WIDTH_EM = 0.42f
    private const val HEADING_SCALE = 0.9f
    private const val BLOCK_GAP_DP = 6f
    private const val SHOW_MORE_HEIGHT_DP = 16f

    fun charsPerLine(widthDp: Float, fontSizeSp: Float, fontScale: Float): Int =
        max(8, floor(widthDp / (fontSizeSp * fontScale * CHAR_WIDTH_EM)).toInt())

    fun linesFor(text: String, charsPerLine: Int): Int =
        text.split('\n').sumOf { max(1, ceil(it.length / charsPerLine.toFloat()).toInt()) }

    fun plan(
        blocks: List<WidgetBlock>,
        bodyWidthDp: Float,
        bodyHeightDp: Float,
        fontSizeSp: Float,
        fontScale: Float = 1f
    ): Plan {
        val perLine = charsPerLine(bodyWidthDp, fontSizeSp, fontScale)
        val lineHeight = fontSizeSp * fontScale * LINE_HEIGHT_EM
        fun lineHeightOf(block: WidgetBlock) = if (block.isHeading) lineHeight * HEADING_SCALE else lineHeight
        fun heightOf(block: WidgetBlock) = linesFor(block.text, perLine) * lineHeightOf(block)

        val needed = blocks.withIndex().sumOf { (index, block) ->
            (heightOf(block) + if (index > 0) BLOCK_GAP_DP else 0f).toDouble()
        }
        if (needed <= bodyHeightDp) return Plan(Mode.FULL, blocks, Int.MAX_VALUE)

        var remaining = bodyHeightDp - SHOW_MORE_HEIGHT_DP * fontScale
        val visible = mutableListOf<WidgetBlock>()
        var lastMaxLines = Int.MAX_VALUE
        for ((index, block) in blocks.withIndex()) {
            val gap = if (index > 0) BLOCK_GAP_DP else 0f
            val height = heightOf(block)
            if (gap + height <= remaining) {
                visible += block
                remaining -= gap + height
                continue
            }
            val available = floor((remaining - gap) / lineHeightOf(block)).toInt()
            // A heading alone at the bottom says nothing; stop before it.
            if (available >= 1 && !block.isHeading) {
                visible += block.copy(text = truncate(block.text, available * perLine))
                lastMaxLines = available
            }
            break
        }
        if (visible.isEmpty()) {
            val first = blocks.first()
            visible += first.copy(text = truncate(first.text, perLine))
            lastMaxLines = 1
        }
        return Plan(Mode.TRUNCATED, visible, lastMaxLines)
    }

    /** Cuts at a word boundary inside [maxChars], leaving room for the dots. */
    fun truncate(text: String, maxChars: Int): String {
        if (text.length <= maxChars) return text
        val limit = max(1, maxChars - 3)
        val cut = text.lastIndexOf(' ', limit).takeIf { it > limit / 2 } ?: limit
        // Romm Vilna has no "…" glyph.
        return text.substring(0, cut).trimEnd(' ', ',', '.', ':', ';', '–', '-', '\u05C3') + "..."
    }
}
