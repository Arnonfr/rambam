package com.example.data.reviews

import android.content.Context
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.time.Instant
import java.time.ZoneId

/** Local counters only. A request attempt never implies that a review was shown or sent. */
class ReviewEngagement(context: Context, private val now: () -> Long = System::currentTimeMillis) {
    private val prefs=context.getSharedPreferences("play_review_engagement",Context.MODE_PRIVATE)

    fun recordUse() {
        val day=Instant.ofEpochMilli(now()).atZone(ZoneId.systemDefault()).toLocalDate().toString()
        val days=prefs.getStringSet("usage_days",emptySet()).orEmpty()+day
        prefs.edit().putStringSet("usage_days",days.sorted().takeLast(30).toSet()).apply()
    }

    fun completed(track:String,studyDate:String) {
        synchronized(LOCK) {
            val key="$studyDate|$track"
            val completions=prefs.getStringSet("completions",emptySet()).orEmpty()
            if(key in completions) return
            prefs.edit().putStringSet("completions",(completions+key).sorted().takeLast(1000).toSet())
                .putInt("count",prefs.getInt("count",0)+1).apply()
        }
        opportunities.tryEmit(Unit)
    }

    fun eligible():Boolean {
        val last=prefs.getLong("last_attempt",0)
        return prefs.getStringSet("usage_days",emptySet()).orEmpty().size>=3 &&
            prefs.getInt("count",0)-prefs.getInt("count_at_attempt",0)>=5 &&
            (last==0L || now()-last>=COOLDOWN)
    }

    fun claimAttempt():Boolean=synchronized(LOCK) {
        if(!eligible()) false else {
            prefs.edit().putLong("last_attempt",now()).putInt("count_at_attempt",prefs.getInt("count",0)).commit()
        }
    }

    /** A manual store visit also suppresses automatic requests for the same period. */
    fun manualStoreVisit() {
        prefs.edit().putLong("last_attempt",now()).putInt("count_at_attempt",prefs.getInt("count",0)).apply()
    }

    companion object {
        private val LOCK=Any()
        private const val COOLDOWN=90L*24*60*60*1000
        private val opportunities=MutableSharedFlow<Unit>(extraBufferCapacity=1)
        val completions=opportunities.asSharedFlow()
    }
}
