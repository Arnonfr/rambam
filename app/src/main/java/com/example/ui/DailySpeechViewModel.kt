package com.example.ui

import android.app.Application
import android.content.ComponentName
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.session.*
import com.example.data.prayers.audio.PrayerPlaybackService
import com.example.data.speech.*
import com.example.domain.speech.DailySpeechLesson
import kotlinx.coroutines.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class DailySpeechViewModel(app: Application) : AndroidViewModel(app) {
    val state = DailySpeechStatus.state
    private var future: com.google.common.util.concurrent.ListenableFuture<MediaController>? = null
    private var starting: Job? = null
    private var request = 0
    fun preparing() { DailySpeechStatus.mutable.value = DailySpeechState(preparing=true) }
    fun prepare(loader: ((DailySpeechLesson) -> Unit, (String) -> Unit) -> Unit) {
        val ticket = ++request
        starting?.cancel()
        viewModelScope.launch {
            try {
                send(DailySpeechStatus.STOP, Bundle.EMPTY)
                if (ticket != request) return@launch
                preparing()
                loader({ if(ticket == request) start(it) }, { if(ticket == request) error(it) })
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { if(ticket == request) error("לא ניתן להתחבר לנגן. נסו שוב.") }
        }
    }
    fun error(message: String) { DailySpeechStatus.mutable.value = DailySpeechStatus.mutable.value.copy(preparing=false, error=message) }
    fun start(lesson: DailySpeechLesson, restart: Boolean = false) {
        starting?.cancel()
        starting = viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { SpeechLessonStore(getApplication()).save(lesson) }
                send(DailySpeechStatus.START, Bundle().apply { putString("lesson", lesson.id); putBoolean("restart", restart) })
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { error("לא ניתן להתחיל את ההקראה. נסו שוב.") }
        }
    }
    fun action(action: String) {
        if(action == DailySpeechStatus.STOP) {
            request++; starting?.cancel()
            if(!state.value.active) DailySpeechStatus.mutable.value = DailySpeechState()
        }
        viewModelScope.launch { runCatching { send(action, Bundle.EMPTY) }.onFailure { error("לא ניתן להתחבר לנגן. נסו שוב.") } }
    }
    fun restart(id: String) { viewModelScope.launch {
        runCatching { send(DailySpeechStatus.START, Bundle().apply { putString("lesson", id); putBoolean("restart", true) }) }
            .onFailure { error("לא ניתן להתחיל את השיעור מחדש.") }
    } }
    fun seek(position:Long) {viewModelScope.launch {
        runCatching {send(DailySpeechStatus.SEEK,Bundle().apply {putLong("position",position)})}
            .onFailure {error("לא ניתן לשנות את מיקום ההקראה.")}
    }}
    private suspend fun send(action: String, args: Bundle) {
        val f = future ?: MediaController.Builder(getApplication(), SessionToken(getApplication(),
            ComponentName(getApplication(), PrayerPlaybackService::class.java))).buildAsync().also { future = it }
        val c = suspendCancellableCoroutine<MediaController> { continuation ->
            f.addListener({ if (continuation.isActive) runCatching { f.get() }.onSuccess { continuation.resume(it) }
                .onFailure { continuation.resumeWithException(it) } }, ContextCompat.getMainExecutor(getApplication()))
        }
        val result = c.sendCustomCommand(SessionCommand(action, Bundle.EMPTY), args)
        val outcome = suspendCancellableCoroutine<SessionResult> { continuation ->
            result.addListener({ if(continuation.isActive) runCatching { result.get() }
                .onSuccess { continuation.resume(it) }.onFailure { continuation.resumeWithException(it) } },
                ContextCompat.getMainExecutor(getApplication()))
        }
        check(outcome.resultCode == SessionResult.RESULT_SUCCESS)

    }
    override fun onCleared() { starting?.cancel(); future?.let(MediaController::releaseFuture) }
}
