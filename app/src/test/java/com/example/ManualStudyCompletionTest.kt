package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.CompletionText
import com.example.data.local.SavedStudyCompletion
import com.example.data.local.StudyCompletionStore
import com.example.domain.content.CompletionBoundary
import com.example.domain.content.CompletionReaderRow
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ManualStudyCompletionTest {
    private val first = CompletionText("1", "פסוק א", "בְּרֵאשִׁית בָּרָא", "בראשית ברא", listOf("פירוש"))
    private val second = CompletionText("2", "פסוק ב", "וְהָאָרֶץ", "והארץ")

    @Test fun remainderStartsAfterTheChosenVerse() {
        val rows = listOf(CompletionReaderRow(2, first), CompletionReaderRow(4, second))
        assertEquals(listOf(second), CompletionBoundary.remainder(rows, 3))
        assertTrue(CompletionBoundary.remainder(rows, 5).isEmpty())
    }

    @Test fun middleOfTextUsesTheSamePositionWithAndWithoutNikud() {
        val rows = listOf(CompletionReaderRow(2, first), CompletionReaderRow(4, second))
        val result = CompletionBoundary.remainder(rows, 2, 7)
        assertEquals("בָּרָא", result.first().textWithNikud)
        assertEquals("ברא", result.first().textPlain)
        assertEquals(second, result.last())
    }

    @Test fun savedDaysSurviveReloadAndRemovalOfAnotherDay() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("manual_study_completions_v1", Context.MODE_PRIVATE).edit().clear().commit()
        val store = StudyCompletionStore(context)
        val older = SavedStudyCompletion("chumash", "2026-10-05", "אתמול", "חומש", "אחרי פסוק א", listOf(second), 1)
        val newer = older.copy(studyDate = "2026-10-06", dateLabel = "היום", blocks = listOf(first, second), updatedAt = 2)
        assertTrue(store.save(older))
        assertTrue(store.save(newer))
        assertEquals(listOf(older, newer), StudyCompletionStore(context).all())
        assertTrue(store.remove(newer.id))
        assertEquals(listOf(older), StudyCompletionStore(context).all())
    }
}
