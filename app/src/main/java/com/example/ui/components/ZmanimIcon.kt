package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.domain.zmanim.ZmanId
import kotlin.math.cos
import kotlin.math.sin

/** Original 24-unit line icons: consistent rounded strokes, no filled decorative shapes. */
@Composable
fun ZmanimIcon(id: ZmanId, modifier: Modifier = Modifier, color: Color = Color(0xFF6B6A65)) {
    Canvas(modifier) {
        val unit = size.minDimension / 24f
        val stroke = 1.45f * unit
        fun point(x: Float,y: Float) = Offset(x*unit,y*unit)
        fun line(x1:Float,y1:Float,x2:Float,y2:Float) = drawLine(color,point(x1,y1),point(x2,y2),stroke,StrokeCap.Round)
        fun circle(x:Float,y:Float,r:Float) = drawCircle(color,r*unit,point(x,y),style=Stroke(stroke))
        when(id) {
            ZmanId.CANDLES -> {
                line(8f,12f,8f,21f);line(16f,12f,16f,21f)
                line(5f,21f,11f,21f);line(13f,21f,19f,21f)
                listOf(8f,16f).forEach { x ->
                    val flame=Path().apply { moveTo(x*unit,3f*unit);cubicTo((x-5f)*unit,9f*unit,(x+5f)*unit,11f*unit,x*unit,3f*unit) }
                    drawPath(flame,color,style=Stroke(stroke,cap=StrokeCap.Round))
                }
            }
            ZmanId.NIGHT -> {
                val moon=Path().apply {
                    moveTo(16f*unit,3f*unit)
                    cubicTo(2f*unit,1f*unit,1f*unit,21f*unit,15f*unit,21f*unit)
                    cubicTo(19f*unit,21f*unit,21f*unit,18f*unit,21f*unit,16f*unit)
                    cubicTo(11f*unit,20f*unit,8f*unit,9f*unit,16f*unit,3f*unit)
                    close()
                }; drawPath(moon,color,style=Stroke(stroke))
            }
            ZmanId.SUNRISE,ZmanId.SUNSET,ZmanId.DAWN,ZmanId.TALLIT -> {
                drawArc(color,180f,180f,false,point(6f,10f),Size(12f*unit,12f*unit),style=Stroke(stroke,cap=StrokeCap.Round))
                line(3f,16f,21f,16f);line(12f,4f,12f,7f)
                line(4f,7f,6f,9f);line(18f,9f,20f,7f)
                if(id==ZmanId.SUNSET) { line(9f,20f,12f,22f);line(12f,22f,15f,20f) }
                else { line(9f,22f,12f,20f);line(12f,20f,15f,22f) }
            }
            ZmanId.SHEMA,ZmanId.PRAYER -> {
                circle(12f,12f,8f);line(12f,7f,12f,12f);line(12f,12f,16f,14f)
            }
            else -> {
                circle(12f,12f,4f)
                repeat(8) { i ->
                    val angle=(i*Math.PI/4).toFloat()
                    line(12f+cos(angle)*7f,12f+sin(angle)*7f,12f+cos(angle)*9f,12f+sin(angle)*9f)
                }
            }
        }
    }
}
