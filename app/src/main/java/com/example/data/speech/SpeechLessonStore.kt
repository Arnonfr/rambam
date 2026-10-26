package com.example.data.speech

import android.content.Context
import com.example.domain.speech.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class SpeechLessonStore(private val context: Context) {
    val voiceCached: Boolean get() = com.example.data.speech.PiperVoiceFiles.assets.all {
        val file = File(context.noBackupFilesDir,"piper-michael-v1/${it.name}")
        file.isFile && file.length() == it.bytes
    }
    private val root = File(context.filesDir, "daily_speech").apply { mkdirs() }
    val audioRoot = File(context.cacheDir, "daily_speech_audio").apply { mkdirs() }
    private val prefs = context.getSharedPreferences("daily_speech_position", Context.MODE_PRIVATE)
    fun save(lesson: DailySpeechLesson) {
        require(lesson.parts.isNotEmpty())
        val json = JSONObject().put("id", lesson.id).put("title", lesson.title).put("source", lesson.source)
            .put("parts", JSONArray().apply { lesson.parts.forEach { put(JSONObject()
                .put("chapterId", it.chapterId).put("chapterTitle", it.chapterTitle).put("text", it.text)
                .put("unit", it.unit).put("unitLabel", it.unitLabel).put("audioUrl", it.audioUrl)) } })
        val file = planFile(lesson.id)
        val temp = File(root, "${file.name}.tmp")
        temp.writeText(json.toString()); check(temp.renameTo(file))
        // Plans are small, but do not retain an unlimited history of lesson text.
        root.listFiles()?.filter { it.extension == "json" && it != file }?.sortedByDescending { it.lastModified() }
            ?.drop(6)?.forEach { it.delete() }
    }
    fun load(id: String): DailySpeechLesson {
        val json = JSONObject(planFile(id).readText())
        require(json.getString("id") == id)
        val a = json.getJSONArray("parts")
        return DailySpeechLesson(id, json.getString("title"), (0 until a.length()).map { i ->
            val p = a.getJSONObject(i); SpeechPart(p.getString("chapterId"), p.getString("chapterTitle"), p.getString("text"),
                p.optInt("unit"), p.optString("unitLabel"), p.optString("audioUrl"))
        }, json.optString("source", "michael")).also {
            require(it.parts.isNotEmpty())
            require(it.source in setOf("michael", "nadav", "peretz"))
            if (it.recorded) require(it.parts.all { part -> RambamRecordingRepository.allowedUrl(part.audioUrl) })
        }
    }
    private fun planFile(id: String): File { require(id.matches(Regex("[a-zA-Z0-9_-]{1,160}"))); return File(root, "$id.json") }
    fun position(id: String) = prefs.getInt("$id.part", 0) to prefs.getLong("$id.ms", 0)
    fun savePosition(id: String, part: Int, ms: Long) { prefs.edit().putInt("$id.part", part).putLong("$id.ms", ms).apply() }
    fun speed() = prefs.getFloat("speed", 1f).coerceIn(.6f, 1.4f)
    fun speed(value: Float) { prefs.edit().putFloat("speed", value.coerceIn(.6f, 1.4f)).apply() }
    fun pruneAudio(keep: Set<String>, maxBytes: Long = 160L * 1024 * 1024) {
        val files = audioRoot.listFiles().orEmpty().filter { it.extension == "wav" }.sortedBy { it.lastModified() }
        var bytes = files.sumOf { it.length() }
        files.filter { it.name !in keep }.forEach { if (bytes > maxBytes) { val size = it.length(); if (it.delete()) bytes -= size } }
    }
}
