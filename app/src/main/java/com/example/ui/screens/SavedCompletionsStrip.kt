package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import com.example.data.local.SavedStudyCompletion

/** Same hue as the study card, with 18% less light, for every track. */
internal fun completionStripColor(color: Color): Color =
    Color(color.red * .82f, color.green * .82f, color.blue * .82f, color.alpha)

@Composable
fun SavedCompletionsStrip(
    studyColor: Color,
    entries: List<SavedStudyCompletion>,
    onOpen: (SavedStudyCompletion) -> Unit,
    onRemove: (String) -> Unit
) {
    if (entries.isEmpty()) return
    var expanded by remember { mutableStateOf(false) }
    var removeEntry by remember { mutableStateOf<SavedStudyCompletion?>(null) }
    val color = completionStripColor(studyColor)
    Column(Modifier.fillMaxWidth().background(color).testTag("pending_${entries.first().track}")) {
        Row(Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(start = 18.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text("שיעורים להשלים", fontSize = 14.sp, fontWeight = FontWeight.Bold,
                color = Color.Black, textAlign = TextAlign.Start, modifier = Modifier.weight(1f))
            Text("${entries.size}", fontSize = 14.sp, color = Color.Black)
            IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(48.dp)) {
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "סגור שיעורים להשלים" else "פתח שיעורים להשלים",
                    tint = Color.Black, modifier = Modifier.size(28.dp))
            }
        }
        if (expanded) entries.forEach { entry ->
            Row(Modifier.fillMaxWidth().clickable { onOpen(entry) }.padding(start = 18.dp, end = 6.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                    Text(entry.dateLabel.ifBlank { entry.studyDate }, color = Color.Black, fontWeight = FontWeight.Bold,
                        fontSize = 13.sp, textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth())
                    Text(entry.boundaryLabel, color = Color.Black.copy(alpha = .8f), fontSize = 12.sp,
                        textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth())
                    TextButton(onClick = { onOpen(entry) }, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text("המשך השלמה", color = Color.Black, fontSize = 14.sp)
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null,
                            tint = Color.Black, modifier = Modifier.size(24.dp))
                    }
                }
                IconButton(onClick = { removeEntry = entry }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "הסר את ההשלמה מ${entry.dateLabel}", tint = Color.Black,
                        modifier = Modifier.size(26.dp))
                }
            }
        }
    }
    removeEntry?.let { entry ->
        AlertDialog(onDismissRequest = { removeEntry = null }, title = { Text("הסרת השלמה") },
            text = { Text("להסיר את ההשלמה מ${entry.dateLabel.ifBlank { entry.studyDate }}?") },
            confirmButton = { TextButton(onClick = { onRemove(entry.id); removeEntry = null }) { Text("הסר") } },
            dismissButton = { TextButton(onClick = { removeEntry = null }) { Text("ביטול") } })
    }
}
