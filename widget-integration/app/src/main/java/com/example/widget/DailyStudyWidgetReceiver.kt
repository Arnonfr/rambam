package com.example.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.example.data.local.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DailyStudyWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DailyStudyWidget()

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        DailyStudyWidgetUpdater.scope.launch { WidgetRefreshScheduler.scheduleNext(context) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            WidgetRefreshScheduler.ACTION_DAY_BOUNDARY,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                val pending = goAsync()
                DailyStudyWidgetUpdater.scope.launch {
                    try {
                        DailyStudyWidgetUpdater.refreshAll(context)
                    } finally {
                        pending.finish()
                    }
                }
            }
        }
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WidgetRefreshScheduler.cancel(context)
    }
}

/** Entry point for the app: refresh every placed widget with today's content and settings. */
object DailyStudyWidgetUpdater {
    internal val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    suspend fun refreshAll(context: Context) {
        val widget = DailyStudyWidget()
        val ids = GlanceAppWidgetManager(context).getGlanceIds(DailyStudyWidget::class.java)
        ids.forEach { id ->
            WidgetConfigStore.bumpRefreshToken(context, id)
            widget.update(context, id)
        }
        if (ids.isNotEmpty()) WidgetRefreshScheduler.scheduleNext(context)
    }

    /** Fire-and-forget version for Activity callbacks such as onStop. */
    fun requestRefresh(context: Context) {
        val appContext = context.applicationContext
        scope.launch { runCatching { refreshAll(appContext) } }
    }
}

/** Wakes the widget when the study day changes: midnight, or sunset if the user chose it. */
object WidgetRefreshScheduler {
    const val ACTION_DAY_BOUNDARY = "com.example.widget.action.DAY_BOUNDARY"
    private const val GRACE_MS = 60_000L

    suspend fun scheduleNext(context: Context) {
        val prefs = UserPreferencesRepository(context.applicationContext).userPreferencesFlow.first()
        val next = WidgetStudyDay.nextBoundary(WidgetContentLoader.engine(context), prefs)
        val alarms = context.getSystemService(AlarmManager::class.java) ?: return
        // A non-wakeup alarm: it fires as soon as the screen is on, which is when the widget is seen.
        alarms.set(AlarmManager.RTC, next.toEpochMilli() + GRACE_MS, pendingIntent(context))
    }

    fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java)?.cancel(pendingIntent(context))
    }

    private fun pendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, DailyStudyWidgetReceiver::class.java).setAction(ACTION_DAY_BOUNDARY),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}
