package com.example

import com.example.domain.tanya.TanyaDailyReference
import org.junit.Assert.*
import org.junit.Test

class TanyaDailyReferenceTest {
    @Test fun `calendar opening segments expand to complete end of epistle assignments`() {
        assertEquals("Tanya, Part IV; Iggeret HaKodesh 23:10-14",
            TanyaDailyReference.complete("Tanya, Part IV; Iggeret HaKodesh 23:10"))
        assertEquals("Tanya, Part IV; Iggeret HaKodesh 24:1-9",
            TanyaDailyReference.complete("Tanya, Part IV; Iggeret HaKodesh 24:1"))
    }
    @Test fun `complete and unrelated references remain unchanged`() {
        listOf("Tanya, Part IV; Iggeret HaKodesh 23:10-14",
            "Tanya, Part IV; Iggeret HaKodesh 23:8-9",
            "Tanya, Part IV; Iggeret HaKodesh 25:1-5").forEach {
            assertEquals(it, TanyaDailyReference.complete(it))
        }
    }
}
