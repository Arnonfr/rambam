package com.example.data.reviews

import android.content.Context

/** Counts foreground sessions, not recompositions, rotations or returns from the store. */
class TesterFeedbackEngagement(context: Context, private val now: () -> Long = System::currentTimeMillis) {
    private val prefs = context.getSharedPreferences("tester_feedback_sessions", Context.MODE_PRIVATE)

    fun recordUse() {
        val count = prefs.getInt("sessions", 0)
        val background = prefs.getLong("background", 0)
        if (count == 0 || background == 0L || now() - background >= SESSION_GAP) {
            prefs.edit().putInt("sessions", count + 1).putLong("background", now()).commit()
        }
    }

    fun recordBackground() { prefs.edit().putLong("background", now()).apply() }
    fun eligible() = prefs.getInt("sessions", 0) >= prefs.getInt("next_prompt", 3)
    fun claimPrompt(): Boolean {
        if (!eligible()) return false
        // Persist before showing: a process restart must not immediately repeat the prompt.
        return prefs.edit().putInt("next_prompt", Int.MAX_VALUE).commit()
    }

    companion object { const val SESSION_GAP = 30 * 60 * 1000L }
}
