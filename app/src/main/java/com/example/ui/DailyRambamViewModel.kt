package com.example.ui
import com.example.domain.chumash.ChumashVerse
import com.example.domain.chumash.ChumashAliya
import kotlinx.coroutines.flow.firstOrNull

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ChapterCompletionEntity
import com.example.data.local.ChapterEntity
import com.example.data.local.ContentSectionEntity
import com.example.data.local.DatabaseInitializer
import com.example.data.local.HalachaEntity
import com.example.data.local.RambamDatabase
import com.example.data.local.ReadingPositionEntity
import com.example.data.local.ReadingStateManager
import com.example.data.local.SavedReadingAnchor
import com.example.data.local.UserPreferences
import com.example.data.local.UserPreferencesRepository
import com.example.data.repository.RambamRepository
import com.example.data.chumash.ChumashRepository
import com.example.data.tehillim.TehillimRepository
import com.example.data.tanya.TanyaRepository
import com.example.domain.chumash.DailyChumashLesson
import com.example.domain.tehillim.DailyTehillimLesson
import com.example.domain.tehillim.TehillimChapter
import com.example.domain.tanya.DailyTanyaLesson
import com.example.domain.tanya.TanyaSection
import com.example.domain.schedule.DailyLessonResult
import com.example.domain.schedule.DailyScheduleEngine
import com.example.ui.util.HebrewNumberFormatter
import com.example.ui.util.HebrewDateHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

enum class DateDrawerState {
    COLLAPSED,
    EXPANDED
}

enum class HomeTab {
    STUDY,
    PRAYERS
}

data class ReaderChapterData(
    val chapter: ChapterEntity,
    val section: ContentSectionEntity?,
    val halachot: List<HalachaEntity>,
    val isCompleted: Boolean
)

data class DailyRambamUiState(
    val isLoading: Boolean = true,
    val preferences: UserPreferences = UserPreferences(),
    val dailyLesson: DailyLessonResult? = null,
    val effectiveToday: LocalDate = LocalDate.now(),
    val selectedStudyDate: LocalDate = LocalDate.now(),
    val dateDrawerState: DateDrawerState = DateDrawerState.COLLAPSED,
    val selectedTab: HomeTab = HomeTab.STUDY,
    val showEditStudySheet: Boolean = false,
    val viewingDate: LocalDate = LocalDate.now(),
    val activeChapter: ChapterEntity? = null,
    val activeSection: ContentSectionEntity? = null,
    val activeHalachot: List<HalachaEntity> = emptyList(),
    val activeChaptersList: List<ReaderChapterData> = emptyList(),
    val activeReadingPosition: ReadingPositionEntity? = null,
    val isCurrentChapterCompleted: Boolean = false,
    val latestPosition: ReadingPositionEntity? = null,
    val latestChapter: ChapterEntity? = null,
    val latestHalachaTitle: String? = null,
    val allSections: List<ContentSectionEntity> = emptyList(),
    val sectionChapters: Map<String, List<ChapterEntity>> = emptyMap(),
    val completedChapterIds: Set<String> = emptySet(),
    val showAttributionDialog: Boolean = false,
    val showSettingsDialog: Boolean = false,
    val showDatePickerDialog: Boolean = false,
    val noticeMessage: String? = null,
    val dailyChumash: DailyChumashLesson? = null,
    val isChumashReaderOpen: Boolean = false,
    val selectedChumashAliyaIndex: Int = 1,
    val isChumashLoading: Boolean = false,
    val dailyTehillim: DailyTehillimLesson? = null,
    val isTehillimReaderOpen: Boolean = false,
    val isTehillimLoading: Boolean = false,
    val latestTehillimPosition: ReadingPositionEntity? = null,
    val dailyTanya: DailyTanyaLesson? = null,
    val isTanyaReaderOpen: Boolean = false,
    val isTanyaLoading: Boolean = false,
    val latestTanyaPosition: ReadingPositionEntity? = null
)

class DailyRambamViewModel(application: Application) : AndroidViewModel(application) {

    private val db = RambamDatabase.getInstance(application)
    private val dao = db.rambamDao()
    private val scheduleEngine = DailyScheduleEngine(application)
    private val repository = RambamRepository(dao, scheduleEngine)
    private val prefsRepository = UserPreferencesRepository(application)
    private val chumashRepository = ChumashRepository(application)
    private val tehillimRepository = TehillimRepository(application)
    private val tanyaRepository = TanyaRepository(application)
    private val dbInitializer = DatabaseInitializer(application, dao)
    private val readingStateManager = ReadingStateManager(application)

    private val _uiState = MutableStateFlow(DailyRambamUiState())
    val uiState: StateFlow<DailyRambamUiState> = _uiState.asStateFlow()

    private var autoSaveJob: Job? = null
    private var completionsJob: Job? = null

