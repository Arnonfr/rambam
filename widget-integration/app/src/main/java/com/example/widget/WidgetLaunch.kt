package com.example.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.MainActivity
import com.example.ui.DailyRambamUiState
import com.example.ui.DailyRambamViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/** Intents from the widget into MainActivity. */
object WidgetLaunch {
    const val ACTION_OPEN_STUDY = "com.example.widget.action.OPEN_STUDY"
    const val EXTRA_STUDY = "com.example.widget.extra.STUDY"

    fun intent(context: Context, study: WidgetStudy): Intent =
        Intent(context, MainActivity::class.java).apply {
            action = ACTION_OPEN_STUDY
            putExtra(EXTRA_STUDY, study.key)
            // A distinct data URI keeps the PendingIntent of each button separate.
            data = Uri.parse("dailystudy://widget/${study.key}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

    /** Returns the requested study once and clears it, so recreation does not reopen it. */
    fun consume(intent: Intent?): WidgetStudy? {
        if (intent?.action != ACTION_OPEN_STUDY) return null
        val study = WidgetStudy.fromKey(intent.getStringExtra(EXTRA_STUDY))
        intent.action = Intent.ACTION_MAIN
        intent.removeExtra(EXTRA_STUDY)
        return study
    }
}

/**
 * Opens today's lesson for [study], the same way the home-screen strips do.
 * Uses only the ViewModel's public API.
 */
suspend fun DailyRambamViewModel.openFromWidget(study: WidgetStudy) {
    if (!awaitState(INITIAL_LOAD_TIMEOUT_MS) { !it.isLoading }) return
    checkAndUpdateCurrentDate()
    if (uiState.value.selectedStudyDate != uiState.value.effectiveToday) returnToToday()

    closeStudyCompletion()
    closeReader()
    closeChumashReader()
    openTehillimReader(false)
    openTanyaReader(false)
    openHayomYomReader(false)
    openMitzvahReader(false)
    setShowSettings(false)
    setShowDatePicker(false)

    val today = uiState.value.effectiveToday
    when (study) {
        WidgetStudy.HAYOM_YOM ->
            if (awaitState { it.dailyHayomYom?.date == today.toString() }) openHayomYomReader(true)
        WidgetStudy.TANYA ->
            if (awaitState { !it.isTanyaLoading && it.dailyTanya?.date == today.toString() }) openTanyaReader(true)
        WidgetStudy.CHUMASH ->
            if (awaitState { !it.isChumashLoading && it.dailyChumash?.date == today.toString() }) openChumashReader()
        WidgetStudy.TEHILLIM ->
            if (awaitState { !it.isTehillimLoading && it.dailyTehillim != null }) openTehillimReader(true)
        WidgetStudy.RAMBAM_ONE -> openRambamTrack("one")
        WidgetStudy.RAMBAM_THREE -> openRambamTrack("three")
        WidgetStudy.MITZVOT ->
            if (awaitState { it.dailyMitzvahAssignment?.studyDate == today }) openMitzvahReader(true)
    }
}

private const val INITIAL_LOAD_TIMEOUT_MS = 20_000L
private const val LESSON_LOAD_TIMEOUT_MS = 15_000L

private suspend fun DailyRambamViewModel.awaitState(
    timeoutMs: Long = LESSON_LOAD_TIMEOUT_MS,
    predicate: (DailyRambamUiState) -> Boolean
): Boolean = withTimeoutOrNull(timeoutMs) { uiState.first(predicate) } != null
