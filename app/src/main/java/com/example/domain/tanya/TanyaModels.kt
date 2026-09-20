package com.example.domain.tanya

data class TanyaSection(
    val sectionIndex: Int,
    val sectionHebrew: String,
    val textWithNikud: String,
    val textPlain: String
)

data class DailyTanyaLesson(
    val date: String,
    val hebrewDate: String,
    val fullRef: String,
    val heRef: String,
    val bookTitle: String,
    val chapterTitle: String,
    val sections: List<TanyaSection>,
    val isCompleted: Boolean = false
)