    init {
        val effective = calculateEffectiveToday(_uiState.value.preferences)
        _uiState.update { it.copy(effectiveToday = effective, selectedStudyDate = effective) }

        // Synchronous immediate restore of last reading anchor from disk
        val savedAnchor = readingStateManager.getSavedAnchor()
        if (savedAnchor != null && savedAnchor.studyDate == effective.toString()) {
            val halachaLetter = HebrewNumberFormatter.toHebrewNumeral(savedAnchor.halachaIndex + 1)
            val dummyPos = ReadingPositionEntity(
                track = savedAnchor.track,
                chapterId = savedAnchor.chapterId,
                sectionId = savedAnchor.sectionId,
                chapterNumber = savedAnchor.chapterNumber,
                halachaId = savedAnchor.halachaId,
                halachaIndex = savedAnchor.halachaIndex,
                textOffset = 0,
                scrollOffsetFraction = savedAnchor.scrollOffsetFraction,
                quoteFingerprint = "",
                updatedAt = savedAnchor.updatedAt
            )
            _uiState.update {
                it.copy(
                    latestPosition = dummyPos,
                    latestHalachaTitle = "הלכה $halachaLetter"
                )
            }
        }

        viewModelScope.launch {
            dbInitializer.initializeIfNeeded()
            observePreferences()
            observeSectionsAndChapters()
            refreshDailyChumash(effective)
            refreshDailyTehillim(effective)
            refreshDailyTanya(effective)
        }
    }

    private fun selectedStudyDateKey(): String =
        _uiState.value.dailyLesson?.studyDate ?: _uiState.value.selectedStudyDate.toString()

    private fun <T : ReadingPositionEntity?> onlyForSelectedStudyDay(position: T, track: String): T? {
        val isToday = _uiState.value.selectedStudyDate == _uiState.value.effectiveToday
        return if (isToday && readingStateManager.isStudyDateCurrent(track, selectedStudyDateKey())) {
            position
        } else {
            null
        }
    }

    private fun observeCompletionsForDate(track: String, studyDate: String) {
        completionsJob?.cancel()
        completionsJob = viewModelScope.launch {
            repository.getCompletionsForDate(track, studyDate).collectLatest { completions ->
                val ids = completions.map { it.chapterId }.toSet()
                _uiState.update { it.copy(completedChapterIds = ids) }
            }
        }
    }

    private fun calculateEffectiveToday(prefs: UserPreferences): LocalDate {
        val now = Instant.now()
        val zoneId = ZoneId.of("Asia/Jerusalem")
        val todayCivil = LocalDate.now(zoneId)
        return if (prefs.dayBoundary == "sunset") {
            val sunsetInstant = scheduleEngine.getSunsetInstant(todayCivil, prefs.selectedCity, zoneId)
            if (sunsetInstant != null && now.isAfter(sunsetInstant)) {
                todayCivil.plusDays(1)
            } else {
                todayCivil
            }
        } else {
            todayCivil
        }
    }

    private fun observePreferences() {
        viewModelScope.launch {
            prefsRepository.userPreferencesFlow.collectLatest { prefs ->
                _uiState.update { it.copy(preferences = prefs) }
                refreshDailyLesson()
                observeLatestPosition()
            }
        }
    }

    private fun observeSectionsAndChapters() {
        viewModelScope.launch {
            repository.allSections.collectLatest { sections ->
                val chaptersMap = mutableMapOf<String, List<ChapterEntity>>()
                sections.forEach { section ->
                    viewModelScope.launch {
                        repository.getChaptersForSection(section.sectionId).collectLatest { chaps ->
                            _uiState.update { state ->
                                val updatedMap = state.sectionChapters.toMutableMap()
                                updatedMap[section.sectionId] = chaps
                                state.copy(allSections = sections, sectionChapters = updatedMap)
                            }
                        }
                    }
                }
                _uiState.update { it.copy(allSections = sections, isLoading = false) }
            }
        }
    }

    private fun observeLatestPosition() {
        viewModelScope.launch {
            repository.getAbsoluteLatestReadingPosition().collectLatest { position ->
                val validPosition = position?.let { onlyForSelectedStudyDay(it, it.track) }
                if (validPosition == null) {
                    _uiState.update {
                        it.copy(
                            latestPosition = null,
                            latestChapter = null,
                            latestHalachaTitle = null
                        )
                    }
                    return@collectLatest
                }
                
                val position = validPosition
                when (position.track) {
                    "chumash" -> {
                        val aliyaNum = position.chapterNumber
                        val aliyaName = when (aliyaNum) {
                            1 -> "ראשון"
                            2 -> "שני"
                            3 -> "שלישי"
                            4 -> "רביעי"
                            5 -> "חמישי"
                            6 -> "שישי"
                            7 -> "שביעי"
                            else -> "עליה $aliyaNum"
                        }
                        val title = "חומש · פרשת ${position.chapterId} · $aliyaName · פסוק ${HebrewNumberFormatter.toHebrewNumeral(position.halachaIndex + 1)}׳"
                        _uiState.update {
                            it.copy(
                                latestPosition = position,
                                latestChapter = null,
                                latestHalachaTitle = title
                            )
                        }
                    }
                    "tehillim" -> {
                        val chNum = position.chapterNumber
                        val chHebrew = HebrewNumberFormatter.toHebrewNumeral(chNum)
                        val title = "תהילים · פרק $chHebrew · פסוק ${HebrewNumberFormatter.toHebrewNumeral(position.halachaIndex + 1)}׳"
                        _uiState.update {
                            it.copy(
                                latestPosition = position,
                                latestChapter = null,
                                latestHalachaTitle = title
                            )
                        }
                    }
                    else -> {
                        val chapter = repository.getChapterById(position.chapterId)
                        val title = if (chapter != null) {
                            val hebrewNums = listOf(
                                "", "א׳", "ב׳", "ג׳", "ד׳", "ה׳", "ו׳", "ז׳", "ח׳", "ט׳", "י׳",
                                "י״א", "י״ב", "י״ג", "י״ד", "ט״ו", "ט״ז", "י״ז", "י״ח", "י״ט", "כ׳",
                                "כ״א", "כ״ב", "כ״ג", "כ״ד", "כ״ה", "כ״ו", "כ״ז", "כ״ח", "כ״ט", "ל׳"
                            )
                            val idx = position.halachaIndex + 1
                            val hNum = if (idx in 1 until hebrewNums.size) hebrewNums[idx] else "$idx"
                            "הלכה $hNum"
                        } else null
                        _uiState.update {
                            it.copy(
                                latestPosition = position,
                                latestChapter = chapter,
                                latestHalachaTitle = title
                            )
                        }
                    }
                }
            }
        }
    }

