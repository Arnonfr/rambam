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
class RambamTrackSelectionTest {
    @Test fun `legacy visibility migrates to its selected track without enabling the other`() = runBlocking {
        val repo = UserPreferencesRepository(ApplicationProvider.getApplicationContext<Context>())
        repo.updateTrack("three")
        repo.updateVisibleStudies(setOf("rambam", "tanya"))
        val prefs = repo.userPreferencesFlow.first()
        assertEquals(setOf("rambam_three", "tanya"), prefs.visibleStudies)
        repo.updateTrack("one")
        repo.updateVisibleStudies(setOf("rambam_one", "mitzvot", "chumash", "tehillim", "tanya"))
    }

    @Test fun `three rambam tracks remain selected independently of the active reader`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = UserPreferencesRepository(context)
        val tracks = setOf("rambam_one", "rambam_three", "mitzvot")
        repo.updateVisibleStudies(tracks)
        repo.updateTrack("three")
        assertEquals(tracks, UserPreferencesRepository(context).userPreferencesFlow.first().visibleStudies)
        repo.updateTrack("one")
        assertEquals(tracks, repo.userPreferencesFlow.first().visibleStudies)
        repo.updateVisibleStudies(setOf("rambam_one", "mitzvot", "chumash", "tehillim", "tanya"))
    }
}
