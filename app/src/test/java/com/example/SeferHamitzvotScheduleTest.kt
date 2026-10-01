package com.example

import com.example.domain.mitzvot.MitzvahAssignmentType
import com.example.domain.mitzvot.SeferHamitzvotSchedule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SeferHamitzvotScheduleTest {
    @Test
    fun `cycle contains every official daily lesson`() {
        val lessons = SeferHamitzvotSchedule.allAssignments()

        assertEquals(339, lessons.size)
        assertEquals(LocalDate.of(2026, 2, 3), lessons.first().studyDate)
        assertEquals(LocalDate.of(2027, 1, 7), lessons.last().studyDate)
    }

    @Test
    fun `current date resolves to lesson 237 and positive commandment 96`() {
        val lesson = SeferHamitzvotSchedule.assignmentFor(LocalDate.of(2026, 9, 27))!!

        assertEquals(237, lesson.lessonNumber)
        assertEquals(MitzvahAssignmentType.POSITIVE, lesson.items.single().type)
        assertEquals(96, lesson.items.single().number)
    }

    @Test
    fun `schedule supports multiple mitzvot and non mitzvah readings`() {
        val multiple = SeferHamitzvotSchedule.assignmentFor(LocalDate.of(2026, 5, 5))!!
        val special = SeferHamitzvotSchedule.assignmentFor(LocalDate.of(2026, 4, 11))!!

        assertEquals(7, multiple.items.size)
        assertTrue(special.items.any { it.type == MitzvahAssignmentType.SPECIAL })
    }

    @Test
    fun `dates outside the bundled cycle do not invent a lesson`() {
        assertNull(SeferHamitzvotSchedule.assignmentFor(LocalDate.of(2026, 2, 2)))
        assertNull(SeferHamitzvotSchedule.assignmentFor(LocalDate.of(2027, 1, 8)))
    }
}
