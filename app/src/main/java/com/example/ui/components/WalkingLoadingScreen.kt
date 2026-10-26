package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import com.example.R

@Composable
fun WalkingLoadingScreen() {
    val transition = rememberInfiniteTransition(label = "walking")
    val step by transition.animateFloat(-1f, 1f, infiniteRepeatable(tween(480, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "step")
    val logo = ImageBitmap.imageResource(R.drawable.app_icon_square)
    // Match the artwork’s slightly warm white to the loading screen.
    val whiteBackground = ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
        1.025f, 0f, 0f, 0f, 0f,
        0f, 1.025f, 0f, 0f, 0f,
        0f, 0f, 1.025f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    )))
    Box(Modifier.fillMaxSize().background(Color.White), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(180.dp)) {
            val split = size.height * .56f
            fun picture() = drawImage(logo, dstOffset = IntOffset.Zero, dstSize = IntSize(size.width.toInt(), size.height.toInt()), colorFilter = whiteBackground)
            clipRect(bottom = split) { picture() }
            clipRect(top = split, right = size.width * .54f) {
                rotate(step * 3f, androidx.compose.ui.geometry.Offset(size.width * .5f, split)) { picture() }
            }
            clipRect(top = split, left = size.width * .54f) {
                rotate(-step * 3f, androidx.compose.ui.geometry.Offset(size.width * .54f, split)) { picture() }
            }
        }
    }
}
