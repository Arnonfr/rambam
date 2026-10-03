package com.example.data.prayers

data class PrayerService(val id: String, val title: String, val sections: List<PrayerSection>)

object PrayerServices {
    fun group(sections: List<PrayerSection>): List<PrayerService> = buildList {
        val morning = sections.filter { it.id in setOf("morning", "blessings", "offerings",
            "psalms", "shema", "amidah", "tachanun", "closing", "shir_shel_yom") }
        if (morning.isNotEmpty()) add(PrayerService("shacharit", "שחרית", morning))
        sections.filter { it !in morning }.forEach { add(PrayerService(it.id, it.title, listOf(it))) }
    }
}
