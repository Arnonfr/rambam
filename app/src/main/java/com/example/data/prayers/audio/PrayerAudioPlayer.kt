package com.example.data.prayers.audio

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

data class PrayerAudioState(
    val stepId: String? = null, val title: String = "", val playing: Boolean = false,
    val active: Boolean = false, val loading: Boolean = false, val positionMs: Long = 0,
    val durationMs: Long = 0, val speed: Float = 1f, val draft: Boolean = false,
    val error: String? = null, val finished: Boolean = false,
    /** After a stop at a missing recording: the next recorded section, offered for an explicit tap. */
    val nextAvailableId: String? = null, val nextAvailableTitle: String? = null
)

/** No controller, foreground service, focus request or playback is created before a user tap. */
class PrayerAudioPlayer(context: Context, private val serviceId: String,
    private val date: LocalDate, private val allowDraft: Boolean) {
    private val app = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val prefs = app.getSharedPreferences("prayer_audio_positions", Context.MODE_PRIVATE)
    private val key = "$serviceId:$date"
    private val mutable = MutableStateFlow(PrayerAudioState())
    val state = mutable.asStateFlow()
    private var controller: MediaController? = null
    private var future: ListenableFuture<MediaController>? = null
    private var currentRun = PrayerAudioRun(emptyList(), null)
    private var allSteps: List<PrayerAudioStep> = emptyList()
    // The stop message is shown once per stop; the 500 ms ticker must not raise it again after dismissal.
    private var acknowledgedStop: String? = null
    private var closed = false
    private var request = 0
    private var ticker: Job? = null
    private var lastSaved = 0L

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) { update(); save() }
        override fun onPlayerError(error: PlaybackException) {
            controller?.pause()
            mutable.value = mutable.value.copy(playing = false, loading = false,
                error = "לא ניתן לנגן את ההקלטה. אפשר לנסות שוב או להמשיך לקרוא.")
        }
    }

    fun start(steps: List<PrayerAudioStep>, startId: String, resume: Boolean = false) {
        if (closed) return
        val saved = prefs.getString("$key:step", null)
        val selectedId = if (resume && steps.any { it.id == saved }) saved!! else startId
        allSteps = steps
        acknowledgedStop = null
        val run = PrayerAudioPlan.run(steps, selectedId, date.dayOfWeek, allowDraft)
        if (run.steps.isEmpty()) {
            controller?.pause()
            val next = run.blockedAt?.let { PrayerAudioPlan.nextPlayable(steps, it.id, date.dayOfWeek, allowDraft) }
            mutable.value = mutable.value.copy(error = "ההקלטה של ${run.blockedAt?.title ?: "המקטע הזה"} עדיין אינה זמינה. אפשר להמשיך לקרוא או לבחור מקטע עם הקלטה.",
                nextAvailableId = next?.id, nextAvailableTitle = next?.title)
            return
        }
        val token = ++request
        save()
        currentRun = run
        mutable.value = mutable.value.copy(active = true, loading = true, error = null, finished = false,
            nextAvailableId = null, nextAvailableTitle = null)
        val offset = if (resume && selectedId == saved) prefs.getLong("$key:position", 0).coerceAtLeast(0) else 0
        fun begin(c: MediaController) {
            if (closed || token != request) return
            c.pause()
            com.example.data.analytics.AudioAnalytics.get(app).tracker.request(key, "prayer", "recording")
            c.setMediaItems(run.steps.map { step -> MediaItem.Builder()
                .setMediaId(step.id).setUri(step.clip!!.uri)
                .setMediaMetadata(MediaMetadata.Builder().setTitle(step.title)
                    .setArtist("${if (allowDraft && !step.clip.approved) "אודיו ניסיוני · " else ""}תפילה")
                    .setExtras(android.os.Bundle().apply { putString("usage_key", key); putString("usage_type", "prayer") })
                    .build()).build() }, 0, offset)
            c.prepare()
            c.play()
            update()
            ticker?.cancel()
            ticker = scope.launch {
                while (isActive) {
                    update()
                    if (System.currentTimeMillis() - lastSaved >= 2000) save()
                    delay(500)
                }
            }
        }
        controller?.let { begin(it); return }
        // Repeated taps replace the pending request instead of leaking connections.
        future?.let { MediaController.releaseFuture(it) }
        val pending = MediaController.Builder(app,
            SessionToken(app, ComponentName(app, PrayerPlaybackService::class.java))).buildAsync()
        future = pending
        pending.addListener({
            if (!closed && token == request) {
                runCatching { pending.get() }.onSuccess {
                    controller = it; it.addListener(listener); begin(it)
                }.onFailure {
                    mutable.value = mutable.value.copy(loading = false, active = false,
                        error = "לא ניתן לפתוח את הנגן. נסו שוב.")
                }
            }
        }, ContextCompat.getMainExecutor(app))
    }

    fun toggle() {
        val c = controller ?: return
        if (c.isPlaying || c.playWhenReady) c.pause() else if (c.playbackState == Player.STATE_ENDED) {
            // A missing next section must not restart the run as though the prayer were complete.
            if (currentRun.blockedAt == null) { com.example.data.analytics.AudioAnalytics.get(app).tracker.request(key,"prayer","recording",restart=true); c.seekTo(0, 0); c.play() }
        } else c.play()
        update(); save()
    }

    fun seek(positionMs: Long) {
        controller?.let { c -> c.seekTo(positionMs.coerceIn(0, c.duration.coerceAtLeast(0))) }
        update(); save()
    }

    fun cycleSpeed() {
        val speed = when (mutable.value.speed) { 1f -> 1.25f; 1.25f -> .85f; else -> 1f }
        controller?.setPlaybackSpeed(speed)
        mutable.value = mutable.value.copy(speed = speed)
    }

    fun clearError() {
        if (controller?.playbackState == Player.STATE_ENDED) acknowledgedStop = currentRun.blockedAt?.id
        mutable.value = mutable.value.copy(error = null, nextAvailableId = null, nextAvailableTitle = null)
    }

    fun stop() {
        ++request
        save()
        ticker?.cancel()
        controller?.takeIf { c -> currentRun.steps.any { it.id == c.currentMediaItem?.mediaId } }?.run { pause(); clearMediaItems() }
        currentRun = PrayerAudioRun(emptyList(), null)
        mutable.value = PrayerAudioState()
    }

    fun close() {
        if (closed) return
        stop(); closed = true
        controller?.removeListener(listener)
        future?.let { MediaController.releaseFuture(it) }
        future = null; controller = null
        scope.cancel()
    }

    private fun update() {
        val c = controller ?: return
        val step = currentRun.steps.firstOrNull { it.id == c.currentMediaItem?.mediaId } ?: return
        val ended = c.playbackState == Player.STATE_ENDED
        val blocked = if (ended) currentRun.blockedAt?.takeIf { it.id != acknowledgedStop } else null
        val next = blocked?.let { PrayerAudioPlan.nextPlayable(allSteps, it.id, date.dayOfWeek, allowDraft) }
        mutable.value = mutable.value.copy(stepId = step.id, title = step.title,
            playing = c.isPlaying, loading = c.playbackState == Player.STATE_BUFFERING,
            positionMs = c.currentPosition.coerceAtLeast(0),
            durationMs = c.duration.takeUnless { it == C.TIME_UNSET }?.coerceAtLeast(0) ?: 0,
            speed = c.playbackParameters.speed, draft = !step.clip!!.approved,
            finished = ended && currentRun.blockedAt == null,
            error = if (blocked != null) "הנגינה נעצרה: עדיין חסרה הקלטה של ${blocked.title}. אפשר להמשיך לקרוא." else mutable.value.error,
            nextAvailableId = next?.id ?: mutable.value.nextAvailableId.takeIf { blocked == null },
            nextAvailableTitle = next?.title ?: mutable.value.nextAvailableTitle.takeIf { blocked == null })
    }

    private fun save() {
        val c = controller ?: return
        val id = c.currentMediaItem?.mediaId ?: return
        if (currentRun.steps.none { it.id == id }) return
        if (c.playbackState == Player.STATE_ENDED) prefs.edit().remove("$key:step").remove("$key:position").apply()
        else prefs.edit().putString("$key:step", id).putLong("$key:position", c.currentPosition.coerceAtLeast(0)).apply()
        lastSaved = System.currentTimeMillis()
    }
}
