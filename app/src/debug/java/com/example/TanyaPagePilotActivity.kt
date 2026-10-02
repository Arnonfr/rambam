package com.example

import android.os.Bundle
import android.graphics.Paint
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.content.res.ResourcesCompat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DailyRambamTheme
import com.example.data.tanya.TanyaRepository
import com.example.domain.tanya.TanyaPrintLessonMatcher
import com.example.domain.tanya.TanyaPrintLessonRange
import org.json.JSONObject
import java.time.LocalDate
import java.time.ZoneId

/** Research harness only: deliberately excluded from release builds. */
class TanyaPagePilotActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Debug-only preview uses the production print components and proofread corpus.
        intent.getStringExtra("print_ref")?.let { reference ->
            val lines = com.example.domain.tanya.VerifiedTanyaPrint.fullPagesForReference(reference)
                .filter { it.page == intent.getIntExtra("print_page", 283) }
            setContent { DailyRambamTheme {
                androidx.compose.foundation.lazy.LazyColumn(
                    Modifier.fillMaxSize().background(Color(0xFFF4F3F0)).statusBarsPadding().navigationBarsPadding(),
                    contentPadding = PaddingValues(horizontal = 6.dp)
                ) {
                    items(lines.size) { index ->
                        com.example.ui.components.TanyaPageLine(lines[index], Color.Black, Color.White,
                            first = index == 0, last = index == lines.lastIndex, showAnchor = false)
                    }
                }
            } }
            return
        }
        val page = JSONObject(assets.open("tanya_print_pilot.json").bufferedReader().use { it.readText() })
        val rows = page.getJSONArray("lines")
        val lines = (0 until rows.length()).map { rows.getJSONObject(it) }
        val printedWords = lines.flatMap { line ->
            val words = line.getJSONArray("words")
            (0 until words.length()).map { words.getJSONObject(it).getString("text") }
        }
        val pageWidth = page.getDouble("width").toFloat()
        val pageHeight = page.getDouble("height").toFloat()
        val prefs = getSharedPreferences("tanya-print-pilot", MODE_PRIVATE)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = ResourcesCompat.getFont(this@TanyaPagePilotActivity, R.font.romm_vilna_regular)
            textSize = page.getDouble("fontSize").toFloat()
            color = android.graphics.Color.BLACK
        }
        setContent {
            DailyRambamTheme {
                var anchor by remember { mutableStateOf(prefs.getString("line", null)) }
                var lessonRange by remember { mutableStateOf<TanyaPrintLessonRange?>(null) }
                var lessonStatus by remember { mutableStateOf("מאתר את גבולות שיעור היום…") }
                LaunchedEffect(Unit) {
                    val date = LocalDate.now(ZoneId.of("Asia/Jerusalem"))
                    val lesson = TanyaRepository(this@TanyaPagePilotActivity).getDailyTanyaLesson(date)
                    if (lesson == null || lesson.date != date.toString()) {
                        lessonStatus = "לא זוהה שיעור היום; אין סימון גבולות"
                    } else {
                        val words = lesson.sections.joinToString(" ") { it.textPlain }.split(Regex("\\s+"))
                        lessonRange = TanyaPrintLessonMatcher.findUnique(printedWords, words)
                        lessonStatus = if (lessonRange == null) "עמוד לדוגמה בלבד; שיעור היום אינו ממופה בו"
                            else "שיעור היום בשחור; הטקסט שמסביב באפור"
                    }
                }
                Column(Modifier.fillMaxSize().background(Color.White).statusBarsPadding().navigationBarsPadding()) {
                    Text("פיילוט מקומי · עמוד 80 · טרם הוגה", fontSize = 14.sp,
                        modifier = Modifier.padding(12.dp))
                    Text("נגיעה מסמנת את מקום העצירה", fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 12.dp))
                    Text(lessonStatus, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 12.dp))
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                        BoxWithConstraints(Modifier.fillMaxWidth()) {
                            Canvas(Modifier.fillMaxWidth().height(maxWidth * (pageHeight / pageWidth))
                                .semantics { contentDescription = lines.joinToString("\n") { it.getString("text") } }
                                .pointerInput(pageWidth, anchor) {
                                    detectTapGestures(onTap = { point ->
                                        val y = point.y * pageWidth / size.width
                                        val line = lines.firstOrNull { y >= it.getDouble("top") && y <= it.getDouble("bottom") }
                                        if (line != null) {
                                            anchor = line.getString("id")
                                            prefs.edit().putString("line", anchor).apply()
                                        }
                                    })
                                }) {
                                val scale = size.width / pageWidth
                                val canvas = drawContext.canvas.nativeCanvas
                                canvas.save()
                                canvas.scale(scale, scale)
                                paint.textSize = 150f
                                val heading = page.getString("heading")
                                canvas.drawText(heading, (pageWidth - paint.measureText(heading)) / 2f, 125f, paint)
                                paint.textSize = 110f
                                canvas.drawText(page.getString("pageLabel"), 2280f, 125f, paint)
                                paint.textSize = page.getDouble("fontSize").toFloat()
                                var wordIndex = 0
                                lines.forEach { line ->
                                    if (anchor == line.getString("id")) {
                                        val old = paint.color
                                        paint.color = android.graphics.Color.rgb(77, 196, 232)
                                        canvas.drawRect(2430f, line.getDouble("top").toFloat(),
                                            2446f, line.getDouble("bottom").toFloat(), paint)
                                        paint.color = old
                                    }
                                    val words = line.getJSONArray("words")
                                    for (i in 0 until words.length()) {
                                        val word = words.getJSONObject(i)
                                        val text = word.getString("text")
                                        val left = word.getDouble("left").toFloat()
                                        val width = word.getDouble("right").toFloat() - left
                                        val baseline = line.getDouble("top").toFloat() + 100f
                                        paint.color = if (lessonRange?.contains(wordIndex) == false)
                                            android.graphics.Color.rgb(155, 155, 155) else android.graphics.Color.BLACK
                                        if (lessonRange?.startWord == wordIndex) {
                                            val old = paint.color
                                            paint.color = android.graphics.Color.rgb(77, 196, 232)
                                            canvas.drawRect(left, baseline + 18f, left + width, baseline + 29f, paint)
                                            paint.color = old
                                        }
                                        canvas.save()
                                        canvas.translate(left, baseline)
                                        // Temporary word-level calibration; letter-level print alternates still need mapping.
                                        canvas.scale(width / paint.measureText(text).coerceAtLeast(1f), 1f)
                                        canvas.drawText(text, 0f, 0f, paint)
                                        canvas.restore()
                                        wordIndex++
                                    }
                                }
                                paint.color = android.graphics.Color.BLACK
                                canvas.restore()
                            }
                        }
                    }
                }
            }
        }
    }
}
