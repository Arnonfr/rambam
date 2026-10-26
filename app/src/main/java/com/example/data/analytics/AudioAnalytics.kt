package com.example.data.analytics

import android.app.Application
import android.content.Context
import android.os.Bundle
import android.os.SystemClock
import com.example.BuildConfig
import com.example.domain.audio.AudioUsageTracker
import com.google.firebase.analytics.FirebaseAnalytics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

class StudyApplication : Application() {
    override fun onCreate() { super.onCreate(); AudioAnalytics.get(this) }
}

/** No text, lesson IDs, coordinates, names or raw exceptions leave the device. */
class AudioAnalytics private constructor(context: Context) {
    private val prefs = context.getSharedPreferences("audio_measurement_v1", Context.MODE_PRIVATE)
    private val journal = context.getSharedPreferences("audio_measurement_pending_v1", Context.MODE_PRIVATE)
    private val sdk = runCatching { FirebaseAnalytics.getInstance(context) }.getOrNull()
    private val mutable = MutableStateFlow(prefs.getBoolean("enabled", true))
    val enabled = mutable.asStateFlow()
    val tracker = AudioUsageTracker(SystemClock::elapsedRealtime, { mutable.value }, ::event) { type, engine, pending ->
        if (mutable.value) journal.edit().putLong("$type:$engine", pending).apply()
    }
    init {
        runCatching { sdk?.setConsent(mapOf(
            FirebaseAnalytics.ConsentType.AD_STORAGE to FirebaseAnalytics.ConsentStatus.DENIED,
            FirebaseAnalytics.ConsentType.AD_USER_DATA to FirebaseAnalytics.ConsentStatus.DENIED,
            FirebaseAnalytics.ConsentType.AD_PERSONALIZATION to FirebaseAnalytics.ConsentStatus.DENIED,
            FirebaseAnalytics.ConsentType.ANALYTICS_STORAGE to if (mutable.value) FirebaseAnalytics.ConsentStatus.GRANTED else FirebaseAnalytics.ConsentStatus.DENIED))
        sdk?.setAnalyticsCollectionEnabled(mutable.value) }
        if (mutable.value) journal.all.forEach { (key, value) ->
            if (value is Long && value > 0) event("audio_listen_time", mapOf("content_type" to key.substringBefore(':'),
                "engine" to key.substringAfter(':'), "listened_ms" to value, "value" to value/60_000.0, "schema_version" to 1L))
        }
        journal.edit().clear().apply()
    }
    fun setEnabled(value: Boolean) {
        tracker.close()
        mutable.value = value
        if(value) prefs.edit().putBoolean("enabled", true).apply()
        else prefs.edit().clear().putBoolean("enabled",false).apply()
        journal.edit().clear().apply()
        runCatching { sdk?.setAnalyticsCollectionEnabled(value)
        sdk?.setConsent(mapOf(FirebaseAnalytics.ConsentType.ANALYTICS_STORAGE to
            if(value) FirebaseAnalytics.ConsentStatus.GRANTED else FirebaseAnalytics.ConsentStatus.DENIED))
        if (!value) sdk?.resetAnalyticsData() }
    }
    @Synchronized fun event(name: String, values: Map<String, Any>) {
        if (!mutable.value) return
        runCatching {
            val params = Bundle().apply {
                putString("build_type", if(BuildConfig.DEBUG) "debug" else "release")
                values.forEach { (key, value) -> when(value) {
                    is Long -> putLong(key, value)
                    is Double -> putDouble(key, value)
                    is String -> putString(key, value)
                } }
            }
            sdk?.logEvent(name, params)
            val type=values["content_type"] as? String
            val suffix=mapOf("audio_available" to "available", "audio_play_requested" to "requested",
                "audio_play_started" to "started", "audio_engaged" to "engaged", "audio_five_minutes" to "five_minutes",
                "audio_listen_time" to "minutes", "audio_error" to "error")[name]
            if(type in setOf("rambam_one","rambam_three","tanya","prayer") && suffix != null)
                sdk?.logEvent("audio_${type}_$suffix",params)
            if(name == "audio_listen_time" && type != null) {
                val today=LocalDate.now().toEpochDay()
                val priorMs=if(prefs.getLong("listening_day:$type",Long.MIN_VALUE)==today) prefs.getLong("daily_ms:$type",0) else 0L
                val nextMs=priorMs+(values["listened_ms"] as? Long ?: 0L)
                prefs.edit().putLong("listening_day:$type",today).putLong("daily_ms:$type",nextMs).apply()
                if(priorMs < 30_000 && nextMs >= 30_000) {
                    sdk?.logEvent("audio_listening_day",params)
                    sdk?.logEvent("audio_${type}_listening_day",params)
                    val previous=prefs.getLong("engaged:$type",Long.MIN_VALUE)
                    if(today > previous && previous >= today-7) {
                        sdk?.logEvent("audio_return_7d",params)
                        sdk?.logEvent("audio_${type}_return_7d",params)
                    }
                    prefs.edit().putLong("engaged:$type",today).apply()
                }
            }
        }
    }
    @Synchronized fun available(type: String) {
        if (!mutable.value) return
        val day = LocalDate.now().toString()
        if (prefs.getString("seen:$type", null) == day) return
        prefs.edit().putString("seen:$type", day).apply()
        panelEvent("audio_available", type)
    }
    fun panelEvent(name: String, type: String) = event(name, mapOf("content_type" to type,
        "engine" to if(type.startsWith("rambam")) "michael" else "recording", "schema_version" to 1L))
    companion object {
        @Volatile private var instance: AudioAnalytics? = null
        fun get(context: Context): AudioAnalytics = instance ?: synchronized(this) {
            instance ?: AudioAnalytics(context.applicationContext).also { instance = it }
        }
        fun rambamType(id: String) = if (id.contains("-three-")) "rambam_three" else "rambam_one"
    }
}
