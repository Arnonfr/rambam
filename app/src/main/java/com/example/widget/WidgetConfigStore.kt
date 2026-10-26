package com.example.widget

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.example.ui.theme.FontStyleOption

/** Per-widget settings, kept in Glance's per-widget DataStore. */
object WidgetConfigStore {
    private val THEME = stringPreferencesKey("widget_theme")
    private val MAIN_LESSON = stringPreferencesKey("widget_main_lesson")
    private val FONT = stringPreferencesKey("widget_font")
    private val REFRESH_TOKEN = longPreferencesKey("widget_refresh_token")

    fun read(prefs: Preferences) = WidgetConfig(
        theme = WidgetTheme.fromKey(prefs[THEME]),
        mainLesson = WidgetMainLesson.fromKey(prefs[MAIN_LESSON]),
        font = prefs[FONT]?.let(FontStyleOption::fromId) ?: FontStyleOption.SERIF
    )

    /** Changes whenever the app asks for fresh content (new day, changed settings). */
    fun refreshToken(prefs: Preferences): Long = prefs[REFRESH_TOKEN] ?: 0L

    suspend fun load(context: Context, id: GlanceId): WidgetConfig =
        read(getAppWidgetState(context, PreferencesGlanceStateDefinition, id))

    suspend fun save(context: Context, id: GlanceId, config: WidgetConfig) {
        updateAppWidgetState(context, id) { prefs ->
            prefs[THEME] = config.theme.key
            prefs[MAIN_LESSON] = config.mainLesson.key
            prefs[FONT] = config.font.id
        }
    }

    suspend fun bumpRefreshToken(context: Context, id: GlanceId) {
        updateAppWidgetState(context, id) { prefs -> prefs[REFRESH_TOKEN] = System.currentTimeMillis() }
    }
}
