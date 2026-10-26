package com.example.data.tehillim

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.tehillim.*
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.personalPsalms by preferencesDataStore("personal_tehillim")

data class PersonalTehillimState(val entries: List<PersonalTehillimEntry> = emptyList(), val completed: Set<String> = emptySet())

class PersonalTehillimStore(context: Context) {
    private val store = context.applicationContext.personalPsalms
    private val entriesKey = stringPreferencesKey("entries_v1")
    private val completedKey = stringSetPreferencesKey("completed_v1")
    val state = store.data.map { PersonalTehillimState(decode(it[entriesKey] ?: "[]"), it[completedKey].orEmpty()) }

    suspend fun save(entry: PersonalTehillimEntry) = change { entries ->
        if (entries.any { it.id == entry.id }) entries.map { if (it.id == entry.id) entry else it } else entries + entry
    }
    suspend fun delete(id: String) = store.edit { prefs ->
        prefs[entriesKey] = encode(decode(prefs[entriesKey] ?: "[]").filterNot { it.id == id })
        prefs[completedKey] = prefs[completedKey].orEmpty().filterNot { it.startsWith("$id|") }.toSet()
    }
    suspend fun move(id: String, delta: Int) = change { entries ->
        val index = entries.indexOfFirst { it.id == id }
        if (index < 0 || index + delta !in entries.indices) entries else entries.toMutableList().apply {
            add(index + delta, removeAt(index))
        }
    }
    suspend fun complete(entry: PersonalTehillimEntry, date: java.time.LocalDate, checked: Boolean) = store.edit { prefs ->
        val chapter = entry.chapter(date) ?: return@edit
        val key = completionKey(entry.id, date.toString(), chapter)
        val current = prefs[completedKey].orEmpty()
        prefs[completedKey] = if (checked) current + key else current - key
    }
    private suspend fun change(transform: (List<PersonalTehillimEntry>) -> List<PersonalTehillimEntry>) = store.edit {
        it[entriesKey] = encode(transform(decode(it[entriesKey] ?: "[]")))
    }

    companion object {
        fun completionKey(id: String, date: String, chapter: Int) = "$id|$date|$chapter"
        fun encode(entries: List<PersonalTehillimEntry>): String = JSONArray().apply {
            entries.forEach { e -> put(JSONObject().apply {
                put("id", e.id); put("label", e.label); put("chapter", e.fixedChapter)
                e.birthday?.let { put("birthday", JSONObject().put("year", it.year).put("month", it.month).put("day", it.day)) }
                put("adar", e.adar.name); put("missingDay", e.missingDay.name)
            }) }
        }.toString()
        fun decode(json: String): List<PersonalTehillimEntry> = runCatching {
            val array = JSONArray(json)
            (0 until array.length()).mapNotNull { index -> runCatching {
                val o = array.getJSONObject(index)
                PersonalTehillimEntry(o.getString("id"), o.optString("label"),
                    if (o.has("chapter") && !o.isNull("chapter")) o.getInt("chapter") else null,
                    o.optJSONObject("birthday")?.let { HebrewBirthday(it.getInt("year"), it.getInt("month"), it.getInt("day")) },
                    AdarBirthday.valueOf(o.optString("adar", "SECOND")),
                    MissingBirthdayDay.valueOf(o.optString("missingDay", "NEXT_MONTH")))
            }.getOrNull() }
        }.getOrDefault(emptyList())
    }
}
