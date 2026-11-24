package com.example

import android.content.ComponentName
import android.content.Context
import android.graphics.BitmapFactory
import android.media.browse.MediaBrowser
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.prayers.audio.PrayerPlaybackService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class StudyMediaServiceTest {
    @Test fun carMediaBrowserShowsOnlyTwoPlayableLessonsWithLocalArtwork() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val items = CompletableFuture<List<MediaBrowser.MediaItem>>()
        lateinit var browser: MediaBrowser
        instrumentation.runOnMainSync {
            browser = MediaBrowser(context, ComponentName(context, PrayerPlaybackService::class.java),
                object : MediaBrowser.ConnectionCallback() {
                    override fun onConnected() {
                        browser.subscribe(browser.root, object : MediaBrowser.SubscriptionCallback() {
                            override fun onChildrenLoaded(parentId: String, children: List<MediaBrowser.MediaItem>) {
                                items.complete(children)
                            }
                            override fun onError(parentId: String) {
                                items.completeExceptionally(AssertionError("Unable to load the study catalog"))
                            }
                        })
                    }
                    override fun onConnectionFailed() {
                        items.completeExceptionally(AssertionError("Unable to open the media browser service"))
                    }
                }, null)
            browser.connect()
        }
        try {
            val lessons = items.get(20, TimeUnit.SECONDS)
            assertEquals(listOf("תניא יומי", "רמב״ם יומי"), lessons.map { it.description.title?.toString() })
            assertTrue(lessons.all { it.isPlayable && !it.isBrowsable })
            lessons.forEach { lesson ->
                val uri = requireNotNull(lesson.description.iconUri)
                context.contentResolver.openInputStream(uri).use { stream ->
                    val cover = BitmapFactory.decodeStream(stream)
                    assertNotNull(cover)
                    cover?.recycle()
                }
            }
        } finally {
            instrumentation.runOnMainSync { browser.disconnect() }
        }
    }
}
