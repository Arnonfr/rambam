package com.example.ui.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.TextUnit

/** Keep sources and editorial notes inline, including nested brackets and footnote markers. */
internal fun withSmallReadingNotes(text: AnnotatedString, fontSize: TextUnit): AnnotatedString = buildAnnotatedString {
    append(text)
    val closingBrackets = mutableListOf<Char>()
    var start = -1
    text.text.forEachIndexed { index, char ->
        when (char) {
            '(', '[' -> {
                if (closingBrackets.isEmpty()) {
                    start = index
                    while (start > 0 && text.text[start - 1] == '*') start--
                }
                closingBrackets.add(if (char == '(') ')' else ']')
            }
            ')', ']' -> if (closingBrackets.lastOrNull() == char) {
                closingBrackets.removeAt(closingBrackets.lastIndex)
                if (closingBrackets.isEmpty()) addStyle(SpanStyle(fontSize = fontSize), start, index + 1)
            }
        }
    }
}
