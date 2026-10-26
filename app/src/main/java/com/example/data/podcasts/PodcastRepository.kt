package com.example.data.podcasts

import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import java.util.concurrent.TimeUnit
import java.io.ByteArrayOutputStream

data class PodcastChannel(val id: String, val teacher: String, val title: String,
    val language: String, val feed: String, val website: String, val artwork: String = "")
data class PodcastEpisode(val title: String, val url: String, val audio: String?, val published: String,
    val duration: String = "")

object PodcastCatalog {
    // Publisher RSS feeds, cross-checked against the publisher's Apple listing.
    // Audio and artwork are not copied or rehosted by this app.
    val channels = listOf(
        PodcastChannel("ashkenazi", "הרב שניאור אשכנזי", "לומדים תניא", "עברית",
            "https://he.chabad.org/tools/rss/itunes/seriesrss_cdo/scope/5287961/variant/30977",
            "https://he.chabad.org/library/article_cdo/aid/5287961",
            "https://w2.chabad.org/media/images/1172/pFoD11722611.png"),
        PodcastChannel("jacobson", "הרב סימון ג׳יקובסון", "Meaningful Life Skills", "אנגלית",
            "https://anchor.fm/s/3a56d08/podcast/rss", "https://www.meaningfullife.com/subscribes/",
            "https://d3t3ozftmdmh3i.cloudfront.net/production/podcast_uploaded/511730/511730-1524996979395-2c81153cc5959.jpg"),
        PodcastChannel("nadav", "הרב נדב כהן", "תניא טו גו", "עברית",
            "https://feeds.captivate.fm/rnadavcohen/", "https://podcasts.apple.com/il/podcast/id1606921229",
            "https://artwork.captivate.fm/9fe5cb75-e31e-46b4-aafd-3a492f3fde57/s1pfSOMc4TbrxwJPTEeaD0um.jpg"),
        PodcastChannel("nachmanson", "הרב יהודה לייב נחמנסון", "שיעורי הרב נחמנסון", "עברית",
            "https://feeds.transistor.fm/19817b21-c5ce-432b-82f9-37e81dfa7eff", "https://hamishpatim.co.il/",
            "https://img.transistorcdn.com/ETgRQxnJUezWoroXAyfnTTwuYbF3L0RS6hO7AEeCnYE/rs:fill:0:0:1/w:1400/h:1400/q:60/mb:500000/aHR0cHM6Ly9pbWct/dXBsb2FkLXByb2R1/Y3Rpb24udHJhbnNp/c3Rvci5mbS9mMzMx/OTg1YzA3MjJlYWZk/NDIxYTJkMTJiM2Vl/YzJmOC5qcGc.jpg"),
        PodcastChannel("slavatitzky", "הרב שבתי סלבטיצקי", "פרשת השבוע · נפש ופרקטיקה", "עברית",
            "https://feeds.captivate.fm/rslavatitsky/", "https://podcasts.apple.com/il/podcast/id1609388814",
            "https://artwork.captivate.fm/1fccd3a6-b67d-4a3c-aceb-72fbbbcce319/n77nFf8_ihmMOGKVzig8vtQJ.jpg")
    )
}

class PodcastRepository {
    private val client = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS).build()
    suspend fun episodes(channel: PodcastChannel): List<PodcastEpisode> = withContext(Dispatchers.IO) {
        client.newCall(Request.Builder().url(channel.feed).build()).execute().use { response ->
            check(response.isSuccessful) { "הערוץ אינו זמין כרגע" }
            val stream = response.body?.byteStream() ?: error("אין תוכן בערוץ")
            val bytes = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                val read = stream.read(buffer)
                if (read < 0) break
                check(bytes.size() + read <= 5_000_000) { "הערוץ גדול מדי" }
                bytes.write(buffer, 0, read)
            }
            parse(bytes.toString("UTF-8"))
        }
    }
    companion object {
        fun parse(xml: String): List<PodcastEpisode> {
            require(!xml.contains("<!DOCTYPE", true) && !xml.contains("<!ENTITY", true))
            val parser = Xml.newPullParser()
            parser.setInput(xml.reader())
            val result = mutableListOf<PodcastEpisode>()
            var inItem = false
            var title = ""; var link = ""; var audio: String? = null; var published = ""; var duration = ""
            while (parser.eventType != XmlPullParser.END_DOCUMENT && result.size < 50) {
                if (parser.eventType == XmlPullParser.START_TAG) {
                    when (parser.name.substringAfter(':')) {
                        "item" -> { inItem = true; title = ""; link = ""; audio = null; published = ""; duration = "" }
                        "title" -> if (inItem) title = parser.nextText().trim().take(300)
                        "link" -> if (inItem) link = parser.nextText().trim()
                        "pubDate" -> if (inItem) published = parser.nextText().trim()
                        "duration" -> if (inItem) duration = parser.nextText().trim().take(24)
                        "enclosure" -> if (inItem) {
                            val type = parser.getAttributeValue(null, "type").orEmpty()
                            val url = parser.getAttributeValue(null, "url").orEmpty()
                            if (type.startsWith("audio/") && url.startsWith("https://")) audio = url
                        }
                    }
                } else if (parser.eventType == XmlPullParser.END_TAG && parser.name == "item") {
                    val destination = link.takeIf { it.startsWith("https://") }
                        ?: audio.takeIf { link.isBlank() }
                    if (title.isNotBlank() && destination != null)
                        result.add(PodcastEpisode(title, destination, audio, published, duration))
                    inItem = false
                }
                parser.next()
            }
            return result.distinctBy { it.audio ?: it.url }
        }
    }
}
