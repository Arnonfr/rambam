package com.example.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.data.tanya.TanyaAvailableLesson
import com.example.domain.tanya.DailyTanyaLesson

/** Lessons for the current chapter first, then audio, then video (which opens outside the app). */
internal fun sortedLessons(records: List<TanyaAvailableLesson>, lesson: DailyTanyaLesson) =
    records.sortedWith(compareBy({ !it.matches(lesson) }, { it.kind != "audio" }))

/** Every row plays on tap; the playing row takes the lesson colour. No nested scroll: the sheet scrolls. */
@Composable
internal fun TanyaAvailableLessonPicker(
    records: List<TanyaAvailableLesson>, selectedId: String?, lesson: DailyTanyaLesson,
    colors: PlayerColors, playingId: String?, playing: Boolean, onSelect: (TanyaAvailableLesson) -> Unit
) {
    androidx.compose.foundation.layout.Column(Modifier.testTag("tanya_available_lessons")) {
        sortedLessons(records, lesson).forEach { record ->
            AvailableLessonRow(record, lesson, colors, playing = playing && record.id == playingId,
                current = record.id == playingId || record.id == selectedId) { onSelect(record) }
        }
    }
}

@Composable
internal fun AvailableLessonRow(record: TanyaAvailableLesson, lesson: DailyTanyaLesson, colors: PlayerColors,
    playing: Boolean, current: Boolean, onClick: () -> Unit) {
    val (name, date) = splitLessonTitle(record.title)
    val video = record.kind == "youtube"
    PlayerLessonRow(
        title = if (video) "שיעור וידאו · $name" else name,
        meta = if (video) "נפתח באתר המקור" else date,
        colors = colors,
        action = when { video -> LessonRowAction.EXTERNAL; playing -> LessonRowAction.PAUSE; else -> LessonRowAction.PLAY },
        onClick = onClick,
        modifier = Modifier.testTag("available_${record.id}"),
        highlighted = current && !video,
        badge = if (record.matches(lesson)) "לפרק הזה" else null
    )
}
