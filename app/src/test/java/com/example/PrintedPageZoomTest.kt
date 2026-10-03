package com.example

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import com.example.ui.components.PrintedPageZoomState
import org.junit.Assert.*
import org.junit.Test

class PrintedPageZoomTest {
    @Test fun `pinch magnifies around the fingers without altering source layout`() {
        val state = PrintedPageZoomState().apply { viewport = IntSize(400, 800) }
        state.transform(2f, Offset.Zero, Offset(200f, 400f))
        assertEquals(2f, state.scale, .001f)
        assertEquals(Offset(-200f, -400f), state.offset)
        assertTrue(state.isZoomed)
    }
    @Test fun `pan stays inside enlarged page and zoom out returns to reading`() {
        val state = PrintedPageZoomState().apply { viewport = IntSize(400, 800) }
        state.transform(2f, Offset.Zero, Offset(200f, 400f))
        state.transform(1f, Offset(-1000f, 1000f), Offset.Zero)
        assertEquals(Offset(-400f, 0f), state.offset)
        state.transform(.1f, Offset.Zero, Offset(200f, 400f))
        assertEquals(1f, state.scale, .001f)
        assertEquals(Offset.Zero, state.offset)
        assertFalse(state.isZoomed)
    }
    @Test fun `zoom has safe upper limit and ignores invalid scale`() {
        val state = PrintedPageZoomState().apply { viewport = IntSize(400, 800) }
        state.transform(100f, Offset.Zero, Offset(200f, 400f))
        assertEquals(4f, state.scale, .001f)
        state.transform(Float.NaN, Offset.Zero, Offset.Zero)
        assertEquals(4f, state.scale, .001f)
    }
}
