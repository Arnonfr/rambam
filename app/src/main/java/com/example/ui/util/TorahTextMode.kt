package com.example.ui.util

/**
 * How the Chumash and שניים מקרא readers show the text: the verse keeps or drops
 * its cantillation and vowels; Rashi and Onkelos have no cantillation, so they
 * only lose their vowels in PLAIN.
 */
enum class TorahTextMode(val id: String, val label: String) {
    TEAMIM("teamim", "ניקוד וטעמים"),
    NIKUD("nikud", "ניקוד בלבד"),
    PLAIN("plain", "ללא ניקוד");

    fun verse(textWithTeamim: String): String = when (this) {
        TEAMIM -> textWithTeamim
        NIKUD -> HebrewTextNormalizer.withoutBiblicalAnnotations(textWithTeamim)
        PLAIN -> HebrewTextNormalizer.stripMarks(HebrewTextNormalizer.withoutBiblicalAnnotations(textWithTeamim))
    }

    fun commentary(text: String): String =
        if (this == PLAIN) HebrewTextNormalizer.stripMarks(text) else text

    companion object {
        fun fromId(id: String?): TorahTextMode = entries.firstOrNull { it.id == id } ?: NIKUD
    }
}
