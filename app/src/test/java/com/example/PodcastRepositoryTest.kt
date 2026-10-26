package com.example

import com.example.data.podcasts.PodcastCatalog
import com.example.data.podcasts.PodcastRepository
import com.example.data.podcasts.podcastDurationLabel
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PodcastRepositoryTest {
    @Test fun `duration renders seconds and hours and ignores bad metadata`() {
        assertEquals("20:15", podcastDurationLabel("1215"))
        assertEquals("1:02:03", podcastDurationLabel("01:02:03"))
        assertEquals("", podcastDurationLabel("1:99"))
        assertEquals("", podcastDurationLabel("unknown"))
    }
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
        val items = (1..80).joinToString("") { "<item><title>x</title><link>https://example.org/e/$it</link></item>" }
        assertEquals(50, PodcastRepository.parse("<rss><channel>$items</channel></rss>").size)
    }
    @Test fun `duplicate recordings are shown only once and duration is read`() {
        val item = """<item><title>שיעור</title><link>https://example.org/e</link>
            <itunes:duration>20:15</itunes:duration><enclosure type="audio/mpeg" url="https://example.org/e.mp3"/></item>"""
        val result = PodcastRepository.parse("<rss xmlns:itunes='http://www.itunes.com/dtds/podcast-1.0.dtd'><channel>$item$item</channel></rss>")
        assertEquals(1, result.size)
        assertEquals("20:15", result.single().duration)
    }
    @Test fun `all requested teachers have distinct secure source feeds`() {
        assertEquals(5, PodcastCatalog.channels.size)
        assertEquals(5, PodcastCatalog.channels.map { it.feed }.distinct().size)
        assertTrue(PodcastCatalog.channels.all { it.feed.startsWith("https://") })
        assertTrue(PodcastCatalog.channels.all { it.artwork.startsWith("https://") })
    }
}
