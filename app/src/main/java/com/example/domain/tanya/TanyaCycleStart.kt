package com.example.domain.tanya

/** Printed source rows for the start of the 5787 (leap-year) cycle.
 * Part I has its own namespace: chapter 25 must never collide with Iggeret 25.
 * Grey context can extend past the last COMPLETE supported assignment.
 */
internal object TanyaCycleStart {
    const val TITLE_REFERENCE = "Tanya, Part I; Likkutei Amarim, Title Page 1-5; Approbation 1:1-9; Approbation 2:1-6"
    private val chapterRef = Regex("Tanya, Part I; Likkutei Amarim (\\d+):(\\d+)(?:-(\\d+))?")
    private val forewordRef = Regex("Tanya, Part I; Likkutei Amarim, Compiler's Foreword (\\d+)(?:-(\\d+))?")
    private val marker = Regex("\\[(\\d+):(\\d+)\\]")
    private val limits = listOf(18, 14, 15, 14, 13, 12, 16, 11, 8, 8, 8, 13, 14, 8, 12, 11, 8, 14, 14, 8, 9, 8, 4)
    private data class Row(val line: TanyaSourceLine, val sections: List<Int>, val columns: List<List<Row>> = emptyList())

    private val rows: List<Row> by lazy {
        TanyaCycleStartData.pages.flatMap { page ->
            val rawLines = page.lines()
            val number = rawLines.first().removePrefix("@").toInt()
            var section = 0
            fun parse(raw: String, column: Boolean = false): Row {
                val options = if (raw.startsWith("~")) raw.substringBefore(' ').removePrefix("~")
                    .split(',').associate { it.substringBefore('=') to it.substringAfter('=', "true") } else emptyMap()
                val body = if (raw.startsWith("~")) raw.substringAfter(' ') else raw
                val sections = mutableListOf<Int>()
                val words = body.split(' ').filter { it.isNotEmpty() }.map { token ->
                    marker.findAll(token).forEach { section = it.groupValues[1].toInt() * 1000 + it.groupValues[2].toInt() }
                    sections.add(section)
                    marker.replace(token, "")
                }
                return Row(TanyaSourceLine(number, words.joinToString(" "), 0,
                    justified = "center" !in options && "catch" !in options && "ragged" !in options,
                    largeOpeningWords = options["large"]?.toInt() ?: 0,
                    rightIndentFraction = options["indent"]?.toFloat() ?: 0f,
                    centered = "center" in options,
                    catchword = "catch" in options,
                    fontScale = (options["scale"]?.toFloat() ?: 1f) * if (column) 2f else 1f), sections)
            }
            val output = mutableListOf<Row>()
            var i = 1
            while (i < rawLines.size) {
                val raw = rawLines[i++]
                if (raw.isBlank()) continue
                if (raw != "~columns") { output.add(parse(raw)); continue }
                val right = mutableListOf<Row>()
                val left = mutableListOf<Row>()
                while (rawLines[i] != "~endcolumns") {
                    val next = rawLines[i++]
                    (if (next.startsWith("~right")) right else left).add(parse(next, column = true))
                }
                i++
                val columns = listOf(right, left)
                val flat = columns.flatten()
                output.add(Row(TanyaSourceLine(number, flat.joinToString(" ") { it.line.text }, 0,
                    columns = columns.map { col -> col.map { it.line } }), flat.flatMap { it.sections }, columns))
            }
            output
        }
    }

    fun fullPagesForReference(ref: String): List<TanyaSourceLine> {
        val wanted: Set<Int>
        val relative: (Int) -> Int
        if (ref == TITLE_REFERENCE) {
            wanted = ((101001..101005) + (102001..102009) + (103001..103006)).toSet()
            relative = { id -> when (id / 1000) { 101 -> id % 1000; 102 -> 5 + id % 1000; else -> 14 + id % 1000 } }
        } else if (ref == "Tanya, Part I; Likkutei Amarim, Approbation 3:1-10") {
            wanted = (104001..104010).toSet()
            relative = { it % 1000 }
        } else {
            val chapter = chapterRef.matchEntire(ref)
            val foreword = forewordRef.matchEntire(ref)
            val group: Int
            val start: Int
            val end: Int
            val limit: Int
            when {
                chapter != null -> {
                    group = chapter.groupValues[1].toInt()
                    start = chapter.groupValues[2].toInt()
                    end = chapter.groupValues[3].toIntOrNull() ?: start
                    limit = limits.getOrNull(group - 1) ?: return emptyList()
                }
                foreword != null -> {
                    group = 105
                    start = foreword.groupValues[1].toInt()
                    end = foreword.groupValues[2].toIntOrNull() ?: start
                    limit = 15
                }
                else -> return emptyList()
            }
            if (start < 1 || end < start || end > limit) return emptyList()
            wanted = (group * 1000 + start..group * 1000 + end).toSet()
            relative = { it % 1000 - start + 1 }
        }
        val pages = rows.filter { row -> row.sections.any { it in wanted } }.map { it.line.page }.toSet()
        fun highlighted(row: Row): TanyaSourceLine {
            val active = row.sections.indices.filter { row.sections[it] in wanted }.toSet()
            return row.line.copy(section = active.firstOrNull()?.let { relative(row.sections[it]) } ?: 0,
                activeWordIndices = active,
                columns = row.columns.map { col -> col.map { highlighted(it) } })
        }
        return rows.filter { it.line.page in pages }.map { highlighted(it) }
    }
}
