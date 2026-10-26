package com.example.data.podcasts

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import android.util.LruCache
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.TimeUnit

/** Only publisher artwork from our curated catalogue. Lazy, bounded, cached and downsampled. */
object PodcastArtwork {
    private val memory = LruCache<String, ImageBitmap>(5)
    private val lock = Mutex()
    private val client = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS).build()
    suspend fun load(context: Context, channel: PodcastChannel): ImageBitmap? = withContext(Dispatchers.IO) { lock.withLock {
        require(PodcastCatalog.channels.any { it.id == channel.id && it.artwork == channel.artwork })
        memory.get(channel.artwork)?.let { return@withLock it }
        val directory = File(context.cacheDir, "podcast-covers").apply { mkdirs() }
        val cached = File(directory, "${channel.id}.image")
        val bytes = if (cached.exists() && System.currentTimeMillis() - cached.lastModified() < 7 * 86400_000L) {
            cached.readBytes()
        } else client.newCall(Request.Builder().url(channel.artwork).build()).execute().use { response ->
            check(response.isSuccessful)
            val stream = response.body?.byteStream() ?: error("No artwork")
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                val count = stream.read(buffer)
                if (count < 0) break
                check(output.size() + count <= 5_000_000)
                output.write(buffer, 0, count)
            }
            output.toByteArray()
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth !in 1..20_000 || bounds.outHeight !in 1..20_000) {
            cached.delete()
            error("Invalid artwork dimensions")
        }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 768) sample *= 2
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size,
            BitmapFactory.Options().apply { inSampleSize = sample }) ?: error("Invalid artwork")
        if (!cached.exists() || System.currentTimeMillis() - cached.lastModified() >= 7 * 86400_000L)
            cached.writeBytes(bytes)
        bitmap.asImageBitmap().also { memory.put(channel.artwork, it) }
    } }
}
