package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.UserPreferences
import com.example.ui.theme.FontStyleOption

/**
 * Reusable Study Text Block ("תבנית לימוד אחודה")
 *
 * Ensures all daily study modules (Rambam, Chumash, Tanya, etc.) render text
 * with identical styling, typography hierarchy, line spacing, font family,
 * and user preferences (nikud / plain text switch, reader theme color).
 */
@Composable
fun StudyTextBlock(
    indexLetter: String,
    textWithNikud: String,
    textPlain: String,
    preferences: UserPreferences,
    rashi: List<String> = emptyList(),
    modifier: Modifier = Modifier,
    fontFamily: FontFamily = FontStyleOption.fromId(preferences.fontFamily).fontFamily,
    textColor: Color = MaterialTheme.colorScheme.onBackground,
    fontSizeSp: Float? = null,
    testTag: String = ""
) {
    val rawText = if (preferences.showNikud) textWithNikud else textPlain
    val activeFontSizeSp = fontSizeSp ?: preferences.fontSizeSp

    // Normalize text to start cleanly with "$indexLetter. " e.g. "א. ", "ב. ", etc.
    val formattedText = remember(rawText, indexLetter) {
        if (indexLetter.isBlank()) {
            rawText
        } else {
            val regex = Regex("^" + Regex.escape(indexLetter) + "\\s*(\\*\\s*)?")
            if (regex.containsMatchIn(rawText)) {
                rawText.replaceFirst(regex, "$indexLetter. ")
            } else {
                "$indexLetter. $rawText"
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (testTag.isNotBlank()) Modifier.testTag(testTag) else Modifier)
    ) {
        Text(
            text = formattedText,
            fontSize = activeFontSizeSp.sp,
            lineHeight = (activeFontSizeSp * preferences.lineSpacingMultiplier).sp,
            fontFamily = fontFamily,
            color = textColor,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )
        
        if (rashi.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            rashi.forEach { rashiText ->
                Text(
                    text = parseHtmlBold(rashiText),
                    fontSize = (activeFontSizeSp * 0.85f).sp,
                    lineHeight = (activeFontSizeSp * 0.85f * preferences.lineSpacingMultiplier).sp,
                    fontFamily = fontFamily,
                    color = textColor.copy(alpha = 0.85f),
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, bottom = 12.dp)
                )
            }
        }
    }
}

private fun parseHtmlBold(text: String) = buildAnnotatedString {
    var currentIndex = 0
    val startTag = "<b>"
    val endTag = "</b>"
    while (currentIndex < text.length) {
        val nextStart = text.indexOf(startTag, currentIndex)
        if (nextStart == -1) {
            append(text.substring(currentIndex))
            break
        }
        append(text.substring(currentIndex, nextStart))
        val nextEnd = text.indexOf(endTag, nextStart + startTag.length)
        if (nextEnd == -1) break
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
            append(text.substring(nextStart + startTag.length, nextEnd))
        }
        currentIndex = nextEnd + endTag.length
    }
}
