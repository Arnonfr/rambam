package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.data.podcasts.*

@Composable
fun PodcastPanel() {
    val context = LocalContext.current
    val repository = remember { PodcastRepository() }
    var selected by remember { mutableStateOf<PodcastChannel?>(null) }
    var refresh by remember { mutableIntStateOf(0) }
    var episodes by remember { mutableStateOf<List<PodcastEpisode>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    BackHandler(enabled = selected != null) { selected = null }
    LaunchedEffect(selected, refresh) {
        val channel = selected ?: return@LaunchedEffect
        loading = true; error = null; episodes = emptyList()
        try { episodes = repository.episodes(channel) }
        catch (e: kotlinx.coroutines.CancellationException) { throw e }
        catch (_: Exception) { error = "לא ניתן לטעון את הפרקים כרגע. אפשר לפתוח את הערוץ המקורי." }
        finally { loading = false }
    }
    fun open(url: String) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    }
    Column(Modifier.fillMaxWidth().padding(18.dp)) {
        Text("פודקאסטים", style = MaterialTheme.typography.headlineMedium)
        Text("חסידות לחיים · ערוצי המקור", style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 16.dp))
        if (selected == null) {
            PodcastCatalog.channels.forEach { channel ->
                Surface(onClick = { selected = channel }, color = Color(0xFFC5B8F5),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    Column(Modifier.padding(18.dp)) {
                        Text(channel.teacher, style = MaterialTheme.typography.titleMedium)
                        Text("${channel.title} · ${channel.language}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Text("כרגע ההאזנה נפתחת בערוץ המקורי, ללא נגן מובנה באפליקציה.",
                style = MaterialTheme.typography.bodySmall)
        } else {
            TextButton(onClick = { selected = null }) { Text("כל הרבנים") }
            Text(selected!!.teacher, style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = { open(selected!!.website) }) { Text("פתיחה בערוץ המקורי") }
            if (loading) CircularProgressIndicator(Modifier.size(28.dp))
            error?.let { Text(it); TextButton(onClick = { refresh++ }) { Text("נסה שוב") } }
            episodes.forEach { episode ->
                TextButton(onClick = { open(episode.url) }, modifier = Modifier.fillMaxWidth()) {
                    Text(episode.title, modifier = Modifier.fillMaxWidth())
                }
                HorizontalDivider()
            }
            if (!loading && error == null && episodes.isEmpty()) Text("אין פרקים זמינים כרגע")
        }
    }
}
