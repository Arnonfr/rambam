package com.example

import com.example.domain.tanya.TanyaPrintLessonMatcher
import com.example.domain.tanya.TanyaPrintLessonRange
import org.junit.Assert.*
import org.junit.Test

class TanyaPrintLessonRangeTest {
    @Test fun boundariesCanStartAndEndWithinTheSamePrintedLine() {
        val range = TanyaPrintLessonMatcher.findUnique(
            listOf("לפני", "כי", "זה", "כל", "האדם", "אחרי"),
            listOf("כִּי", "זֶה", "כָּל", "הָאָדָם."))!!
        assertEquals(TanyaPrintLessonRange(1, 5), range)
        assertFalse(range.contains(0))
        assertTrue(range.contains(1))
        assertTrue(range.contains(4))
        assertFalse(range.contains(5))
    }
    @Test fun ambiguousOrPartialMatchesMustNotGreyUnrelatedWords() {
        assertNull(TanyaPrintLessonMatcher.findUnique(listOf("זה", "כל", "זה", "כל"), listOf("זה", "כל")))
        assertNull(TanyaPrintLessonMatcher.findUnique(listOf("זה", "כל"), listOf("זה", "כל", "האדם")))
        assertNull(TanyaPrintLessonMatcher.findUnique(listOf("זה"), emptyList()))
    }
    @Test fun punctuationDoesNotMoveOriginalWordOffsets() {
        assertEquals(TanyaPrintLessonRange(2, 5), TanyaPrintLessonMatcher.findUnique(
            listOf("לפני", "[", "כי", "-", "זה", "]", "אחרי"), listOf("כי", "זה")))
    }
    @Test fun boundaryCanCrossPrintedPagesWithoutChangingLineBreaks() {
        assertEquals(TanyaPrintLessonRange(1, 4), TanyaPrintLessonMatcher.findUnique(
            listOf("לפני", "סוף", "עמוד", "המשך", "אחרי"), listOf("סוף", "עמוד", "המשך")))
    }
    @Test fun printedAbbreviationsNeedVerifiedMappingNotGuessing() {
        assertNull(TanyaPrintLessonMatcher.findUnique(listOf("ע״י"), listOf("על", "ידי")))
    }
}
