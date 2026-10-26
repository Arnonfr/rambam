package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.em
import com.example.data.local.UserPreferences
import com.example.domain.tanya.OriginalHayomYomStudies
import com.example.ui.theme.FontStyleOption

@Composable
fun OriginalHayomYomHeading(studies: OriginalHayomYomStudies, preferences: UserPreferences, color: Color) {
    val size = preferences.fontSizeSp * .78f
    val font = FontStyleOption.fromId(preferences.fontFamily).fontFamily
    Column(Modifier.fillMaxWidth().padding(vertical = 16.dp).testTag("hayom_original_studies")) {
        HorizontalDivider(color = color.copy(alpha = .55f))
        val date = studies.dateText.split('|').map(String::trim)
        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            date.forEachIndexed { index, part ->
                Text(withStableHebrewMarks(androidx.compose.ui.text.AnnotatedString(part), font), color = color, fontFamily = font, letterSpacing = preferences.letterSpacingEm.em, fontSize = size.sp, fontWeight = FontWeight.Bold,
                    textAlign = when(index) { 0 -> TextAlign.Start; date.lastIndex -> TextAlign.End; else -> TextAlign.Center },
                    modifier = Modifier.weight(if(index == 1) 2f else 1f))
            }
        }
        listOf("חומש" to studies.chumash, "תהלים" to studies.tehillim, "תניא" to studies.tanya).forEach { (label, text) ->
            if (text.isNotBlank()) Text(
                text = withStableHebrewMarks(androidx.compose.ui.text.buildAnnotatedString {
                    pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold))
                    append("$label: ")
                    pop()
                    append(text)
                }, font),
                color = color, fontFamily = font, letterSpacing = preferences.letterSpacingEm.em, fontSize = size.sp,
                lineHeight = (size * preferences.lineSpacingMultiplier).sp,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth().padding(bottom = 5.dp).testTag("hayom_original_$label")
            )
        }

        if(studies.note.isNotBlank()) Text(studies.note, color = color.copy(alpha = .75f), fontSize = 12.sp,
            textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
        HorizontalDivider(color = color.copy(alpha = .55f))
    }
}
