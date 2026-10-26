package com.example
import com.example.domain.content.DailyReadingProgress
import org.junit.Assert.*
import org.junit.Test
class DailyReadingProgressTest {
    @Test fun unfinishedDailyLessonCountsHalachotAcrossChapters() {
        assertEquals(.375f, DailyReadingProgress.ratio(listOf(10, 20, 10), emptySet(), 1, 5f), .0001f)
        assertEquals(.5f, DailyReadingProgress.ratio(listOf(10, 20, 10), setOf(1), -1, 0f), .0001f)
    }
    @Test fun completedDailyLessonIsFullAndUnreadLessonIsEmpty() {
        assertEquals(1f, DailyReadingProgress.ratio(listOf(10, 20, 10), setOf(0, 1, 2), -1, 0f), 0f)
        assertEquals(0f, DailyReadingProgress.ratio(listOf(10), emptySet(), -1, 0f), 0f)
        assertEquals(0f, DailyReadingProgress.ratio(emptyList(), emptySet(), -1, 0f), 0f)
    }
}
