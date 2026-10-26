package com.example

import com.example.domain.audio.AudioUsageTracker
import org.junit.Assert.*
import org.junit.Test

class AudioUsageTrackerTest {
    private var clock = 0L
    private var consent = true
    private val events = mutableListOf<Pair<String,Map<String,Any>>>()
    private val pending = mutableMapOf<String,Long>()
    private val usage = AudioUsageTracker({ clock }, { consent }, { n,p -> events += n to p }, { t,_,v -> pending[t]=v })
    private fun advance(ms: Long) { clock += ms; usage.sample() }
    private fun playing(id:String="lesson") = usage.update(id,"rambam_one","michael",true)
    private fun count(name:String) = events.count { it.first == name }
    private fun milliseconds() = events.filter { it.first == "audio_listen_time" }.sumOf { it.second["listened_ms"] as Long }

    @Test fun `preparation buffering and pauses do not count and resume is one start`() {
        usage.request("lesson","rambam_one","michael",false)
        advance(12_000)
        playing(); advance(15_000)
        usage.update("lesson","rambam_one","michael",false); advance(80_000)
        playing(); advance(15_000); usage.close()
        assertEquals(30_000, milliseconds())
        assertEquals(1,count("audio_play_started")); assertEquals(1,count("audio_engaged"))
        assertEquals(12_000L, events.first { it.first == "audio_play_started" }.second["startup_ms"])
        assertEquals("no",events.first().second["voice_ready"])
    }
    @Test fun `chapter skip and arbitrary position or speed changes do not inflate time`() {
        usage.request("lesson","rambam_one","michael"); playing(); advance(12_000)
        // Media is cleared while the next unit is prepared; logical lesson identity survives.
        usage.update("lesson","rambam_one","michael",false); advance(10_000)
        playing(); advance(18_000); usage.close()
        assertEquals(30_000,milliseconds()); assertEquals(1,count("audio_play_started"))
    }
    @Test fun `thresholds fire once and duration events are deltas`() {
        usage.request("lesson","rambam_one","michael"); playing()
        repeat(320) { advance(1000) }; usage.close()
        assertEquals(320_000,milliseconds())
        assertEquals(1,count("audio_engaged")); assertEquals(1,count("audio_five_minutes"))
        assertEquals(320_000.0/60_000,events.filter { it.first=="audio_listen_time" }.sumOf { it.second["value"] as Double },.00001)
        assertEquals(0L,pending["rambam_one"])
    }
    @Test fun `new source closes only the old interval and preserves source attribution`() {
        playing(); advance(5000)
        usage.request("clip","tanya","recording")
        usage.update("clip","tanya","recording",true); advance(8000); usage.close()
        val deltas=events.filter { it.first=="audio_listen_time" }
        assertEquals(listOf("rambam_one","tanya"), deltas.map { it.second["content_type"] })
        assertEquals(listOf(5000L,8000L),deltas.map { it.second["listened_ms"] })
    }
    @Test fun `queue end is once and does not imply engaged when seeking to the end`() {
        playing(); advance(1000)
        repeat(3) { usage.update("lesson","rambam_one","michael",false,true) }
        assertEquals(1,count("audio_queue_ended")); assertEquals(0,count("audio_engaged"))
        assertEquals(1000,milliseconds())
    }
    @Test fun `disabled periods send and retain nothing including a consent boundary`() {
        consent=false; usage.request("lesson","rambam_one","michael"); playing(); advance(90_000)
        assertTrue(events.isEmpty()); assertTrue(pending.values.all { it==0L })
        consent=true; playing(); advance(5000); usage.close()
        assertEquals(5000,milliseconds()); assertEquals(0,count("audio_engaged"))
    }
    @Test fun `failure before playback is not listening`() {
        usage.request("lesson","rambam_one","michael"); advance(20_000); usage.error("synthesis")
        assertEquals(1,count("audio_error")); assertEquals(0,count("audio_play_started")); assertEquals(0,milliseconds())
    }
    @Test fun `analytics failures cannot interrupt playback`() {
        val broken=AudioUsageTracker({clock},{true},{_,_->error("SDK failed")},{_,_,_->error("disk failed")})
        broken.request("lesson","rambam_three","michael"); broken.update("lesson","rambam_three","michael",true)
        clock+=310_000; broken.sample(); broken.close()
    }
}
