package com.example.widget

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.os.Looper
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceRemoteViews
import androidx.test.core.app.ApplicationProvider
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders the real widget RemoteViews with today's bundled content.
 * Images are written to app/build/outputs/roborazzi/widget/.
 */
@OptIn(ExperimentalGlanceRemoteViewsApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "iw-rIL-w412dp-h892dp-xxhdpi")
class DailyStudyWidgetRenderTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun load(lesson: WidgetMainLesson) =
        runBlocking { WidgetContentLoader(context).load(WidgetConfig(mainLesson = lesson)) }

    private fun render(name: String, content: WidgetContent, theme: WidgetTheme, widthDp: Int, heightDp: Int) {
        val result = runBlocking {
            GlanceRemoteViews().compose(context, DpSize(widthDp.dp, heightDp.dp)) {
                DailyStudyWidgetContent(content, theme)
            }
        }
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val density = activity.resources.displayMetrics.density
        val frame = FrameLayout(activity).apply {
            setBackgroundColor(Color.rgb(0xE9, 0xE9, 0xE9))
            val pad = (20 * density).toInt()
            setPadding(pad, pad, pad, pad)
        }
        val widget = result.remoteViews.apply(activity, frame)
        frame.addView(widget, FrameLayout.LayoutParams((widthDp * density).toInt(), (heightDp * density).toInt()))
        activity.setContentView(frame, ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        shadowOf(Looper.getMainLooper()).idle()
        frame.captureRoboImage("build/outputs/roborazzi/widget/$name.png")
    }

    @Test
    fun hayomYomInAllSizesAndThemes() {
        val content = load(WidgetMainLesson.HAYOM_YOM)
        assertTrue(content.blocks.isNotEmpty())
        render("hayom_yom_4x2_dark", content, WidgetTheme.DARK, 340, 170)
        render("hayom_yom_4x2_light", content, WidgetTheme.LIGHT, 340, 170)
        render("hayom_yom_3x2_dark", content, WidgetTheme.DARK, 260, 170)
        render("hayom_yom_4x3_dark", content, WidgetTheme.DARK, 340, 260)
        render("hayom_yom_4x3_light", content, WidgetTheme.LIGHT, 340, 260)
        render("hayom_yom_4x5_dark", content, WidgetTheme.DARK, 340, 470)
    }

    @Test
    fun longHayomYomIsTruncatedThenShownInFull() {
        val repository = com.example.data.hayomyom.HayomYomRepository(context)
        val start = java.time.LocalDate.now()
        val longest = (0L until 380L).mapNotNull { repository.lessonFor(start.plusDays(it)) }
            .maxBy { lesson -> HayomYomHebrew.paragraphs(lesson.paragraphs).sumOf { it.length } }
        val content = load(WidgetMainLesson.HAYOM_YOM).copy(
            subtitle = longest.dateLabel,
            blocks = HayomYomHebrew.paragraphs(longest.paragraphs).map { WidgetBlock(it) }
        )
        render("hayom_yom_long_4x2_dark", content, WidgetTheme.DARK, 340, 170)
        render("hayom_yom_long_4x4_dark", content, WidgetTheme.DARK, 340, 360)
    }

    @get:org.junit.Rule val composeRule = androidx.compose.ui.test.junit4.createComposeRule()

    @Test
    fun configurationScreen() {
        composeRule.setContent {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl
            ) {
                androidx.compose.material3.MaterialTheme {
                    WidgetConfigScreen(WidgetConfig(), onCancel = {}, onSave = {})
                }
            }
        }
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodes(androidx.compose.ui.test.hasText("טוען…")).fetchSemanticsNodes().isEmpty()
        }
        composeRule.onRoot().captureRoboImage("build/outputs/roborazzi/widget/configuration_screen.png")
    }

    @Test
    fun otherMainLessons() {
        render("mitzvot_4x2_dark", load(WidgetMainLesson.MITZVOT), WidgetTheme.DARK, 340, 170)
        render("chumash_4x3_light", load(WidgetMainLesson.CHUMASH), WidgetTheme.LIGHT, 340, 260)
        render("tehillim_4x2_light", load(WidgetMainLesson.TEHILLIM), WidgetTheme.LIGHT, 340, 170)
    }
}
