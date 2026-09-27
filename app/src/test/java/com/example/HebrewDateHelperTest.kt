package com.example

import com.example.ui.util.HebrewDateHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HebrewDateHelperTest {
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
