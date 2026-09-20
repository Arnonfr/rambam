package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.DailyRambamViewModel
import com.example.ui.components.AttributionDialog
import com.example.ui.components.CalendarDatePickerDialog
import com.example.ui.components.SettingsDialog
import com.example.ui.screens.ChumashReaderScreen
import com.example.ui.screens.TehillimReaderScreen
import com.example.ui.screens.TanyaReaderScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ReaderScreen
import com.example.ui.theme.DailyRambamTheme
import androidx.activity.compose.BackHandler

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: DailyRambamViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val snackbarHostState = remember { SnackbarHostState() }
            val lifecycleOwner = LocalLifecycleOwner.current

            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        viewModel.checkAndUpdateCurrentDate()
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            LaunchedEffect(uiState.noticeMessage) {
                uiState.noticeMessage?.let { msg ->
                    snackbarHostState.showSnackbar(msg)
                    viewModel.clearNotice()
                }
            }

            DailyRambamTheme(readerTheme = uiState.preferences.readerTheme) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        if (uiState.isLoading && uiState.allSections.isEmpty()) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        } else if (uiState.isChumashReaderOpen) {
                            BackHandler(enabled = true) {
                                viewModel.closeChumashReader()
                            }
                            ChumashReaderScreen(
                                chumashLesson = uiState.dailyChumash,
                                selectedAliyaIndex = uiState.selectedChumashAliyaIndex,
                                preferences = uiState.preferences,
                                isLoading = uiState.isChumashLoading,
                                onSelectAliya = { viewModel.selectChumashAliya(it) },
                                onToggleShowTeamim = { viewModel.toggleChumashShowTeamim() },
                                onChangeFontSize = { viewModel.changeChumashFontSize(it) },
                                onToggleAliyaCompletion = { parasha, aliyaIdx ->
                                    viewModel.toggleChumashAliyaCompleted(parasha, aliyaIdx)
                                },
                                onBack = { viewModel.closeChumashReader() },
                                onSavePosition = { verse, aliya ->
                                    viewModel.saveChumashPosition(verse, aliya)
                                },
                                savedPosition = uiState.latestPosition,
                                hebrewDateText = uiState.dailyLesson?.hebrewDate ?: "",
                                onNextDay = { viewModel.stepSelectedDate(1) },
                                onPrevDay = { viewModel.stepSelectedDate(-1) },
                                onOpenDatePicker = { viewModel.setShowDatePicker(true) },
                                onLineSpacingChange = { viewModel.setLineSpacing(it) },
                                onFontFamilyChange = { viewModel.setFontFamily(it) },
                                onThemeChange = { viewModel.setReaderTheme(it) },
                                onKeepScreenOnChange = { viewModel.setKeepScreenOn(it) }
                            )
                        } else if (uiState.isTehillimReaderOpen) {
                            BackHandler(enabled = true) {
                                viewModel.openTehillimReader(false)
                            }
                            TehillimReaderScreen(
                                lesson = uiState.dailyTehillim,
                                isLoading = uiState.isTehillimLoading,
                                onBack = { viewModel.openTehillimReader(false) },
                                preferences = uiState.preferences,
                                onFontSizeChange = { viewModel.changeChumashFontSize(it) },
                                onLineSpacingChange = { viewModel.setLineSpacing(it) },
                                onFontFamilyChange = { viewModel.setFontFamily(it) },
                                onNikudToggle = { viewModel.toggleChumashShowTeamim() },
                                onThemeChange = { viewModel.setReaderTheme(it) },
                                onKeepScreenOnChange = { viewModel.setKeepScreenOn(it) },
                                hebrewDateText = uiState.dailyLesson?.hebrewDate ?: "",
                                onNextDay = { viewModel.stepSelectedDate(1) },
                                onPrevDay = { viewModel.stepSelectedDate(-1) },
                                onOpenDatePicker = { viewModel.setShowDatePicker(true) },
                                onSavePosition = { verse, chapter -> viewModel.saveTehillimPosition(verse, chapter) },
                                savedPosition = uiState.latestTehillimPosition
                            )
                        } else if (uiState.isTanyaReaderOpen) {
                            BackHandler(enabled = true) {
                                viewModel.openTanyaReader(false)
                            }
                            TanyaReaderScreen(
                                lesson = uiState.dailyTanya,
                                isLoading = uiState.isTanyaLoading,
                                onBack = { viewModel.openTanyaReader(false) },
                                preferences = uiState.preferences,
                                onFontSizeChange = { viewModel.changeChumashFontSize(it) },
                                onLineSpacingChange = { viewModel.setLineSpacing(it) },
                                onFontFamilyChange = { viewModel.setFontFamily(it) },
                                onNikudToggle = { viewModel.toggleChumashShowTeamim() },
                                onThemeChange = { viewModel.setReaderTheme(it) },
                                onKeepScreenOnChange = { viewModel.setKeepScreenOn(it) },
                                hebrewDateText = uiState.dailyLesson?.hebrewDate ?: "",
                                onNextDay = { viewModel.stepSelectedDate(1) },
                                onPrevDay = { viewModel.stepSelectedDate(-1) },
                                onOpenDatePicker = { viewModel.setShowDatePicker(true) },
                                onToggleCompletion = { viewModel.toggleTanyaCompletion() },
                                onSavePosition = { sec, les -> viewModel.saveTanyaPosition(sec, les) },
                                savedPosition = uiState.latestTanyaPosition
                            )
                        } else if (uiState.activeChapter != null) {
                            ReaderScreen(
                                chapter = uiState.activeChapter!!,
                                section = uiState.activeSection,
                                halachot = uiState.activeHalachot,
                                activeChaptersList = uiState.activeChaptersList,
                                savedPosition = uiState.activeReadingPosition,
                                isCompleted = uiState.isCurrentChapterCompleted,
                                preferences = uiState.preferences,
                                dailyLesson = uiState.dailyLesson,
                                completedChapterIds = uiState.completedChapterIds,
                                onOpenChapter = { chId -> viewModel.openChapter(chId) },
                                onSaveImmediate = { chapterId, index, fraction ->
                                    viewModel.saveReadingPositionImmediate(chapterId, index, fraction)
                                },
                                onBack = { viewModel.closeReader() },
                                onScrollChanged = { chapterId, index, fraction ->
                                    viewModel.onScrollPositionChanged(chapterId, index, fraction)
                                },
                                onMarkCompleted = { chId ->
                                    viewModel.markChapterCompleted(chId)
                                },
                                onNextChapter = { viewModel.goToNextChapter() },
                                onPreviousChapter = { viewModel.goToPreviousChapter() },
                                onJumpToToday = {
                                    viewModel.resumeLastReading()
                                },
                                onOpenDatePicker = { viewModel.setShowDatePicker(true) },
                                onFontSizeChange = { size -> viewModel.setFontSize(size) },
                                onLineSpacingChange = { spacing -> viewModel.setLineSpacing(spacing) },
                                onFontFamilyChange = { family -> viewModel.setFontFamily(family) },
                                onNikudToggle = { show -> viewModel.toggleShowNikud(show) },
                                onThemeChange = { theme -> viewModel.setReaderTheme(theme) },
                                onKeepScreenOnChange = { keep -> viewModel.setKeepScreenOn(keep) },
                                onNextDay = { viewModel.stepSelectedDate(1) },
                                onPrevDay = { viewModel.stepSelectedDate(-1) }
                            )
                        } else {
                            HomeScreen(
                                uiState = uiState,
                                onGetHebrewDate = { date -> viewModel.getHebrewDateForDate(date) },
                                onOpenChapter = { chapterId ->
                                    viewModel.openChapter(chapterId)
                                },
                                onOpenDailyLesson = {
                                    viewModel.openDailyLesson()
                                },
                                onResumeReading = {
                                    viewModel.resumeLastReading()
                                },
                                onTrackSelect = { track ->
                                    viewModel.selectTrack(track)
                                },
                                onSelectDate = { date ->
                                    viewModel.setSelectedStudyDate(date)
                                },
                                onStepDate = { delta ->
                                    viewModel.stepSelectedDate(delta)
                                },
                                onToggleDateDrawer = {
                                    viewModel.toggleDateDrawer()
                                },
                                onSelectTab = { tab ->
                                    viewModel.selectHomeTab(tab)
                                },
                                onOpenEditSheet = {
                                    viewModel.setShowEditStudySheet(true)
                                },
                                onCloseEditSheet = {
                                    viewModel.setShowEditStudySheet(false)
                                },
                                onToggleStudyVisibility = { key ->
                                    viewModel.toggleStudyVisibility(key)
                                },
                                onDayBoundaryChange = { boundary ->
                                    viewModel.setDayBoundary(boundary)
                                },
                                onCityChange = { city ->
                                    viewModel.setCity(city)
                                },
                                onOpenSettings = {
                                    viewModel.setShowSettings(true)
                                },
                                onOpenAttribution = {
                                    viewModel.setShowAttribution(true)
                                },
                                onOpenChumash = {
                                    viewModel.openChumashReader()
                                },
                                onOpenTehillim = {
                                    viewModel.openTehillimReader(true)
                                },
                                onOpenTanya = {
                                    viewModel.openTanyaReader(true)
                                }
                            )
                        }

                        // Calendar Date Picker Dialog
                        if (uiState.showDatePickerDialog) {
                            CalendarDatePickerDialog(
                                initialDate = uiState.viewingDate,
                                getLessonForDate = { date -> viewModel.getLessonForDate(date) },
                                onDismiss = { viewModel.setShowDatePicker(false) },
                                onDateSelected = { date -> viewModel.selectDateAndOpenLesson(date) }
                            )
                        }

                        // Settings Dialog
                        if (uiState.showSettingsDialog) {
                            SettingsDialog(
                                preferences = uiState.preferences,
                                onDismiss = { viewModel.setShowSettings(false) },
                                onTrackChange = { viewModel.selectTrack(it) },
                                onDayBoundaryChange = { viewModel.setDayBoundary(it) },
                                onCityChange = { viewModel.setCity(it) },
                                onFontFamilyChange = { viewModel.setFontFamily(it) },
                                onOpenAttribution = { viewModel.setShowAttribution(true) }
                            )
                        }

                        // Attribution Dialog
                        if (uiState.showAttributionDialog) {
                            AttributionDialog(
                                onDismiss = { viewModel.setShowAttribution(false) }
                            )
                        }
                    }
                }
            }
        }
    }
}
