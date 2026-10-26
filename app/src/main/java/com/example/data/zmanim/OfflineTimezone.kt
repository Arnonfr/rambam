package com.example.data.zmanim

import android.content.Context
import org.json.JSONObject

/** Port of tz-lookup 6.1.25's compact quadtree (CC0). No location leaves the device. */
object OfflineTimezone {
    @Volatile private var data: Pair<String, List<String>>? = null
    fun at(context: Context, latitude: Double, longitude: Double): String {
        require(latitude.isFinite() && latitude in -90.0..90.0 && longitude.isFinite() && longitude in -180.0..180.0)
        if (latitude >= 90) return "Etc/GMT"
        val (tree, zones) = data ?: synchronized(this) {
            data ?: context.assets.open("zmanim_timezones.json").bufferedReader().use { reader ->
                val json = JSONObject(reader.readText())
                json.getString("data") to json.getJSONArray("zones").let { array ->
                    (0 until array.length()).map { array.getString(it) }
                }
            }.also { data = it }
        }
        var v = 48 * (180 + longitude) / 360.00000000000006
        var x = 24 * (90 - latitude) / 180.00000000000003
        var z = v.toInt()
        var m = x.toInt()
        var s = -1
        var g = 96 * m + 2 * z
        fun decode(index: Int) = 56 * tree[index].code + tree[index + 1].code - 1995
        g = decode(g)
        while (g + zones.size < 3136) {
            s += g + 1
            x = 2 * (x - m) % 2; m = x.toInt()
            v = 2 * (v - z) % 2; z = v.toInt()
            g = decode(8 * s + 4 * m + 2 * z + 2304)
        }
        return zones[g + zones.size - 3136]
    }
}
