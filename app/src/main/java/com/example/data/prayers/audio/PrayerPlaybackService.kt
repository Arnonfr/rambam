package com.example.data.prayers.audio

import android.content.Intent
import androidx.media3.common.Player
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.SessionCommand
import androidx.media3.session.legacy.MediaSessionManager.RemoteUserInfo
import com.example.data.speech.SpeechLessonStore
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class PrayerPlaybackService : MediaLibraryService() {
    private val usageScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Main.immediate)
    private var usage: com.example.domain.audio.AudioUsageTracker? = null
    private var session: MediaLibrarySession? = null
    private var speech: com.example.data.speech.DailySpeechSession? = null
    private val tanyaPositions by lazy { getSharedPreferences("tanya_audio_positions", MODE_PRIVATE) }
    private val jumpBack = SessionCommand("study.seek_back", android.os.Bundle.EMPTY)
    private val jumpForward = SessionCommand("study.seek_forward", android.os.Bundle.EMPTY)
    private val jumpButtons by lazy {
        listOf(
            CommandButton.Builder(CommandButton.ICON_SKIP_BACK_10).setSessionCommand(jumpBack)
                .setDisplayName("אחורה 10 שניות").setSlots(CommandButton.SLOT_BACK).build(),
            CommandButton.Builder(CommandButton.ICON_SKIP_FORWARD_10).setSessionCommand(jumpForward)
                .setDisplayName("קדימה 10 שניות").setSlots(CommandButton.SLOT_FORWARD).build()
        )
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        usage = com.example.data.analytics.AudioAnalytics.get(this).tracker
        AudioPlaybackActivity.busy=false
        StudyPlaybackStatus.report(null, false)
        val player = ExoPlayer.Builder(this).setSeekBackIncrementMs(10_000).setSeekForwardIncrementMs(10_000).build().apply {
            addListener(object:androidx.media3.common.Player.Listener {
                override fun onEvents(player:androidx.media3.common.Player,events:androidx.media3.common.Player.Events) {
                    measure(player)
                    AudioPlaybackActivity.busy=player.mediaItemCount>0 && player.playbackState!=androidx.media3.common.Player.STATE_ENDED
                    if (events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION)) {
                        session?.setMediaButtonPreferences(if (isStudyItem(player)) jumpButtons else emptyList())
                    }
                    if (events.containsAny(Player.EVENT_POSITION_DISCONTINUITY, Player.EVENT_PLAY_WHEN_READY_CHANGED, Player.EVENT_PLAYBACK_STATE_CHANGED)) {
                        saveTanyaPosition(player)
                    }
                }
            })
            addListener(object: androidx.media3.common.Player.Listener {
                override fun onPlayerError(error: androidx.media3.common.PlaybackException) { usage?.error("player") }
            })
            setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH).build(), true)
            setHandleAudioBecomingNoisy(true)
            setWakeMode(C.WAKE_MODE_LOCAL)
            playWhenReady = false
        }
        usageScope.launch {
            while (kotlinx.coroutines.currentCoroutineContext().isActive) {
                measure(player)
                saveTanyaPosition(player)
                kotlinx.coroutines.delay(1000)
            }
        }
        val speechStore = SpeechLessonStore(this)
        speech = com.example.data.speech.DailySpeechSession(player,
            speechStore, { com.example.data.speech.PiperHebrewSpeech(this) }, usage = usage)
        session = MediaLibrarySession.Builder(this, player, object : StudyCarLibrary(this@PrayerPlaybackService, usageScope, speechStore) {
            override fun onConnect(session: MediaSession, controller: MediaSession.ControllerInfo): MediaSession.ConnectionResult {
                if (!canConnect(session, controller)) return MediaSession.ConnectionResult.reject()
                val commands = MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS.buildUpon()
                    .add(jumpBack).add(jumpForward)
                if (controller.packageName == packageName) com.example.data.speech.DailySpeechStatus.actions.forEach {
                    commands.add(androidx.media3.session.SessionCommand(it, android.os.Bundle.EMPTY))
                }
                return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                    .setAvailableSessionCommands(commands.build()).build()
            }
            override fun onCustomCommand(session: MediaSession, controller: MediaSession.ControllerInfo,
                command: androidx.media3.session.SessionCommand, args: android.os.Bundle):
                com.google.common.util.concurrent.ListenableFuture<androidx.media3.session.SessionResult> {
                val result = if (command == jumpBack || command == jumpForward) {
                    if (!isStudyItem(player)) androidx.media3.session.SessionError.ERROR_NOT_SUPPORTED
                    else {
                        if (command == jumpBack) player.seekBack() else player.seekForward()
                        saveTanyaPosition(player)
                        androidx.media3.session.SessionResult.RESULT_SUCCESS
                    }
                } else if (controller.packageName != packageName) androidx.media3.session.SessionError.ERROR_PERMISSION_DENIED
                else {
                    when (command.customAction) {
                        com.example.data.speech.DailySpeechStatus.START -> {
                            cancelPendingPlayback()
                            speech?.start(args.getString("lesson").orEmpty(), args.getBoolean("restart"))
                        }
                        com.example.data.speech.DailySpeechStatus.TOGGLE -> speech?.toggle()
                        com.example.data.speech.DailySpeechStatus.STOP -> speech?.stop()
                        com.example.data.speech.DailySpeechStatus.REWIND -> speech?.rewind()
                        com.example.data.speech.DailySpeechStatus.SPEED -> speech?.speed()
                        com.example.data.speech.DailySpeechStatus.NEXT -> speech?.skipUnit(true)
                        com.example.data.speech.DailySpeechStatus.PREVIOUS -> speech?.skipUnit(false)
                        com.example.data.speech.DailySpeechStatus.SEEK -> speech?.seek(args.getLong("position"))
                        else -> return com.google.common.util.concurrent.Futures.immediateFuture(androidx.media3.session.SessionResult(androidx.media3.session.SessionError.ERROR_NOT_SUPPORTED))
                    }
                    androidx.media3.session.SessionResult.RESULT_SUCCESS
                }
                return com.google.common.util.concurrent.Futures.immediateFuture(androidx.media3.session.SessionResult(result))
            }
        }).setSessionActivity(android.app.PendingIntent.getActivity(this, 0,
            Intent(this, com.example.MainActivity::class.java), android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE))
            .build()
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? =
        session?.takeIf {
            // Media3 asks for the browser binder with a placeholder before identifying the client in onConnect.
            (controllerInfo.uid == -1 && controllerInfo.packageName == RemoteUserInfo.LEGACY_CONTROLLER) ||
                canConnect(it, controllerInfo)
        }

    private fun canConnect(session: MediaSession, controller: MediaSession.ControllerInfo): Boolean =
        controller.packageName == packageName || controller.isTrusted || session.isAutoCompanionController(controller)

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (session?.player?.playWhenReady != true) stopSelf()
    }

    private fun measure(player: androidx.media3.common.Player) {
        val item = player.currentMediaItem
        val speechState = com.example.data.speech.DailySpeechStatus.state.value
        val speechOwned = item?.mediaId?.startsWith("daily-speech:") == true || (item == null && speechState.active)
        val key = if(speechOwned) speechState.lessonId else item?.mediaMetadata?.extras?.getString("usage_key")
        val type = if(speechOwned) com.example.data.analytics.AudioAnalytics.rambamType(key.orEmpty()) else item?.mediaMetadata?.extras?.getString("usage_type") ?: "prayer"
        StudyPlaybackStatus.report(type, player.isPlaying && item != null)
        usage?.update(key, type, if(speechOwned && !speechState.recorded) "michael" else "recording", player.isPlaying && item != null,
            if(speechOwned) speechState.completed else player.playbackState == androidx.media3.common.Player.STATE_ENDED)
    }

    private fun isStudyItem(player: Player): Boolean =
        player.currentMediaItem?.let { item ->
            item.mediaId.startsWith("daily-speech:") || item.mediaMetadata.extras?.getString("usage_type") == "tanya"
        } == true

    private fun saveTanyaPosition(player: Player) {
        val item = player.currentMediaItem ?: return
        if (item.mediaMetadata.extras?.getString("usage_type") != "tanya") return
        val position = if (player.playbackState == Player.STATE_ENDED) 0L else player.currentPosition.coerceAtLeast(0)
        if (tanyaPositions.getLong(item.mediaId, 0L) != position) tanyaPositions.edit().putLong(item.mediaId, position).apply()
    }

    override fun onDestroy() {
        usage?.close(); usageScope.cancel()
        speech?.close(); speech = null
        session?.run { saveTanyaPosition(player); player.release(); release() }
        session = null
        AudioPlaybackActivity.busy=false
        StudyPlaybackStatus.report(null, false)
        super.onDestroy()
    }
}
