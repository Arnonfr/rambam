package com.example

import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import com.example.domain.RambamDiagrams
import com.example.domain.RambamDiagram
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RambamDiagramsTest {
    @Test fun catalogContainsAllEighteenDiagramsAtFourteenLocations() {
        assertEquals(18, RambamDiagrams.all.size)
        assertEquals(18, RambamDiagrams.all.map { it.id }.distinct().size)
        assertEquals(14, RambamDiagrams.all.map { Triple(it.sectionId, it.chapter, it.halacha) }.distinct().size)
        assertEquals(2, RambamDiagrams.forHalacha("tefillin", 8, 1).size)
        assertEquals(3, RambamDiagrams.forHalacha("tefillin", 8, 2).size)
        assertEquals(2, RambamDiagrams.forHalacha("klei_hamikdash", 9, 9).size)
        assertTrue(RambamDiagrams.forHalacha("gerushin", 1, 1).isEmpty())
    }

    @Test fun everyDiagramHasAnExistingDatabaseHalacha() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val file = File(context.cacheDir, "diagram-test-rambam.db")
        context.assets.open("database/rambam.db").use { input -> file.outputStream().use { input.copyTo(it) } }
        val db = SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY)
        try {
            RambamDiagrams.all.forEach { diagram ->
                db.rawQuery("SELECT COUNT(*) FROM halachot WHERE sectionId=? AND chapterNumber=? AND halachaNumber=?",
                    arrayOf(diagram.sectionId, diagram.chapter.toString(), diagram.halacha.toString())).use {
                    assertTrue(it.moveToFirst())
                    assertEquals(diagram.id, 1, it.getInt(0))
                }
            }
        } finally { db.close() }
    }

    @Test fun originalMenorahIsExplicitlyAttributedToCommentaryNotMishnehTorahAutograph() {
        val original = RambamDiagrams.all.single { it.kind == RambamDiagram.Kind.MENORAH_ORIGINAL }
        assertTrue(original.note.contains("מפירוש המשנה"))
        assertTrue(original.note.contains("לא מכתב־יד של משנה תורה"))
        assertEquals("beit_habechirah", original.sectionId)
        assertEquals(10, original.halacha)
    }
}
