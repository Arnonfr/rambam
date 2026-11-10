package com.example.ui

import android.app.Application
import android.content.ComponentName
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.*
import androidx.media3.session.*
import com.example.data.prayers.audio.PrayerPlaybackService
import com.example.data.speech.DailySpeechStatus
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class PlaybackBarState(val visible: Boolean = false, val title: String = "", val subtitle: String = "",
    val playing: Boolean = false, val loading: Boolean = false, val mediaId: String? = null,
    val position: Long = 0, val duration: Long = 0)

class PlaybackViewModel(app: Application): AndroidViewModel(app) {
    private val mutable = MutableStateFlow(PlaybackBarState())
    val state = mutable.asStateFlow()
    private var controller: MediaController? = null
    private var pending: (() -> Unit)? = null
    private var cleared = false
    private val future = MediaController.Builder(app, SessionToken(app, ComponentName(app, PrayerPlaybackService::class.java))).buildAsync()
    private val listener = object: Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) { update() }
    }
    init {
        future.addListener({
            if (!cleared) runCatching { future.get() }.onSuccess {
                controller = it; it.addListener(listener); pending?.invoke(); pending = null; update()
            }
        }, ContextCompat.getMainExecutor(app))
        viewModelScope.launch { while (isActive) { update(); delay(500) } }
    }
    private fun update() {
        val c = controller ?: return
        val speech = DailySpeechStatus.state.value
        val speechOwned = c.currentMediaItem?.mediaId?.startsWith("daily-speech:") == true ||
            (c.currentMediaItem == null && speech.active)
        val item = c.currentMediaItem
        mutable.value = PlaybackBarState(visible = item != null || speech.preparing || speech.active,
            title = if (speechOwned || item == null) speech.title.ifBlank { "מכין שיעור להאזנה…" } else item.mediaMetadata.title?.toString().orEmpty(),
            subtitle = if (speechOwned) speech.voiceStatus else item?.mediaMetadata?.artist?.toString().orEmpty(),
            playing = c.playWhenReady && c.playbackState != Player.STATE_ENDED, loading = c.playbackState == Player.STATE_BUFFERING || speech.preparing,
            mediaId = item?.mediaId, position = c.currentPosition.coerceAtLeast(0),
            duration = c.duration.takeIf { it > 0 } ?: 0)
    }
    fun toggle() {
        val c = controller ?: return
        if (c.currentMediaItem?.mediaId?.startsWith("daily-speech:") == true || DailySpeechStatus.state.value.active) {
            c.sendCustomCommand(SessionCommand(DailySpeechStatus.TOGGLE, android.os.Bundle.EMPTY), android.os.Bundle.EMPTY)
        } else if (c.playWhenReady) c.pause() else {
            if (c.playbackState == Player.STATE_ENDED) c.seekTo(0)
            if (c.playbackState == Player.STATE_IDLE) c.prepare()
            c.play()
        }
        update()
    }
    fun stop() {
        pending = null
        controller?.let { c ->
            c.sendCustomCommand(SessionCommand(DailySpeechStatus.STOP, android.os.Bundle.EMPTY), android.os.Bundle.EMPTY)
            c.pause(); c.clearMediaItems()
        }
        update()
    }
    fun seek(position: Long) { controller?.seekTo(position.coerceAtLeast(0)); update() }
    fun playPodcast(id: String, title: String, teacher: String, url: String) {
        val action = {
            controller?.let { c ->
                if (c.currentMediaItem?.mediaId == id) toggle() else {
                    c.setMediaItem(MediaItem.Builder().setMediaId(id).setUri(url).setMediaMetadata(
                        MediaMetadata.Builder().setTitle(title).setArtist(teacher).setExtras(android.os.Bundle().apply {
                            putString("usage_key", id); putString("usage_type", "podcast")
                        }).build()).build())
                    c.prepare(); c.play(); update()
                }
            }
            Unit
        }
        if (controller == null) pending = action else action()
    }
    override fun onCleared() {
        cleared = true; pending = null; controller?.removeListener(listener); MediaController.releaseFuture(future)
    }
}
