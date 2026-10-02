package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import com.example.domain.tanya.TanyaSourceLine

/** Page boundaries are visual paper edges, never additional source text lines.
 * Keeping one lazy row per source line preserves line-specific reading anchors.
 */
@Composable
fun TanyaPageLine(
    line: TanyaSourceLine, textColor: Color, pageColor: Color,
    first: Boolean, last: Boolean, showAnchor: Boolean
) {
    Column(Modifier.fillMaxWidth().background(pageColor)) {
        if (first) {
            HorizontalDivider(color = textColor.copy(alpha = .1f), thickness = 1.dp)
            Box(Modifier.fillMaxWidth().height(26.dp).padding(horizontal = 8.dp)
                .testTag("tanya_page_start_${line.page}")) {
                Text("אגרת הקדש", color = textColor.copy(alpha = .8f), fontSize = 11.sp,
                    modifier = Modifier.align(Alignment.Center))
                Text(line.page.toString(), color = textColor.copy(alpha = .5f), fontSize = 9.sp,
                    modifier = Modifier.align(if (line.page % 2 == 0) Alignment.CenterEnd else Alignment.CenterStart))
            }
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
            if (showAnchor) HorizontalDivider(color = Color(0xFF73D4ED), thickness = 2.dp)
            TanyaPrintedLine(line, textColor)
        }
        if (last) {
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = textColor.copy(alpha = .18f), thickness = 1.dp)
            Box(Modifier.fillMaxWidth().height(10.dp).background(textColor.copy(alpha = .07f))
                .testTag("tanya_page_end_${line.page}"))
        }
    }
}
