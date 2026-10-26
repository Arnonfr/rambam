package com.example.data.tanya

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.*
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.data.prayers.audio.PrayerPlaybackService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class TanyaPlaybackState(val active: Boolean = false, val playing: Boolean = false, val loading: Boolean = false,
    val position: Long = 0, val duration: Long = 0, val speed: Float = 1f, val error: String? = null)

/** Both prayer and Tanya bind to one MediaSession, so two recordings cannot play together. */
class TanyaAudioPlayer(context: Context) {
    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences("tanya_audio_positions", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mutable = MutableStateFlow(TanyaPlaybackState(speed = prefs.getFloat("speed", 1f)))
    val state = mutable.asStateFlow()
    private var closed = false
    private var current: TanyaAudioClip? = null
    private var controller: MediaController? = null
    private val future = MediaController.Builder(app, SessionToken(app, ComponentName(app, PrayerPlaybackService::class.java))).buildAsync()
    private var pending: TanyaAudioClip? = null
    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) { update(); save() }
        override fun onPlayerError(error: PlaybackException) {
            mutable.value = mutable.value.copy(loading = false, playing = false, error = "ההקלטה לא נטענה. בדקו את החיבור ונסו שוב.")
        }
    }
    init {
        future.addListener({
            if (!closed) runCatching { future.get() }.onSuccess {
                controller = it; it.addListener(listener)
                pending?.let { clip -> pending = null; play(clip) }
            }.onFailure { mutable.value = mutable.value.copy(loading = false, error = "לא ניתן לפתוח את הנגן. נסו שוב.") }
        }, ContextCompat.getMainExecutor(app))
        scope.launch { while (isActive) { update(); save(); delay(1000) } }
    }
    fun play(clip: TanyaAudioClip) {
        if (closed) return
        val c = controller
        if (c == null) { pending = clip; mutable.value = mutable.value.copy(loading = true); return }
        if (current?.id == clip.id && c.currentMediaItem?.mediaId == clip.id && c.playbackState != Player.STATE_IDLE) {
            if (c.playbackState == Player.STATE_ENDED) { com.example.data.analytics.AudioAnalytics.get(app).tracker.request(clip.id,"tanya","recording",restart=true); c.seekTo(0); c.play() }
            else if (c.isPlaying || c.playWhenReady) c.pause() else c.play()
        } else {
            save(); current = clip
            mutable.value = TanyaPlaybackState(active = true, loading = true, speed = prefs.getFloat("speed", 1f))
            c.pause()
            com.example.data.analytics.AudioAnalytics.get(app).tracker.request(clip.id, "tanya", "recording")
            c.setMediaItem(MediaItem.Builder().setMediaId(clip.id).setUri(clip.url)
                .setMediaMetadata(MediaMetadata.Builder().setTitle(clip.title).setArtist("תניא · ${clip.teacher}").setExtras(android.os.Bundle().apply { putString("usage_key", clip.id); putString("usage_type", "tanya") }).build()).build(),
                prefs.getLong(clip.id, 0).coerceAtLeast(0))
            c.setPlaybackSpeed(prefs.getFloat("speed", 1f))
            c.prepare(); c.play()
        }
        update()
    }
    fun seek(position: Long) { controller?.takeIf { owns(it) }?.let { it.seekTo(position.coerceIn(0, it.duration.coerceAtLeast(0))) }; update(); save() }
    fun speed() {
        val value = when (mutable.value.speed) { 1f -> 1.25f; 1.25f -> 1.5f; else -> 1f }
        prefs.edit().putFloat("speed", value).apply()
        controller?.takeIf { owns(it) }?.setPlaybackSpeed(value)
        mutable.value = mutable.value.copy(speed = value)
    }
    fun stop() {
        pending = null
        save(); controller?.takeIf { owns(it) }?.pause()
        current = null; mutable.value = TanyaPlaybackState(speed = prefs.getFloat("speed", 1f))
    }
    fun close() {
        if (closed) return
        stop(); closed = true
        controller?.removeListener(listener)
        MediaController.releaseFuture(future); controller = null; scope.cancel()
    }
    private fun owns(c: MediaController) = current != null && c.currentMediaItem?.mediaId == current?.id
    private fun update() {
        val c = controller ?: return
        if (!owns(c)) { if (current != null) mutable.value = TanyaPlaybackState(speed = prefs.getFloat("speed", 1f)); return }
        mutable.value = mutable.value.copy(active = true, playing = c.isPlaying,
            loading = c.playbackState == Player.STATE_BUFFERING,
            position = c.currentPosition.coerceAtLeast(0), duration = c.duration.takeUnless { it == C.TIME_UNSET }?.coerceAtLeast(0) ?: 0,
            speed = c.playbackParameters.speed)
    }
    private fun save() {
        val c = controller ?: return
        if (owns(c)) prefs.edit().putLong(current!!.id, if (c.playbackState == Player.STATE_ENDED) 0 else c.currentPosition.coerceAtLeast(0)).apply()
    }
}
