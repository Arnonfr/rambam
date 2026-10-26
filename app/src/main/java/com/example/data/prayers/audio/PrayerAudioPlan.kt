package com.example.data.prayers.audio

import android.content.Context
import com.example.data.prayers.PrayerService
import com.example.ui.util.HebrewTextNormalizer
import org.json.JSONObject
import java.time.DayOfWeek

data class PrayerAudioClip(val id: String, val uri: String, val approved: Boolean,
    val weekday: DayOfWeek? = null)

data class PrayerAudioStep(val id: String, val title: String, val textSectionId: String,
    val paragraphIndex: Int = 0, val clip: PrayerAudioClip? = null)

data class PrayerAudioRun(val steps: List<PrayerAudioStep>, val blockedAt: PrayerAudioStep?)

/** One entry of assets/prayer_audio_order.json: where a section is recited and on which days. */
data class PrayerAudioOrderStep(val id: String, val title: String, val section: String,
    val anchor: String?, val conditions: List<String>, val sheetId: String) {
    /** All conditions hold; "!name" requires the flag to be absent. */
    fun applies(flags: Set<String>): Boolean = conditions.all {
        if (it.startsWith("!")) it.drop(1) !in flags else it in flags
    }
}

object PrayerAudioOrder {
    fun parse(text: String): List<PrayerAudioOrderStep> {
        val steps = JSONObject(text).getJSONArray("steps")
        return (0 until steps.length()).map { i ->
            val s = steps.getJSONObject(i)
            val conditions = s.getJSONArray("when")
            PrayerAudioOrderStep(s.getString("id"), s.getString("title"), s.getString("section"),
                if (s.isNull("anchor")) null else s.getString("anchor"),
                (0 until conditions.length()).map { conditions.getString(it) }, s.getString("sheet"))
        }.also { require(it.map { s -> s.id }.distinct().size == it.size) { "Duplicate prayer audio step" } }
    }

    fun load(context: Context): List<PrayerAudioOrderStep> =
        parse(context.assets.open("prayer_audio_order.json").bufferedReader().use { it.readText() })
}

/** Never removes a required recording from a service or skips past a gap. */
object PrayerAudioPlan {
    fun playable(clip: PrayerAudioClip?, day: DayOfWeek, allowDraft: Boolean): Boolean =
        clip != null && (clip.approved || allowDraft) &&
            (clip.weekday == null || clip.weekday == day) &&
            (clip.uri.startsWith("https://") || (allowDraft && clip.uri.startsWith("asset:///prayer-audio/")))

    fun run(steps: List<PrayerAudioStep>, startId: String, day: DayOfWeek, allowDraft: Boolean): PrayerAudioRun {
        val index = steps.indexOfFirst { it.id == startId }
        if (index < 0) return PrayerAudioRun(emptyList(), null)
        val tail = steps.drop(index)
        val ready = tail.takeWhile { playable(it.clip, day, allowDraft) }
        return PrayerAudioRun(ready, tail.getOrNull(ready.size))
    }

    /** The first recorded section after [blockedId]; offered to the user, never entered automatically. */
    fun nextPlayable(steps: List<PrayerAudioStep>, blockedId: String, day: DayOfWeek, allowDraft: Boolean): PrayerAudioStep? =
        steps.drop(steps.indexOfFirst { it.id == blockedId } + 1).firstOrNull { playable(it.clip, day, allowDraft) }

    private fun normalized(text: String) =
        HebrewTextNormalizer.stripMarks(text).replace("|", "").replace(Regex("\\s+"), " ").trim()

    /**
     * The day's steps in prayer order. Steps whose conditions do not hold for the day are not part of
     * that day's prayer; steps that apply but have no recording stay in place and stop playback.
     */
    fun forService(service: PrayerService, clips: Map<String, PrayerAudioClip>,
        order: List<PrayerAudioOrderStep>, flags: Set<String>): List<PrayerAudioStep> {
        val sections = service.sections.associateBy { it.id }
        val last = mutableMapOf<String, Int>()
        return order.filter { it.applies(flags) && it.section in sections }.map { step ->
            val paragraphs = sections.getValue(step.section).paragraphs
            val index = when (step.anchor) {
                null -> 0
                "END" -> paragraphs.lastIndex
                // Anchors refer to the displayed text; an unresolved anchor keeps the previous position
                // rather than dropping the step (dropping would silently skip a prayer).
                else -> paragraphs.indexOfFirst { normalized(it).startsWith(step.anchor) }
                    .takeIf { it >= 0 } ?: last[step.section] ?: 0
            }
            last[step.section] = index
            PrayerAudioStep(step.id, step.title, step.section, index, clips[step.id])
        }
    }

    fun unresolvedAnchors(service: PrayerService, order: List<PrayerAudioOrderStep>): List<String> {
        val sections = service.sections.associateBy { it.id }
        return order.filter { it.anchor != null && it.anchor != "END" }.filter { step ->
            sections[step.section]?.paragraphs?.none { normalized(it).startsWith(step.anchor!!) } ?: true
        }.map { it.id }
    }
}
