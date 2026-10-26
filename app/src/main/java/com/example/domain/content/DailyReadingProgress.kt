package com.example.domain.content

/** Progress counts units in this day's assignment, rather than calendar position. */
object DailyReadingProgress {
    fun ratio(counts: List<Int>, completed: Set<Int>, currentChapter: Int, currentUnit: Float): Float {
        val sizes = counts.map { it.coerceAtLeast(1) }
        if (sizes.isEmpty()) return 0f
        val units = sizes.indices.sumOf { index ->
            when {
                index in completed -> sizes[index].toDouble()
                currentChapter in sizes.indices && index < currentChapter -> sizes[index].toDouble()
                index == currentChapter -> currentUnit.coerceIn(0f, sizes[index].toFloat()).toDouble()
                else -> 0.0
            }
        }
        return (units / sizes.sum()).toFloat().coerceIn(0f, 1f)
    }
}