    fun checkAndUpdateCurrentDate(forceUpdate: Boolean = false) {
        val prefs = _uiState.value.preferences
        val newEffectiveToday = calculateEffectiveToday(prefs)
        val oldEffectiveToday = _uiState.value.effectiveToday
        val isTrackingToday = _uiState.value.selectedStudyDate == oldEffectiveToday

        if (forceUpdate || newEffectiveToday != oldEffectiveToday || (isTrackingToday && _uiState.value.selectedStudyDate != newEffectiveToday)) {
            val targetDate = if (isTrackingToday || forceUpdate) newEffectiveToday else _uiState.value.selectedStudyDate
            val israelZone = ZoneId.of("Asia/Jerusalem")
            val testInstant = targetDate.atTime(12, 0).atZone(israelZone).toInstant()
            val lesson = repository.getDailyLesson(
                instant = testInstant,
                zoneId = israelZone,
                track = prefs.selectedTrack,
                dayBoundary = prefs.dayBoundary,
                cityName = prefs.selectedCity
            )
            _uiState.update {
                it.copy(
                    effectiveToday = newEffectiveToday,
                    selectedStudyDate = targetDate,
                    viewingDate = targetDate,
                    dailyLesson = lesson
                )
            }
            if (newEffectiveToday != oldEffectiveToday) {
                _uiState.update {
                    it.copy(
                        latestPosition = null,
                        latestChapter = null,
                        latestHalachaTitle = null,
                        latestTehillimPosition = null,
                        latestTanyaPosition = null
                    )
                }
            }
            observeCompletionsForDate(prefs.selectedTrack, lesson.studyDate)
            refreshDailyChumash(targetDate)
            refreshDailyTehillim(targetDate)
            refreshDailyTanya(targetDate)
        }
    }

    fun refreshDailyLesson() {
        val prefs = _uiState.value.preferences
        val effective = calculateEffectiveToday(prefs)
        val oldEffective = _uiState.value.effectiveToday
        val isTrackingToday = _uiState.value.selectedStudyDate == oldEffective
        val selected = if (isTrackingToday) effective else _uiState.value.selectedStudyDate
        val israelZone = ZoneId.of("Asia/Jerusalem")
        val testInstant = selected.atTime(12, 0).atZone(israelZone).toInstant()
        val lesson = repository.getDailyLesson(
            instant = testInstant,
            zoneId = israelZone,
            track = prefs.selectedTrack,
            dayBoundary = prefs.dayBoundary,
            cityName = prefs.selectedCity
        )
        _uiState.update {
            it.copy(
                effectiveToday = effective,
                selectedStudyDate = selected,
                viewingDate = selected,
                dailyLesson = lesson
            )
        }
        observeCompletionsForDate(prefs.selectedTrack, lesson.studyDate)
        refreshDailyChumash(selected)
        refreshDailyTehillim(selected)
        refreshDailyTanya(selected)
    }

    fun setSelectedStudyDate(date: LocalDate) {
        val prefs = _uiState.value.preferences
        val israelZone = ZoneId.of("Asia/Jerusalem")
        val testInstant = date.atTime(12, 0).atZone(israelZone).toInstant()
        val lesson = repository.getDailyLesson(
            instant = testInstant,
            zoneId = israelZone,
            track = prefs.selectedTrack,
            dayBoundary = prefs.dayBoundary,
            cityName = prefs.selectedCity
        )
        _uiState.update {
            it.copy(
                selectedStudyDate = date,
                viewingDate = date,
                dailyLesson = lesson,
                showDatePickerDialog = false
            )
        }
        observeCompletionsForDate(prefs.selectedTrack, lesson.studyDate)
        refreshDailyChumash(date)
        refreshDailyTehillim(date)
        refreshDailyTanya(date)
        if (_uiState.value.activeChapter != null) {
            openDailyLesson(forceStartAtBeginning = true)
        }
    }

    fun refreshDailyTehillim(date: LocalDate = _uiState.value.selectedStudyDate) {
        viewModelScope.launch {
            _uiState.update { it.copy(isTehillimLoading = true) }
            val hebDateStr = _uiState.value.dailyLesson?.hebrewDate ?: "ה׳ בתשרי תשפ״ז"
            val dayNumStr = HebrewDateHelper.extractHebrewDayNumeral(hebDateStr) // e.g. "ה׳"
            val monthName = HebrewDateHelper.extractHebrewMonth(hebDateStr) // e.g. "תשרי"
            
            val dayOfMonth = when (dayNumStr.trim().removeSuffix("׳")) {
                "א" -> 1; "ב" -> 2; "ג" -> 3; "ד" -> 4; "ה" -> 5; "ו" -> 6; "ז" -> 7; "ח" -> 8; "ט" -> 9; "י" -> 10
                "יא" -> 11; "יב" -> 12; "יג" -> 13; "יד" -> 14; "טו" -> 15; "טז" -> 16; "יז" -> 17; "יח" -> 18; "יט" -> 19; "כ" -> 20
                "כא" -> 21; "כב" -> 22; "כג" -> 23; "כד" -> 24; "כה" -> 25; "כו" -> 26; "כז" -> 27; "כח" -> 28; "כט" -> 29; "ל" -> 30
                else -> 1
            }

            val lesson = tehillimRepository.getDailyTehillimLesson(
                dayOfMonth = dayOfMonth,
                hebrewMonthName = monthName,
                hebrewDayOfMonthStr = dayNumStr
            )
            _uiState.update {
                it.copy(
                    dailyTehillim = lesson,
                    isTehillimLoading = false
                )
            }
        }
    }

