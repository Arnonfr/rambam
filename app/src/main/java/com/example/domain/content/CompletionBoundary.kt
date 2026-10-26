package com.example.domain.content

import com.example.data.local.CompletionText

data class CompletionReaderRow(val listIndex: Int, val block: CompletionText)

/** Offsets use unpointed source characters, so switching fonts or nikud cannot move the cut. */
object CompletionBoundary {
    fun plain(text: String): String = text.replace(Regex("[\\u0591-\\u05BD\\u05BF-\\u05C7]"), "")

    fun cut(text: String, plainOffset: Int): String {
        if (plainOffset <= 0) return text
        var letters = 0
        var i = 0
        while (i < text.length && letters < plainOffset) {
            if (plain(text[i].toString()).isNotEmpty()) letters++
            i++
        }
        while (i < text.length && plain(text[i].toString()).isEmpty()) i++
        return text.substring(i).trimStart()
    }

    fun remainder(rows: List<CompletionReaderRow>, listIndex: Int, plainOffset: Int = 0): List<CompletionText> =
        rows.filter { it.listIndex >= listIndex }.map { row ->
            if (row.listIndex == listIndex && plainOffset > 0) row.block.copy(
                textWithNikud = cut(row.block.textWithNikud, plainOffset),
                textPlain = cut(row.block.textPlain, plainOffset)
            ) else row.block
        }.filter { it.textPlain.isNotBlank() || it.textWithNikud.isNotBlank() || it.rashi.isNotEmpty() }
}
