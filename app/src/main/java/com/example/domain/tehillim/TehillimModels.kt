package com.example.domain.tehillim

data class TehillimVerse(
    val verseNumber: Int,
    val verseHebrew: String,
    val textWithTeamim: String,
    val textPlainNikud: String
)

data class TehillimChapter(
    val chapterNumber: Int,
    val chapterHebrew: String,
    val verses: List<TehillimVerse>
)

data class DailyTehillimLesson(
    val date: String,
    val dayOfMonth: Int,
    val dayOfMonthHebrew: String,
    val chapters: List<TehillimChapter>,
    val elulChapters: List<TehillimChapter> = emptyList(),
    val elulDaysDescription: String? = null
)
