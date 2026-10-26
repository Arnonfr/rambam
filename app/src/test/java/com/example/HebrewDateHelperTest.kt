package com.example

import com.example.ui.util.HebrewDateHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HebrewDateHelperTest {
    @Test fun readerDateIncludesWeekdayWithoutYear() {
        assertEquals("יום שבת כ״ט בתשרי", HebrewDateHelper.formatReaderDate("כ״ט בתשרי תשפ״ז", "2026-10-10"))
        assertEquals("יום שישי כ״ח בתשרי", HebrewDateHelper.formatReaderDate("כ״ח בתשרי תשפ״ז", "2026-10-09"))
        assertEquals("יום ראשון ל׳ בתשרי", HebrewDateHelper.formatReaderDate("ל׳ בתשרי תשפ״ז", "2026-10-11"))
    }

    @Test fun readerDatePreservesBothAdarMonthsAndHandlesMissingDate() {
        assertEquals("יום שבת ט״ו באדר א׳", HebrewDateHelper.formatReaderDate("ט״ו באדר א׳ תשפ״ז", "2026-10-10"))
        assertEquals("יום שבת ט״ו באדר ב׳", HebrewDateHelper.formatReaderDate("ט״ו באדר ב׳", "2026-10-10"))
        assertEquals("כ״ט בתשרי", HebrewDateHelper.formatReaderDate("כ״ט בתשרי תשפ״ז", ""))
        assertEquals("תהלים", HebrewDateHelper.formatReaderDate("תהלים", ""))
    }
    @Test
    fun parsesHebrewDaysWithGereshAndGershayim() {
        assertEquals(1, HebrewDateHelper.parseHebrewNumeral("א׳"))
        assertEquals(15, HebrewDateHelper.parseHebrewNumeral("ט״ו"))
        assertEquals(16, HebrewDateHelper.parseHebrewNumeral("ט״ז"))
        assertEquals(29, HebrewDateHelper.parseHebrewNumeral("כ״ט"))
        assertEquals(30, HebrewDateHelper.parseHebrewNumeral("ל׳"))
    }

    @Test
    fun rejectsNonNumeralText() {
        assertNull(HebrewDateHelper.parseHebrewNumeral(""))
        assertNull(HebrewDateHelper.parseHebrewNumeral("16"))
    }
}
