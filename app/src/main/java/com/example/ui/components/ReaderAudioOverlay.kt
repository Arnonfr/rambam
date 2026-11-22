package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

/** One sheet, no inner card: the player inside draws its own header with this close action. */
@Composable
fun BoxScope.ReaderAudioOverlay(onClose: () -> Unit, content: @Composable () -> Unit) {
    val colors = playerColors(LessonColors.Tanya)
    Surface(modifier = Modifier.align(Alignment.TopCenter).padding(top = 58.dp, start = 8.dp, end = 8.dp)
        .fillMaxWidth().heightIn(max = LocalConfiguration.current.screenHeightDp.dp * .72f).testTag("reader_audio_overlay"),
        shape = RoundedCornerShape(32.dp), shadowElevation = 10.dp, color = colors.sheet,
        border = BorderStroke(1.dp, colors.outline)) {
        CompositionLocalProvider(LocalPlayerClose provides onClose) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 16.dp)) {
                content()
            }
        }
    }
}
