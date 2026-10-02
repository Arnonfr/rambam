package com.example.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.tanya.TanyaSourceLine
import kotlin.math.roundToInt
import kotlin.math.ceil

private val PrintTypeface = FontFamily(Font(R.font.romm_vilna_regular))

/** One source line is always exactly one rendered line, at every screen/font scale. */
@Composable
fun TanyaPrintedLine(line: TanyaSourceLine, textColor: Color) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val density = LocalDensity.current
        val sizeSp = maxWidth.value / 300f * 17f * line.fontScale
        CompositionLocalProvider(LocalDensity provides Density(density.density, 1f)) {
            Layout(content = {
                line.text.split(' ').forEachIndexed { index, word ->
                    val wordScale = line.wordFontScales[index] ?: if (index < line.largeOpeningWords) 1.45f else 1f
                    Text(word, fontFamily = PrintTypeface, fontSize = (sizeSp * wordScale).sp,
                        lineHeight = (sizeSp * 1.25f).sp,
                        color = if (line.greyFromWord?.let { index >= it } == true ||
                            line.greyBeforeWord?.let { index < it } == true) textColor.copy(alpha = 0.4f) else textColor,
                        maxLines = 1, softWrap = false)
                }
            }) { measurables, constraints ->
                val words = measurables.map { it.measure(Constraints()) }
                val naturalWidth = words.sumOf { it.width }
                val naturalGap = (sizeSp * density.density * 0.22f).roundToInt()
                val neededWidth = naturalWidth + naturalGap * (words.size - 1)
                val width = constraints.maxWidth
                // The print font's ink extends slightly outside its advance bounds.
                // Keep only that optical allowance, not a page margin or side gutter.
                val inkInset = ceil(sizeSp * density.density * 0.13f).toInt()
                val rightIndent = (width * line.rightIndentFraction).roundToInt()
                val textWidth = (width - inkInset * 2 - rightIndent).coerceAtLeast(1)
                val scale = minOf(1f, textWidth.toFloat() / neededWidth.coerceAtLeast(1))
                val scaledWidth = naturalWidth * scale
                val gap = if (line.justified && words.size > 1) (textWidth - scaledWidth) / (words.size - 1)
                    else naturalGap * scale
                val baseline = words.maxOfOrNull { it[FirstBaseline] } ?: 0
                val height = words.maxOfOrNull { baseline - it[FirstBaseline] + it.height } ?: 0
                layout(width, height) {
                    val centeredWidth = scaledWidth + naturalGap * scale * (words.size - 1)
                    // Physical left, regardless of the reader's RTL layout direction.
                    var right = when {
                        line.isCatchword -> inkInset + centeredWidth
                        line.centered -> (width + centeredWidth) / 2f
                        else -> (width - inkInset - rightIndent).toFloat()
                    }
                    words.forEach { word ->
                        val left = right - word.width * scale
                        word.placeWithLayer(left.roundToInt(), baseline - word[FirstBaseline]) {
                            scaleX = scale
                            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0f)
                        }
                        right = left - gap
                    }
                }
            }
        }
    }
}
