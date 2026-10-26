package com.example.data.prayers.audio

import android.content.Intent
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class PrayerPlaybackService : MediaSessionService() {
    private val usageScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Main.immediate)
    private var usage: com.example.domain.audio.AudioUsageTracker? = null
    private var session: MediaSession? = null
    private var speech: com.example.data.speech.DailySpeechSession? = null

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        usage = com.example.data.analytics.AudioAnalytics.get(this).tracker
        AudioPlaybackActivity.busy=false
        StudyPlaybackStatus.report(null, false)
        val player = ExoPlayer.Builder(this).build().apply {
            addListener(object:androidx.media3.common.Player.Listener {
                override fun onEvents(player:androidx.media3.common.Player,events:androidx.media3.common.Player.Events) {
                    measure(player)
                    AudioPlaybackActivity.busy=player.mediaItemCount>0 && player.playbackState!=androidx.media3.common.Player.STATE_ENDED
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
        usageScope.launch { while (kotlinx.coroutines.currentCoroutineContext().isActive) { measure(player); kotlinx.coroutines.delay(1000) } }
        speech = com.example.data.speech.DailySpeechSession(player,
            com.example.data.speech.SpeechLessonStore(this), { com.example.data.speech.PiperHebrewSpeech(this) }, usage = usage)
        session = MediaSession.Builder(this, player).setSessionActivity(android.app.PendingIntent.getActivity(this, 0,
            Intent(this, com.example.MainActivity::class.java), android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE))
            .setCallback(object : MediaSession.Callback {
            override fun onConnect(session: MediaSession, controller: MediaSession.ControllerInfo): MediaSession.ConnectionResult {
                val commands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                if (controller.packageName == packageName) com.example.data.speech.DailySpeechStatus.actions.forEach {
                    commands.add(androidx.media3.session.SessionCommand(it, android.os.Bundle.EMPTY))
                }
                return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                    .setAvailableSessionCommands(commands.build()).build()
            }
            override fun onCustomCommand(session: MediaSession, controller: MediaSession.ControllerInfo,
                command: androidx.media3.session.SessionCommand, args: android.os.Bundle):
                com.google.common.util.concurrent.ListenableFuture<androidx.media3.session.SessionResult> {
                val result = if (controller.packageName != packageName) androidx.media3.session.SessionError.ERROR_PERMISSION_DENIED
                else {
                    when (command.customAction) {
                        com.example.data.speech.DailySpeechStatus.START -> speech?.start(args.getString("lesson").orEmpty(), args.getBoolean("restart"))
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
        }).build()
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        session?.takeIf { controllerInfo.packageName == packageName || controllerInfo.isTrusted }

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

    override fun onDestroy() {
        usage?.close(); usageScope.cancel()
        speech?.close(); speech = null
        session?.run { player.release(); release() }
        session = null
        AudioPlaybackActivity.busy=false
        StudyPlaybackStatus.report(null, false)
        super.onDestroy()
    }
}
