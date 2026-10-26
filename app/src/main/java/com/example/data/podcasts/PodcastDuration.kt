package com.example.data.podcasts

/** RSS allows seconds or HH:MM:SS. Display only a validated, readable time. */
fun podcastDurationLabel(value: String): String {
    val text = value.trim()
    val seconds = if (text.all { it.isDigit() } && text.isNotEmpty()) text.toLongOrNull()
    else {
        val parts = text.split(':')
        if (parts.size !in 2..3 || parts.any { it.isBlank() || it.any { c -> !c.isDigit() } }) return ""
        val numbers = parts.map { it.toLongOrNull() ?: return "" }
        if (numbers.drop(1).any { it > 59 } || numbers.first() > 999) return ""
        numbers.fold(0L) { sum, next -> sum * 60 + next }
    } ?: return ""
    if (seconds !in 1..359999) return ""
    return if (seconds >= 3600) "%d:%02d:%02d".format(seconds / 3600, seconds / 60 % 60, seconds % 60)
    else "%d:%02d".format(seconds / 60, seconds % 60)
}
