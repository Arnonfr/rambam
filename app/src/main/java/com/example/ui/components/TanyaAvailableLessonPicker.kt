package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.tanya.TanyaAvailableLesson
import com.example.domain.tanya.DailyTanyaLesson

@Composable
internal fun TanyaAvailableLessonPicker(
    records: List<TanyaAvailableLesson>, selectedId: String?, lesson: DailyTanyaLesson,
    ink: Color, accent: Color, onSelect: (TanyaAvailableLesson) -> Unit
) {
    Text("השיעורים הזמינים · ${records.size}", color = ink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(6.dp))
    LazyColumn(Modifier.fillMaxWidth().heightIn(max = 224.dp).testTag("tanya_available_lessons"),
        verticalArrangement = Arrangement.spacedBy(5.dp), contentPadding = PaddingValues(bottom = 8.dp)) {
        items(records.sortedBy { if (it.kind == "audio") 0 else 1 }, key = { it.id }) { record ->
            val selected = record.id == selectedId
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .background(if (selected) accent.copy(alpha = .3f) else ink.copy(alpha = .04f))
                .selectable(selected, role = Role.RadioButton, onClick = { onSelect(record) })
                .padding(horizontal = 12.dp, vertical = 10.dp).testTag("available_${record.id}")) {
                Text(record.title, color = ink, fontSize = 13.sp, lineHeight = 18.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                Text(if (record.matches(lesson)) "לפרק הנוכחי · ${if (record.kind == "youtube") "וידאו" else "שמע"}"
                    else if (record.kind == "audio") record.subtitle + " · האזנה ברקע"
                    else "וידאו באתר המקור · נפתח מחוץ לאפליקציה",
                    color = ink.copy(alpha = .7f), fontSize = 11.sp, lineHeight = 16.sp)
            }
        }
    }
}
