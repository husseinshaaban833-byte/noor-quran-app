package com.example.ui

import android.app.Application
import android.content.Context
import android.location.LocationManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.QuranAiAssistantRepository
import com.example.ai.RagCitation
import com.example.audio.ActiveDownloadProgress
import com.example.audio.AudioPlayerState
import com.example.audio.DownloadRepository
import com.example.audio.QuranAudioController
import com.example.audio.RadioStation
import com.example.audio.Reciter
import com.example.cloud.CloudSyncRepository
import com.example.cloud.authStateFlow
import com.example.core.PrefsDataStore
import com.example.core.UserPreferences
import com.example.data.local.AdhkarProgressEntity
import com.example.data.local.BookmarkEntity
import com.example.data.local.DownloadedItemEntity
import com.example.data.local.KhatmahPlanEntity
import com.example.data.local.MemorizationPlanEntity
import com.example.data.local.NoorDatabase
import com.example.data.local.RecitationAttemptEntity
import com.example.data.local.TasbeehLogEntity
import com.example.islamic.AsrMadhab
import com.example.islamic.CalculationMethod
import com.example.islamic.CityOption
import com.example.islamic.DailyPrayerSchedule
import com.example.islamic.DhikrItem
import com.example.islamic.HijriCalendarHelper
import com.example.islamic.HijriDateInfo
import com.example.islamic.IslamicRepository
import com.example.islamic.PrayerTimesCalculator
import com.example.islamic.QiblaCalculator
import com.example.memorization.RecitationAnalysisResult
import com.example.memorization.RecitationAnalyzer
import com.example.memorization.SpacedRepetitionEngine
import com.example.quran.Ayah
import com.example.quran.DuaKhatm
import com.example.quran.HizbInfo
import com.example.quran.JuzInfo
import com.example.quran.MadinahPage
import com.example.quran.QuranIntegrityReport
import com.example.quran.QuranRepository
import com.example.quran.QuranTopic
import com.example.quran.SababAlNuzul
import com.example.quran.Surah
import com.example.quran.SurahVirtueHadith
import com.example.quran.WordMeaning
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

sealed interface AiUiState {
    data object Idle : AiUiState
    data object Loading : AiUiState
    data class Streaming(val text: String, val citations: List<RagCitation>) : AiUiState
    data class Success(val text: String, val citations: List<RagCitation>) : AiUiState
    data class Error(val message: String) : AiUiState
}

data class CloudUiState(
    val isBusy: Boolean = false,
    val statusMessage: String? = null,
    val errorMessage: String? = null
)

class NoorViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val db = NoorDatabase.getInstance(appContext)
    private val dao = db.noorDao()
    val prefsStore = PrefsDataStore(appContext)
    val quranRepository = QuranRepository(appContext)
    val islamicRepository = IslamicRepository(appContext)
    val audioController = QuranAudioController(appContext)
    val downloadRepository = DownloadRepository(appContext, dao, quranRepository)
    private val aiRepository = QuranAiAssistantRepository(quranRepository)
    val cloudSyncRepository = CloudSyncRepository(appContext, dao, prefsStore)

    private val todayKey: String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private val _isQuranReady = MutableStateFlow(false)
    val isQuranReady: StateFlow<Boolean> = _isQuranReady.asStateFlow()

    private val _integrityReport = MutableStateFlow<QuranIntegrityReport?>(null)
    val integrityReport: StateFlow<QuranIntegrityReport?> = _integrityReport.asStateFlow()

    val prefs: StateFlow<UserPreferences> = prefsStore.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserPreferences())

    val bookmarks: StateFlow<List<BookmarkEntity>> = dao.observeBookmarks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val memorizationPlans: StateFlow<List<MemorizationPlanEntity>> = dao.observeMemorizationPlans()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recitationAttempts: StateFlow<List<RecitationAttemptEntity>> = dao.observeRecitationAttempts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val khatmahPlans: StateFlow<List<KhatmahPlanEntity>> = dao.observeKhatmahPlans()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloads: StateFlow<List<DownloadedItemEntity>> = downloadRepository.downloadedItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeDownloads: StateFlow<Map<String, ActiveDownloadProgress>> = downloadRepository.activeDownloads

    val tasbeehLogs: StateFlow<List<TasbeehLogEntity>> = dao.observeTasbeehLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val adhkarProgress: StateFlow<List<AdhkarProgressEntity>> = dao.observeAdhkarProgress(todayKey)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val audioState: StateFlow<AudioPlayerState> = audioController.state

    val currentUser: StateFlow<FirebaseUser?> = Firebase.auth.authStateFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Firebase.auth.currentUser)

    private val _aiState = MutableStateFlow<AiUiState>(AiUiState.Idle)
    val aiState: StateFlow<AiUiState> = _aiState.asStateFlow()

    private val _cloudState = MutableStateFlow(CloudUiState())
    val cloudState: StateFlow<CloudUiState> = _cloudState.asStateFlow()

    private val _lastRecitationResult = MutableStateFlow<RecitationAnalysisResult?>(null)
    val lastRecitationResult: StateFlow<RecitationAnalysisResult?> = _lastRecitationResult.asStateFlow()

    private val _showDuaKhatmPrompt = MutableStateFlow(false)
    val showDuaKhatmPrompt: StateFlow<Boolean> = _showDuaKhatmPrompt.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Ayah>>(emptyList())
    val searchResults: StateFlow<List<Ayah>> = _searchResults.asStateFlow()

    private val _selectedReaderPage = MutableStateFlow(1)
    val selectedReaderPage: StateFlow<Int> = _selectedReaderPage.asStateFlow()

    private val _selectedSurahForContinuous = MutableStateFlow(1)
    val selectedSurahForContinuous: StateFlow<Int> = _selectedSurahForContinuous.asStateFlow()

    private val _highlightedAyahKey = MutableStateFlow<String?>(null)
    val highlightedAyahKey: StateFlow<String?> = _highlightedAyahKey.asStateFlow()

    val prayerSchedule: StateFlow<DailyPrayerSchedule> = prefs
        .combine(_isQuranReady) { p, _ ->
            val cal = Calendar.getInstance()
            PrayerTimesCalculator.calculate(
                year = cal.get(Calendar.YEAR),
                month = cal.get(Calendar.MONTH) + 1,
                day = cal.get(Calendar.DAY_OF_MONTH),
                lat = p.cityLat,
                lon = p.cityLon,
                timezoneHours = p.cityTz,
                method = CalculationMethod.fromId(p.calculationMethod),
                madhab = AsrMadhab.fromId(p.asrMadhab)
            )
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            PrayerTimesCalculator.calculate(
                2026, 11, 24, 21.4225, 39.8262, 3.0,
                CalculationMethod.UMM_AL_QURA, AsrMadhab.SHAFII
            )
        )

    val hijriDate: StateFlow<HijriDateInfo> = prefs
        .combine(_isQuranReady) { p, _ -> HijriCalendarHelper.today(p.hijriOffsetDays) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, HijriCalendarHelper.today(0))

    init {
        viewModelScope.launch {
            quranRepository.ensureLoaded()
            val initialPrefs = prefsStore.preferencesFlow.first()
            _selectedReaderPage.value = initialPrefs.lastPage
            _selectedSurahForContinuous.value = initialPrefs.lastSurah
            _isQuranReady.value = true
            _integrityReport.value = quranRepository.verifyIntegrity()
            audioController.connect()
        }
    }

    fun getSurahs(): List<Surah> = quranRepository.getSurahs()
    fun getSurah(n: Int): Surah? = quranRepository.getSurah(n)
    fun getPage(page: Int): MadinahPage? = quranRepository.getPage(page)
    fun getJuzList(): List<JuzInfo> = quranRepository.getJuzList()
    fun getHizbList(): List<HizbInfo> = quranRepository.getHizbList()
    fun getTopics(): List<QuranTopic> = quranRepository.getCuratedTopics()
    fun getReciters(): List<Reciter> = islamicRepository.getReciters()
    fun getRadioStations(): List<RadioStation> = islamicRepository.getRadioStations()
    fun getCities(): List<CityOption> = islamicRepository.getCities()
    fun getAdhkar(): List<DhikrItem> = islamicRepository.getAdhkar()
    fun getSurahVirtues(): List<SurahVirtueHadith> = quranRepository.getSurahVirtues()
    fun getVirtuesDisclaimer(): String = quranRepository.getVirtuesDisclaimer()
    fun getDuaKhatm(): DuaKhatm? = quranRepository.getDuaKhatm()
    fun getAllAsbab(): List<SababAlNuzul> = quranRepository.getAllAsbab()
    fun getAsbabForAyah(s: Int, a: Int): SababAlNuzul? = quranRepository.getAsbabForAyah(s, a)
    fun getTafsirMuyassar(s: Int, a: Int): String = quranRepository.getTafsirMuyassar(s, a)
    fun getDownloadedTafsir(id: String, s: Int, a: Int): String? = quranRepository.getDownloadedTafsir(id, s, a)
    fun getAvailableDownloadedTafsirs(): Set<String> = quranRepository.getAvailableDownloadedTafsirs()
    fun getEnglishTranslation(s: Int, a: Int): String = quranRepository.getEnglishTranslation(s, a)
    fun getWordMeanings(s: Int, a: Int): List<WordMeaning> = quranRepository.getWordMeanings(s, a)

    fun getAyahOfTheDay(): Ayah? {
        val day = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        return quranRepository.getAyahOfTheDay(day)
    }

    fun getCurrentReciter(): Reciter {
        val list = getReciters()
        return list.firstOrNull { it.id == prefs.value.selectedReciterId } ?: list.first()
    }

    fun getQiblaBearing(): Double =
        QiblaCalculator.calculateBearingDegrees(prefs.value.cityLat, prefs.value.cityLon)

    fun openPage(pageNumber: Int, highlightKey: String? = null) {
        val clamped = pageNumber.coerceIn(1, 604)
        _selectedReaderPage.value = clamped
        _highlightedAyahKey.value = highlightKey
        val page = quranRepository.getPage(clamped)
        val firstAyah = page?.ayahs?.firstOrNull()
        if (firstAyah != null) {
            _selectedSurahForContinuous.value = firstAyah.surahNumber
            viewModelScope.launch {
                prefsStore.updateLastPosition(firstAyah.surahNumber, firstAyah.ayahNumber, clamped)
            }
        }
    }

    fun openSurah(surahNumber: Int, ayahNumber: Int = 1) {
        val surah = quranRepository.getSurah(surahNumber) ?: return
        val ayah = surah.ayahs.getOrNull(ayahNumber - 1) ?: surah.ayahs.firstOrNull() ?: return
        _selectedSurahForContinuous.value = surahNumber
        _selectedReaderPage.value = ayah.pageNumber
        _highlightedAyahKey.value = ayah.key
        viewModelScope.launch {
            prefsStore.updateLastPosition(surahNumber, ayah.ayahNumber, ayah.pageNumber)
        }
    }

    fun saveLastReadAyah(ayah: Ayah) {
        _highlightedAyahKey.value = ayah.key
        viewModelScope.launch {
            prefsStore.updateLastPosition(ayah.surahNumber, ayah.ayahNumber, ayah.pageNumber)
        }
    }

    fun addBookmark(ayah: Ayah, customLabel: String) {
        viewModelScope.launch {
            val label = customLabel.trim().ifBlank { "سورة ${ayah.surahNameAr} • الآية ${ayah.ayahNumber}" }
            dao.insertBookmark(
                BookmarkEntity(
                    surahNumber = ayah.surahNumber,
                    ayahNumber = ayah.ayahNumber,
                    pageNumber = ayah.pageNumber,
                    label = label
                )
            )
        }
    }

    fun deleteBookmark(id: Long) {
        viewModelScope.launch { dao.deleteBookmarkById(id) }
    }

    // Audio actions
    fun playAyah(ayah: Ayah, repeatCount: Int = prefs.value.repeatCount) {
        val surahAyahs = quranRepository.getSurah(ayah.surahNumber)?.ayahs.orEmpty()
        val idx = surahAyahs.indexOfFirst { it.ayahNumber == ayah.ayahNumber }.coerceAtLeast(0)
        audioController.playAyahRange(
            ayahs = surahAyahs,
            startIndex = idx,
            reciter = getCurrentReciter(),
            repeatCount = repeatCount,
            speed = prefs.value.playbackSpeed,
            autoAdvance = prefs.value.autoAdvanceAyah
        )
    }

    fun playSurah(surahNumber: Int, reciter: Reciter = getCurrentReciter()) {
        val surah = quranRepository.getSurah(surahNumber) ?: return
        viewModelScope.launch { prefsStore.updateAudioSettings(reciterId = reciter.id) }
        audioController.playSurahFileOrStream(surah.number, surah.nameAr, reciter, surah.ayahs)
    }

    fun playAyahRange(surahNumber: Int, fromAyah: Int, toAyah: Int) {
        val surah = quranRepository.getSurah(surahNumber) ?: return
        val subset = surah.ayahs.filter { it.ayahNumber in fromAyah..toAyah }
        if (subset.isNotEmpty()) {
            audioController.playAyahRange(
                ayahs = subset,
                startIndex = 0,
                reciter = getCurrentReciter(),
                repeatCount = prefs.value.repeatCount,
                speed = prefs.value.playbackSpeed,
                autoAdvance = true
            )
        }
    }

    fun selectReciter(reciter: Reciter) {
        viewModelScope.launch { prefsStore.updateAudioSettings(reciterId = reciter.id) }
    }

    fun setPlaybackSpeed(speed: Float) {
        audioController.setSpeed(speed)
        viewModelScope.launch { prefsStore.updateAudioSettings(speed = speed) }
    }

    fun setRepeatCount(count: Int) {
        audioController.setRepeatCount(count)
        viewModelScope.launch { prefsStore.updateAudioSettings(repeatCount = count) }
    }

    fun setAutoAdvance(enabled: Boolean) {
        audioController.setAutoAdvance(enabled)
        viewModelScope.launch { prefsStore.updateAudioSettings(autoAdvance = enabled) }
    }

    // Downloads
    fun startSurahDownload(reciter: Reciter, surahNumber: Int, surahName: String) {
        val id = "audio_${reciter.id}_$surahNumber"
        val job = viewModelScope.launch {
            downloadRepository.downloadSurahAudio(reciter, surahNumber, surahName)
        }
        downloadRepository.registerJob(id, job)
    }

    fun startTafsirDownload(editionId: String, title: String) {
        val id = "tafsir_$editionId"
        val job = viewModelScope.launch {
            downloadRepository.downloadExtraTafsir(editionId, title)
        }
        downloadRepository.registerJob(id, job)
    }

    fun pauseDownload(id: String) = downloadRepository.pauseDownload(id)

    fun deleteDownload(item: DownloadedItemEntity) {
        viewModelScope.launch { downloadRepository.deleteDownload(item) }
    }

    // Memorization & Khatmah
    fun createMemorizationPlan(surah: Surah, dailyAyahs: Int, reminderHour: Int) {
        viewModelScope.launch {
            dao.insertPlan(
                MemorizationPlanEntity(
                    surahNumber = surah.number,
                    surahName = surah.nameAr,
                    dailyAyahTarget = dailyAyahs.coerceIn(1, surah.ayahCount),
                    completedAyahs = 0,
                    totalAyahs = surah.ayahCount,
                    reminderHour = reminderHour
                )
            )
        }
    }

    fun advanceMemorizationPlan(plan: MemorizationPlanEntity, quality: Int) {
        viewModelScope.launch {
            val newCompleted = (plan.completedAyahs + plan.dailyAyahTarget).coerceAtMost(plan.totalAyahs)
            val updated = SpacedRepetitionEngine.scheduleNextReview(plan, quality).copy(
                completedAyahs = newCompleted,
                status = if (newCompleted >= plan.totalAyahs) "COMPLETED" else "ACTIVE"
            )
            dao.updatePlan(updated)
        }
    }

    fun deleteMemorizationPlan(id: Long) {
        viewModelScope.launch { dao.deletePlanById(id) }
    }

    fun evaluateRecitation(surahNumber: Int, fromAyah: Int, toAyah: Int, spokenText: String) {
        val surah = quranRepository.getSurah(surahNumber) ?: return
        val expectedText = surah.ayahs
            .filter { it.ayahNumber in fromAyah..toAyah }
            .joinToString(" ") { it.textUthmani }
        val result = RecitationAnalyzer.analyze(expectedText, spokenText)
        _lastRecitationResult.value = result
        viewModelScope.launch {
            dao.insertAttempt(
                RecitationAttemptEntity(
                    surahNumber = surahNumber,
                    fromAyah = fromAyah,
                    toAyah = toAyah,
                    accuracyPercent = result.accuracyPercent,
                    omittedWords = result.omittedCount,
                    addedWords = result.addedCount,
                    replacedWords = result.replacedCount,
                    detailsJson = result.summaryMessage
                )
            )
        }
    }

    fun clearRecitationResult() {
        _lastRecitationResult.value = null
    }

    fun createKhatmahPlan(title: String, targetDays: Int) {
        viewModelScope.launch {
            dao.insertKhatmahPlan(
                KhatmahPlanEntity(
                    title = title.ifBlank { "ختمة مباركة" },
                    targetDays = targetDays.coerceIn(1, 365),
                    pagesCompleted = 0
                )
            )
        }
    }

    fun addKhatmahPages(plan: KhatmahPlanEntity, pagesDelta: Int) {
        viewModelScope.launch {
            val next = (plan.pagesCompleted + pagesDelta).coerceIn(0, plan.totalPages)
            val nowDone = next >= plan.totalPages && !plan.completed
            dao.updateKhatmahPlan(plan.copy(pagesCompleted = next, completed = next >= plan.totalPages))
            if (nowDone) {
                prefsStore.incrementKhatmahCount()
                _showDuaKhatmPrompt.value = true
            }
        }
    }

    fun dismissDuaKhatmPrompt() {
        _showDuaKhatmPrompt.value = false
    }

    fun deleteKhatmahPlan(id: Long) {
        viewModelScope.launch { dao.deleteKhatmahPlanById(id) }
    }

    // Search
    fun updateSearchQuery(query: String, searchTafsir: Boolean = true) {
        _searchQuery.value = query
        viewModelScope.launch {
            _searchResults.value = quranRepository.searchQuran(query, searchTafsir)
        }
    }

    // Tasbeeh & Adhkar
    fun incrementTasbeeh() {
        val current = prefs.value
        val nextCurrent = current.tasbeehCurrent + 1
        val nextTotal = current.tasbeehTotal + 1
        viewModelScope.launch {
            prefsStore.updateTasbeeh(nextCurrent, nextTotal)
            if (nextCurrent % current.tasbeehTarget == 0) {
                dao.insertTasbeehLog(TasbeehLogEntity(dhikr = current.selectedDhikr, count = current.tasbeehTarget))
            }
        }
    }

    fun resetTasbeehCurrent() {
        val current = prefs.value
        viewModelScope.launch {
            if (current.tasbeehCurrent > 0) {
                dao.insertTasbeehLog(TasbeehLogEntity(dhikr = current.selectedDhikr, count = current.tasbeehCurrent))
            }
            prefsStore.updateTasbeeh(0, current.tasbeehTotal)
        }
    }

    fun setTasbeehSettings(dhikr: String, target: Int) {
        viewModelScope.launch {
            prefsStore.updateTasbeeh(0, prefs.value.tasbeehTotal, target, dhikr)
        }
    }

    fun advanceDhikr(item: DhikrItem) {
        val existing = adhkarProgress.value.firstOrNull { it.dhikrId == item.id }
        val next = ((existing?.currentCount ?: 0) + 1).coerceAtMost(item.repeat)
        viewModelScope.launch {
            dao.upsertAdhkarProgress(
                AdhkarProgressEntity(
                    id = "${todayKey}_${item.id}",
                    dhikrId = item.id,
                    dateKey = todayKey,
                    currentCount = next,
                    completed = next >= item.repeat
                )
            )
        }
    }

    // Location & Prayer settings
    fun selectCity(city: CityOption) {
        viewModelScope.launch {
            prefsStore.updateLocation(city.name, city.lat, city.lon, city.tz)
        }
    }

    fun detectCoarseLocation(context: Context) {
        try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return
            @Suppress("MissingPermission")
            val loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                ?: lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
            if (loc != null) {
                val tzHours = java.util.TimeZone.getDefault().rawOffset / 3_600_000.0
                viewModelScope.launch {
                    prefsStore.updateLocation("موقعي الحالي", loc.latitude, loc.longitude, tzHours)
                }
            }
        } catch (_: Exception) {
        }
    }

    // AI Quran Assistant
    fun askAiAssistant(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _aiState.value = AiUiState.Loading
            try {
                var finalText = ""
                var finalCitations: List<RagCitation> = emptyList()
                aiRepository.askWithRag(query).collect { chunk ->
                    finalText = chunk.accumulatedText
                    finalCitations = chunk.citations
                    _aiState.value = AiUiState.Streaming(finalText, finalCitations)
                }
                _aiState.value = AiUiState.Success(finalText, finalCitations)
            } catch (e: TimeoutCancellationException) {
                _aiState.value = AiUiState.Error("انتهت مهلة الاتصال بالمساعد الذكي (60 ثانية). يرجى المحاولة مجددًا.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _aiState.value = AiUiState.Error(e.localizedMessage ?: "تعذر الاتصال بالمساعد الذكي.")
            }
        }
    }

    // Cloud Backup & Restore
    fun backupToCloud() {
        viewModelScope.launch {
            _cloudState.value = CloudUiState(isBusy = true)
            val result = cloudSyncRepository.backupLocalToCloud()
            _cloudState.value = result.fold(
                onSuccess = { CloudUiState(isBusy = false, statusMessage = "تم رفع النسخة الاحتياطية إلى السحابة بنجاح") },
                onFailure = { CloudUiState(isBusy = false, errorMessage = it.localizedMessage ?: "فشل النسخ الاحتياطي") }
            )
        }
    }

    fun restoreFromCloud() {
        viewModelScope.launch {
            _cloudState.value = CloudUiState(isBusy = true)
            val result = cloudSyncRepository.restoreFromCloud()
            _cloudState.value = result.fold(
                onSuccess = { count ->
                    CloudUiState(isBusy = false, statusMessage = "تمت استعادة $count عنصر من السحابة")
                },
                onFailure = { CloudUiState(isBusy = false, errorMessage = it.localizedMessage ?: "فشل الاستعادة") }
            )
        }
    }

    fun deleteCloudData() {
        viewModelScope.launch {
            _cloudState.value = CloudUiState(isBusy = true)
            val result = cloudSyncRepository.deleteAllCloudUserData()
            _cloudState.value = result.fold(
                onSuccess = { CloudUiState(isBusy = false, statusMessage = "تم حذف جميع بياناتك السحابية بنجاح") },
                onFailure = { CloudUiState(isBusy = false, errorMessage = it.localizedMessage ?: "تعذر حذف البيانات") }
            )
        }
    }

    fun runIntegrityVerification() {
        viewModelScope.launch {
            _integrityReport.value = quranRepository.verifyIntegrity()
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioController.release()
    }
}
