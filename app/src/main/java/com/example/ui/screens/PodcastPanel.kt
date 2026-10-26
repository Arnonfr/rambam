package com.example.ui.screens

import android.content.Intent
import android.media.MediaPlayer
import android.media.AudioAttributes
import android.media.AudioManager
import android.content.Context
import android.content.BroadcastReceiver
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.podcasts.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

private val PodcastPaper = Color(0xFFFFFCF6)
private val PodcastInk = Color(0xFF111111)
private val PodcastMuted = Color(0xFF666666)
private val PodcastCyan = Color(0xFF9FDEEF)

@Suppress("DEPRECATION")
@Composable
fun PodcastPanel(modifier: Modifier = Modifier, onOpenSettings: () -> Unit = {}) {
    val context = LocalContext.current
    val repository = remember { PodcastRepository() }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    val selected = PodcastCatalog.channels.find { it.id == selectedId }
    var refresh by remember { mutableIntStateOf(0) }
    var episodes by remember { mutableStateOf<List<PodcastEpisode>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    BackHandler(enabled = selected != null) { selectedId = null }
    LaunchedEffect(selectedId, refresh) {
        val channel = selected ?: return@LaunchedEffect
        loading = true; error = null; episodes = emptyList()
        try { episodes = repository.episodes(channel); loading = false }
        catch (e: CancellationException) { throw e }
        catch (_: Exception) { error = "לא ניתן לטעון את הפרקים כרגע"; loading = false }
    }
    fun open(url: String) {
        if (runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }.isFailure)
            Toast.makeText(context, "לא נמצאה אפליקציה לפתיחת הקישור", Toast.LENGTH_SHORT).show()
    }
    val player = remember { MediaPlayer().apply {
        setAudioAttributes(AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .setUsage(AudioAttributes.USAGE_MEDIA).build())
    } }
    var active by remember { mutableStateOf<PodcastEpisode?>(null) }
    var playing by remember { mutableStateOf(false) }
    var preparing by remember { mutableStateOf(false) }
    var ready by remember { mutableStateOf(false) }
    var position by remember { mutableIntStateOf(0) }
    var duration by remember { mutableIntStateOf(0) }
    val owner = LocalLifecycleOwner.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val focusListener = remember(player) { AudioManager.OnAudioFocusChangeListener { focus ->
        if (focus < 0 && ready && playing) { player.pause(); playing = false }
    } }
    fun startPlayback() {
        if (!owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) return
        val granted = audioManager.requestAudioFocus(focusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        if (granted == AudioManager.AUDIOFOCUS_REQUEST_GRANTED && owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            player.start(); playing = true
        }
    }
    DisposableEffect(player, owner) {
        player.setOnPreparedListener { ready = true; preparing = false; duration = it.duration; startPlayback() }
        player.setOnCompletionListener { playing = false; position = duration; audioManager.abandonAudioFocus(focusListener) }
        player.setOnErrorListener { _, _, _ ->
            preparing = false; playing = false; ready = false; active = null
            audioManager.abandonAudioFocus(focusListener)
            Toast.makeText(context, "ההקלטה לא זמינה כרגע. נסו לפתוח במקור", Toast.LENGTH_LONG).show()
            true
        }
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                if (ready && playing) player.pause()
                playing = false
                audioManager.abandonAudioFocus(focusListener)
            }
        }
        val headphones = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (ready && playing) player.pause()
                playing = false
                audioManager.abandonAudioFocus(focusListener)
            }
        }
        ContextCompat.registerReceiver(context, headphones, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY), ContextCompat.RECEIVER_NOT_EXPORTED)
        owner.lifecycle.addObserver(observer)
        onDispose { context.unregisterReceiver(headphones); owner.lifecycle.removeObserver(observer); audioManager.abandonAudioFocus(focusListener); player.release() }
    }
    LaunchedEffect(playing) {
        while (playing) { position = player.currentPosition; delay(500) }
    }
    fun play(episode: PodcastEpisode) {
        val audio = episode.audio
        if (audio == null) { open(episode.url); return }
        if (active == episode && ready) {
            if (playing) { player.pause(); playing = false; audioManager.abandonAudioFocus(focusListener) }
            else startPlayback()
            return
        }
        runCatching {
            player.reset(); active = episode; playing = false; preparing = true; ready = false
            position = 0; duration = 0
            player.setAudioAttributes(AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .setUsage(AudioAttributes.USAGE_MEDIA).build())
            player.setDataSource(audio); player.prepareAsync()
        }.onFailure {
            active = null; preparing = false; ready = false
            Toast.makeText(context, "לא ניתן להפעיל את ההקלטה", Toast.LENGTH_SHORT).show()
        }
    }
    Column(modifier.background(PodcastPaper).padding(bottom = 100.dp)) {
        if (selected == null) PodcastLibrary(onOpen = { selectedId = it.id }, modifier = Modifier.weight(1f), onSettings = onOpenSettings)
        else PodcastSeriesDetail(selected, episodes, loading, error, onBack = { selectedId = null },
            onRetry = { refresh++ }, onSource = { open(selected.website) }, onPlay = ::play,
            activeUrl = active?.url, playing = playing, modifier = Modifier.weight(1f))
        active?.let { episode ->
            Surface(color = PodcastCyan, shape = RoundedCornerShape(20.dp),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp).testTag("podcast_player")) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(episode.title, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                fontWeight = FontWeight.Bold, color = PodcastInk)
                            Text(if (preparing) "טוען הקלטה…" else "${audioTime(position)} / ${audioTime(duration)}",
                                fontSize = 12.sp, color = PodcastInk)
                        }
                        IconButton(onClick = { play(episode) }, enabled = ready) {
                            if (preparing) CircularProgressIndicator(Modifier.size(20.dp), color = PodcastInk)
                            else Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                                if (playing) "השהיה" else "הפעלה", tint = PodcastInk)
                        }
                        IconButton(onClick = { player.reset(); audioManager.abandonAudioFocus(focusListener); active = null; ready = false; playing = false; preparing = false }) {
                            Icon(Icons.Default.Close, "סגירת הנגן", tint = PodcastInk)
                        }
                    }
                    if (ready && duration > 0) Slider(value = position.toFloat().coerceIn(0f, duration.toFloat()),
                        onValueChange = { position = it.toInt(); player.seekTo(position) },
                        valueRange = 0f..duration.toFloat(), modifier = Modifier.height(24.dp),
                        colors = SliderDefaults.colors(thumbColor = PodcastInk, activeTrackColor = PodcastInk))
                }
            }
        }
    }
}

