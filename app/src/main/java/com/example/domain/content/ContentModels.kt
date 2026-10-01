package com.example.domain.content

enum class ContentKind {
    RAMBAM,
    SEFER_HAMITZVOT,
    CHUMASH,
    TANYA,
    TEHILLIM,
    PRAYER
}

enum class ContentBlockKind {
    HEADING,
    PARAGRAPH,
    INSTRUCTION,
    PAGE_IMAGE
}

data class ContentDocument(
    val id: String,
    val kind: ContentKind,
    val title: String,
    val subtitle: String = "",
    val sections: List<ContentSection>,
    val sourceName: String,
    val sourceUrl: String,
    val license: String,
    val contentVersion: String
)

data class ContentSection(
    val id: String,
    val title: String,
    val order: Int,
    val blocks: List<ContentBlock>,
    val displayConditions: Set<String> = emptySet()
)

data class ContentBlock(
    val id: String,
    val kind: ContentBlockKind,
    val textWithNikud: String = "",
    val textPlain: String = "",
    val order: Int,
    val displayConditions: Set<String> = emptySet(),
    val pageNumber: Int? = null,
    val normalizedTop: Float? = null,
    val normalizedBottom: Float? = null
)

data class DailyContentAssignment(
    val contentId: String,
    val studyDate: String,
    val lessonNumber: Int? = null,
    val title: String,
    val itemIds: List<String>,
    val cycleNumber: Int? = null
)
