package com.example.domain.chumash

data class ChumashVerse(
    val verseNumber: Int,
    val verseHebrew: String,
    val textWithTeamim: String,
    val textPlainNikud: String,
    val rashi: List<String> = emptyList()
)

data class ChumashAliya(
    val aliyaIndex: Int, // 1..7 (ראשון, שני, שלישי, רביעי, חמישי, ששי, שביעי)
    val aliyaName: String,
    val dayName: String,
    val ref: String,
    val heRef: String,
    val verses: List<ChumashVerse>,
    val isCompleted: Boolean = false
)

data class ChumashParasha(
    val parashaName: String,
    val bookName: String,
    val fullRef: String,
    val aliyot: List<ChumashAliya>
)

data class DailyChumashLesson(
    val date: String,
    val parashaName: String,
    val bookName: String,
    val currentAliyaIndex: Int,
    val todayAliya: ChumashAliya?,
    val allAliyot: List<ChumashAliya>,
    val completedCount: Int
)