private fun audioTime(milliseconds: Int): String {
    val seconds = milliseconds.coerceAtLeast(0) / 1000
    return "%d:%02d".format(seconds / 60, seconds % 60)
}

@Composable
fun PodcastCover(channel: PodcastChannel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var failed by remember(channel.id) { mutableStateOf(false) }
    var retry by remember(channel.id) { mutableIntStateOf(0) }
    val bitmap by produceState<ImageBitmap?>(null, channel.artwork, retry) {
        failed = false
        try { value = PodcastArtwork.load(context, channel) }
        catch (e: CancellationException) { throw e }
        catch (_: Exception) { failed = true }
    }
    Box(modifier.clip(RoundedCornerShape(12.dp)).background(Color(0xFFF0EEEA)), contentAlignment = Alignment.Center) {
        val loaded = bitmap
        if (loaded != null) Image(loaded, contentDescription = "עטיפת ${channel.title}",
            modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        else if (failed) TextButton(onClick = { retry++ }) { Text("טעינה מחדש", color = PodcastMuted, fontSize = 12.sp) }
        else CircularProgressIndicator(Modifier.size(24.dp), color = PodcastMuted, strokeWidth = 2.dp)
    }
}

@Composable
fun PodcastLibrary(onOpen: (PodcastChannel) -> Unit, modifier: Modifier = Modifier,
    onSettings: (() -> Unit)? = null,
    cover: @Composable (PodcastChannel, Modifier) -> Unit = { channel, m -> PodcastCover(channel, m) }) {
    var query by rememberSaveable { mutableStateOf("") }
    val matches = PodcastCatalog.channels.filter {
        query.isBlank() || "${it.title} ${it.teacher} ${it.language}".contains(query.trim(), ignoreCase = true)
    }
    LazyColumn(modifier.fillMaxSize().testTag("podcast_library"), contentPadding = PaddingValues(vertical = 20.dp)) {
        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("פודקאסטים", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = PodcastInk, modifier = Modifier.weight(1f))
                    if (onSettings != null) IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, "הגדרות", tint = PodcastInk) }
                }
                Text("שיעורים להאזנה", color = PodcastMuted, modifier = Modifier.padding(top = 4.dp, bottom = 20.dp))
                OutlinedTextField(query, onValueChange = { query = it }, singleLine = true,
                    placeholder = { Text("חיפוש סדרה או רב", color = PodcastMuted) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = PodcastMuted) },
                    shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth().testTag("podcast_search"),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PodcastCyan,
                        unfocusedBorderColor = Color.Transparent, focusedContainerColor = Color(0xFFF2F1EE),
                        unfocusedContainerColor = Color(0xFFF2F1EE), focusedTextColor = PodcastInk, unfocusedTextColor = PodcastInk))
            }
        }
        if (query.isNotBlank()) {
            item { PodcastRail("תוצאות חיפוש", matches, 220, onOpen, cover) }
            if (matches.isEmpty()) item { Text("לא נמצאו סדרות מתאימות", color = PodcastMuted, modifier = Modifier.padding(20.dp)) }
        } else {
            item { PodcastRail("סדרות להאזנה", listOf(PodcastCatalog.channels[0], PodcastCatalog.channels[2]), 236, onOpen, cover) }
            item { PodcastRail("עוד סדרות", listOf(PodcastCatalog.channels[4], PodcastCatalog.channels[1], PodcastCatalog.channels[3]), 176, onOpen, cover) }
        }
    }
}

