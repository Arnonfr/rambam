package com.example.data.prayers.audio

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.example.R
import com.example.data.tanya.TanyaAudioClip
import com.example.domain.speech.DailySpeechLesson

internal enum class StudyMedia(val id: String, val title: String, private val artwork: Int) {
    TANYA("today-tanya", "תניא יומי", R.drawable.media_art_tanya),
    RAMBAM("today-rambam", "רמב״ם יומי", R.drawable.media_art_rambam);

    fun metadata(context: Context, title: String, subtitle: String? = null): MediaMetadata.Builder {
        val resources = context.resources
        return MediaMetadata.Builder()
            .setTitle(title).setArtist(subtitle).setIsPlayable(true).setIsBrowsable(false)
            .setArtworkUri(Uri.Builder().scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                .authority(resources.getResourcePackageName(artwork))
                .appendPath(resources.getResourceTypeName(artwork))
                .appendPath(resources.getResourceEntryName(artwork)).build())
    }

    fun browseItem(context: Context, subtitle: String?): MediaItem =
        MediaItem.Builder().setMediaId(id)
            .setMediaMetadata(metadata(context, title, subtitle).setSubtitle(subtitle).build()).build()
}

internal fun TanyaAudioClip.mediaItem(context: Context): MediaItem = MediaItem.Builder().setMediaId(id).setUri(url)
    .setMediaMetadata(StudyMedia.TANYA.metadata(context, title, "תניא · $teacher")
        .setExtras(Bundle().apply { putString("usage_key", id); putString("usage_type", "tanya") }).build())
    .build()

internal fun DailySpeechLesson.recordingItem(context: Context, index: Int, fromCar: Boolean = false): MediaItem {
    val part = parts[index]
    val speaker = if (source == "peretz") "ר׳ אליהו פרץ" else "הרב נדב כהן"
    return MediaItem.Builder().setMediaId("daily-speech:$id:$index").setUri(part.audioUrl)
        .setTag(if (fromCar) this else null)
        .setMediaMetadata(StudyMedia.RAMBAM.metadata(context, part.chapterTitle, "רמב״ם · $speaker").build())
        .build()
}
