package com.example

import androidx.test.core.app.ApplicationProvider
import com.example.data.speech.RambamRecordingRepository
import com.example.domain.schedule.DailyScheduleEngine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.ZoneId

/** Explicit publisher connectivity check, opt in to avoid making ordinary tests network-dependent. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
class RambamRecordingLiveTest {
    @Test fun datedPublisherPagesAndMp3sResolveThroughTheActualAppClient() = runBlocking {
        assumeTrue(System.getenv("VERIFY_RAMBAM_LIVE") == "1")
        val context=ApplicationProvider.getApplicationContext<android.content.Context>()
        context.filesDir.resolve("rambam_recording_pages").deleteRecursively()
        for (track in listOf("one","three")) {
            val day=DailyScheduleEngine(context).getDailyLesson(Instant.parse("2026-10-23T12:00:00Z"),ZoneId.of("UTC"),track)
            val plan=RambamRecordingRepository(context).resolve(day)
            assertTrue(plan.recorded)
            assertTrue(plan.parts.single().audioUrl.endsWith(if(track=="one") "0264.mp3" else "shiur263.mp3"))
        }
    }
}
