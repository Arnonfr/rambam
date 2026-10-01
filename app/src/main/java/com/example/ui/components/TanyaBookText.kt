package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import kotlin.math.roundToInt

private val TanyaPrintFont = FontFamily(Font(R.font.frank_ruhl_libre))

/** Live, selectable type: measure once at the same page width, then scale the layout.
 * This preserves line endings across screen sizes/zoom. It is NOT a verified
 * transcription of the traditional edition's line/page boundaries.
 */
@Composable
fun TanyaBookText(text: String, textColor: Color, zoom: Float, isReadingAnchor: Boolean) {
    val density = LocalDensity.current
    Column(Modifier.drawBehind {
        if (isReadingAnchor) drawLine(Color(0xFF73D4ED),
            Offset(size.width, 0f), Offset(size.width, size.height), strokeWidth = 3.dp.toPx())
    }) {
        if (isReadingAnchor) {
            Text("כאן עצרת בפעם הקודמת", color = Color(0xFF159BC4), fontSize = 12.sp)
            Spacer(Modifier.height(5.dp).fillMaxWidth().background(Color(0xFF73D4ED)))
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val viewportWidth = with(density) { maxWidth.toPx() }
            val canonicalWidth = with(density) { 300.dp.roundToPx() }
            val scale = viewportWidth / canonicalWidth * zoom.coerceIn(.7f, 1.65f)
            Box(Modifier.horizontalScroll(rememberScrollState())) {
                CompositionLocalProvider(LocalDensity provides Density(density.density, 1f)) {
                    Layout(content = {
                        SelectionContainer {
                            Text(text, style = TextStyle(fontFamily = TanyaPrintFont,
                                fontSize = 17.sp, lineHeight = 21.sp, color = textColor,
                                textAlign = TextAlign.Justify, textDirection = TextDirection.Rtl))
                        }
                    }) { measurables, _ ->
                        val page = measurables.single().measure(Constraints.fixedWidth(canonicalWidth))
                        layout((page.width * scale).roundToInt(), (page.height * scale).roundToInt()) {
                            page.placeWithLayer(0, 0) {
                                scaleX = scale
                                scaleY = scale
                                transformOrigin = TransformOrigin(0f, 0f)
                            }
                        }
                    }
                }
            }
        }
    }
}
