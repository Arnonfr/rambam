package com.example.data.speech

import android.net.Uri
import androidx.media3.common.*
import com.example.domain.speech.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

data class DailySpeechState(val lessonId: String? = null, val title: String = "", val chapter: String = "",
    val playing: Boolean = false, val preparing: Boolean = false, val active: Boolean = false,
    val completed: Boolean = false, val speed: Float = 1f, val error: String? = null, val paused: Boolean = false,
    val canPrevious: Boolean = false, val canNext: Boolean = false,
    val position:Long=0, val duration:Long=0, val voiceStatus:String=PiperVoiceFiles.VOICE, val recorded:Boolean=false)

object DailySpeechStatus {
    internal val mutable = MutableStateFlow(DailySpeechState())
    val state = mutable.asStateFlow()
    const val START = "daily_speech.start"
    const val TOGGLE = "daily_speech.toggle"
    const val STOP = "daily_speech.stop"
    const val REWIND = "daily_speech.rewind"
    const val SPEED = "daily_speech.speed"
    const val NEXT = "daily_speech.next"
    const val PREVIOUS = "daily_speech.previous"
    const val SEEK = "daily_speech.seek"
    val actions = listOf(START, TOGGLE, STOP, REWIND, SPEED, NEXT, PREVIOUS, SEEK)
}