    fun openTehillimReader(open: Boolean) {
        if (open) {
            viewModelScope.launch {
                val pos = onlyForSelectedStudyDay(repository.getLatestReadingPosition("tehillim").firstOrNull(), "tehillim")
                _uiState.update {
                    it.copy(
                        isTehillimReaderOpen = true,
                        latestTehillimPosition = pos
                    )
                }
            }
        } else {
            _uiState.update {
                it.copy(isTehillimReaderOpen = false)
            }
        }
    }

    fun saveTehillimPosition(verse: com.example.domain.tehillim.TehillimVerse, chapter: TehillimChapter) {
        val track = "tehillim"
        
        // Save using anchor
        readingStateManager.saveAnchor(
            isReaderActive = true,
            track = track,
            chapterId = "chapter_${chapter.chapterNumber}",
            sectionId = "tehillim",
            chapterNumber = chapter.chapterNumber,
            halachaId = "verse_${verse.verseNumber}",
            halachaIndex = verse.verseNumber - 1,
            scrollOffsetFraction = 0f,
            studyDate = selectedStudyDateKey(),
            immediateCommit = true
        )
        
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch(NonCancellable) {
            val position = ReadingPositionEntity(
                track = track,
                chapterId = "chapter_${chapter.chapterNumber}",
                sectionId = "tehillim",
                chapterNumber = chapter.chapterNumber,
                halachaId = "verse_${verse.verseNumber}",
                halachaIndex = verse.verseNumber - 1,
                textOffset = 0,
                scrollOffsetFraction = 0f,
                quoteFingerprint = ""
            )
            repository.saveReadingPosition(position)
        }
    }

    fun refreshDailyTanya(date: LocalDate = _uiState.value.selectedStudyDate) {
        viewModelScope.launch {
            _uiState.update { it.copy(isTanyaLoading = true) }
            val lesson = tanyaRepository.getDailyTanyaLesson(date)
            val pos = onlyForSelectedStudyDay(repository.getLatestReadingPosition("tanya").firstOrNull(), "tanya")
            _uiState.update {
                it.copy(
                    dailyTanya = lesson,
                    isTanyaLoading = false,
                    latestTanyaPosition = pos
                )
            }
        }
    }

    fun openTanyaReader(open: Boolean = true) {
        if (open) {
            viewModelScope.launch {
                val pos = onlyForSelectedStudyDay(repository.getLatestReadingPosition("tanya").firstOrNull(), "tanya")
                _uiState.update {
                    it.copy(
                        isTanyaReaderOpen = true,
                        latestTanyaPosition = pos
                    )
                }
            }
        } else {
            _uiState.update {
                it.copy(isTanyaReaderOpen = false)
            }
        }
    }

    fun saveTanyaPosition(section: TanyaSection, lesson: DailyTanyaLesson) {
        val track = "tanya"
        readingStateManager.saveAnchor(
            isReaderActive = true,
            track = track,
            chapterId = lesson.fullRef,
            sectionId = lesson.bookTitle,
            chapterNumber = 0,
            halachaId = "section_${section.sectionIndex}",
            halachaIndex = section.sectionIndex - 1,
            scrollOffsetFraction = 0f,
            studyDate = selectedStudyDateKey(),
            immediateCommit = true
        )
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch(NonCancellable) {
            val position = ReadingPositionEntity(
                track = track,
                chapterId = lesson.fullRef,
                sectionId = lesson.bookTitle,
                chapterNumber = 0,
                halachaId = "section_${section.sectionIndex}",
                halachaIndex = section.sectionIndex - 1,
                textOffset = 0,
                scrollOffsetFraction = 0f,
                quoteFingerprint = ""
            )
            repository.saveReadingPosition(position)
            _uiState.update {
                it.copy(
                    latestPosition = position,
                    latestTanyaPosition = position
                )
            }
        }
    }

    fun toggleTanyaCompletion() {
        val currentLesson = _uiState.value.dailyTanya ?: return
        val newStatus = !currentLesson.isCompleted
        _uiState.update {
            it.copy(dailyTanya = currentLesson.copy(isCompleted = newStatus))
        }
    }

    fun refreshDailyChumash(date: LocalDate = _uiState.value.selectedStudyDate) {
        viewModelScope.launch {
            _uiState.update { it.copy(isChumashLoading = true) }
            val completed = _uiState.value.preferences.completedAliyot
            val lesson = chumashRepository.getDailyChumashLesson(date, completed)
            val pos = onlyForSelectedStudyDay(repository.getLatestReadingPosition("chumash").firstOrNull(), "chumash")
            val activeParasha = lesson?.parashaName ?: ""
            val isSameParasha = pos != null && pos.chapterId == activeParasha
            val targetAliya = lesson?.currentAliyaIndex ?: 1
            _uiState.update {
                it.copy(
                    dailyChumash = lesson,
                    isChumashLoading = false,
                    selectedChumashAliyaIndex = targetAliya,
                    latestPosition = if (isSameParasha) pos else null
                )
            }
        }
    }

