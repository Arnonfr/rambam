package com.example.data.local

import android.content.Context
import android.content.SharedPreferences

data class SavedReadingAnchor(
    val isReaderActive: Boolean,
    val track: String,
    val chapterId: String,
    val sectionId: String,
    val chapterNumber: Int,
    val halachaId: String,
    val halachaIndex: Int,
    val scrollOffsetFraction: Float,
    val studyDate: String,
    val updatedAt: Long
)

/**
 * Synchronous, low-level disk persistence for reading position.
 * Uses SharedPreferences with commit() so that positions are guaranteed
 * to be written to disk immediately without coroutine delays, surviving
 * abrupt process deaths, force-stops, and app switches.
 */
class ReadingStateManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("rambam_reading_anchor", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_IS_READER_ACTIVE = "is_reader_active"
        private const val KEY_TRACK = "track"
        private const val KEY_CHAPTER_ID = "chapter_id"
        private const val KEY_SECTION_ID = "section_id"
        private const val KEY_CHAPTER_NUMBER = "chapter_number"
        private const val KEY_HALACHA_ID = "halacha_id"
        private const val KEY_HALACHA_INDEX = "halacha_index"
        private const val KEY_OFFSET_FRACTION = "offset_fraction"
        private const val KEY_UPDATED_AT = "updated_at"
        private const val KEY_STUDY_DATE = "study_date"
    }

    fun saveAnchor(
        isReaderActive: Boolean,
        track: String,
        chapterId: String,
        sectionId: String,
        chapterNumber: Int,
        halachaId: String,
        halachaIndex: Int,
        scrollOffsetFraction: Float,
        studyDate: String,
        immediateCommit: Boolean = true
    ) {
        val editor = prefs.edit()
            .putBoolean(KEY_IS_READER_ACTIVE, isReaderActive)
            .putString(KEY_TRACK, track)
            .putString(KEY_CHAPTER_ID, chapterId)
            .putString(KEY_SECTION_ID, sectionId)
            .putInt(KEY_CHAPTER_NUMBER, chapterNumber)
            .putString(KEY_HALACHA_ID, halachaId)
            .putInt(KEY_HALACHA_INDEX, halachaIndex)
            .putFloat(KEY_OFFSET_FRACTION, scrollOffsetFraction)
            .putString(KEY_STUDY_DATE, studyDate)
            .putString("${KEY_STUDY_DATE}_$track", studyDate)
            .putLong(KEY_UPDATED_AT, System.currentTimeMillis())

        // Keep an independent crash-safe anchor for every study track. The
        // un-suffixed values above remain the "most recently read" anchor.
        editor
            .putBoolean(key(KEY_IS_READER_ACTIVE, track), isReaderActive)
            .putString(key(KEY_TRACK, track), track)
            .putString(key(KEY_CHAPTER_ID, track), chapterId)
            .putString(key(KEY_SECTION_ID, track), sectionId)
            .putInt(key(KEY_CHAPTER_NUMBER, track), chapterNumber)
            .putString(key(KEY_HALACHA_ID, track), halachaId)
            .putInt(key(KEY_HALACHA_INDEX, track), halachaIndex)
            .putFloat(key(KEY_OFFSET_FRACTION, track), scrollOffsetFraction)
            .putString(key(KEY_STUDY_DATE, track), studyDate)
            .putLong(key(KEY_UPDATED_AT, track), System.currentTimeMillis())

        if (immediateCommit) {
            editor.commit()
        } else {
            editor.apply()
        }
    }

    fun setReaderActive(isActive: Boolean) {
        prefs.edit().putBoolean(KEY_IS_READER_ACTIVE, isActive).commit()
    }

    fun getSavedAnchor(): SavedReadingAnchor? {
        val chapterId = prefs.getString(KEY_CHAPTER_ID, null) ?: return null
        return SavedReadingAnchor(
            isReaderActive = prefs.getBoolean(KEY_IS_READER_ACTIVE, false),
            track = prefs.getString(KEY_TRACK, "one") ?: "one",
            chapterId = chapterId,
            sectionId = prefs.getString(KEY_SECTION_ID, "") ?: "",
            chapterNumber = prefs.getInt(KEY_CHAPTER_NUMBER, 1),
            halachaId = prefs.getString(KEY_HALACHA_ID, "") ?: "",
            halachaIndex = prefs.getInt(KEY_HALACHA_INDEX, 0),
            scrollOffsetFraction = prefs.getFloat(KEY_OFFSET_FRACTION, 0f),
            studyDate = prefs.getString(KEY_STUDY_DATE, "") ?: "",
            updatedAt = prefs.getLong(KEY_UPDATED_AT, 0L)
        )
    }

    fun getSavedAnchor(track: String): SavedReadingAnchor? {
        val chapterId = prefs.getString(key(KEY_CHAPTER_ID, track), null) ?: return null
        return SavedReadingAnchor(
            isReaderActive = prefs.getBoolean(key(KEY_IS_READER_ACTIVE, track), false),
            track = prefs.getString(key(KEY_TRACK, track), track) ?: track,
            chapterId = chapterId,
            sectionId = prefs.getString(key(KEY_SECTION_ID, track), "") ?: "",
            chapterNumber = prefs.getInt(key(KEY_CHAPTER_NUMBER, track), 1),
            halachaId = prefs.getString(key(KEY_HALACHA_ID, track), "") ?: "",
            halachaIndex = prefs.getInt(key(KEY_HALACHA_INDEX, track), 0),
            scrollOffsetFraction = prefs.getFloat(key(KEY_OFFSET_FRACTION, track), 0f),
            studyDate = prefs.getString(key(KEY_STUDY_DATE, track), "") ?: "",
            updatedAt = prefs.getLong(key(KEY_UPDATED_AT, track), 0L)
        )
    }

    fun getStudyDate(track: String): String? =
        prefs.getString("${KEY_STUDY_DATE}_$track", null)

    fun isStudyDateCurrent(track: String, studyDate: String): Boolean =
        getStudyDate(track) == studyDate

    fun clear() {
        prefs.edit().clear().commit()
    }

    private fun key(base: String, track: String): String = "${base}_$track"
}
