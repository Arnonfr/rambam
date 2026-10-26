package com.example.widget

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.UserPreferences
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DailyStudyWidgetIntegrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test fun widgetPickerPreviewCanBeInflatedAsRemoteViews() {
        val preview = android.widget.RemoteViews(context.packageName, com.example.R.layout.daily_study_widget_preview)
            .apply(context, android.widget.FrameLayout(context))
        assertNotNull(preview)
    }

    @Test fun widgetProviderAndConfigurationAreRegisteredInTheAppManifest() {
        val manager = context.packageManager
        val receiver = manager.getReceiverInfo(ComponentName(context, DailyStudyWidgetReceiver::class.java), PackageManager.GET_META_DATA)
        assertTrue(receiver.exported)
        assertEquals(com.example.R.xml.daily_study_widget_info, receiver.metaData.getInt("android.appwidget.provider"))
        val activity = manager.getActivityInfo(ComponentName(context, DailyStudyWidgetConfigureActivity::class.java), 0)
        assertTrue(activity.exported)
        assertTrue(manager.queryBroadcastReceivers(Intent("android.appwidget.action.APPWIDGET_UPDATE").setPackage(context.packageName), 0)
            .any { it.activityInfo.name == DailyStudyWidgetReceiver::class.java.name })
    }

    @Test fun eachWidgetShortcutTargetsTheMainActivityAndIsConsumedOnce() {
        val uris = mutableSetOf<String>()
        WidgetStudy.entries.forEach { study ->
            val intent = WidgetLaunch.intent(context, study)
            assertEquals(com.example.MainActivity::class.java.name, intent.component?.className)
            assertTrue(intent.flags and Intent.FLAG_ACTIVITY_SINGLE_TOP != 0)
            assertTrue(uris.add(intent.data.toString()))
            assertEquals(study, WidgetLaunch.consume(intent))
            assertNull(WidgetLaunch.consume(intent))
        }
        assertNull(WidgetLaunch.consume(Intent(Intent.ACTION_MAIN)))
    }

    @Test fun widgetUsesTheIsraelDayAndHonorsMidnightOrSunsetAcrossTheBoundary() {
        val engine = WidgetContentLoader.engine(context)
        val date = LocalDate.of(2026, 10, 10)
        val midnightPrefs = UserPreferences(dayBoundary = "midnight")
        val midnight = date.plusDays(1).atStartOfDay(WidgetStudyDay.zone).toInstant()
        assertEquals(date, WidgetStudyDay.today(engine, midnightPrefs, midnight.minusSeconds(1)))
        assertEquals(date.plusDays(1), WidgetStudyDay.today(engine, midnightPrefs, midnight))
        assertEquals(midnight, WidgetStudyDay.nextBoundary(engine, midnightPrefs, midnight.minusSeconds(1)))
        val sunsetPrefs = UserPreferences(dayBoundary = "sunset")
        val sunset = requireNotNull(engine.getSunsetInstant(date, sunsetPrefs.selectedCity, WidgetStudyDay.zone))
        assertEquals(date, WidgetStudyDay.today(engine, sunsetPrefs, sunset.minusSeconds(1)))
        assertEquals(date.plusDays(1), WidgetStudyDay.today(engine, sunsetPrefs, sunset.plusSeconds(1)))
        assertEquals(sunset, WidgetStudyDay.nextBoundary(engine, sunsetPrefs, sunset.minusSeconds(1)))
        assertTrue(WidgetStudyDay.nextBoundary(engine, sunsetPrefs, sunset.plusSeconds(1)).isAfter(sunset))
    }
}