    fun openChumashReader(aliyaIndex: Int? = null) {
        viewModelScope.launch {
            val lesson = _uiState.value.dailyChumash
            val target = aliyaIndex ?: lesson?.currentAliyaIndex ?: 1
            val pos = onlyForSelectedStudyDay(repository.getLatestReadingPosition("chumash").firstOrNull(), "chumash")
            val activeParasha = lesson?.parashaName ?: ""
            val isSameParasha = pos != null && pos.chapterId == activeParasha
            _uiState.update {
                it.copy(
                    isChumashReaderOpen = true,
                    selectedChumashAliyaIndex = target,
                    latestPosition = if (isSameParasha) pos else null
                )
            }
        }
    }

    fun saveChumashPosition(verse: ChumashVerse, aliya: ChumashAliya) {
        val lesson = _uiState.value.dailyChumash ?: return
        val track = "chumash"
        
        // Save using anchor
        readingStateManager.saveAnchor(
            isReaderActive = true,
            track = track,
            chapterId = lesson.parashaName,
            sectionId = "aliya_${aliya.aliyaIndex}",
            chapterNumber = aliya.aliyaIndex,
            halachaId = "verse_${verse.verseNumber}",
            halachaIndex = verse.verseNumber - 1,
            scrollOffsetFraction = 0f,
            studyDate = selectedStudyDateKey(),
            immediateCommit = true
        )
        
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch(NonCancellable) {
            val position = ReadingPositionEntity(
                track = track,
                chapterId = lesson.parashaName,
                sectionId = "aliya_${aliya.aliyaIndex}",
                chapterNumber = aliya.aliyaIndex,
                halachaId = "verse_${verse.verseNumber}",
                halachaIndex = verse.verseNumber - 1,
                textOffset = 0,
                scrollOffsetFraction = 0f,
                quoteFingerprint = ""
            )
            repository.saveReadingPosition(position)
        }
    }

    fun resumeChumashReading() {
        viewModelScope.launch {
            val pos = onlyForSelectedStudyDay(repository.getLatestReadingPosition("chumash").firstOrNull(), "chumash")
            if (pos != null && _uiState.value.dailyChumash?.parashaName == pos.chapterId) {
                openChumashReader(pos.chapterNumber)
            } else {
                openChumashReader()
            }
        }
    }

    fun closeChumashReader() {
        _uiState.update { it.copy(isChumashReaderOpen = false) }
    }

    fun selectChumashAliya(index: Int) {
        _uiState.update { it.copy(selectedChumashAliyaIndex = index) }
    }

    fun toggleChumashShowTeamim() {
        viewModelScope.launch {
            val current = _uiState.value.preferences.chumashShowTeamim
            prefsRepository.updateChumashShowTeamim(!current)
        }
    }

    fun changeChumashFontSize(delta: Float) {
        viewModelScope.launch {
            val current = _uiState.value.preferences.chumashFontSizeSp
            prefsRepository.updateChumashFontSize(current + delta)
        }
    }

    fun toggleChumashAliyaCompleted(parashaName: String, aliyaIndex: Int) {
        viewModelScope.launch {
            prefsRepository.toggleAliyaCompletion(parashaName, aliyaIndex)
            val updatedKeys = _uiState.value.preferences.completedAliyot.toMutableSet()
            val key = "$parashaName-$aliyaIndex"
            if (updatedKeys.contains(key)) updatedKeys.remove(key) else updatedKeys.add(key)
            val lesson = chumashRepository.getDailyChumashLesson(_uiState.value.selectedStudyDate, updatedKeys)
            _uiState.update { it.copy(dailyChumash = lesson) }
        }
    }

    fun stepSelectedDate(deltaDays: Long) {
        setSelectedStudyDate(_uiState.value.selectedStudyDate.plusDays(deltaDays))
    }

    fun returnToToday() {
        setSelectedStudyDate(_uiState.value.effectiveToday)
    }

    fun toggleDateDrawer() {
        _uiState.update {
            it.copy(
                dateDrawerState = if (it.dateDrawerState == DateDrawerState.EXPANDED)
                    DateDrawerState.COLLAPSED else DateDrawerState.EXPANDED
            )
        }
    }

    fun getHebrewDateForDate(date: LocalDate): String {
        return scheduleEngine.getHebrewDate(date)
    }

    fun setDateDrawerState(state: DateDrawerState) {
        _uiState.update { it.copy(dateDrawerState = state) }
    }

