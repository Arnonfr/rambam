package com.example

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.data.prayers.PrayerBook
import com.example.data.prayers.PrayerRepository
import com.example.ui.screens.PrayerReaderScreen
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
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ChumashReaderScreen
import com.example.ui.screens.TehillimReaderScreen
import com.example.ui.screens.TanyaReaderScreen
import com.example.ui.screens.MitzvahReaderScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ReaderScreen
import com.example.ui.theme.DailyRambamTheme
import androidx.activity.compose.BackHandler
import com.example.widget.WidgetLaunch
import com.example.widget.WidgetStudy
import com.example.widget.DailyStudyWidgetUpdater
import com.example.widget.openFromWidget
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {
    private val widgetRequest = MutableStateFlow<WidgetStudy?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        widgetRequest.value = WidgetLaunch.consume(intent)
        setContent {
            val viewModel: DailyRambamViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val reviewEngagement=remember { com.example.data.reviews.ReviewEngagement(applicationContext) }
            val reviewCoordinator=remember { com.example.data.reviews.PlayReviewCoordinator(this@MainActivity) }
            var reviewPending by remember { mutableStateOf(false) }
            val speechViewModel: com.example.ui.DailySpeechViewModel = viewModel()
            val speechState by speechViewModel.state.collectAsStateWithLifecycle()
            val zmanimViewModel: com.example.ui.ZmanimViewModel = viewModel()
            val zmanimPreferences by zmanimViewModel.preferences.collectAsStateWithLifecycle()
            val calculatedZmanimDay by zmanimViewModel.day.collectAsStateWithLifecycle()
            val zmanimLocating by zmanimViewModel.locating.collectAsStateWithLifecycle()
            val zmanimMessage by zmanimViewModel.message.collectAsStateWithLifecycle()
            var showZmanim by rememberSaveable { mutableStateOf(false) }
            var zmanimSettings by rememberSaveable { mutableStateOf(false) }
            var zmanimFocus by remember { mutableStateOf<com.example.domain.zmanim.ZmanId?>(null) }
            val zmanimDay = calculatedZmanimDay?.takeIf {
                it.date == uiState.selectedStudyDate && it.location == zmanimPreferences.location &&
                    it.candleLightingMinutesOverride == zmanimPreferences.candleLightingMinutes &&
                    it.holidayInIsraelOverride == zmanimPreferences.holidayInIsrael && it.method == zmanimPreferences.method
            }
            val foreground by androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsStateWithLifecycle()
            val zmanimNow by produceState(java.time.Instant.now(), foreground, zmanimDay) {
                while (foreground.isAtLeast(Lifecycle.State.STARTED)) {
                    value = java.time.Instant.now()
                    val nextChange = zmanimDay?.let { it.times + listOfNotNull(it.candleLighting?.result) }?.flatMap { listOfNotNull(it.rawInstant, it.displayedInstant) }
                        ?.filter { it > value }?.minOrNull()
                    val delay = nextChange?.let { java.time.Duration.between(value, it).toMillis().coerceAtLeast(1) }
                        ?.coerceAtMost(60_000) ?: 60_000
                    kotlinx.coroutines.delay(delay)
                }
            }
            val locationPermission = androidx.activity.compose.rememberLauncherForActivityResult(
                androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()) { grants ->
                if (grants.values.any { it }) zmanimViewModel.locate() else zmanimViewModel.permissionDenied()
            }
            val requestZmanimLocation: () -> Unit = {
                if (androidx.core.content.ContextCompat.checkSelfPermission(applicationContext, android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
                    androidx.core.content.ContextCompat.checkSelfPermission(applicationContext, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED)
                    zmanimViewModel.locate()
                else locationPermission.launch(arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.ACCESS_FINE_LOCATION))
            }
            LaunchedEffect(uiState.selectedStudyDate) { zmanimViewModel.selectDate(uiState.selectedStudyDate) }
            LaunchedEffect(zmanimPreferences.enabled, zmanimPreferences.mode) { zmanimViewModel.refreshIfNeeded() }
            val personalViewModel: com.example.ui.PersonalTehillimViewModel = viewModel()
            val personalState by personalViewModel.state.collectAsStateWithLifecycle()
            val personalError by personalViewModel.error.collectAsStateWithLifecycle()
            var personalOpen by rememberSaveable { mutableStateOf(false) }
            var personalManagement by rememberSaveable { mutableStateOf(false) }
            val personalDate by produceState(uiState.selectedStudyDate, uiState.selectedStudyDate, uiState.effectiveToday, uiState.preferences.selectedCity, personalOpen) {
                while (personalOpen) {
                    val city = com.example.domain.sunset.SupportedCities.findCityByName(uiState.preferences.selectedCity)
                    value = com.example.domain.tehillim.PersonalPsalmCalendar.readingDate(
                        uiState.selectedStudyDate, uiState.effectiveToday, city, java.time.Instant.now())
                    kotlinx.coroutines.delay(30_000)
                }
            }
            androidx.compose.runtime.CompositionLocalProvider(
                com.example.ui.components.LocalLetterSpacingChange provides { value: Float -> viewModel.setLetterSpacing(value) }
            ) {
            val snackbarHostState = remember { SnackbarHostState() }
            val lifecycleOwner = LocalLifecycleOwner.current
            var prayerId by rememberSaveable { mutableStateOf<String?>(null) }
            val pendingWidgetStudy by widgetRequest.collectAsStateWithLifecycle()
            LaunchedEffect(pendingWidgetStudy) {
                val study = pendingWidgetStudy ?: return@LaunchedEffect
                prayerId = null
                personalOpen = false
                viewModel.openFromWidget(study)
                widgetRequest.value = null
            }
            val prayers by produceState<PrayerBook?>(null, uiState.effectiveToday) {
                value = withContext(Dispatchers.IO) { PrayerRepository(applicationContext).load(uiState.effectiveToday) }
            }
            val prayerServices = prayers?.let { com.example.data.prayers.PrayerServices.group(it.sections) }.orEmpty()
            val prayer = prayerServices.firstOrNull { it.id == prayerId }
            val reviewHome=!uiState.isLoading && uiState.activeChapter==null && uiState.activeCompletion==null &&
                !uiState.isChumashReaderOpen && !uiState.isTehillimReaderOpen && !uiState.isTanyaReaderOpen &&
                !uiState.isHayomYomReaderOpen && !uiState.isMitzvahReaderOpen && !uiState.showSettingsDialog &&
                !uiState.showAttributionDialog && !uiState.showDatePickerDialog && !uiState.showEditStudySheet && uiState.noticeMessage==null &&
                uiState.dateDrawerState==com.example.ui.DateDrawerState.COLLAPSED &&
                prayerId==null && !personalOpen && !showZmanim
            val latestReviewHome=androidx.compose.runtime.rememberUpdatedState(reviewHome)
            LaunchedEffect(Unit) {
                com.example.data.reviews.ReviewEngagement.completions.collect { reviewPending=true }
            }
            LaunchedEffect(reviewPending,reviewHome,foreground) {
                if(reviewPending && reviewHome && foreground.isAtLeast(Lifecycle.State.RESUMED)) {
                    kotlinx.coroutines.delay(1200)
                    reviewPending=false
                    reviewCoordinator.requestAfterStudy { latestReviewHome.value }
                }
            }

            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_STOP) zmanimViewModel.stopLocation()
                    if (event == Lifecycle.Event.ON_RESUME) {
                        reviewEngagement.recordUse()
                        viewModel.checkAndUpdateCurrentDate()
                        zmanimViewModel.refreshIfNeeded()
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            LaunchedEffect(uiState.selectedStudyDate, uiState.dailyLesson, uiState.dailyChumash,
                uiState.dailyTehillim, uiState.dailyTanya, uiState.dailyHayomYom,
                uiState.sectionChapters, uiState.completedChapterIds) { viewModel.refreshStudyProgress() }

            LaunchedEffect(uiState.dailyTanya?.date, uiState.dailyTanya?.fullRef, uiState.selectedStudyDate) {
                uiState.dailyTanya?.let { lesson ->
                    com.example.data.tanya.TanyaAudioRepository.get(applicationContext).resolve(lesson, uiState.selectedStudyDate.toString())
                }
            }

            LaunchedEffect(uiState.noticeMessage) {
                uiState.noticeMessage?.let { msg ->
                    snackbarHostState.showSnackbar(msg)
                    viewModel.clearNotice()
                }
            }

            DailyRambamTheme(readerTheme = uiState.preferences.readerTheme) {
                if (showZmanim) com.example.ui.components.ZmanimSheet(
                    date = uiState.selectedStudyDate,
                    fullDate = viewModel.getHebrewDateForDate(uiState.selectedStudyDate),
                    preferences = zmanimPreferences, day = zmanimDay, now = zmanimNow,
                    focus = zmanimFocus,
                    onOpenSettings = { showZmanim=false;zmanimSettings=true;viewModel.setShowSettings(true) },
                    onDismiss = { showZmanim = false; zmanimViewModel.clearMessage() }
                )
                Scaffold(
                    containerColor = if (uiState.isLoading && uiState.allSections.isEmpty()) androidx.compose.ui.graphics.Color.White else androidx.compose.material3.MaterialTheme.colorScheme.background,
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .consumeWindowInsets(innerPadding)
                    ) {
                        if (personalOpen) {
                            com.example.ui.screens.PersonalTehillimScreen(
                                state = personalState, date = personalDate, preferences = uiState.preferences,
                                onBack = { personalOpen = false },
                                onSave = personalViewModel::save, onDelete = personalViewModel::delete,
                                onMove = personalViewModel::move,
                                onComplete = { entry, done -> personalViewModel.complete(entry, personalDate, done) },
                                onFontSizeChange = viewModel::setFontSize, onLineSpacingChange = viewModel::setLineSpacing,
                                onFontFamilyChange = viewModel::setFontFamily, onNikudToggle = viewModel::toggleShowNikud,
                                onThemeChange = viewModel::setReaderTheme, onKeepScreenOnChange = viewModel::setKeepScreenOn,
                                error = personalError, startWithManagement = personalManagement
                            )
                        } else if (uiState.showSettingsDialog) {
                            SettingsScreen(
                                onRateInStore={ if(!reviewCoordinator.openStore()) android.widget.Toast.makeText(
                                    this@MainActivity,"לא ניתן לפתוח את החנות כרגע.",android.widget.Toast.LENGTH_SHORT).show() },
                                zmanimPreferences = zmanimPreferences,
                                onZmanimChange = zmanimViewModel::update,
                                onZmanimLocate = requestZmanimLocation,
                                zmanimLocating = zmanimLocating,
                                zmanimMessage = zmanimMessage,
                                expandZmanim = zmanimSettings,
                                personalPsalmCount = personalState.entries.size,
                                onOpenPersonalPsalms = { personalManagement = true; personalOpen = true },
                                preferences = uiState.preferences,
                                onBack = { viewModel.setShowSettings(false) },
                                onDayBoundaryChange = { viewModel.setDayBoundary(it) },
                                onCityChange = { viewModel.setCity(it) },
                                onFontFamilyChange = { viewModel.setFontFamily(it) },
                                onToggleStudy = { viewModel.toggleStudyVisibility(it) },
                                onTanyaBookViewChange = { viewModel.setTanyaBookView(it) },
                                onOpenAttribution = { viewModel.setShowAttribution(true) },
                                onFontSizeChange = { viewModel.setFontSize(it) },
                                onLineSpacingChange = { viewModel.setLineSpacing(it) },
                                onLetterSpacingChange = { viewModel.setLetterSpacing(it) },
                                onNikudToggle = { viewModel.toggleShowNikud(it) },
                                onThemeChange = { viewModel.setReaderTheme(it) },
                                onKeepScreenOnChange = { viewModel.setKeepScreenOn(it) }
                            )
                        } else if (uiState.isLoading && uiState.allSections.isEmpty()) {
                            com.example.ui.components.WalkingLoadingScreen()
                        } else if (uiState.activeCompletion != null) {
                            com.example.ui.screens.StudyCompletionReaderScreen(
                                entry = uiState.activeCompletion!!, preferences = uiState.preferences,
                                onBack = { viewModel.closeStudyCompletion() },
                                onSave = { viewModel.saveStudyCompletion(it) },
                                onCompleted = { viewModel.removeStudyCompletion(uiState.activeCompletion!!.id) }
                            )
                        } else if (prayer != null) {
                            androidx.compose.runtime.key(prayer.id) {
                                val prayerSections = prayerServices
                                val prayerIndex = prayerSections.indexOfFirst { it.id == prayer.id }
                                PrayerReaderScreen(
                                    service = prayer,
                                    date = uiState.effectiveToday,
                                    preferences = uiState.preferences,
                                    onBack = { prayerId = null },
                                    onNextPrayer = if (prayerIndex < prayerSections.lastIndex) ({ prayerId = prayerSections[prayerIndex + 1].id }) else null,
                                    onPrevPrayer = if (prayerIndex > 0) ({ prayerId = prayerSections[prayerIndex - 1].id }) else null,
                                    onFontSizeChange = { viewModel.setFontSize(it) },
                                    onLineSpacingChange = { viewModel.setLineSpacing(it) },
                                    onFontFamilyChange = { viewModel.setFontFamily(it) },
                                    onNikudToggle = { viewModel.toggleShowNikud(it) },
                                    onThemeChange = { viewModel.setReaderTheme(it) },
                                    onKeepScreenOnChange = { viewModel.setKeepScreenOn(it) }
                                )
                            }
                        } else if (uiState.isChumashReaderOpen) {
                            BackHandler(enabled = true) {
                                viewModel.closeChumashReader()
                            }
                            ChumashReaderScreen(
                                onSaveCompletion = { viewModel.saveStudyCompletion(it) },
                                chumashLesson = uiState.dailyChumash,
                                selectedAliyaIndex = uiState.selectedChumashAliyaIndex,
                                preferences = uiState.preferences,
                                isLoading = uiState.isChumashLoading,
                                onSelectAliya = { viewModel.selectChumashAliya(it) },
                                onToggleShowTeamim = { viewModel.toggleChumashShowTeamim() },
                                onNikudToggle = { viewModel.toggleShowNikud(it) },
                                onChangeFontSize = { viewModel.setFontSize(it) },
                                onToggleAliyaCompletion = { parasha, aliyaIdx ->
                                    viewModel.toggleChumashAliyaCompleted(parasha, aliyaIdx)
                                },
                                onBack = { viewModel.closeChumashReader() },
                                onSavePosition = { verse, aliya ->
                                    viewModel.saveChumashPosition(verse, aliya)
                                },
                                savedPosition = uiState.latestChumashPosition,
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
                                onOpenPersonalPsalms = { personalManagement = false; personalOpen = true },
                                onSaveCompletion = { viewModel.saveStudyCompletion(it) },
                                onCompleted = { viewModel.completeStudy("tehillim") },
                                lesson = uiState.dailyTehillim,
                                isLoading = uiState.isTehillimLoading,
                                onBack = { viewModel.openTehillimReader(false) },
                                preferences = uiState.preferences,
                                onFontSizeChange = { viewModel.setFontSize(it) },
                                onLineSpacingChange = { viewModel.setLineSpacing(it) },
                                onFontFamilyChange = { viewModel.setFontFamily(it) },
                                onNikudToggle = { viewModel.toggleShowNikud(it) },
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
                                audioStudyDate = uiState.selectedStudyDate.toString(),
                                onSaveCompletion = { viewModel.saveStudyCompletion(it) },
                                onBookViewChange = { viewModel.setTanyaBookView(it) },
                                onBookNikudChange = { viewModel.setTanyaBookNikud(it) },
                                lesson = uiState.dailyTanya,
                                isLoading = uiState.isTanyaLoading,
                                onBack = { viewModel.openTanyaReader(false) },
                                preferences = uiState.preferences,
                                onFontSizeChange = { viewModel.setFontSize(it) },
                                onLineSpacingChange = { viewModel.setLineSpacing(it) },
                                onFontFamilyChange = { viewModel.setFontFamily(it) },
                                onNikudToggle = { viewModel.toggleShowNikud(it) },
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
                        } else if (uiState.isHayomYomReaderOpen) {
                            TanyaReaderScreen(
                                onSaveCompletion = { viewModel.saveStudyCompletion(it) },
                                lesson = uiState.dailyHayomYom, isLoading = false,
                                readerTitle = "היום יום", contentId = "hayom_yom", allowPrintLayout = false,
                                onBack = { viewModel.openHayomYomReader(false) },
                                preferences = uiState.preferences,
                                onFontSizeChange = { viewModel.setFontSize(it) },
                                onLineSpacingChange = { viewModel.setLineSpacing(it) },
                                onFontFamilyChange = { viewModel.setFontFamily(it) },
                                onNikudToggle = { viewModel.toggleShowNikud(it) },
                                onThemeChange = { viewModel.setReaderTheme(it) },
                                onKeepScreenOnChange = { viewModel.setKeepScreenOn(it) },
                                hebrewDateText = uiState.dailyHayomYom?.hebrewDate.orEmpty(),
                                onNextDay = { viewModel.stepSelectedDate(1) },
                                onPrevDay = { viewModel.stepSelectedDate(-1) },
                                onOpenDatePicker = { viewModel.setShowDatePicker(true) },
                                onToggleCompletion = { viewModel.completeHayomYom() },
                                onSavePosition = { section, lesson -> viewModel.saveHayomYomPosition(section, lesson) }
                            )
                        } else if (uiState.isMitzvahReaderOpen && uiState.dailyMitzvahAssignment != null) {
                            BackHandler(enabled = true) {
                                viewModel.openMitzvahReader(false)
                            }
                            MitzvahReaderScreen(
                                onSaveCompletion = { viewModel.saveStudyCompletion(it) },
                                onCompleted = { viewModel.completeStudy("mitzvot") },
                                assignment = uiState.dailyMitzvahAssignment!!,
                                entries = uiState.dailyMitzvahEntries,
                                preferences = uiState.preferences,
                                hebrewDateText = uiState.dailyLesson?.hebrewDate ?: "",
                                savedAnchor = uiState.savedMitzvahAnchor,
                                onBack = { viewModel.openMitzvahReader(false) },
                                onSavePosition = { index, blockId, sectionId ->
                                    viewModel.saveMitzvahPosition(index, blockId, sectionId)
                                },
                                onNextDay = { viewModel.stepSelectedDate(1) },
                                onPrevDay = { viewModel.stepSelectedDate(-1) },
                                onOpenDatePicker = { viewModel.setShowDatePicker(true) },
                                onFontSizeChange = { viewModel.setFontSize(it) },
                                onLineSpacingChange = { viewModel.setLineSpacing(it) },
                                onFontFamilyChange = { viewModel.setFontFamily(it) },
                                onNikudToggle = { viewModel.toggleShowNikud(it) },
                                onThemeChange = { viewModel.setReaderTheme(it) },
                                onKeepScreenOnChange = { viewModel.setKeepScreenOn(it) }
                            )
                        } else if (uiState.activeChapter != null) {
                            val audioTrack = uiState.dailyLesson?.track ?: "one"
                            val sourcePrefs = remember { getSharedPreferences("rambam_audio_source", MODE_PRIVATE) }
                            var rambamSource by rememberSaveable(audioTrack) { mutableStateOf(sourcePrefs.getString(audioTrack, "recording") ?: "recording") }
                            val selectedAudioState = if (speechState.active && speechState.lessonId?.startsWith("rambam-${uiState.dailyLesson?.studyDate}-$audioTrack-") != true)
                                com.example.data.speech.DailySpeechState() else speechState
                            ReaderScreen(
                                audioPlaying = speechState.playing,
                                audioLoading = speechState.preparing && !speechState.paused,
                                audioControls = {
                                    com.example.ui.components.DailySpeechControls(selectedAudioState,
                                        uiState.dailyLesson?.hebrewDate.orEmpty(), uiState.dailyLesson?.track ?: "one",
                                        onStart={ if(rambamSource=="recording") speechViewModel.prepare(viewModel::prepareRecordedListening)
                                            else speechViewModel.prepare(viewModel::prepareDailyListening) },
                                        source=rambamSource, onSourceChange={ selected ->
                                            if (rambamSource != selected) {
                                                speechViewModel.action(com.example.data.speech.DailySpeechStatus.STOP)
                                                rambamSource=selected; sourcePrefs.edit().putString(audioTrack,selected).apply()
                                            }
                                        },
                                        onSeek=speechViewModel::seek,
                                        onAction={ action ->
                                            if(action=="daily_speech.restart") speechState.lessonId?.let { speechViewModel.restart(it) }
                                            else speechViewModel.action(action)
                                        })
                                },
                                onSaveCompletion = { viewModel.saveStudyCompletion(it) },
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
                                showCivilDate = zmanimPreferences.showCivilDate,
                                zmanimHeader = {
                                    com.example.ui.components.ZmanimHeader(zmanimPreferences, zmanimDay, zmanimNow,
                                        onScrollHintShown = zmanimViewModel::markScrollHintShown) {
                                        zmanimViewModel.refreshIfNeeded(); zmanimFocus = it; zmanimSettings = false; showZmanim = true
                                    }
                                },
                                zmanimDateContext = zmanimPreferences.takeIf { it.enabled }?.location?.let { location ->
                                    val zone = java.time.ZoneId.of(location.timezoneId)
                                    listOfNotNull(
                                        "מחר".takeIf { uiState.selectedStudyDate == zmanimNow.atZone(zone).toLocalDate().plusDays(1) },
                                        (if (location.gps) "UTC${uiState.selectedStudyDate.atTime(12, 0).atZone(zone).offset.id}" else location.name.substringBefore(",")).takeIf { zone != java.time.ZoneId.systemDefault() }
                                    ).joinToString(" · ").ifBlank { null }
                                },
                                personalPsalmCount = personalState.entries.size,
                                onOpenPersonalPsalms = { personalManagement = false; personalOpen = true },
                                onOpenCompletion = { viewModel.openStudyCompletion(it) },
                                onRemoveCompletion = { viewModel.removeStudyCompletion(it) },
                                uiState = uiState,
                                onGetRambamLesson = { track -> viewModel.getLessonForDate(uiState.selectedStudyDate, track) },
                                onOpenRambamTrack = { track -> viewModel.openRambamTrack(track) },
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
                                onResumeStudy = { track -> viewModel.resumeStudy(track) },
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
                                    zmanimSettings=false
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
                                },
                                onOpenMitzvah = {
                                    viewModel.openMitzvahReader(true)
                                },
                                onOpenHayomYom = { viewModel.openHayomYomReader(true) },
                                prayerSections = prayers?.sections.orEmpty(),
                                onOpenPrayer = { prayerId = it }
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
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        WidgetLaunch.consume(intent)?.let { widgetRequest.value = it }
    }

    override fun onStop() {
        super.onStop()
        DailyStudyWidgetUpdater.requestRefresh(this)
    }
}
