package com.example.data.zmanim

import android.content.Context
import com.example.domain.zmanim.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class ZmanimStore(context: Context) {
    private val storage = context.applicationContext.getSharedPreferences("zmanim_v1", Context.MODE_PRIVATE)
    private val mutableState = MutableStateFlow(read())
    val state = mutableState.asStateFlow()
    private fun read(): ZmanimPreferences = runCatching {
        val json = JSONObject(storage.getString("preferences", "{}")!!)
        val selected = json.optJSONArray("selected")?.let { array ->
            (0 until array.length()).mapNotNull { index -> ZmanId.entries.find { it.name == array.getString(index) } }.filter { it != ZmanId.CANDLES }.distinct()
        }?.takeIf { it.isNotEmpty() } ?: ZmanId.defaults
        ZmanimPreferences(json.optBoolean("enabled", true), selected, json.optBoolean("highlight", true),
            if (json.optString("mode") == "gps") "gps" else "fixed",
            location(json.optJSONObject("fixed")) ?: ZmanimPreferences.defaultLocation, location(json.optJSONObject("gps")),
            showCivilDate = json.optBoolean("civilDate", false),
            showCandleLighting = json.optBoolean("candles", true),
            candleLightingMinutes = if (json.has("candleMinutes")) json.getInt("candleMinutes").coerceIn(5,60) else null,
            holidayInIsrael = if (json.has("israel")) json.getBoolean("israel") else null,
            scrollHintsShown = json.optInt("scrollHints",0).coerceIn(0,2),
            method = ZmanimMethod.from(json.optString("method")))
    }.getOrDefault(ZmanimPreferences())
    private fun location(json: JSONObject?): ZmanimLocation? = json?.let { runCatching {
        ZmanimLocation(it.getString("name"), it.getDouble("lat"), it.getDouble("lon"), it.getString("zone"),
            it.optBoolean("gps"), if (it.has("accuracy")) it.getDouble("accuracy").toFloat() else null,
            if (it.has("captured")) it.getLong("captured") else null)
    }.getOrNull() }
    private fun json(location: ZmanimLocation?): JSONObject? = location?.let {
        JSONObject().put("name", it.name).put("lat", it.latitude).put("lon", it.longitude)
            .put("zone", it.timezoneId).put("gps", it.gps).put("accuracy", it.accuracyMeters).put("captured", it.capturedAt)
    }
    fun update(value: ZmanimPreferences) {
        val safe = value.copy(selected = value.selected.filter { it != ZmanId.CANDLES }.distinct().ifEmpty { ZmanId.defaults })
        storage.edit().putString("preferences", JSONObject().put("enabled", safe.enabled)
            .put("selected", JSONArray(safe.selected.map { it.name })).put("highlight", safe.highlightNext)
            .put("mode", safe.mode).put("fixed", json(safe.fixedLocation)).put("gps", json(safe.gpsLocation))
            .put("civilDate", safe.showCivilDate).put("candles",safe.showCandleLighting)
            .put("candleMinutes",safe.candleLightingMinutes).put("israel",safe.holidayInIsrael)
            .put("method",safe.method.name).put("scrollHints",safe.scrollHintsShown.coerceIn(0,2)).toString()).apply()
        mutableState.value = safe
    }
}
