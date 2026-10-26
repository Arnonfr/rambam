package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.SavedStudyCompletion
import com.example.domain.content.CompletionBoundary
import com.example.domain.content.CompletionReaderRow
import kotlinx.coroutines.launch

private data class CompletionDraft(
    val markerIndex: Int, val markerY: Int, val startIndex: Int,
    val plainOffset: Int, val label: String
)

@Stable
class ManualStudyCompletionState {
    internal val textLayouts = mutableMapOf<Int, TextLayoutResult>()
    internal val textTops = mutableMapOf<Int, Float>()
    fun measure(index: Int, layout: TextLayoutResult) { textLayouts[index] = layout }
    fun textTop(index: Int, top: Float) { textTops[index] = top }
}

@Composable
fun rememberManualStudyCompletionState(key: Any?): ManualStudyCompletionState =
    remember(key) { ManualStudyCompletionState() }

/** Nothing is persisted on scroll. The menu opens a preview; only Save persists a remainder. */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun BoxScope.ManualStudyCompletionControls(
    state: ManualStudyCompletionState,
    listState: LazyListState,
    rows: List<CompletionReaderRow>,
    track: String,
    studyDate: String,
    dateLabel: String,
    title: String,
    cutAtMiddle: Boolean = false,
    enabled: Boolean = true,
    onSave: (SavedStudyCompletion) -> Unit
) {
    var menuOpen by remember(studyDate, track) { mutableStateOf(false) }
    var draft by remember(studyDate, track, rows) { mutableStateOf<CompletionDraft?>(null) }
    var message by remember(studyDate, track) { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val accent = Color(0xFF6D52A0)

    fun preview() {
        menuOpen = false
        scope.launch {
            val info = listState.layoutInfo
            val middle = (info.viewportStartOffset + info.viewportEndOffset) / 2
            val visible = info.visibleItemsInfo.filter { item -> rows.any { it.listIndex == item.index } }
            val current = visible.firstOrNull { it.offset <= middle && it.offset + it.size > middle }
                ?: visible.minByOrNull { kotlin.math.abs(it.offset - middle) }
                ?: return@launch
            val row = rows.first { it.listIndex == current.index }
            val next = rows.firstOrNull { it.listIndex > current.index }
            val layout = state.textLayouts[current.index]
            val textTop = state.textTops[current.index] ?: 0f
            val localMiddle = (middle - current.offset - textTop).coerceAtLeast(0f)
            val line = if (cutAtMiddle && layout != null && layout.lineCount > 0)
                layout.getLineForVerticalPosition(localMiddle) else null
            val renderedOffset = line?.let { layout!!.getLineEnd(it, visibleEnd = false) }
            val offset = renderedOffset?.let { CompletionBoundary.plain(layout!!.layoutInput.text.text.take(it)).length } ?: 0
            val keepsCurrent = renderedOffset != null && renderedOffset < layout!!.layoutInput.text.length
            val startIndex = if (keepsCurrent) current.index else next?.listIndex ?: (rows.lastOrNull()?.listIndex ?: 0) + 1
            val markerY = if (line != null) (textTop + layout!!.getLineBottom(line)).toInt() else current.size
            val label = if (cutAtMiddle) "מהנקודה המסומנת בטקסט" else "אחרי ${row.block.label}"
            val remaining = CompletionBoundary.remainder(rows, startIndex, if (keepsCurrent) offset else 0)
            if (remaining.isEmpty()) {
                message = "אין טקסט נוסף אחרי הנקודה הזאת לשמירה"
                return@launch
            }
            draft = CompletionDraft(current.index, markerY, startIndex, if (keepsCurrent) offset else 0, label)
            // A long halacha may end below the screen: bring its actual end into view before confirmation.
            if (current.offset + markerY > info.viewportEndOffset - 150 || current.offset + markerY < 60) {
                listState.scrollToItem(current.index, (markerY - middle).coerceAtLeast(0))
            }
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
    Box(Modifier.align(Alignment.TopEnd).size(52.dp)) {
        IconButton(onClick = { menuOpen = true }, enabled = enabled && rows.isNotEmpty(),
            modifier = Modifier.fillMaxSize().testTag("manual_completion_menu")) {
            Icon(Icons.Default.MoreVert, contentDescription = "אפשרויות שיעור", modifier = Modifier.size(26.dp))
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(text = { Text("שמור להשלמה בהמשך", textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()) }, onClick = ::preview,
                modifier = Modifier.testTag("save_for_later"))
        }
    }

    draft?.let { selected ->
        val item = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == selected.markerIndex }
        item?.let {
            val y = it.offset + selected.markerY
            if (y in 54..listState.layoutInfo.viewportEndOffset) {
                Column(Modifier.align(Alignment.TopStart).offset { IntOffset(0, y) }.fillMaxWidth()
                    .testTag("completion_boundary_line")) {
                    HorizontalDivider(thickness = 3.dp, color = accent)
                    Text("מכאן יישמר להשלמה", fontSize = 12.sp, color = Color.White,
                        modifier = Modifier.background(accent).padding(horizontal = 10.dp, vertical = 4.dp))
                }
            }
        }
        ModalBottomSheet(onDismissRequest = { draft = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 20.dp)
                .testTag("completion_boundary_preview"), horizontalAlignment = Alignment.Start) {
                Text("שמירה להשלמה", style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth())
                Text(selected.label, fontSize = 14.sp, textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = {
                        val blocks = CompletionBoundary.remainder(rows, selected.startIndex, selected.plainOffset)
                        if (blocks.isNotEmpty()) onSave(SavedStudyCompletion(track, studyDate, dateLabel, title, selected.label, blocks))
                        draft = null
                    }, modifier = Modifier.weight(1f).testTag("confirm_save_completion")) { Text("שמור מכאן") }
                    OutlinedButton(onClick = { draft = null }, modifier = Modifier.weight(1f)) { Text("ביטול") }
                }
            }
        }
    }
    message?.let { text ->
        AlertDialog(onDismissRequest = { message = null }, text = { Text(text) },
            confirmButton = { TextButton(onClick = { message = null }) { Text("סגור") } })
    }
    }
}
