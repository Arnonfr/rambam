package com.example.domain.tanya

/** Each printed word keeps its absolute section, including shared daily-boundary lines. */
internal object TanyaPrintExtension {
    private data class Row(val line: TanyaSourceLine, val wordSections: List<Int>)
    private val marker = Regex("\\[(\\d+):(\\d+)\\]")
    private val reference = Regex("Tanya, Part IV; Iggeret HaKodesh (\\d+):(\\d+)(?:-(\\d+))?")
    private val finalReference = Regex("Tanya, Part V; Kuntres Acharon (\\d+):(\\d+)(?:-(\\d+))?")
    private val completeSectionLimits = mapOf(25 to 40, 26 to 36, 27 to 19, 28 to 9,
        29 to 27, 30 to 5, 31 to 9, 32 to 10, 101 to 6, 102 to 6, 103 to 7,
        104 to 58, 105 to 10, 106 to 15, 107 to 6, 108 to 8, 109 to 12)
    private val rows by lazy {
        var section = 0
        TanyaPrintExtensionData.pages.flatMap { page ->
            val lines = page.lines()
            val pageNumber = lines.first().removePrefix("@").toInt()
            lines.drop(1).filter { it.isNotBlank() }.map { raw ->
                val style = if (raw.startsWith("~")) raw.substringBefore(' ').removePrefix("~")
                    .split(',').associate { it.substringBefore('=') to it.substringAfter('=', "true") }
                    else emptyMap()
                val body = if (raw.startsWith("~")) raw.substringAfter(' ') else raw
                val refs = mutableListOf<Int>()
                val words = body.split(' ').map { token ->
                    marker.findAll(token).forEach {
                        section = it.groupValues[1].toInt() * 1000 + it.groupValues[2].toInt()
                    }
                    refs.add(section)
                    marker.replace(token, "")
                }
                val smallFrom = style["smallFrom"]?.toInt() ?: 0
                val smallTo = style["smallTo"]?.toInt() ?: words.size
                val small = if ("smallFrom" in style || "smallTo" in style)
                    (smallFrom until smallTo).associateWith { .68f } else emptyMap()
                val centered = "center" in style
                Row(TanyaSourceLine(pageNumber, words.joinToString(" "), 0,
                    justified = !centered, largeOpeningWords = style["large"]?.toInt() ?: 0,
                    rightIndentFraction = style["indent"]?.toFloat() ?: 0f,
                    centered = centered, fontScale = style["scale"]?.toFloat() ?: 1f,
                    wordFontScales = small), refs)
            }
        }
    }

    fun fullPagesForReference(ref: String): List<TanyaSourceLine> {
        val isFinal = finalReference.matches(ref)
        val match = (if (isFinal) finalReference else reference).matchEntire(ref) ?: return emptyList()
        val chapter = match.groupValues[1].toInt() + if (isFinal) 100 else 0
        val start = match.groupValues[2].toInt()
        val end = match.groupValues[3].toIntOrNull() ?: start
        // Do not show a truncated lesson if its end lies beyond the proofread coverage.
        if (start < 1 || end < start || end > (completeSectionLimits[chapter] ?: 0)) return emptyList()
        val ids = (chapter * 1000 + start)..(chapter * 1000 + end)
        val pages = rows.filter { row -> row.wordSections.any { it in ids } }.map { it.line.page }.toSet()
        return rows.filter { it.line.page in pages }.map { row ->
            val first = row.wordSections.indexOfFirst { it in ids }
            val last = row.wordSections.indexOfLast { it in ids }
            if (first < 0) row.line.copy(greyFromWord = 0)
            else row.line.copy(section = row.wordSections[first] % 1000 - start + 1,
                greyBeforeWord = first.takeIf { it > 0 },
                greyFromWord = (last + 1).takeIf { it < row.wordSections.size })
        }
    }

    fun excerpt(ref: String) = fullPagesForReference(ref).filter { VerifiedTanyaPrint.hasDailyWords(it) }
}
