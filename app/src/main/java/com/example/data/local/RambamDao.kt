package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RambamDao {

    @Query("SELECT * FROM content_sections ORDER BY orderIndex ASC")
    fun getAllSections(): Flow<List<ContentSectionEntity>>

    @Query("SELECT * FROM chapters WHERE sectionId = :sectionId ORDER BY chapterNumber ASC")
    fun getChaptersForSection(sectionId: String): Flow<List<ChapterEntity>>

    @Query("SELECT * FROM chapters WHERE id = :chapterId LIMIT 1")
    suspend fun getChapterById(chapterId: String): ChapterEntity?

    @Query("SELECT * FROM halachot WHERE chapterId = :chapterId ORDER BY halachaNumber ASC")
    fun getHalachotForChapter(chapterId: String): Flow<List<HalachaEntity>>

    @Query("SELECT * FROM halachot WHERE chapterId = :chapterId ORDER BY halachaNumber ASC")
    suspend fun getHalachotForChapterList(chapterId: String): List<HalachaEntity>

    @Query("SELECT * FROM halachot WHERE id = :halachaId LIMIT 1")
    suspend fun getHalachaById(halachaId: String): HalachaEntity?

    @Query("SELECT * FROM reading_positions WHERE track = :track ORDER BY updatedAt DESC LIMIT 1")
    fun getLatestReadingPosition(track: String): Flow<ReadingPositionEntity?>

    @Query("SELECT * FROM reading_positions ORDER BY updatedAt DESC LIMIT 1")
    fun getAbsoluteLatestReadingPosition(): Flow<ReadingPositionEntity?>

    @Query("SELECT * FROM reading_positions WHERE track = :track AND chapterId = :chapterId LIMIT 1")
    suspend fun getReadingPosition(track: String, chapterId: String): ReadingPositionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveReadingPosition(position: ReadingPositionEntity)

    @Query("SELECT * FROM chapter_completions WHERE track = :track AND chapterId = :chapterId LIMIT 1")
    fun getCompletion(track: String, chapterId: String): Flow<ChapterCompletionEntity?>

    @Query("SELECT * FROM chapter_completions WHERE track = :track AND studyDate = :studyDate")
    fun getCompletionsForDate(track: String, studyDate: String): Flow<List<ChapterCompletionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setChapterCompleted(completion: ChapterCompletionEntity)

    @Query("DELETE FROM chapter_completions WHERE track = :track AND chapterId = :chapterId")
    suspend fun removeChapterCompletion(track: String, chapterId: String)

    @Query("SELECT COUNT(*) FROM halachot")
    suspend fun getHalachotCount(): Int

    @Query("SELECT * FROM content_sections WHERE sectionId = :sectionId LIMIT 1")
    suspend fun getSectionById(sectionId: String): ContentSectionEntity?

    @Query("SELECT * FROM content_sections WHERE orderIndex > :currentOrder ORDER BY orderIndex ASC LIMIT 1")
    suspend fun getNextSection(currentOrder: Int): ContentSectionEntity?

    @Query("SELECT * FROM content_sections WHERE orderIndex < :currentOrder ORDER BY orderIndex DESC LIMIT 1")
    suspend fun getPreviousSection(currentOrder: Int): ContentSectionEntity?

    @Query("SELECT * FROM chapters WHERE sectionId = :sectionId ORDER BY chapterNumber ASC LIMIT 1")
    suspend fun getFirstChapterForSection(sectionId: String): ChapterEntity?

    @Query("SELECT * FROM chapters WHERE sectionId = :sectionId ORDER BY chapterNumber DESC LIMIT 1")
    suspend fun getLastChapterForSection(sectionId: String): ChapterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSections(sections: List<ContentSectionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapters(chapters: List<ChapterEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHalachot(halachot: List<HalachaEntity>)
}
