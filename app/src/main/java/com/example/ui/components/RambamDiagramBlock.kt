package com.example.ui.components

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.RambamDiagram
import com.example.domain.RambamDiagram.Kind

@Composable
fun RambamDiagramBlock(diagram: RambamDiagram) {
    val uriHandler = LocalUriHandler.current
    Column(
        Modifier.fillMaxWidth().padding(vertical = 12.dp).testTag("rambam_diagram_${diagram.id}"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(diagram.title, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(10.dp))
        if (diagram.kind == Kind.MENORAH_ORIGINAL) {
            Image(
                painterResource(R.drawable.rambam_menorah_original), diagram.title,
                Modifier.fillMaxWidth().heightIn(max = 450.dp).aspectRatio(383f / 525f),
                contentScale = ContentScale.Fit
            )
        } else {
            val drawingHeight = if (diagram.kind == Kind.HEICHAL || diagram.kind == Kind.COURTYARD) 440f else 260f
            Canvas(Modifier.fillMaxWidth().aspectRatio(360f / drawingHeight).background(Color(0xFFFFFCF6))
                .semantics { contentDescription = diagram.title + ". " + diagram.note }) {
                val factor = size.width / 360f
                scale(factor, factor, pivot = Offset.Zero) { drawDiagram(diagram.kind) }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(diagram.note, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("מקור ומיקום בהלכה ↗", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 6.dp).clickable { uriHandler.openUri(diagram.sourceUrl) })
        diagram.visualReferenceUrl?.let { reference ->
            Text("ציור כתב־היד להשוואה ↗", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp).clickable { uriHandler.openUri(reference) })
        }
    }
}

private val ink = Color(0xFF222222)
private val accent = Color(0xFFB46D2B)
private val pale = Color(0xFFF0E4D0)
private val blue = Color(0xFF52BDD5)

private fun DrawScope.line(x: Float, y: Float, x2: Float, y2: Float, color: Color = ink, width: Float = 2f) =
    drawLine(color, Offset(x, y), Offset(x2, y2), width)

private fun DrawScope.box(x: Float, y: Float, w: Float, h: Float, fill: Color = pale) {
    drawRect(fill, Offset(x, y), Size(w, h))
    drawRect(ink, Offset(x, y), Size(w, h), style = Stroke(1.5f))
}

private fun DrawScope.label(value: String, x: Float, y: Float, textSize: Float = 13f) {
    drawContext.canvas.nativeCanvas.drawText(value, x, y, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(34, 34, 34)
        this.textSize = textSize
        textAlign = Paint.Align.CENTER
    })
}

private fun DrawScope.drawDiagram(kind: Kind) {
    when (kind) {
        Kind.TEFILLIN_MOLD -> {
            for (i in 0..3) box(54f + i * 64f, 50f, 60f, 150f)
            for (i in 1..3) line(50f + i * 64f, 45f, 50f + i * 64f, 205f, blue, 3f)
            label("ארבעה ראשים · שלושה חריצים", 180f, 238f)
        }
        Kind.HEAD_ORDER, Kind.HAND_ORDER -> {
            // Right-to-left reading; head view is facing the wearer.
            val names = listOf("קדש", "והיה כי", "שמע", "והיה אם")
            if (kind == Kind.HAND_ORDER) box(18f, 55f, 324f, 142f)
            names.forEachIndexed { i, name ->
                val x = 267f - i * 81f
                if (kind == Kind.HEAD_ORDER) box(x, 55f, 72f, 142f)
                else if (i > 0) line(x + 76f, 62f, x + 76f, 190f, accent)
                label(name, x + 36f, 118f, 15f)
                val second = when (i) { 1 -> "יביאך"; 3 -> "שמוע"; else -> "" }
                label(second, x + 36f, 141f, 15f)
            }
            if (kind == Kind.HEAD_ORDER) {
                label("ימין המניח", 64f, 231f)
                label("שמאל המניח", 300f, 231f)
            } else label("עור אחד · ארבעה דפים", 180f, 231f)
        }
        Kind.OPEN_FIRST, Kind.OPEN_SECOND, Kind.CLOSED_FIRST, Kind.CLOSED_SECOND, Kind.CLOSED_THIRD -> {
            fun writing(y: Float, left: Float = 32f, right: Float = 328f, color: Color = ink) {
                var x = right
                while (x - 13f >= left) { line(x - 13f, y, x, y, color, 5f); x -= 20f }
            }
            writing(45f)
            when (kind) {
                Kind.OPEN_FIRST -> { writing(85f, 168f); writing(125f, color = accent); writing(165f, color = accent) }
                Kind.OPEN_SECOND -> { writing(85f); writing(165f, color = accent); label("שיטה פנויה", 180f, 130f) }
                Kind.CLOSED_FIRST -> { writing(85f, 222f); writing(85f, 32f, 146f, accent); writing(125f, color = accent); writing(165f, color = accent) }
                Kind.CLOSED_SECOND -> { writing(85f, 50f); writing(125f, 32f, 212f, accent); writing(165f, color = accent) }
                Kind.CLOSED_THIRD -> { writing(85f); writing(125f, 32f, 212f, accent); writing(165f, color = accent) }
                else -> Unit
            }
            label("שחור: סיום פרשה · חום: תחילת הפרשה הבאה", 180f, 230f, 12f)
        }
        Kind.LAND_BOUNDARY -> {
            box(185f, 35f, 140f, 190f, Color(0xFFF4EACF))
            label("ארץ ישראל", 255f, 130f, 17f)
            label("ים", 66f, 120f, 17f)
            line(135f, 35f, 168f, 220f, blue, 3f)
            label("טורי סמנום", 155f, 25f)
            label("נחל מצרים", 166f, 245f)
            drawCircle(pale, 8f, Offset(182f, 110f)); drawCircle(pale, 8f, Offset(125f, 175f))
            label("לפנים", 205f, 162f); label("לחוץ", 92f, 213f)
        }
        Kind.ALTAR_HEIGHT -> {
            // Exact vertical proportions in tefachim: 5 + 30 + 18 + 5 = 58.
            val bottom = 222f
            box(30f, bottom - 15f, 252f, 15f)
            box(42f, bottom - 105f, 228f, 90f)
            box(54f, bottom - 159f, 204f, 54f)
            box(54f, bottom - 174f, 18f, 15f); box(240f, bottom - 174f, 18f, 15f)
            line(39f, bottom - 87f, 273f, bottom - 87f, Color(0xFFBB4A45), 3f)
            label("חוט הסיקרא", 154f, bottom - 92f, 12f)
            label("יסוד: ה׳", 316f, 216f, 11f); label("ל׳", 310f, 166f)
            label("י״ח", 310f, 96f); label("ה׳", 310f, 58f)
            label("גובה כולל: נ״ח טפחים", 180f, 249f)
        }
        Kind.ALTAR_FOUNDATION -> {
            box(82f, 44f, 170f, 170f)
            // West (left) and north (top); one ama at the adjacent ends.
            box(74f, 36f, 186f, 8f, blue); box(74f, 44f, 8f, 178f, blue)
            box(252f, 44f, 8f, 8f, blue); box(82f, 214f, 8f, 8f, blue)
            drawCircle(ink, 3f, Offset(91f, 199f)); drawCircle(ink, 3f, Offset(100f, 205f))
            line(95f, 210f, 95f, 237f)
            label("צפון", 167f, 25f); label("מערב", 39f, 132f); label("מזרח", 298f, 132f)
            label("דרום · נקבים לשיתין בקרן מערבית־דרומית", 180f, 255f, 11f)
            label("מזבח", 167f, 130f, 17f)
        }
        Kind.LINTELS -> {
            box(92f, 112f, 15f, 110f); box(253f, 112f, 15f, 110f)
            for (i in 0..4) {
                val w = 176f + 16f * i
                val y = 100f - 18f * i
                box(180f - w / 2f, y, w, 11f)
                label("${22 + 2 * i}", 180f, y + 10f, 10f)
            }
            label("פתח: כ׳ אמה · גובה: מ׳ אמה", 180f, 247f)
        }
        Kind.HEICHAL -> {
            // Huntington 452 fol. 9v: west above, north right, nested rectangular
            // sanctuary (not the T-shaped modern floor plan). Red lines echo its ink.
            val red = Color(0xFF9E514F)
            fun outline(x: Float, y: Float, w: Float, h: Float) {
                drawRect(red, Offset(x, y), Size(w, h), style = Stroke(1.4f))
            }
            outline(48f, 39f, 264f, 359f)
            outline(78f, 56f, 204f, 301f)
            outline(96f, 71f, 168f, 271f)
            outline(111f, 83f, 138f, 244f)
            outline(122f, 93f, 116f, 220f)
            // Twenty cubits + one-cubit amah teraksin + forty cubits.
            line(122f, 165f, 238f, 165f, red, 1.4f)
            line(122f, 170f, 238f, 170f, red, 1.4f)
            // Openings are real breaks in the plan, not an opaque room block.
            line(167f, 313f, 193f, 313f, Color(0xFFFFFCF6), 4f)
            line(166f, 342f, 194f, 342f, Color(0xFFFFFCF6), 4f)
            line(161f, 357f, 199f, 357f, Color(0xFFFFFCF6), 4f)
            line(159f, 398f, 201f, 398f, Color(0xFFFFFCF6), 4f)
            label("מערב", 180f, 22f, 15f)
            label("קודש הקודשים", 180f, 127f, 13f)
            label("כ׳ אמה", 180f, 148f, 12f)
            label("הקודש", 180f, 235f, 16f)
            label("מ׳ אמה", 180f, 259f, 12f)
            label("אולם", 180f, 380f, 16f)
            label("מזרח · פתח האולם", 180f, 427f, 13f)
            label("דרום", 23f, 211f, 12f); label("צפון", 337f, 211f, 12f)
            label("תאים", 88f, 208f, 10f); label("תאים", 273f, 208f, 10f)
        }
        Kind.COURTYARD -> {
            // The visual reference is Huntington 452 fol. 12r. Preserve its
            // vertical west/east axis, sanctuary rings and south-side ramp.
            val red = Color(0xFF9E514F)
            fun outline(x: Float, y: Float, w: Float, h: Float) {
                drawRect(red, Offset(x, y), Size(w, h), style = Stroke(1.4f))
            }
            label("מערב", 180f, 21f, 15f)
            outline(58f, 34f, 244f, 364f)
            outline(111f, 56f, 138f, 168f)
            outline(127f, 67f, 106f, 140f)
            outline(139f, 78f, 82f, 116f)
            line(139f, 114f, 221f, 114f, red, 1.4f)
            line(166f, 194f, 194f, 194f, Color(0xFFFFFCF6), 4f)
            label("קודש הקודשים", 180f, 101f, 10f)
            label("קודש", 180f, 157f, 13f)
            label("אולם", 180f, 219f, 13f)
            // North right, south left in this manuscript orientation.
            outline(145f, 263f, 63f, 62f)
            outline(105f, 268f, 40f, 52f)
            label("מזבח", 177f, 299f, 13f); label("כבש", 124f, 299f, 11f)
            outline(95f, 239f, 26f, 24f); label("כיור", 108f, 255f, 9f)
            line(58f, 342f, 302f, 342f, red, 1.4f)
            line(58f, 367f, 302f, 367f, red, 1.4f)
            label("עזרת כוהנים", 180f, 359f, 12f)
            label("עזרת ישראל", 180f, 389f, 12f)
            line(159f, 398f, 201f, 398f, Color(0xFFFFFCF6), 4f)
            label("מזרח · שער ניקנור", 180f, 422f, 13f)
            label("דרום", 27f, 182f, 12f); label("צפון", 332f, 182f, 12f)
            listOf("מלח", "פרווה", "מדיחין").forEachIndexed { i, name ->
                outline(3f, 235f + i * 30f, 53f, 30f)
                label(name, 29f, 254f + i * 30f, 10f)
            }
            listOf("עץ", "גולה", "גזית").forEachIndexed { i, name ->
                outline(304f, 235f + i * 30f, 53f, 30f)
                label(name, 330f, 254f + i * 30f, 10f)
            }
            label("קפ״ז × קל״ה אמה", 181f, 48f, 10f)
        }
        Kind.SHOHAM -> {
            val right = listOf("ראובן", "לוי", "יששכר", "נפתלי", "גד", "יהוסף")
            val left = listOf("שמעון", "יהודה", "זבולן", "דן", "אשר", "בנימן")
            box(194f, 33f, 112f, 193f); box(54f, 33f, 112f, 193f)
            right.forEachIndexed { i, t -> label(t, 250f, 63f + i*27f, 19f) }
            left.forEachIndexed { i, t -> label(t, 110f, 63f + i*27f, 19f) }
            label("כתף ימין", 250f, 247f); label("כתף שמאל", 110f, 247f)
        }
        Kind.EPHOD -> {
            box(102f, 99f, 156f, 139f)
            box(102f, 30f, 30f, 69f); box(228f, 30f, 30f, 69f)
            box(29f, 99f, 302f, 20f, Color(0xFFE3CAA0))
            box(105f, 36f, 24f, 20f, Color(0xFFC2B4CF)); box(231f, 36f, 24f, 20f, Color(0xFFC2B4CF))
            for (x in listOf(117f, 243f)) {
                drawCircle(accent, 5f, Offset(x, 66f), style = Stroke(2f))
                drawCircle(accent, 5f, Offset(x, 88f), style = Stroke(2f))
            }
            label("אבני שוהם על הכתפות", 180f, 20f, 12f)
            label("חשב האיפוד", 180f, 114f, 12f)
            label("מבט מאחור", 180f, 180f, 16f)
            label("מכנגד אצילי הידיים עד הרגליים", 180f, 253f, 11f)
        }
        Kind.ROUND_BEAM -> {
            val center = Offset(180f, 115f)
            drawCircle(pale, 96f, center); drawCircle(ink, 96f, center, style = Stroke(2f))
            line(25f, 211f, 335f, 211f)
            // A unit square below the outer curve, adjacent to the resting point.
            drawRect(blue.copy(alpha = .25f), Offset(249f, 187f), Size(24f, 24f))
            drawRect(blue, Offset(249f, 187f), Size(24f, 24f), style = Stroke(1.5f))
            line(277f, 190f, 310f, 163f, blue)
            label("טפח על טפח", 298f, 153f, 10f)
            label("היקף: כ״ד טפחים", 180f, 113f, 17f)
            label("חתך העמוד · הקרקע למטה", 180f, 245f, 12f)
        }
        Kind.MENORAH_ORIGINAL -> Unit
    }
}
