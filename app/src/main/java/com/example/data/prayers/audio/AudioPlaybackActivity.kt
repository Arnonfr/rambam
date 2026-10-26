package com.example.data.prayers.audio

/** Shared by all recordings using the application's media session, including paused audio. */
object AudioPlaybackActivity {
    @Volatile var busy:Boolean=false
        internal set
}

/** Service-owned observable state, independent of whichever screen is visible. */
data class StudyPlaybackState(val track: String? = null, val playing: Boolean = false)

object StudyPlaybackStatus {
    private val mutable = kotlinx.coroutines.flow.MutableStateFlow(StudyPlaybackState())
    val state: kotlinx.coroutines.flow.StateFlow<StudyPlaybackState> = mutable
    internal fun report(type: String?, playing: Boolean) {
        val track = when (type) {
            "rambam_one" -> "one"
            "rambam_three" -> "three"
            "tanya" -> "tanya"
            else -> null
        }
        mutable.value = StudyPlaybackState(track, playing && track != null)
    }
}
