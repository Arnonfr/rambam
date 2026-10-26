package com.example

import android.content.Context
import androidx.media3.common.*
import androidx.test.core.app.ApplicationProvider
import com.example.data.speech.*
import com.example.domain.speech.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.lang.reflect.Proxy

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
@OptIn(ExperimentalCoroutinesApi::class)
class DailySpeechTest {
    private fun lesson() = DailySpeechText.lesson("2026-10-22", "three", "י״א חשוון", (1..3).map {
        Triple("ishut_$it", "הלכות אישות פרק $it", listOf("מילים של פרק $it. ".repeat(80)))
    })
    @Test fun `daily assignment contains all three chapters in order with bounded speech requests`() {
        val p = lesson()
        assertEquals(listOf("ishut_1", "ishut_2", "ishut_3"), p.parts.map { it.chapterId }.distinct())
        assertTrue(p.parts.all { it.text.length <= 700 })
        val chapter = p.parts.filter { it.chapterId == "ishut_2" }.joinToString(" ") { it.text }
        assertEquals(DailySpeechText.prepare("הלכות אישות פרק 2. " + "מילים של פרק 2. ".repeat(80)), chapter)
        assertEquals(p.id, lesson().id)
        assertNotEquals(p.id, DailySpeechText.lesson("2026-10-23", "three", "מחר",
            listOf(Triple("ishut_1", "אישות", listOf("טקסט")))).id)
        assertThrows(IllegalArgumentException::class.java) { DailySpeechText.lesson("x", "one", "", listOf(Triple("missing", "", emptyList()))) }
    }
    @Test fun `source niqqud remains intact while markup and cantillation never become spoken instructions`() {
        val source = "<b>בְּרֵאשִׁ֖ית</b> בָּרָ֣א אֱלֹהִ֑ים׃"
        assertEquals("בְּרֵאשִׁית בָּרָא אֱלֹהִים.", DailySpeechText.prepare(source))
        assertTrue(source.contains("<b>"))
        val long = "א".repeat(2000)
        assertEquals(long, DailySpeechText.split(long).joinToString(""))
    }
    @Test fun `next and previous jump to halacha boundaries rather than synthesis chunks`() = runTest {
        val plan = DailySpeechText.lesson("2026-10-22","three","היום", listOf(
            Triple("a_1","פרק 1",listOf("מילה ".repeat(400),"הלכה שנייה.","הלכה שלישית.")),
            Triple("a_2","פרק 2",listOf("תחילת הפרק השני."))))
        assertTrue(plan.parts.count { it.unit==0 }>1)
        val second = plan.parts.indexOfFirst { it.unit==1 }
        assertEquals(second,plan.nextUnitStart(0))
        assertEquals(0,plan.previousUnitStart(second))
        assertNull(plan.previousUnitStart(0));assertNull(plan.nextUnitStart(plan.parts.lastIndex))
        val store=SpeechLessonStore(ApplicationProvider.getApplicationContext());store.save(plan);store.savePosition(plan.id,0,0)
        val p=QueuePlayer();val session=DailySpeechSession(p.player,store,{StubSpeech()},backgroundScope)
        session.start(plan.id);runCurrent();session.toggle()
        session.skipUnit(true);runCurrent()
        assertTrue(p.items.first().mediaId.endsWith(":$second"));assertFalse(p.ready)
        session.skipUnit(false);runCurrent()
        assertTrue(p.items.first().mediaId.endsWith(":0"));assertFalse(p.ready)
        session.close()
    }
    @Test fun `the entire daily lesson is prepared and its resume point is saved`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = SpeechLessonStore(context)
        val plan = lesson(); store.save(plan); store.savePosition(plan.id,0,0)
        val p = QueuePlayer()
        val session = DailySpeechSession(p.player, store, { StubSpeech() }, backgroundScope)
        session.start(plan.id); runCurrent()
        assertEquals(plan.parts.size, p.items.size)
        assertTrue(p.ready)
        p.position = 12_345
        advanceTimeBy(1100); runCurrent()
        assertEquals(0 to 12_345L, store.position(plan.id))
        for (i in plan.parts.indices) {
            p.index = i; p.position = 0
            advanceTimeBy(250); runCurrent()
        }
        assertEquals(plan.parts.size, p.items.size)
        assertEquals(plan.parts.map { it.chapterTitle }, p.items.map { it.mediaMetadata.artist.toString() })
        p.phase = Player.STATE_ENDED; p.notifyEvents(); runCurrent()
        assertTrue(DailySpeechStatus.state.value.completed)
        assertEquals(0 to 0L, store.position(plan.id))
        session.close()
    }
    @Test fun `pause before the first audio cannot autoplay later and a different recording cancels preparation`() = runTest {
        val store = SpeechLessonStore(ApplicationProvider.getApplicationContext())
        val plan = lesson(); store.save(plan); store.savePosition(plan.id,0,0)
        store.audioRoot.listFiles()?.forEach { it.delete() }
        val gate = CompletableDeferred<Unit>()
        val engine = StubSpeech(gate)
        val p = QueuePlayer()
        val session = DailySpeechSession(p.player,store,{engine},backgroundScope)
        session.start(plan.id); runCurrent(); session.toggle()
        gate.complete(Unit); runCurrent()
        assertFalse(p.ready)
        assertFalse(DailySpeechStatus.state.value.playing)
        p.items.clear(); p.items += MediaItem.Builder().setMediaId("tanya-recording").setUri("https://example.org/lesson.mp3").build()
        p.notifyEvents(); runCurrent()
        assertFalse(DailySpeechStatus.state.value.active)
        assertTrue(engine.closed)
        assertEquals("tanya-recording",p.items.single().mediaId)
        session.close()
    }
    @Test fun `resume reopens the right part and milliseconds instead of starting at the first chapter`() = runTest {
        val store = SpeechLessonStore(ApplicationProvider.getApplicationContext())
        val plan=lesson();store.save(plan);store.savePosition(plan.id,3,4500)
        val p=QueuePlayer();val session=DailySpeechSession(p.player,store,{StubSpeech()},backgroundScope)
        session.start(plan.id);runCurrent()
        assertTrue(p.items.first().mediaId.endsWith(":3"));assertEquals(4500L,p.position)
        session.close()
    }
    @Test fun `shared player seeks across generated parts without losing pause state`() = runTest {
        val store=SpeechLessonStore(ApplicationProvider.getApplicationContext())
        val plan=DailySpeechText.lesson("seek-test","one","היום",listOf(Triple("a_1","פרק",listOf("הלכה ראשונה.","הלכה שנייה.","הלכה שלישית."))))
        store.save(plan);store.savePosition(plan.id,0,0)
        val p=QueuePlayer()
        val session=DailySpeechSession(p.player,store,{object:HebrewSpeechSynthesizer {
            override suspend fun initialize()="seek-wave-v1"
            override suspend fun synthesize(text:String,file:File) {PiperHebrewSpeech.writeWave(file,FloatArray(22050){.1f},22050)}
            override fun close() {}
        }},backgroundScope)
        session.start(plan.id);runCurrent()
        assertEquals(3000L,DailySpeechStatus.state.value.duration)
        session.toggle();session.seek(1500)
        assertEquals(1,p.index);assertEquals(500L,p.position);assertFalse(p.ready)
        assertEquals(1500L,DailySpeechStatus.state.value.position)
        assertEquals(1 to 500L,store.position(plan.id))
        session.seek(99_000)
        assertEquals(2,p.index);assertEquals(1000L,p.position)
        session.seek(-100)
        assertEquals(0,p.index);assertEquals(0L,p.position)
        session.close()
    }
    @Test fun `publisher recording uses shared playback and resumes without initializing a voice`() = runTest {
        val store=SpeechLessonStore(ApplicationProvider.getApplicationContext())
        val plan=DailySpeechLesson("rambam-2026-10-23-one-nadav-0264", "רמב״ם · י״ב חשוון",
            listOf(SpeechPart("issurei-biah_4", "איסורי ביאה ד", "", audioUrl="https://www.chabad.fm/mp3/rambamRabbiNadav/0264.mp3")), "nadav")
        store.save(plan);store.savePosition(plan.id,0,3450)
        val p=QueuePlayer();p.duration=120000
        val session=DailySpeechSession(p.player,store,{error("Recording must never initialize TTS")},backgroundScope)
        session.start(plan.id);runCurrent()
        assertEquals(plan.parts.single().audioUrl,p.items.single().localConfiguration!!.uri.toString())
        assertEquals(3450L,p.position);assertTrue(p.ready)
        assertTrue(DailySpeechStatus.state.value.recorded)
        assertEquals(120000L,DailySpeechStatus.state.value.duration)
        assertFalse(DailySpeechStatus.state.value.canNext)
        session.toggle();session.seek(20000)
        assertFalse(p.ready);assertEquals(20000L,p.position)
        assertEquals(0 to 20000L,store.position(plan.id))
        session.close()
    }
    private class StubSpeech(private val gate: CompletableDeferred<Unit>? = null) : HebrewSpeechSynthesizer {
        var closed=false
        override suspend fun initialize()="test-hebrew"
        override suspend fun synthesize(text:String,file:File) { gate?.await();file.writeBytes(ByteArray(128)) }
        override fun close() { closed=true }
    }
    /** Controllable transport: no real audio device or network is involved in queue tests. */
    private class QueuePlayer {
        val items=mutableListOf<MediaItem>();var index=0;var position=0L;var ready=false;var phase=Player.STATE_IDLE;var duration=0L
        private val listeners=mutableListOf<Player.Listener>()
        fun notifyEvents() { listeners.toList().forEach { it.onEvents(player,Player.Events(androidx.media3.common.FlagSet.Builder().build())) } }
        val player:Player=Proxy.newProxyInstance(Player::class.java.classLoader,arrayOf(Player::class.java)) { _, m, a ->
            when(m.name) {
                "addListener" -> {listeners += a!![0] as Player.Listener;null}
                "removeListener" -> {listeners -= a!![0] as Player.Listener;null}
                "addMediaItem" -> {items += a!![0] as MediaItem;null}
                "clearMediaItems" -> {items.clear();index=0;null}
                "getCurrentMediaItem" -> items.getOrNull(index)
                "getMediaItemCount" -> items.size
                "getMediaItemAt" -> items[a!![0] as Int]
                "getCurrentPosition" -> position
                "getDuration" -> duration
                "getPlaybackState" -> phase
                "getPlayWhenReady" -> ready
                "isPlaying" -> ready && phase==Player.STATE_READY
                "setPlayWhenReady" -> {ready=a!![0] as Boolean;listeners.toList().forEach { it.onPlayWhenReadyChanged(ready,Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST) };null}
                "pause" -> {ready=false;null}
                "prepare" -> {phase=Player.STATE_READY;null}
                "seekTo" -> {if(a!!.size==2) {index=a[0] as Int;position=a[1] as Long} else position=a[0] as Long;null}
                "setPlaybackSpeed" -> null
                "toString" -> "QueuePlayer"
                else -> when(m.returnType) {java.lang.Boolean.TYPE -> false;java.lang.Integer.TYPE -> 0;java.lang.Long.TYPE -> 0L;java.lang.Float.TYPE -> 0f;else -> null}
            }
        } as Player
    }
}
