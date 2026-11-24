package com.example.data.prayers.audio

import android.content.Context
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaConstants
import androidx.media3.session.MediaLibraryService.LibraryParams
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionError
import com.example.data.analytics.AudioAnalytics
import com.example.data.local.UserPreferences
import com.example.data.local.UserPreferencesRepository
import com.example.data.speech.RambamRecordingRepository
import com.example.data.speech.SpeechLessonStore
import com.example.data.tanya.TanyaAudioRepository
import com.example.data.tanya.TanyaRepository
import com.example.widget.WidgetContentLoader
import com.example.widget.WidgetStudyDay
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant

@OptIn(UnstableApi::class)
internal open class StudyCarLibrary(
    private val context: Context,
    private val scope: CoroutineScope,
    private val speechStore: SpeechLessonStore
) : MediaLibrarySession.Callback {
    private val preferences = UserPreferencesRepository(context)
    private val tanya by lazy { TanyaRepository(context) }
    private val rambam by lazy { RambamRecordingRepository(context) }
    private val tanyaPositions = context.getSharedPreferences("tanya_audio_positions", Context.MODE_PRIVATE)
    private var playRequest: Job? = null
    private val root = MediaItem.Builder().setMediaId(ROOT).setMediaMetadata(MediaMetadata.Builder()
        .setTitle("שיעורי היום").setIsBrowsable(true).setIsPlayable(false)
        .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED).build()).build()

    override fun onGetLibraryRoot(session: MediaLibrarySession, browser: MediaSession.ControllerInfo, params: LibraryParams?) =
        Futures.immediateFuture(if (mayBrowse(session, browser)) {
            val styles = LibraryParams.Builder().setExtras(Bundle().apply {
                putInt(MediaConstants.EXTRAS_KEY_CONTENT_STYLE_PLAYABLE, MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM)
            }).build()
            LibraryResult.ofItem(root, styles)
        } else LibraryResult.ofError(SessionError.ERROR_PERMISSION_DENIED))

    override fun onGetChildren(
        session: MediaLibrarySession, browser: MediaSession.ControllerInfo, parentId: String,
        page: Int, pageSize: Int, params: LibraryParams?
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        if (!mayBrowse(session, browser)) return Futures.immediateFuture(LibraryResult.ofError(SessionError.ERROR_PERMISSION_DENIED))
        if (parentId != ROOT) return Futures.immediateFuture(LibraryResult.ofError(SessionError.ERROR_BAD_VALUE))
        return load {
            val prefs = preferences.userPreferencesFlow.first()
            LibraryResult.ofItemList(StudyMedia.entries.map { browseItem(it, prefs) }, params)
        }
    }

    override fun onGetItem(session: MediaLibrarySession, browser: MediaSession.ControllerInfo, mediaId: String): ListenableFuture<LibraryResult<MediaItem>> {
        if (!mayBrowse(session, browser)) return Futures.immediateFuture(LibraryResult.ofError(SessionError.ERROR_PERMISSION_DENIED))
        if (mediaId == ROOT) return Futures.immediateFuture(LibraryResult.ofItem(root, null))
        val study = StudyMedia.entries.firstOrNull { it.id == mediaId }
            ?: return Futures.immediateFuture(LibraryResult.ofError(SessionError.ERROR_BAD_VALUE))
        return load { LibraryResult.ofItem(browseItem(study, preferences.userPreferencesFlow.first()), null) }
    }

    override fun onAddMediaItems(session: MediaSession, controller: MediaSession.ControllerInfo, mediaItems: List<MediaItem>): ListenableFuture<List<MediaItem>> =
        if (controller.packageName == context.packageName) {
            super.onAddMediaItems(session, controller, mediaItems)
        } else Futures.immediateFailedFuture(SecurityException("Only the app can add arbitrary media items"))

    override fun onSetMediaItems(
        session: MediaSession, controller: MediaSession.ControllerInfo, mediaItems: List<MediaItem>,
        startIndex: Int, startPositionMs: Long
    ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
        val study = StudyMedia.entries.firstOrNull { it.id == mediaItems.singleOrNull()?.mediaId }
        if (study == null && controller.packageName == context.packageName) {
            playRequest?.cancel()
            return super.onSetMediaItems(session, controller, mediaItems, startIndex, startPositionMs)
        }
        if (study == null || !mayBrowse(session, controller)) {
            return Futures.immediateFailedFuture(SecurityException("Only today's lessons are available in the car"))
        }
        playRequest?.cancel()
        return load({ playRequest = it }) {
            val prefs = preferences.userPreferencesFlow.first()
            when (study) {
                StudyMedia.TANYA -> {
                    val date = WidgetStudyDay.today(WidgetContentLoader.engine(context), prefs)
                    val lesson = tanya.getDailyTanyaLesson(date) ?: error("שיעור התניא של היום אינו זמין")
                    val clip = TanyaAudioRepository.get(context).resolve(lesson, date.toString())
                        ?: error("הקלטת התניא של היום אינה זמינה")
                    Selection(clip.mediaItem(context), tanyaPositions.getLong(clip.id, 0), tanyaPositions.getFloat("speed", 1f))
                }
                StudyMedia.RAMBAM -> {
                    val plan = rambam.resolve(rambamAssignment(prefs))
                    speechStore.save(plan)
                    Selection(plan.recordingItem(context, 0, fromCar = true), speechStore.position(plan.id).second, speechStore.speed())
                }
            }
        }.let { pending ->
            Futures.transform(pending, { selection ->
                requireNotNull(selection)
                if (study == StudyMedia.TANYA && session.player.currentMediaItem?.mediaId != selection.item.mediaId) {
                    val key = selection.item.mediaId
                    AudioAnalytics.get(context).tracker.request(key, "tanya", "recording")
                }
                session.player.setPlaybackSpeed(selection.speed)
                val position = if (startPositionMs == C.TIME_UNSET) selection.position else startPositionMs
                MediaSession.MediaItemsWithStartPosition(listOf(selection.item), 0, position.coerceAtLeast(0))
            }, ContextCompat.getMainExecutor(context))
        }
    }

    fun cancelPendingPlayback() {
        playRequest?.cancel()
    }

    private data class Selection(val item: MediaItem, val position: Long, val speed: Float)

    private fun browseItem(study: StudyMedia, prefs: UserPreferences): MediaItem {
        val subtitle = when (study) {
            StudyMedia.TANYA -> {
                val engine = WidgetContentLoader.engine(context)
                engine.getHebrewDate(WidgetStudyDay.today(engine, prefs))
            }
            StudyMedia.RAMBAM -> rambamAssignment(prefs).chapters.joinToString(" · ") {
                "${it.sectionNameHebrew} · פרק ${com.example.ui.util.HebrewNumberFormatter.toHebrewNumeral(it.chapterNumber)}"
            }
        }
        return study.browseItem(context, subtitle)
    }

    private fun rambamAssignment(prefs: UserPreferences) = WidgetContentLoader.engine(context).getDailyLesson(
        instant = Instant.now(), zoneId = WidgetStudyDay.zone,
        track = if (prefs.selectedTrack == "three") "three" else "one",
        dayBoundary = prefs.dayBoundary, cityName = prefs.selectedCity
    )

    private fun mayBrowse(session: MediaSession, controller: MediaSession.ControllerInfo): Boolean =
        controller.packageName == context.packageName || session.isAutoCompanionController(controller)

    private fun <T> load(track: (Job) -> Unit = {}, block: suspend () -> T): ListenableFuture<T> {
        val future = SettableFuture.create<T>()
        val job = scope.launch {
            try {
                future.set(withContext(Dispatchers.IO) { block() })
            } catch (e: CancellationException) {
                future.cancel(false)
                throw e
            } catch (e: Exception) {
                future.setException(e)
            }
        }
        job.invokeOnCompletion { if (!future.isDone) future.cancel(false) }
        future.addListener({ if (future.isCancelled) job.cancel() }, MoreExecutors.directExecutor())
        track(job)
        return future
    }

    companion object {
        private const val ROOT = "today"
    }
}
