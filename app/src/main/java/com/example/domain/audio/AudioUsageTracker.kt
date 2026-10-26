package com.example.domain.audio

/** Measures wall-clock playback, never media position (seeks and speed are irrelevant). */
class AudioUsageTracker(
    private val now: () -> Long,
    private val enabled: () -> Boolean,
    private val send: (String, Map<String, Any>) -> Unit,
    private val checkpoint: (String, String, Long) -> Unit = { _, _, _ -> }
) {
    private var key: String? = null
    private var type = "rambam_one"
    private var engine = "michael"
    private var last = now()
    private var playing = false
    private var total = 0L
    private var pending = 0L
    private var requestedAt = last
    private var started = false
    private var engaged = false
    private var deep = false
    private var ended = false
    private var allowed = enabled()

    private fun emit(name: String, values: Map<String, Any> = emptyMap()) {
        if (enabled()) runCatching { send(name, mapOf("content_type" to type, "engine" to engine, "schema_version" to 1L) + values) }
    }
    fun request(id: String, contentType: String, source: String, voiceReady: Boolean = true, restart: Boolean = false) {
        sample()
        if (key != id || restart || ended) {
            flush(); key = id; type = contentType; engine = source
            total = 0; pending = 0; started = false; engaged = false; deep = false; ended = false
        }
        playing = false
        requestedAt = now()
        emit("audio_play_requested", mapOf("voice_ready" to if (voiceReady) "yes" else "no"))
    }
    fun update(id: String?, contentType: String, source: String, isPlaying: Boolean, queueEnded: Boolean = false) {
        sample()
        if (id == null) { flush(); key = null; playing = false; return }
        if (key != id) {
            flush(); key = id; type = contentType; engine = source
            total = 0; pending = 0; started = false; engaged = false; deep = false; ended = false; requestedAt = now()
        }
        if (!isPlaying) flush()
        playing = isPlaying && enabled()
        if (playing && !started) {
            started = true
            emit("audio_play_started", mapOf("startup_ms" to (now() - requestedAt).coerceAtLeast(0)))
        }
        if (queueEnded && !ended) { ended = true; emit("audio_queue_ended"); flush() }
    }
    fun sample() {
        val time = now()
        val consent = enabled()
        if (consent != allowed) {
            // Do not attribute any interval spanning a privacy change, or retain disabled use.
            pending = 0; total = 0; started = false; engaged = false; deep = false; playing = false
            runCatching { checkpoint(type, engine, 0) }; allowed = consent
        }
        if (playing && consent && key != null) {
            val delta = (time - last).coerceAtLeast(0)
            total += delta; pending += delta
            runCatching { checkpoint(type, engine, pending) }
            if (!engaged && total >= 30_000) { engaged = true; emit("audio_engaged") }
            if (!deep && total >= 300_000) { deep = true; emit("audio_five_minutes") }
            if (pending >= 30_000) flush()
        }
        last = time
    }
    fun error(reason: String) { sample(); emit("audio_error", mapOf("reason" to reason)); playing = false; flush() }
    fun close() { sample(); flush(); playing = false; key = null }
    private fun flush() {
        if (pending > 0) emit("audio_listen_time", mapOf("listened_ms" to pending, "value" to pending / 60_000.0))
        pending = 0
        runCatching { checkpoint(type, engine, 0) }
    }
}
