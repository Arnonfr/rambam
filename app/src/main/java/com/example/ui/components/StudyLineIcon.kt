package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale

/** Same optical weight and 48-unit grid for all lesson illustrations. */
@Composable
fun StudyLineIcon(kind: String, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        scale(size.width / 48f, size.height / 48f, pivot = androidx.compose.ui.geometry.Offset.Zero) {
            fun line(build: Path.() -> Unit) = drawPath(Path().apply(build), Color.Black,
                style = Stroke(1.8f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            when (kind) {
                "hayom_yom" -> {
                    line { moveTo(8f,10f); lineTo(40f,10f); lineTo(40f,41f); lineTo(8f,41f); close()
                        moveTo(8f,18f); lineTo(40f,18f)
                        moveTo(16f,6f); lineTo(16f,13f); moveTo(32f,6f); lineTo(32f,13f)
                        moveTo(16f,26f); lineTo(32f,26f); moveTo(16f,32f); lineTo(27f,32f) }
                }
                "one", "three" -> {
                    line { moveTo(7f, 12f); lineTo(32f, 6f); lineTo(43f, 9f); lineTo(18f, 16f); close() }
                    listOf(16f, 25f, 34f).forEach { y ->
                        line { moveTo(18f, y); lineTo(42f, y-6f); moveTo(18f,y); lineTo(8f,y-3f)
                            cubicTo(4f,y-4f,4f,y+3f,8f,y+5f); lineTo(18f,y+8f); lineTo(42f,y+1f) }
                        line { moveTo(18f,y); cubicTo(15f,y+2f,15f,y+5f,18f,y+8f) }
                    }
                }
                "chumash" -> {
                    line { moveTo(11f,12f); lineTo(37f,12f); lineTo(37f,36f); lineTo(11f,36f); close()
                        moveTo(8f,9f); lineTo(13f,9f); lineTo(13f,39f); lineTo(8f,39f); close()
                        moveTo(35f,9f); lineTo(40f,9f); lineTo(40f,39f); lineTo(35f,39f); close()
                        moveTo(10.5f,5f); lineTo(10.5f,9f); moveTo(10.5f,39f); lineTo(10.5f,43f)
                        moveTo(37.5f,5f); lineTo(37.5f,9f); moveTo(37.5f,39f); lineTo(37.5f,43f) }
                }
                "tehillim" -> {
                    line { moveTo(12f,7f); cubicTo(3f,3f,5f,16f,12f,12f)
                        cubicTo(12f,22f,4f,29f,9f,37f); cubicTo(16f,49f,34f,46f,39f,35f)
                        cubicTo(43f,27f,35f,20f,36f,12f); cubicTo(44f,16f,44f,3f,36f,7f)
                        moveTo(12f,10f); lineTo(36f,10f) }
                    listOf(16f,21f,26f,31f).forEach { x -> line { moveTo(x,14f); lineTo(x,38f) } }
                    line { moveTo(12f,35f); quadraticTo(24f,46f,36f,35f) }
                }
                else -> {
                    line { moveTo(24f,14f); cubicTo(18f,7f,9f,8f,6f,9f); lineTo(6f,35f)
                        cubicTo(13f,33f,20f,37f,24f,41f); cubicTo(28f,37f,35f,33f,42f,35f)
                        lineTo(42f,9f); cubicTo(35f,8f,28f,7f,24f,14f); lineTo(24f,41f)
                        moveTo(6f,13f); lineTo(3f,13f); lineTo(3f,39f); quadraticTo(15f,36f,24f,43f)
                        quadraticTo(33f,36f,45f,39f); lineTo(45f,13f); lineTo(42f,13f) }
                }
            }
        }
    }
}
