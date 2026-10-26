package com.example.widget

import androidx.compose.ui.graphics.Color

/** A study the widget can show or open. [key] matches the app's track keys. */
enum class WidgetStudy(val key: String, val label: String) {
    HAYOM_YOM("hayom_yom", "היום יום"),
    CHUMASH("chumash", "חומש"),
    TEHILLIM("tehillim", "תהילים"),
    TANYA("tanya", "תניא"),
    RAMBAM_ONE("one", "רמב״ם"),
    RAMBAM_THREE("three", "רמב״ם"),
    MITZVOT("mitzvot", "רמב״ם");

    companion object {
        fun fromKey(key: String?): WidgetStudy? = entries.firstOrNull { it.key == key }
    }
}

/** What the user can choose as the widget's main text. [RAMBAM] follows the user's own track. */
enum class WidgetMainLesson(val key: String, val label: String, val hint: String) {
    HAYOM_YOM("hayom_yom", "היום יום", "ברירת מחדל"),
    TANYA("tanya", "תניא", "השיעור היומי"),
    CHUMASH("chumash", "חומש", "העלייה של היום"),
    TEHILLIM("tehillim", "תהילים", "לפי ימי החודש"),
    RAMBAM("rambam", "רמב״ם", "לפי המסלול שנבחר באפליקציה"),
    MITZVOT("mitzvot", "ספר המצוות", "שיעור היום");

    companion object {
        fun fromKey(key: String?): WidgetMainLesson = entries.firstOrNull { it.key == key } ?: HAYOM_YOM
    }
}

enum class WidgetTheme(val key: String, val label: String) {
    DARK("dark", "שחור"),
    LIGHT("light", "לבן");

    companion object {
        fun fromKey(key: String?): WidgetTheme = entries.firstOrNull { it.key == key } ?: DARK
    }
}

data class WidgetConfig(
    val theme: WidgetTheme = WidgetTheme.DARK,
    val mainLesson: WidgetMainLesson = WidgetMainLesson.HAYOM_YOM
)

data class WidgetBlock(val text: String, val isHeading: Boolean = false)

data class WidgetButton(val study: WidgetStudy, val label: String)

data class WidgetContent(
    val main: WidgetStudy,
    val title: String,
    val subtitle: String,
    val blocks: List<WidgetBlock>,
    val buttons: List<WidgetButton>,
    /** Shown instead of [blocks] when today's text cannot be loaded. */
    val unavailableMessage: String? = null
)

/**
 * Lesson colours match the home-screen study strips (HomeNewViews.kt). On the white
 * background the same hues are deepened so text-only buttons keep a 4.5:1 contrast.
 */
object WidgetPalette {
    fun accent(study: WidgetStudy, theme: WidgetTheme): Color = when (theme) {
        WidgetTheme.DARK -> when (study) {
            WidgetStudy.HAYOM_YOM -> Color(0xFFC5B8F5)
            WidgetStudy.CHUMASH -> Color(0xFFF4F66A)
            WidgetStudy.TEHILLIM -> Color(0xFF9FDEEF)
            WidgetStudy.TANYA -> Color(0xFFF0A0DD)
            WidgetStudy.RAMBAM_ONE, WidgetStudy.RAMBAM_THREE -> Color(0xFFFF873F)
            WidgetStudy.MITZVOT -> Color(0xFF72D7E8)
        }
        WidgetTheme.LIGHT -> when (study) {
            WidgetStudy.HAYOM_YOM -> Color(0xFF5B3FC4)
            WidgetStudy.CHUMASH -> Color(0xFF7A6500)
            WidgetStudy.TEHILLIM -> Color(0xFF0B6A8A)
            WidgetStudy.TANYA -> Color(0xFFA21C83)
            WidgetStudy.RAMBAM_ONE, WidgetStudy.RAMBAM_THREE -> Color(0xFFC2410C)
            WidgetStudy.MITZVOT -> Color(0xFF0E7490)
        }
    }

    fun background(theme: WidgetTheme) = if (theme == WidgetTheme.DARK) Color(0xFF141416) else Color(0xFFFFFFFF)
    fun text(theme: WidgetTheme) = if (theme == WidgetTheme.DARK) Color(0xFFF2F2F0) else Color(0xFF17120F)
    fun muted(theme: WidgetTheme) = if (theme == WidgetTheme.DARK) Color(0xFF8E8E93) else Color(0xFF7A7570)
    fun divider(theme: WidgetTheme) = if (theme == WidgetTheme.DARK) Color(0x1FFFFFFF) else Color(0x14000000)
}
