package com.example

import com.example.data.podcasts.PodcastCatalog
import com.example.data.podcasts.PodcastRepository
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PodcastRepositoryTest {
    @Test fun `parses episode CDATA and audio without channel metadata`() {
        val feed = """<rss><channel><title>Channel</title><item>
            <title><![CDATA[שיעור & תניא]]></title><link>https://example.org/episode</link>
            <enclosure type="audio/mpeg" url="https://example.org/audio.mp3"/>
            <pubDate>today</pubDate></item></channel></rss>"""
        val episode = PodcastRepository.parse(feed).single()
        assertEquals("שיעור & תניא", episode.title)
        assertEquals("https://example.org/audio.mp3", episode.audio)
        assertEquals("today", episode.published)
    }
    @Test fun `untrusted links are never opened`() {
        assertTrue(PodcastRepository.parse("<rss><channel><item><title>x</title><link>javascript:alert(1)</link></item></channel></rss>").isEmpty())
    }
    @Test fun `Chabad feeds without episode link can play original audio`() {
        val episode = PodcastRepository.parse("<rss><channel><item><title>תניא</title><enclosure type='audio/mpeg' url='https://cdn.chabad.org/lesson.mp3'/></item></channel></rss>").single()
        assertEquals(episode.audio, episode.url)
    }
    @Test(expected = IllegalArgumentException::class)
    fun `external XML entities are refused`() {
        PodcastRepository.parse("<!DOCTYPE rss [<!ENTITY x SYSTEM 'file:///secret'>]><rss/>")
    }
    @Test fun `episode list is bounded`() {
        val item = "<item><title>x</title><link>https://example.org/e</link></item>"
        assertEquals(50, PodcastRepository.parse("<rss><channel>${item.repeat(80)}</channel></rss>").size)
    }
    @Test fun `all requested teachers have distinct secure source feeds`() {
        assertEquals(5, PodcastCatalog.channels.size)
        assertEquals(5, PodcastCatalog.channels.map { it.feed }.distinct().size)
        assertTrue(PodcastCatalog.channels.all { it.feed.startsWith("https://") })
    }
}
