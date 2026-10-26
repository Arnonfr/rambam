package com.example.ui.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.local.ContentReadingAnchor
import com.example.data.local.ContentReadingAnchorManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.delay

/** Preserve the entire viewport offset, including lines deep inside a long paragraph.
 * Stable item keys survive inserted/removed headers. Date scoping prevents cross-day jumps.
 * No fraction, fixed pixel divisor or rounding to the beginning of a halacha is used.
 */
@Composable
fun rememberReaderViewportState(
    contentId: String,
    assignmentDate: String,
    rowKeys: List<String>,
    fallbackIndex: Int = 0,
    fallbackOffset: Int = 0
): LazyListState {
    val context = LocalContext.current
    val manager = remember(context) { ContentReadingAnchorManager(context) }
    val ready = rowKeys.isNotEmpty()
    val state = remember(contentId, assignmentDate, ready) {
        val anchor = manager.get("viewport_$contentId", assignmentDate)
        val index = anchor?.let { rowKeys.indexOf(it.blockId) } ?: -1
        LazyListState(
            firstVisibleItemIndex = if (index >= 0) index else fallbackIndex.coerceIn(0, rowKeys.lastIndex.coerceAtLeast(0)),
            firstVisibleItemScrollOffset = if (index >= 0) anchor!!.textOffset.coerceAtLeast(0) else fallbackOffset.coerceAtLeast(0)
        )
    }
    fun save() {
        if (!ready || state.layoutInfo.visibleItemsInfo.isEmpty()) return
        val index = state.firstVisibleItemIndex
        val blockId = rowKeys.getOrNull(index) ?: return
        if (state.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }?.key?.toString() != blockId) return
        manager.save(ContentReadingAnchor("viewport_$contentId", assignmentDate, "viewport",
            blockId, index, textOffset = state.firstVisibleItemScrollOffset))
    }
    LaunchedEffect(state, contentId, assignmentDate, ready, rowKeys) {
        if (!ready) return@LaunchedEffect
        snapshotFlow { state.firstVisibleItemIndex to state.firstVisibleItemScrollOffset }
            .collectLatest { delay(200); save() }
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(state, lifecycle, contentId, assignmentDate, ready, rowKeys) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) save()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer); save() }
    }
    return state
}
