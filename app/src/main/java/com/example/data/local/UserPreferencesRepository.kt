package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

data class UserPreferences(
    val selectedTrack: String = "one", // "one" or "three"
    val dayBoundary: String = "midnight", // "midnight" or "sunset"
    val selectedCity: String = "ירושלים",
    val fontSizeSp: Float = 20f,
    val lineSpacingMultiplier: Float = 1.65f,
    val fontFamily: String = "bonanova", // "bonanova", "sans", "serif", "libertinus"
    val showNikud: Boolean = true,
    val readerTheme: String = "light", // "light", "sepia", "dark"
    val lastOpenedChapterId: String? = null,
    val keepScreenOn: Boolean = true,
    val visibleStudies: Set<String> = setOf("rambam", "chumash", "tehillim", "tanya"),
    val chumashFontSizeSp: Float = 22f,
    val chumashShowTeamim: Boolean = false,
    val completedAliyot: Set<String> = emptySet()
)

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val SELECTED_TRACK = stringPreferencesKey("selected_track")
        val DAY_BOUNDARY = stringPreferencesKey("day_boundary")
        val SELECTED_CITY = stringPreferencesKey("selected_city")
        val FONT_SIZE_SP = floatPreferencesKey("font_size_sp")
        val LINE_SPACING = floatPreferencesKey("line_spacing_multiplier")
        val FONT_FAMILY = stringPreferencesKey("font_family")
        val SHOW_NIKUD = booleanPreferencesKey("show_nikud")
        val READER_THEME = stringPreferencesKey("reader_theme")
        val LAST_OPENED_CHAPTER_ID = stringPreferencesKey("last_opened_chapter_id")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val VISIBLE_STUDIES = stringSetPreferencesKey("visible_studies")
        val CHUMASH_FONT_SIZE = floatPreferencesKey("chumash_font_size")
        val CHUMASH_SHOW_TEAMIM = booleanPreferencesKey("chumash_show_teamim")
        val COMPLETED_ALIYOT = stringSetPreferencesKey("completed_aliyot")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data
        .map { preferences ->
            UserPreferences(
                selectedTrack = preferences[PreferencesKeys.SELECTED_TRACK] ?: "one",
                dayBoundary = preferences[PreferencesKeys.DAY_BOUNDARY] ?: "midnight",
                selectedCity = preferences[PreferencesKeys.SELECTED_CITY] ?: "ירושלים",
                fontSizeSp = preferences[PreferencesKeys.FONT_SIZE_SP] ?: 20f,
                lineSpacingMultiplier = preferences[PreferencesKeys.LINE_SPACING] ?: 1.65f,
                fontFamily = preferences[PreferencesKeys.FONT_FAMILY] ?: "bonanova",
                showNikud = preferences[PreferencesKeys.SHOW_NIKUD] ?: true,
                readerTheme = preferences[PreferencesKeys.READER_THEME] ?: "light",
                lastOpenedChapterId = preferences[PreferencesKeys.LAST_OPENED_CHAPTER_ID],
                keepScreenOn = preferences[PreferencesKeys.KEEP_SCREEN_ON] ?: true,
                visibleStudies = preferences[PreferencesKeys.VISIBLE_STUDIES] ?: setOf("rambam", "chumash", "tehillim", "tanya"),
                chumashFontSizeSp = preferences[PreferencesKeys.CHUMASH_FONT_SIZE] ?: 22f,
                chumashShowTeamim = preferences[PreferencesKeys.CHUMASH_SHOW_TEAMIM] ?: false,
                completedAliyot = preferences[PreferencesKeys.COMPLETED_ALIYOT] ?: emptySet()
            )
        }

    suspend fun updateTrack(track: String) {
        context.dataStore.edit { it[PreferencesKeys.SELECTED_TRACK] = track }
    }

    suspend fun updateDayBoundary(boundary: String) {
        context.dataStore.edit { it[PreferencesKeys.DAY_BOUNDARY] = boundary }
    }

    suspend fun updateCity(city: String) {
        context.dataStore.edit { it[PreferencesKeys.SELECTED_CITY] = city }
    }

    suspend fun updateFontSize(sizeSp: Float) {
        context.dataStore.edit { it[PreferencesKeys.FONT_SIZE_SP] = sizeSp.coerceIn(16f, 30f) }
    }

    suspend fun updateLineSpacing(multiplier: Float) {
        context.dataStore.edit { it[PreferencesKeys.LINE_SPACING] = multiplier.coerceIn(1.3f, 2.2f) }
    }

    suspend fun updateFontFamily(family: String) {
        context.dataStore.edit { it[PreferencesKeys.FONT_FAMILY] = family }
    }

    suspend fun updateShowNikud(show: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.SHOW_NIKUD] = show }
    }

    suspend fun updateReaderTheme(theme: String) {
        context.dataStore.edit { it[PreferencesKeys.READER_THEME] = theme }
    }

    suspend fun setLastOpenedChapterId(chapterId: String) {
        context.dataStore.edit { it[PreferencesKeys.LAST_OPENED_CHAPTER_ID] = chapterId }
    }

    suspend fun updateKeepScreenOn(keep: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.KEEP_SCREEN_ON] = keep }
    }

    suspend fun updateVisibleStudies(studies: Set<String>) {
        context.dataStore.edit { it[PreferencesKeys.VISIBLE_STUDIES] = studies }
    }

    suspend fun updateChumashFontSize(sizeSp: Float) {
        context.dataStore.edit { it[PreferencesKeys.CHUMASH_FONT_SIZE] = sizeSp.coerceIn(16f, 36f) }
    }

    suspend fun updateChumashShowTeamim(show: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.CHUMASH_SHOW_TEAMIM] = show }
    }

    suspend fun toggleAliyaCompletion(parasha: String, aliyaIndex: Int) {
        val key = "$parasha-$aliyaIndex"
        context.dataStore.edit { prefs ->
            val current = prefs[PreferencesKeys.COMPLETED_ALIYOT]?.toMutableSet() ?: mutableSetOf()
            if (current.contains(key)) {
                current.remove(key)
            } else {
                current.add(key)
            }
            prefs[PreferencesKeys.COMPLETED_ALIYOT] = current
        }
    }
}
