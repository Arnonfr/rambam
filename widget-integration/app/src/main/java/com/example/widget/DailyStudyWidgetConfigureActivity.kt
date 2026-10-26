package com.example.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.lifecycle.lifecycleScope
import com.example.ui.theme.TanyaPrintFontFamily
import kotlinx.coroutines.launch

/**
 * Shown when the widget is placed (Android 11 and below) and from "Settings" on the
 * widget's long-press menu (Android 12+, where configuration is optional).
 */
class DailyStudyWidgetConfigureActivity : ComponentActivity() {
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        appWidgetId = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        // Backing out cancels placement on older launchers.
        setResult(RESULT_CANCELED, resultIntent())
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val glanceId = GlanceAppWidgetManager(this).getGlanceIdBy(appWidgetId)
        setContent {
            val initial by produceState<WidgetConfig?>(null) { value = WidgetConfigStore.load(this@DailyStudyWidgetConfigureActivity, glanceId) }
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MaterialTheme {
                    initial?.let { config ->
                        WidgetConfigScreen(
                            initial = config,
                            onCancel = { finish() },
                            onSave = { chosen ->
                                lifecycleScope.launch {
                                    WidgetConfigStore.save(this@DailyStudyWidgetConfigureActivity, glanceId, chosen)
                                    DailyStudyWidget().update(this@DailyStudyWidgetConfigureActivity, glanceId)
                                    WidgetRefreshScheduler.scheduleNext(this@DailyStudyWidgetConfigureActivity)
                                    setResult(RESULT_OK, resultIntent())
                                    finish()
                                }
                            }
                        )
                    } ?: Box(Modifier.fillMaxSize().background(Color(0xFFF6F5F2)))
                }
            }
        }
    }

    private fun resultIntent() = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
}

private val ScreenBackground = Color(0xFFF6F5F2)
private val Ink = Color(0xFF17120F)
private val Subtle = Color(0xFF7A7570)

@Composable
internal fun WidgetConfigScreen(initial: WidgetConfig, onCancel: () -> Unit, onSave: (WidgetConfig) -> Unit) {
    var config by remember { mutableStateOf(initial) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val loader = remember { WidgetContentLoader(context) }
    val content by produceState<WidgetContent?>(null, config.mainLesson) { value = loader.load(config) }

    Column(Modifier.fillMaxSize().background(ScreenBackground).safeDrawingPadding()) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text("ווידג׳ט היום יום", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
            Text(
                "פתגם היום וגישה מהירה לשיעורי חת״ת ורמב״ם",
                fontSize = 14.sp, color = Subtle, modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(Modifier.height(20.dp))
            WidgetPreviewCard(content, config.theme)
            Spacer(Modifier.height(28.dp))

            SectionTitle("צבע הרקע")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                WidgetTheme.entries.forEach { theme ->
                    ThemeChip(theme, selected = config.theme == theme) { config = config.copy(theme = theme) }
                }
            }
            Spacer(Modifier.height(28.dp))

            SectionTitle("השיעור המרכזי")
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color.White)
            ) {
                WidgetMainLesson.entries.forEachIndexed { index, lesson ->
                    if (index > 0) Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(1.dp).background(Color(0x0F000000)))
                    Row(
                        Modifier.fillMaxWidth().clickable { config = config.copy(mainLesson = lesson) }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = config.mainLesson == lesson,
                            onClick = { config = config.copy(mainLesson = lesson) },
                            colors = RadioButtonDefaults.colors(selectedColor = Ink)
                        )
                        Column(Modifier.weight(1f)) {
                            Text(lesson.label, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                            Text(lesson.hint, fontSize = 12.sp, color = Subtle)
                        }
                    }
                }
            }
            Text(
                "השיעור שנבחר יחליף את הכפתור שלו בתחתית בכפתור ״היום יום״. הגדלת הווידג׳ט מציגה את השיעור במלואו.",
                fontSize = 12.sp, color = Subtle, modifier = Modifier.padding(top = 10.dp, start = 4.dp, end = 4.dp)
            )
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { onSave(config) },
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = Color.White)
            ) { Text("שמירה", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.height(52.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0x22000000))
            ) { Text("ביטול", fontSize = 16.sp, color = Ink) }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Subtle, modifier = Modifier.padding(bottom = 10.dp, start = 4.dp))
}

@Composable
private fun ThemeChip(theme: WidgetTheme, selected: Boolean, onClick: () -> Unit) {
    val swatch = WidgetPalette.background(theme)
    Row(
        Modifier.clip(RoundedCornerShape(16.dp)).background(Color.White)
            .border(if (selected) 2.dp else 1.dp, if (selected) Ink else Color(0x14000000), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(22.dp).clip(CircleShape).background(swatch).border(1.dp, Color(0x22000000), CircleShape))
        Spacer(Modifier.width(10.dp))
        Text(theme.label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Ink)
    }
}

/** A Compose replica of the Glance layout, at the default 4×2 size. */
@Composable
private fun WidgetPreviewCard(content: WidgetContent?, theme: WidgetTheme) {
    val main = content?.main ?: WidgetStudy.HAYOM_YOM
    val accent = WidgetPalette.accent(main, theme)
    val shape = RectangleShape
    Box(
        Modifier.fillMaxWidth().height(176.dp).clip(shape).background(WidgetPalette.background(theme))
            .border(1.dp, if (theme == WidgetTheme.DARK) Color(0x14FFFFFF) else Color(0x14000000), shape)
            .background(
                Brush.radialGradient(
                    listOf(accent.copy(alpha = if (theme == WidgetTheme.DARK) 0.18f else 0.10f), Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(60f, 520f), radius = 700f
                )
            )
    ) {
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.weight(1f).padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(content?.title ?: main.label, color = accent, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = TanyaPrintFontFamily)
                    Text(
                        "  ·  ${content?.subtitle.orEmpty()}", color = WidgetPalette.muted(theme), fontSize = 14.sp,
                        fontFamily = TanyaPrintFontFamily, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    content?.unavailableMessage ?: content?.blocks?.joinToString("\n") { it.text } ?: "טוען…",
                    color = WidgetPalette.text(theme), fontSize = 16.sp, fontFamily = TanyaPrintFontFamily, maxLines = 2,
                    overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Start
                )
                Text("הצג עוד", color = accent, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = TanyaPrintFontFamily, modifier = Modifier.padding(top = 2.dp))
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(WidgetPalette.divider(theme)))
            Row(Modifier.fillMaxWidth().height(46.dp).padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                (content?.buttons ?: WidgetContentLoader.buttonsFor(main, WidgetStudy.RAMBAM_ONE)).forEachIndexed { index, button ->
                    if (index > 0) Box(Modifier.width(1.dp).height(14.dp).background(WidgetPalette.divider(theme)))
                    Text(
                        button.label, color = WidgetPalette.accent(button.study, theme), fontSize = 16.sp,
                        fontWeight = FontWeight.Bold, fontFamily = TanyaPrintFontFamily, textAlign = TextAlign.Center, modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