    fun selectHomeTab(tab: HomeTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun setShowEditStudySheet(show: Boolean) {
        _uiState.update { it.copy(showEditStudySheet = show) }
    }

    fun toggleStudyVisibility(studyKey: String) {
        viewModelScope.launch {
            val current = _uiState.value.preferences.visibleStudies.toMutableSet()
            if (current.contains(studyKey)) {
                if (current.size > 1) {
                    current.remove(studyKey)
                }
            } else {
                current.add(studyKey)
            }
            prefsRepository.updateVisibleStudies(current)
        }
    }

    fun setViewingDate(date: LocalDate) {
        setSelectedStudyDate(date)
    }

    fun getLessonForDate(date: LocalDate): DailyLessonResult? {
        val prefs = _uiState.value.preferences
        val israelZone = ZoneId.of("Asia/Jerusalem")
        val testInstant = date.atTime(12, 0).atZone(israelZone).toInstant()
        return repository.getDailyLesson(
            instant = testInstant,
            zoneId = israelZone,
            track = prefs.selectedTrack,
            dayBoundary = prefs.dayBoundary,
            cityName = prefs.selectedCity
        )
    }

    fun openDailyLesson(forceStartAtBeginning: Boolean = false) {
        val lesson = _uiState.value.dailyLesson ?: return
        val chapters = lesson.chapters
        if (chapters.isEmpty()) return

        val completedIds = _uiState.value.completedChapterIds
        val latestPos = _uiState.value.latestPosition
        
        // Only resume if not forced to start at beginning AND selected date is actually today
        val isToday = _uiState.value.selectedStudyDate == _uiState.value.effectiveToday
        val shouldResume = !forceStartAtBeginning && isToday

        // If user was currently reading a chapter from this lesson, resume it
        val matchingLatest = if (shouldResume && latestPos != null) {
            chapters.firstOrNull { "${it.sectionId}_${it.chapterNumber}" == latestPos.chapterId }
        } else null

        val uncompleted = chapters.firstOrNull { ch ->
            val chId = "${ch.sectionId}_${ch.chapterNumber}"
            !completedIds.contains(chId)
        }

        val target = matchingLatest ?: uncompleted ?: chapters.first()
        val chId = "${target.sectionId}_${target.chapterNumber}"
        val targetIndex = if (target == matchingLatest && latestPos != null) latestPos.halachaIndex else -1
        val targetOffset = if (target == matchingLatest && latestPos != null) latestPos.textOffset else 0
        openChapter(chId, targetIndex, targetOffset)
    }

    fun selectDateAndOpenLesson(date: LocalDate) {
        setViewingDate(date)
        openDailyLesson()
    }

    fun resumeLastReading() {
        val pos = _uiState.value.latestPosition
        if (pos != null) {
            when (pos.track) {
                "chumash" -> {
                    openChumashReader(pos.chapterNumber)
                }
                "tehillim" -> {
                    openTehillimReader(true)
                }
                "tanya" -> {
                    openTanyaReader(true)
                }
                else -> {
                    openChapter(pos.chapterId, pos.halachaIndex, pos.textOffset)
                }
            }
        } else {
            openDailyLesson()
        }
    }

    fun openChapter(
        chapterId: String,
        targetHalachaIndex: Int = -1,
        targetTextOffset: Int = 0
    ) {
        viewModelScope.launch {
            val chapter = repository.getChapterById(chapterId)
            if (chapter == null) {
                _uiState.update { it.copy(noticeMessage = "התוכן למסלול זה אינו זמין כעת") }
                return@launch
            }
            val halachot = repository.getHalachotForChapterList(chapterId)
            val section = _uiState.value.allSections.find { it.sectionId == chapter.sectionId }

            // Continuous multi-chapter lesson support
            val dailyLesson = _uiState.value.dailyLesson
            val isPartOfLesson = dailyLesson?.chapters?.any { "${it.sectionId}_${it.chapterNumber}" == chapterId } == true

            val chaptersList = mutableListOf<ReaderChapterData>()
            if (isPartOfLesson && dailyLesson != null && dailyLesson.chapters.size > 1) {
                for (sch in dailyLesson.chapters) {
                    val chId = "${sch.sectionId}_${sch.chapterNumber}"
                    val chEntity = repository.getChapterById(chId)
                    if (chEntity != null) {
                        val chHalachot = repository.getHalachotForChapterList(chId)
                        val sec = _uiState.value.allSections.find { it.sectionId == chEntity.sectionId }
                        val isDone = _uiState.value.completedChapterIds.contains(chId)
                        chaptersList.add(
                            ReaderChapterData(
                                chapter = chEntity,
                                section = sec,
                                halachot = chHalachot,
                                isCompleted = isDone
                            )
                        )
                    }
                }
            } else {
                chaptersList.add(
                    ReaderChapterData(
                        chapter = chapter,
                        section = section,
                        halachot = halachot,
                        isCompleted = _uiState.value.completedChapterIds.contains(chapterId)
                    )
                )
            }

            val track = _uiState.value.preferences.selectedTrack
            val savedPos = onlyForSelectedStudyDay(
                repository.getReadingPosition(track, chapterId),
                track
            )
            val anchor = readingStateManager.getSavedAnchor()
                ?.takeIf { it.studyDate == selectedStudyDateKey() && it.track == track }
            val effectiveSavedPos = savedPos ?: if (anchor != null && anchor.chapterId == chapterId) {
                ReadingPositionEntity(
                    track = track,
                    chapterId = chapterId,
                    sectionId = chapter.sectionId,
                    chapterNumber = chapter.chapterNumber,
                    halachaId = anchor.halachaId,
                    halachaIndex = anchor.halachaIndex,
                    textOffset = 0,
                    scrollOffsetFraction = anchor.scrollOffsetFraction,
                    quoteFingerprint = halachot.getOrNull(anchor.halachaIndex)?.quoteFingerprint ?: "",
                    contentVersion = "2026.09",
                    updatedAt = anchor.updatedAt
                )
            } else null

            val posToUse = if (targetHalachaIndex >= 0 && targetHalachaIndex in halachot.indices) {
                effectiveSavedPos?.copy(halachaIndex = targetHalachaIndex, textOffset = targetTextOffset) ?: ReadingPositionEntity(
                    track = track,
                    chapterId = chapterId,
                    sectionId = chapter.sectionId,
                    chapterNumber = chapter.chapterNumber,
                    halachaId = halachot.getOrNull(targetHalachaIndex)?.id ?: "${chapterId}_${targetHalachaIndex + 1}",
                    halachaIndex = targetHalachaIndex,
                    textOffset = targetTextOffset,
                    scrollOffsetFraction = 0f,
                    quoteFingerprint = halachot.getOrNull(targetHalachaIndex)?.quoteFingerprint ?: "",
                    contentVersion = "2026.09",
                    updatedAt = System.currentTimeMillis()
                )
            } else {
                effectiveSavedPos
            }

            val targetHalachaIdx = posToUse?.halachaIndex ?: 0
            val targetHalachaId = halachot.getOrNull(targetHalachaIdx)?.id ?: "${chapterId}_${targetHalachaIdx + 1}"

            // Persist to low-level synchronous storage immediately
            readingStateManager.saveAnchor(
                isReaderActive = true,
                track = track,
                chapterId = chapterId,
                sectionId = chapter.sectionId,
                chapterNumber = chapter.chapterNumber,
                halachaId = targetHalachaId,
                halachaIndex = targetHalachaIdx,
                scrollOffsetFraction = posToUse?.scrollOffsetFraction ?: 0f,
                studyDate = selectedStudyDateKey(),
                immediateCommit = true
            )

            // Check completion
            val completionFlow = repository.getCompletion(track, chapterId)
            viewModelScope.launch {
                completionFlow.collectLatest { completion ->
                    _uiState.update { it.copy(isCurrentChapterCompleted = completion != null) }
                }
            }

            prefsRepository.setLastOpenedChapterId(chapterId)

            val halachaLetter = HebrewNumberFormatter.toHebrewNumeral(targetHalachaIdx + 1)
            _uiState.update {
                it.copy(
                    activeChapter = chapter,
                    activeSection = section,
                    activeHalachot = halachot,
                    activeChaptersList = chaptersList,
                    activeReadingPosition = posToUse,
                    latestPosition = posToUse ?: it.latestPosition,
                    latestChapter = chapter,
                    latestHalachaTitle = "${chapter.chapterHebrew} · הלכה $halachaLetter"
                )
            }
        }
    }

    fun closeReader() {
        autoSaveJob?.cancel()
        readingStateManager.setReaderActive(false)
        _uiState.update {
            it.copy(
                activeChapter = null,
                activeSection = null,
                activeHalachot = emptyList(),
                activeChaptersList = emptyList(),
                activeReadingPosition = null
            )
        }
    }

    fun markAllLessonChaptersCompleted() {
        viewModelScope.launch {
            val chapters = _uiState.value.activeChaptersList.map { it.chapter }
            val dateStr = _uiState.value.dailyLesson?.studyDate ?: DateTimeFormatter.ISO_LOCAL_DATE.format(_uiState.value.selectedStudyDate)
            val track = _uiState.value.preferences.selectedTrack
            chapters.forEach { ch ->
                repository.setChapterCompleted(track, ch.id, dateStr)
            }
        }
    }

    /**
     * Debounced atomic save of reading position anchor:
     * {workId, sectionId, chapter, halachaId, textOffset, scrollOffsetFraction}
     */
    fun onScrollPositionChanged(
        chapterId: String,
        halachaIndex: Int,
        scrollOffsetFraction: Float
    ) {
        val chapter = _uiState.value.activeChaptersList.find { it.chapter.id == chapterId }?.chapter
            ?: _uiState.value.activeChapter ?: return
        val halachot = _uiState.value.activeChaptersList.find { it.chapter.id == chapterId }?.halachot
            ?: _uiState.value.activeHalachot
        if (halachot.isEmpty()) return
        val safeIndex = halachaIndex.coerceIn(0, halachot.lastIndex)
        val halacha = halachot[safeIndex]
        val track = _uiState.value.preferences.selectedTrack

        // Synchronous disk write of anchor to ensure survival of sudden kill
        readingStateManager.saveAnchor(
            isReaderActive = true,
            track = track,
            chapterId = chapter.id,
            sectionId = chapter.sectionId,
            chapterNumber = chapter.chapterNumber,
            halachaId = halacha.id,
            halachaIndex = safeIndex,
            scrollOffsetFraction = scrollOffsetFraction,
            studyDate = selectedStudyDateKey(),
            immediateCommit = true
        )

        val halachaLetter = HebrewNumberFormatter.toHebrewNumeral(safeIndex + 1)
        _uiState.update {
            it.copy(
                latestHalachaTitle = "${chapter.chapterHebrew} · הלכה $halachaLetter",
                latestChapter = chapter,
                activeChapter = chapter
            )
        }

        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch(NonCancellable) {
            val position = ReadingPositionEntity(
                track = track,
                chapterId = chapter.id,
                sectionId = chapter.sectionId,
                chapterNumber = chapter.chapterNumber,
                halachaId = halacha.id,
                halachaIndex = safeIndex,
                textOffset = 0,
                scrollOffsetFraction = scrollOffsetFraction,
                quoteFingerprint = halacha.quoteFingerprint,
                contentVersion = "2026.09",
                updatedAt = System.currentTimeMillis()
            )
            repository.saveReadingPosition(position)
            _uiState.update {
                it.copy(
                    activeReadingPosition = position,
                    latestPosition = position
                )
            }
        }
    }

    fun onScrollPositionChanged(
        halachaIndex: Int,
        scrollOffsetFraction: Float
    ) {
        val currentChId = _uiState.value.activeChapter?.id ?: return
        onScrollPositionChanged(currentChId, halachaIndex, scrollOffsetFraction)
    }

    /**
     * Immediate (non-debounced) save of reading position anchor,
     * invoked on lifecycle pause/stop/dispose to ensure zero position loss.
     */
    fun saveReadingPositionImmediate(
        chapterId: String,
        halachaIndex: Int,
        scrollOffsetFraction: Float
    ) {
        val chapter = _uiState.value.activeChaptersList.find { it.chapter.id == chapterId }?.chapter
            ?: _uiState.value.activeChapter ?: return
        val halachot = _uiState.value.activeChaptersList.find { it.chapter.id == chapterId }?.halachot
            ?: _uiState.value.activeHalachot
        if (halachot.isEmpty()) return
        val safeIndex = halachaIndex.coerceIn(0, halachot.lastIndex)
        val halacha = halachot[safeIndex]
        val track = _uiState.value.preferences.selectedTrack

        // Synchronous disk write with commit() - guaranteed before process termination
        readingStateManager.saveAnchor(
            isReaderActive = true,
            track = track,
            chapterId = chapter.id,
            sectionId = chapter.sectionId,
            chapterNumber = chapter.chapterNumber,
            halachaId = halacha.id,
            halachaIndex = safeIndex,
            scrollOffsetFraction = scrollOffsetFraction,
            studyDate = selectedStudyDateKey(),
            immediateCommit = true
        )

        val halachaLetter = HebrewNumberFormatter.toHebrewNumeral(safeIndex + 1)
        _uiState.update {
            it.copy(
                latestHalachaTitle = "${chapter.chapterHebrew} · הלכה $halachaLetter",
                latestChapter = chapter,
                activeChapter = chapter
            )
        }

        autoSaveJob?.cancel()
        viewModelScope.launch(NonCancellable) {
            val position = ReadingPositionEntity(
                track = track,
                chapterId = chapter.id,
                sectionId = chapter.sectionId,
                chapterNumber = chapter.chapterNumber,
                halachaId = halacha.id,
                halachaIndex = safeIndex,
                textOffset = 0,
                scrollOffsetFraction = scrollOffsetFraction,
                quoteFingerprint = halacha.quoteFingerprint,
                contentVersion = "2026.09",
                updatedAt = System.currentTimeMillis()
            )
            repository.saveReadingPosition(position)
            _uiState.update {
                it.copy(
                    activeReadingPosition = position,
                    latestPosition = position
                )
            }
        }
    }

    fun saveReadingPositionImmediate(
        halachaIndex: Int,
        scrollOffsetFraction: Float
    ) {
        val currentChId = _uiState.value.activeChapter?.id ?: return
        saveReadingPositionImmediate(currentChId, halachaIndex, scrollOffsetFraction)
    }

    fun markChapterCompleted(chapterId: String) {
        viewModelScope.launch {
            val track = _uiState.value.preferences.selectedTrack
            val studyDate = _uiState.value.dailyLesson?.studyDate ?: LocalDate.now().toString()
            if (_uiState.value.isCurrentChapterCompleted) {
                repository.removeChapterCompletion(track, chapterId)
            } else {
                repository.setChapterCompleted(track, chapterId, studyDate)
            }
        }
    }

    fun goToNextChapter() {
        val active = _uiState.value.activeChapter ?: return
        viewModelScope.launch {
            val next = repository.getNextChapter(active.id)
            if (next != null) {
                openChapter(next.id)
            } else {
                _uiState.update { it.copy(noticeMessage = "הגעת לסוף משנה תורה!") }
            }
        }
    }

    fun goToPreviousChapter() {
        val active = _uiState.value.activeChapter ?: return
        viewModelScope.launch {
            val prev = repository.getPreviousChapter(active.id)
            if (prev != null) {
                openChapter(prev.id)
            }
        }
    }

    fun selectTrack(track: String) {
        viewModelScope.launch {
            prefsRepository.updateTrack(track)
        }
    }

    fun setDayBoundary(boundary: String) {
        viewModelScope.launch {
            prefsRepository.updateDayBoundary(boundary)
        }
    }

    fun setCity(city: String) {
        viewModelScope.launch {
            prefsRepository.updateCity(city)
        }
    }

    fun setFontSize(sizeSp: Float) {
        viewModelScope.launch {
            prefsRepository.updateFontSize(sizeSp)
        }
    }

    fun setLineSpacing(multiplier: Float) {
        viewModelScope.launch {
            prefsRepository.updateLineSpacing(multiplier)
        }
    }

    fun setFontFamily(family: String) {
        viewModelScope.launch {
            prefsRepository.updateFontFamily(family)
        }
    }

    fun toggleShowNikud(show: Boolean) {
        viewModelScope.launch {
            prefsRepository.updateShowNikud(show)
        }
    }

    fun setReaderTheme(theme: String) {
        viewModelScope.launch {
            prefsRepository.updateReaderTheme(theme)
        }
    }

    fun setKeepScreenOn(keep: Boolean) {
        viewModelScope.launch {
            prefsRepository.updateKeepScreenOn(keep)
        }
    }

    fun setShowAttribution(show: Boolean) {
        _uiState.update { it.copy(showAttributionDialog = show) }
    }

    fun setShowSettings(show: Boolean) {
        _uiState.update { it.copy(showSettingsDialog = show) }
    }

    fun setShowDatePicker(show: Boolean) {
        _uiState.update { it.copy(showDatePickerDialog = show) }
    }

    fun clearNotice() {
        _uiState.update { it.copy(noticeMessage = null) }
    }
}
