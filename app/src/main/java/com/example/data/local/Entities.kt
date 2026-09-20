package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "content_sections")
data class ContentSectionEntity(
    @PrimaryKey val sectionId: String,
    val workId: String = "mishneh-torah",
    val titleHebrew: String,
    val chapterCount: Int,
    val orderIndex: Int
)

@Entity(tableName = "chapters")
data class ChapterEntity(
    @PrimaryKey val id: String, // e.g. "ishut_17"
    val sectionId: String,
    val chapterNumber: Int,
    val chapterHebrew: String,
    val halachotCount: Int,
    val sourceCredit: String,
    val license: String
)

@Entity(tableName = "halachot")
data class HalachaEntity(
    @PrimaryKey val id: String, // e.g. "ishut_17_1"
    val chapterId: String,
    val sectionId: String,
    val chapterNumber: Int,
    val halachaNumber: Int,
    val halachaHebrew: String,
    val letter: String,
    val textWithNikud: String,
    val textPlain: String,
    val quoteFingerprint: String
)

@Entity(
    tableName = "reading_positions",
    primaryKeys = ["track", "chapterId"]
)
data class ReadingPositionEntity(
    val track: String, // "one" or "three"
    val chapterId: String,
    val sectionId: String,
    val chapterNumber: Int,
    val halachaId: String,
    val halachaIndex: Int,
    val textOffset: Int,
    val scrollOffsetFraction: Float,
    val quoteFingerprint: String,
    val contentVersion: String = "2026.09",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "chapter_completions",
    primaryKeys = ["track", "chapterId", "studyDate"]
)
data class ChapterCompletionEntity(
    val track: String,
    val chapterId: String,
    val studyDate: String, // YYYY-MM-DD
    val completedAt: Long = System.currentTimeMillis()
)
