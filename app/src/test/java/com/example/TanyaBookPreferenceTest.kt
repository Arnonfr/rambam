package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.UserPreferences
import com.example.data.local.UserPreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TanyaBookPreferenceTest {
    @Test fun `book view is opt in and preference survives new repository`() = runBlocking {
        assertFalse(UserPreferences().tanyaBookView)
        assertFalse(UserPreferences().tanyaBookShowNikud)
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = UserPreferencesRepository(context)
        repo.updateTanyaBookView(true)
        assertTrue(UserPreferencesRepository(context).userPreferencesFlow.first().tanyaBookView)
        repo.updateTanyaBookView(false)
        assertFalse(repo.userPreferencesFlow.first().tanyaBookView)
        repo.updateTanyaBookNikud(true)
        assertTrue(repo.userPreferencesFlow.first().tanyaBookShowNikud)
        assertTrue(repo.userPreferencesFlow.first().showNikud)
        repo.updateTanyaBookNikud(false)
    }
}