@Composable
private fun PodcastRail(title: String, channels: List<PodcastChannel>, width: Int, onOpen: (PodcastChannel) -> Unit,
    cover: @Composable (PodcastChannel, Modifier) -> Unit) {
    Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PodcastInk,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 26.dp, bottom = 14.dp))
    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        items(channels, key = { it.id }) { channel ->
            Surface(onClick = { onOpen(channel) }, color = Color.Transparent,
                modifier = Modifier.width(width.dp).testTag("podcast_series_${channel.id}")) {
                Column {
                    cover(channel, Modifier.fillMaxWidth().aspectRatio(1f))
                    Text(channel.title, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = PodcastInk,
                        maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
                    Text(channel.teacher, color = PodcastMuted, fontSize = 13.sp, maxLines = 2)
                    if (channel.language != "עברית") Text(channel.language, color = PodcastMuted, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun PodcastSeriesDetail(channel: PodcastChannel, episodes: List<PodcastEpisode>, loading: Boolean, error: String?,
    onBack: () -> Unit, onRetry: () -> Unit, onSource: () -> Unit, onPlay: (PodcastEpisode) -> Unit,
    activeUrl: String? = null, playing: Boolean = false, modifier: Modifier = Modifier,
    cover: @Composable (PodcastChannel, Modifier) -> Unit = { channel, m -> PodcastCover(channel, m) }) {
    Column(modifier.fillMaxSize().testTag("podcast_detail")) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("podcast_back")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "חזרה לסדרות", tint = PodcastInk)
            }
            Text(channel.title, fontWeight = FontWeight.Bold, color = PodcastInk, maxLines = 1,
                overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            IconButton(onClick = onSource) { Icon(Icons.Default.OpenInNew, "פתיחה במקור", tint = PodcastMuted) }
        }
        LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)) {
            item {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    cover(channel, Modifier.width(220.dp).aspectRatio(1f))
                    Text(channel.title, fontSize = 25.sp, fontWeight = FontWeight.Bold, color = PodcastInk,
                        modifier = Modifier.padding(top = 16.dp))
                    Text(channel.teacher, color = PodcastMuted, modifier = Modifier.padding(top = 4.dp, bottom = 24.dp))
                }
                Text("פרקים", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PodcastInk, modifier = Modifier.padding(bottom = 12.dp))
            }
            if (loading) item { Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = PodcastInk) } }
            if (error != null) item {
                Text(error, color = PodcastMuted)
                Row { TextButton(onClick = onRetry) { Text("נסה שוב") }; TextButton(onClick = onSource) { Text("פתיחה במקור") } }
            }
            if (!loading && error == null && episodes.isEmpty()) item { Text("אין פרקים זמינים כרגע", color = PodcastMuted) }
            items(episodes, key = { it.audio ?: it.url }) { episode ->
                Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    cover(channel, Modifier.size(56.dp))
                    Column(Modifier.weight(1f)) {
                        Text(episode.title, fontWeight = FontWeight.SemiBold, color = PodcastInk, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        val durationLabel = podcastDurationLabel(episode.duration)
                        Text(if (durationLabel.isBlank()) "שיעור להאזנה" else "משך ההקלטה · $durationLabel",
                            color = PodcastMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                    FilledIconButton(onClick = { onPlay(episode) }, shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = PodcastInk, contentColor = Color.White)) {
                        // Media transport is intentionally not mirrored in RTL.
                        Icon(if (activeUrl == episode.url && playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                            if (activeUrl == episode.url && playing) "השהיה" else "הפעלת ${episode.title}")
                    }
                }
                HorizontalDivider(color = Color(0xFFE8E5DE))
            }
        }
    }
}
