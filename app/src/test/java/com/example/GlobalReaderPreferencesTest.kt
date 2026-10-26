package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.UserPreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class GlobalReaderPreferencesTest {
    @Test fun readerChangesPersistGloballyAsAbsoluteValuesWithoutChangingTanyaPrintMode() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = UserPreferencesRepository(context)
        val original = repository.userPreferencesFlow.first()
        try {
        repository.updateTanyaBookView(true)
        repository.updateFontSize(32f)
        repository.updateFontSize(21f)
        repository.updateLineSpacing(1.8f)
        repository.updateLetterSpacing(.08f)
        repository.updateFontFamily("sans")
        repository.updateReaderTheme("sepia")
        repository.updateShowNikud(false)
        val prefs = UserPreferencesRepository(context).userPreferencesFlow.first()
        assertEquals(21f, prefs.fontSizeSp)
        assertEquals(1.8f, prefs.lineSpacingMultiplier)
        assertEquals(.08f, prefs.letterSpacingEm)
        assertEquals("sans", prefs.fontFamily)
        assertEquals("sepia", prefs.readerTheme)
        assertFalse(prefs.showNikud)
        assertTrue(prefs.tanyaBookView)
        } finally {
            repository.updateFontSize(original.fontSizeSp)
            repository.updateLineSpacing(original.lineSpacingMultiplier)
            repository.updateLetterSpacing(original.letterSpacingEm)
            repository.updateFontFamily(original.fontFamily)
            repository.updateReaderTheme(original.readerTheme)
            repository.updateShowNikud(original.showNikud)
            repository.updateTanyaBookView(original.tanyaBookView)
        }
    }
}
