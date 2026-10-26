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
    val isCompleted: Boolean = false,
    val originalStudies: OriginalHayomYomStudies? = null,
    val additionalOriginalStudies: List<OriginalHayomYomStudies> = emptyList()
)

data class OriginalHayomYomStudies(
    val dateText: String,
    val chumash: String,
    val tehillim: String,
    val tanya: String,
    val note: String = "",
    val sourceUrl: String
)
