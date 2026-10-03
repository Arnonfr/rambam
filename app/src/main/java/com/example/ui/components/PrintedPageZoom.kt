package com.example.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize

/** Magnifies the laid-out viewport, never changes font sizes, constraints, or source rows. */
@Stable
class PrintedPageZoomState {
    var scale by mutableFloatStateOf(1f)
        private set
    var offset by mutableStateOf(Offset.Zero)
        private set
    var viewport by mutableStateOf(IntSize.Zero)
    val isZoomed: Boolean get() = scale > 1.001f

    fun transform(zoom: Float, pan: Offset, centroid: Offset) {
        if (!zoom.isFinite() || zoom <= 0f) return
        val next = (scale * zoom).coerceIn(1f, 4f)
        if (next <= 1.001f) {
            scale = 1f
            offset = Offset.Zero
            return
        }
        val ratio = next / scale
        val translated = (offset - centroid) * ratio + centroid + pan
        scale = next
        offset = Offset(
            translated.x.coerceIn(-viewport.width * (next - 1f), 0f),
            translated.y.coerceIn(-viewport.height * (next - 1f), 0f))
    }
}

fun Modifier.printedPageZoom(state: PrintedPageZoomState): Modifier = this
    .onSizeChanged { state.viewport = it }
    .pointerInput(state) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            do {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val fingers = event.changes.count { it.pressed && it.previousPressed }
                // At normal size a single finger is reserved for reading/scrolling.
                // Intercept a pinch before LazyColumn can consume its movement.
                if (fingers >= 2 || (state.isZoomed && fingers >= 1)) {
                    state.transform(event.calculateZoom(), event.calculatePan(),
                        event.calculateCentroid(useCurrent = false))
                    event.changes.forEach { it.consume() }
                }
            } while (event.changes.any { it.pressed })
        }
    }

fun Modifier.printedPageZoomLayer(state: PrintedPageZoomState): Modifier = this.graphicsLayer {
    transformOrigin = TransformOrigin(0f, 0f)
    scaleX = state.scale
    scaleY = state.scale
    translationX = state.offset.x
    translationY = state.offset.y
}
