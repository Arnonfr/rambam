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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.em
import com.example.data.local.UserPreferences
import com.example.ui.theme.FontStyleOption
import com.example.ui.theme.NotoSansHebrewFontFamily
import com.example.ui.theme.TanyaPrintFontFamily
import com.example.ui.theme.FrankRuhlFontFamily
import com.example.ui.util.HebrewTextNormalizer

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
    isReadingAnchor: Boolean = false,
    onTextLayout: (androidx.compose.ui.text.TextLayoutResult) -> Unit = {},
    onTextTop: (Float) -> Unit = {},
    testTag: String = "",
    textIncludesIndex: Boolean = false
) {
    val sourceText = if (preferences.showNikud) textWithNikud else textPlain
    val rawText = remember(sourceText) { HebrewTextNormalizer.forDisplay(sourceText) }
    val activeFontSizeSp = fontSizeSp ?: preferences.fontSizeSp
    // Keep each Hebrew word and its marks in one font; individual glyph fallback
    // separates vowel anchors from the letters they belong to.
    val displayFontFamily = fontFamily

    // Only strip an index when the source explicitly includes it (Rambam).
    // A verse can itself begin with the same letters, e.g. ה׳ in Psalm 11:5.
    val formattedText = remember(rawText, indexLetter, textIncludesIndex) {
        if (indexLetter.isBlank()) {
            rawText
        } else {
            val regex = Regex("^" + Regex.escape(indexLetter) + "(?:\\s*\\*\\s*|\\.\\s*|\\s+)")
            if (textIncludesIndex && regex.containsMatchIn(rawText)) {
                rawText.replaceFirst(regex, "$indexLetter. ")
            } else {
                "$indexLetter. $rawText"
            }
        }
    }
    val markedText = remember(formattedText, fontFamily, activeFontSizeSp) {
        withStableHebrewMarks(withSmallReadingNotes(buildAnnotatedString {
            append(formattedText)
            if (indexLetter.isNotBlank()) addStyle(SpanStyle(fontWeight = FontWeight.Bold), 0, (indexLetter.length + 1).coerceAtMost(length))
        }, (activeFontSizeSp * 0.65f).sp), fontFamily)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (testTag.isNotBlank()) Modifier.testTag(testTag) else Modifier)
    ) {
        if (isReadingAnchor) {
            Text(
                text = "כאן עצרת בפעם הקודמת",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2799CC),
                modifier = Modifier.padding(bottom = 4.dp)
            )
            androidx.compose.material3.HorizontalDivider(
                modifier = Modifier.fillMaxWidth().padding(bottom = 7.dp),
                thickness = 3.dp,
                color = Color(0xFF69C8F2)
            )
        }
        Text(
            text = markedText,
            onTextLayout = onTextLayout,
            fontSize = activeFontSizeSp.sp,
            lineHeight = (activeFontSizeSp * preferences.lineSpacingMultiplier).sp,
            fontFamily = displayFontFamily,
            letterSpacing = preferences.letterSpacingEm.em,
            color = textColor,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth().onGloballyPositioned { onTextTop(it.positionInParent().y) }
        )
        
        if (rashi.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            rashi.forEach { rashiText ->
                Text(
                    text = withStableHebrewMarks(withSmallReadingNotes(parseHtmlBold(rashiText),
                        (activeFontSizeSp * 0.65f).sp), fontFamily),
                    fontSize = (activeFontSizeSp * 0.85f).sp,
                    lineHeight = (activeFontSizeSp * 0.85f * preferences.lineSpacingMultiplier).sp,
                    fontFamily = displayFontFamily,
            letterSpacing = preferences.letterSpacingEm.em,
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

/** Keep punctuation in the selected face, together with its size and weight.
 * Hebrew abbreviation marks use the font's Hebrew glyph, never a foreign-font span. */
internal fun withStableHebrewMarks(
    text: androidx.compose.ui.text.AnnotatedString,
    fontFamily: FontFamily
): androidx.compose.ui.text.AnnotatedString {
    val normalized = text.text.toCharArray()
    text.text.forEachIndexed { index, char ->
        if (char in "\"“”״" && index > 0 && index < text.length - 1) {
            var previous = index - 1
            while (previous >= 0 && text.text[previous] in '\u0591'..'\u05C7') previous--
            if (previous >= 0 && text.text[previous] in 'א'..'ת' && text.text[index + 1] in 'א'..'ת')
                normalized[index] = '\u05F4'
        }
    }
    return androidx.compose.ui.text.AnnotatedString(String(normalized), text.spanStyles, text.paragraphStyles)
}
