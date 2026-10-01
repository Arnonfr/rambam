package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.mitzvot.SeferHamitzvotRepository
import com.example.domain.mitzvot.MitzvahAssignmentItem
import com.example.domain.mitzvot.MitzvahAssignmentType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SeferHamitzvotRepositoryTest {
    private val repository by lazy {
        SeferHamitzvotRepository(ApplicationProvider.getApplicationContext<Context>())
    }

    @Test
    fun `loads a positive commandment from the public domain pack`() {
        val text = repository.getText(
            MitzvahAssignmentItem(MitzvahAssignmentType.POSITIVE, number = 96)
        )!!

        assertEquals("Public Domain", text.license)
        assertTrue(text.paragraphs.isNotEmpty())
        assertTrue(text.paragraphs.joinToString().contains("טומאת נבלה"))
    }

    @Test
    fun `contains all fourteen roots`() {
        for (number in 1..14) {
            assertTrue(repository.getRoot(number).isNotEmpty())
        }
    }
}
