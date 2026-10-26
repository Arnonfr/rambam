package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

@Composable
fun BoxScope.ReaderAudioOverlay(onClose: () -> Unit, content: @Composable () -> Unit) {
    Surface(modifier = Modifier.align(Alignment.TopCenter).padding(top = 58.dp, start = 10.dp, end = 10.dp)
        .fillMaxWidth().heightIn(max = LocalConfiguration.current.screenHeightDp.dp * .68f).testTag("reader_audio_overlay"),
        shape = RoundedCornerShape(20.dp), shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(8.dp)) {
            IconButton(onClick = onClose, modifier = Modifier.align(Alignment.End)) {
                Icon(Icons.Default.Close, contentDescription = "סגירת חלונית השמע")
            }
            content()
        }
    }
}
