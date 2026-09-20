package com.example.data.repository

import com.example.data.local.ChapterCompletionEntity
import com.example.data.local.ChapterEntity
import com.example.data.local.ContentSectionEntity
import com.example.data.local.HalachaEntity
import com.example.data.local.RambamDao
import com.example.data.local.ReadingPositionEntity
import com.example.domain.schedule.DailyLessonResult
import com.example.domain.schedule.DailyScheduleEngine
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.ZoneId

class RambamRepository(
    private val rambamDao: RambamDao,
    private val scheduleEngine: DailyScheduleEngine
) {

    val allSections: Flow<List<ContentSectionEntity>> = rambamDao.getAllSections()

    fun getChaptersForSection(sectionId: String): Flow<List<ChapterEntity>> {
        return rambamDao.getChaptersForSection(sectionId)
    }

    suspend fun getChapterById(chapterId: String): ChapterEntity? {
        return rambamDao.getChapterById(chapterId)
    }

    fun getHalachotForChapter(chapterId: String): Flow<List<HalachaEntity>> {
        return rambamDao.getHalachotForChapter(chapterId)
    }

    suspend fun getHalachotForChapterList(chapterId: String): List<HalachaEntity> {
        return rambamDao.getHalachotForChapterList(chapterId)
    }

    fun getLatestReadingPosition(track: String): Flow<ReadingPositionEntity?> {
        return rambamDao.getLatestReadingPosition(track)
    }

    fun getAbsoluteLatestReadingPosition(): Flow<ReadingPositionEntity?> {
        return rambamDao.getAbsoluteLatestReadingPosition()
    }

    suspend fun getReadingPosition(track: String, chapterId: String): ReadingPositionEntity? {
        return rambamDao.getReadingPosition(track, chapterId)
    }

    suspend fun saveReadingPosition(position: ReadingPositionEntity) {
        rambamDao.saveReadingPosition(position)
    }

    fun getCompletion(track: String, chapterId: String): Flow<ChapterCompletionEntity?> {
        return rambamDao.getCompletion(track, chapterId)
    }

    fun getCompletionsForDate(track: String, studyDate: String): Flow<List<ChapterCompletionEntity>> {
        return rambamDao.getCompletionsForDate(track, studyDate)
    }

    suspend fun setChapterCompleted(track: String, chapterId: String, studyDate: String) {
        rambamDao.setChapterCompleted(
            ChapterCompletionEntity(
                track = track,
                chapterId = chapterId,
                studyDate = studyDate,
                completedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun removeChapterCompletion(track: String, chapterId: String) {
        rambamDao.removeChapterCompletion(track, chapterId)
    }

    fun getDailyLesson(
        instant: Instant = Instant.now(),
        zoneId: ZoneId = ZoneId.systemDefault(),
        track: String = "one",
        dayBoundary: String = "midnight",
        cityName: String = "ירושלים"
    ): DailyLessonResult {
        return scheduleEngine.getDailyLesson(
            instant = instant,
            zoneId = zoneId,
            track = track,
            dayBoundary = dayBoundary,
            cityName = cityName
        )
    }

    suspend fun getNextChapter(currentChapterId: String): ChapterEntity? {
        val current = rambamDao.getChapterById(currentChapterId) ?: return null
        // Try next chapter in same section
        val nextInSameSectionId = "${current.sectionId}_${current.chapterNumber + 1}"
        val nextInSame = rambamDao.getChapterById(nextInSameSectionId)
        if (nextInSame != null) return nextInSame

        // Cross-section progression across all 14 books
        val currentSection = rambamDao.getSectionById(current.sectionId) ?: return null
        val nextSection = rambamDao.getNextSection(currentSection.orderIndex) ?: return null
        return rambamDao.getFirstChapterForSection(nextSection.sectionId)
    }

    suspend fun getPreviousChapter(currentChapterId: String): ChapterEntity? {
        val current = rambamDao.getChapterById(currentChapterId) ?: return null
        if (current.chapterNumber > 1) {
            val prevInSame = rambamDao.getChapterById("${current.sectionId}_${current.chapterNumber - 1}")
            if (prevInSame != null) return prevInSame
        }
        val currentSection = rambamDao.getSectionById(current.sectionId) ?: return null
        val prevSection = rambamDao.getPreviousSection(currentSection.orderIndex) ?: return null
        return rambamDao.getLastChapterForSection(prevSection.sectionId)
    }
}