/** Owned by the MediaSessionService, never by the reader composable/activity. */
class DailySpeechSession(private val player: Player, private val store: SpeechLessonStore,
    private val createSynthesizer: () -> HebrewSpeechSynthesizer,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
    private val usage: com.example.domain.audio.AudioUsageTracker? = null) {
    private var lesson: DailySpeechLesson? = null
    private var job: Job? = null
    private var synth: HebrewSpeechSynthesizer? = null
    private var wantPlay = true
    private var changing = false
    private var playbackStarted = false
    private var generatedAll = false
    private var requestedPart = 0
    private var firstPart = 0
    private val files = mutableSetOf<String>()
    private val durations = mutableMapOf<String, Long>()
    private val status get() = DailySpeechStatus.mutable
    private fun id(index: Int) = "daily-speech:${lesson!!.id}:$index"
    private fun owns() = lesson != null && player.currentMediaItem?.mediaId?.startsWith("daily-speech:${lesson!!.id}:") == true
    private fun currentPart(): Int? = if (owns()) player.currentMediaItem!!.mediaId.substringAfterLast(':').toIntOrNull() else null
    private val listener = object : Player.Listener {
        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            if (owns() && !changing) wantPlay = playWhenReady
        }
        override fun onEvents(player: Player, events: Player.Events) {
            if (changing) return
            if (lesson != null && (playbackStarted || player.currentMediaItem != null) && !owns()) {
                save(); cancelGeneration(); lesson = null; playbackStarted = false; status.value = DailySpeechState(); return
            }
            refresh(); save()
        }
        override fun onPlayerError(error: PlaybackException) {
            if (owns()) { cancelGeneration(); status.value = status.value.copy(playing=false, preparing=false, error="ההקראה נעצרה. לחצו על האזנה כדי לנסות שוב.") }
        }
    }
    init {
        player.addListener(listener)
        scope.launch { while (isActive) { refresh(); save(); delay(500) } }
    }
    fun start(id: String, restart: Boolean = false, partOverride: Int? = null, autoPlay: Boolean = true) {
        val plan = runCatching { store.load(id) }.getOrElse { status.value = status.value.copy(error="לא ניתן לטעון את השיעור להאזנה."); return }
        if (lesson?.id == id && owns() && !restart && partOverride==null && status.value.error == null && !status.value.completed) { toggle(); return }
        save(); cancelGeneration()
        changing = true
        player.pause(); player.clearMediaItems()
        if(partOverride == null) usage?.request(id, com.example.data.analytics.AudioAnalytics.rambamType(id), if (plan.recorded) "recording" else "michael", voiceReady=plan.recorded || store.voiceCached, restart=restart)
        lesson = plan; playbackStarted = false; generatedAll = false; wantPlay = autoPlay; files.clear(); durations.clear()
        changing = false
        val saved = partOverride?.let { it to 0L } ?: if (restart) 0 to 0L else store.position(id)
        val start = saved.first.coerceIn(0, plan.parts.lastIndex)
        requestedPart = start
        firstPart = start
        status.value = DailySpeechState(id, plan.title, plan.parts[start].chapterTitle, preparing=true, active=true, speed=store.speed(),paused=!autoPlay,
            canPrevious=plan.previousUnitStart(start)!=null,canNext=plan.nextUnitStart(start)!=null,
            recorded=plan.recorded, voiceStatus=when(plan.source) { "nadav" -> "הרב נדב כהן · חב״ד FM"; "peretz" -> "ר׳ אליהו פרץ · חב״ד FM"; else -> PiperVoiceFiles.VOICE })
        if (plan.recorded) {
            generatedAll = true
            changing = true
            plan.parts.drop(start).forEachIndexed { offset, part ->
                player.addMediaItem(MediaItem.Builder().setMediaId(id(start + offset)).setUri(part.audioUrl)
                    .setMediaMetadata(MediaMetadata.Builder().setTitle(plan.title).setArtist(status.value.voiceStatus).build()).build())
            }
            player.seekTo(0, saved.second.coerceAtLeast(0)); player.setPlaybackSpeed(store.speed()); player.prepare()
            playbackStarted = true; player.playWhenReady = wantPlay
            changing = false
            refresh()
            return
        }
        job = scope.launch {
            var pending: File? = null
            var localEngine: HebrewSpeechSynthesizer? = null
            try {
                val engine = createSynthesizer().also { synth = it; localEngine = it }
                val voiceKey = engine.initialize()
                for (index in start..plan.parts.lastIndex) {
                    ensureActive()
                    val part = plan.parts[index]
                    val key = DailySpeechText.hash("daily-hebrew-audio-v2:$voiceKey:${part.text}")
                    val file = File(store.audioRoot, "$key.wav")
                    files += file.name
                    if (!file.isFile || file.length() <= 44) {
                        val temp = File(store.audioRoot, "$key-${java.util.UUID.randomUUID()}.tmp")
                        pending = temp
                        engine.synthesize(part.text, temp)
                        ensureActive()
                        check(temp.renameTo(file)) { "לא ניתן לשמור את קובץ ההקראה." }
                        pending = null
                    }
                    file.setLastModified(System.currentTimeMillis())
                    durations[id(index)] = waveDuration(file)
                    val ended = player.playbackState == Player.STATE_ENDED
                    changing = true
                    val item = MediaItem.Builder().setMediaId(id(index)).setUri(Uri.fromFile(file))
                        .setMediaMetadata(MediaMetadata.Builder().setTitle(plan.title).setArtist(part.chapterTitle).build()).build()
                    player.addMediaItem(item)
                    if (!playbackStarted) {
                        player.seekTo(0, saved.second.coerceAtLeast(0)); player.setPlaybackSpeed(store.speed()); player.prepare()
                        playbackStarted = true
                    } else if (ended) { player.seekTo(player.mediaItemCount - 1, 0); player.prepare() }
                    player.playWhenReady = wantPlay
                    changing = false
                    refresh()
                }
                generatedAll = true
                refresh()
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                changing = false
                usage?.error("synthesis")
                status.value = status.value.copy(preparing=false, error=e.message ?: "לא ניתן להכין את ההקראה.")
            } finally {
                pending?.delete(); localEngine?.close()
                if (synth === localEngine) synth = null
                store.pruneAudio(files)
            }
        }
    }
    fun toggle() {
        if (status.value.error != null || status.value.completed) { lesson?.let { start(it.id, status.value.completed) }; return }
        wantPlay = !wantPlay
        if (owns()) player.playWhenReady = wantPlay
        refresh()
    }
    fun rewind() {
        if (!owns()) return
        if (player.currentPosition >= 10_000 || !player.hasPreviousMediaItem()) player.seekTo((player.currentPosition - 10_000).coerceAtLeast(0))
        else {
            val previous = player.previousMediaItemIndex
            val duration = durations[player.getMediaItemAt(previous).mediaId] ?: 0L
            player.seekTo(previous, (duration - (10_000 - player.currentPosition)).coerceAtLeast(0))
        }
        save(); refresh()
    }
    fun seek(position:Long) {
        if(!owns() || !generatedAll) return
        if (lesson?.recorded == true) { player.seekTo(position.coerceIn(0, player.duration.coerceAtLeast(0))); save(); refresh(); return }
        var remaining=position.coerceIn(0,status.value.duration)
        for(i in 0 until player.mediaItemCount) {
            val duration=durations[player.getMediaItemAt(i).mediaId] ?: return
            if(remaining<duration || i==player.mediaItemCount-1) {player.seekTo(i,remaining.coerceAtMost(duration));break}
            remaining-=duration
        }
        save();refresh()
    }
    fun speed() {
        val next = when (store.speed()) { .75f -> .85f; .85f -> 1f; 1f -> 1.15f; else -> .75f }
        store.speed(next); if (owns()) player.setPlaybackSpeed(next)
        status.value = status.value.copy(speed=next)
    }
    fun skipUnit(forward: Boolean) {
        val plan = lesson ?: return
        val index = currentPart() ?: requestedPart
        val target = if(forward) plan.nextUnitStart(index) else plan.previousUnitStart(index)
        target?.let { start(plan.id,partOverride=it,autoPlay=wantPlay) }
    }
    fun stop() {
        save(); cancelGeneration()
        changing = true
        if (owns()) { player.pause(); player.clearMediaItems() }
        changing = false; lesson = null; playbackStarted = false; status.value = DailySpeechState(speed=store.speed())
    }
    private fun refresh() {
        val plan = lesson ?: return
        val index = currentPart()
        if (owns() && player.duration > 0) durations[player.currentMediaItem!!.mediaId] = player.duration
        val done = owns() && generatedAll && player.playbackState == Player.STATE_ENDED
        val waiting = !done && (!playbackStarted || player.playbackState in listOf(Player.STATE_BUFFERING, Player.STATE_ENDED))
        val partIndex = index ?: requestedPart
        val part = plan.parts[partIndex]
        val elapsed=(firstPart until partIndex).sumOf { durations[id(it)] ?: 0L } + if(owns()) player.currentPosition.coerceAtLeast(0) else 0
        val total=if(plan.recorded) player.duration.takeIf { it > 0 } ?: 0L else if(generatedAll) (firstPart..plan.parts.lastIndex).sumOf { durations[id(it)] ?: 0L } else 0L
        status.value = status.value.copy(chapter="${part.chapterTitle} · ${part.unitLabel}", active=true,
            playing=owns() && player.isPlaying, preparing=waiting && status.value.error == null,
            completed=done, speed=store.speed(), paused=!wantPlay,
            canPrevious=plan.previousUnitStart(partIndex)!=null,canNext=plan.nextUnitStart(partIndex)!=null,
            position=if(plan.recorded) player.currentPosition.coerceAtLeast(0) else elapsed,duration=total)
        if (done) store.savePosition(plan.id, 0, 0)
    }
    private fun save() {
        val plan = lesson ?: return
        val part = currentPart() ?: return
        if (!status.value.completed) store.savePosition(plan.id, part, player.currentPosition.coerceAtLeast(0))
    }
    private fun cancelGeneration() { job?.cancel(); job = null; synth?.close(); synth = null }
    fun close() { save(); cancelGeneration(); player.removeListener(listener); scope.cancel(); status.value = DailySpeechState(speed=store.speed()) }
    private fun waveDuration(file:File):Long = runCatching {
        java.io.RandomAccessFile(file,"r").use { f ->
            f.seek(24);val rate=Integer.reverseBytes(f.readInt());f.seek(40)
            val size=Integer.reverseBytes(f.readInt())
            if(rate>0 && size>0) size*1000L/(rate*2L) else 0
        }
    }.getOrDefault(0)
}
