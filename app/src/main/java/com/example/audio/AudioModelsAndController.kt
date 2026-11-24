package com.example.audio

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.quran.Ayah
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

data class Reciter(
    val id: String,
    val name: String,
    val style: String,
    val ayahBaseUrl: String,
    val surahBaseUrl: String
)

data class RadioStation(
    val id: Int,
    val name: String,
    val category: String,
    val url: String
)

data class AudioPlayerState(
    val isConnected: Boolean = false,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val isRadioMode: Boolean = false,
    val radioStationName: String = "",
    val reciter: Reciter? = null,
    val surahNumber: Int = 1,
    val surahName: String = "الفاتحة",
    val activeAyahNumber: Int? = null,
    val activeGlobalAyah: Int? = null,
    val speed: Float = 1.0f,
    val repeatCount: Int = 1,
    val currentRepeatIteration: Int = 1,
    val autoAdvance: Boolean = true,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val errorMessage: String? = null
)

class QuranAudioController(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var progressJob: Job? = null

    private var currentQueueAyahs: List<Ayah> = emptyList()
    private var remainingRepeats: Int = 0
    private var targetRepeats: Int = 1

    private val _state = MutableStateFlow(AudioPlayerState())
    val state: StateFlow<AudioPlayerState> = _state.asStateFlow()

    fun connect() {
        if (controllerFuture != null) return
        if (android.os.Build.FINGERPRINT.equals("robolectric", ignoreCase = true)) return
        val token = SessionToken(context, ComponentName(context, QuranPlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        controllerFuture = future
        future.addListener({
            try {
                val ctrl = future.get()
                controller = ctrl
                attachListener(ctrl)
                _state.update { it.copy(isConnected = true) }
                startProgressLoop()
            } catch (e: Exception) {
                _state.update { it.copy(errorMessage = "تعذر تهيئة المشغل الصوتي: ${e.localizedMessage}") }
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun attachListener(ctrl: MediaController) {
        ctrl.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.update { it.copy(isPlaying = isPlaying, isBuffering = false) }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> _state.update { it.copy(isBuffering = true) }
                    Player.STATE_READY -> _state.update {
                        it.copy(
                            isBuffering = false,
                            durationMs = ctrl.duration.coerceAtLeast(0L),
                            errorMessage = null
                        )
                    }
                    Player.STATE_ENDED -> {
                        if (remainingRepeats > 1 && currentQueueAyahs.isNotEmpty()) {
                            remainingRepeats--
                            val iteration = (targetRepeats - remainingRepeats) + 1
                            _state.update { it.copy(currentRepeatIteration = iteration) }
                            ctrl.seekTo(0, 0L)
                            ctrl.play()
                        } else {
                            _state.update {
                                it.copy(
                                    isPlaying = false,
                                    isBuffering = false,
                                    activeAyahNumber = null
                                )
                            }
                        }
                    }
                    Player.STATE_IDLE -> _state.update { it.copy(isBuffering = false) }
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val extras = mediaItem?.mediaMetadata?.extras
                val ayahNum = extras?.getInt("ayahNumber")?.takeIf { it > 0 }
                val globalNum = extras?.getInt("globalNumber")?.takeIf { it > 0 }
                val surahNum = extras?.getInt("surahNumber")?.takeIf { it > 0 }
                val surahName = extras?.getString("surahName")

                if (!_state.value.autoAdvance && reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                    ctrl.pause()
                }
                _state.update { st ->
                    st.copy(
                        surahNumber = surahNum ?: st.surahNumber,
                        surahName = surahName ?: st.surahName,
                        activeAyahNumber = ayahNum,
                        activeGlobalAyah = globalNum
                    )
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                _state.update {
                    it.copy(
                        isPlaying = false,
                        isBuffering = false,
                        errorMessage = "تعذر تشغيل الصوت (تأكد من الاتصال أو تنزيل السورة): ${error.localizedMessage ?: ""}"
                    )
                }
            }
        })
    }

    private fun startProgressLoop() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                val ctrl = controller
                if (ctrl != null && ctrl.isPlaying) {
                    _state.update {
                        it.copy(
                            positionMs = ctrl.currentPosition.coerceAtLeast(0L),
                            durationMs = ctrl.duration.coerceAtLeast(0L)
                        )
                    }
                }
                delay(350L)
            }
        }
    }

    fun playAyahRange(
        ayahs: List<Ayah>,
        startIndex: Int,
        reciter: Reciter,
        repeatCount: Int = _state.value.repeatCount,
        speed: Float = _state.value.speed,
        autoAdvance: Boolean = _state.value.autoAdvance
    ) {
        if (ayahs.isEmpty()) return
        connect()
        val ctrl = controller
        currentQueueAyahs = ayahs
        targetRepeats = repeatCount.coerceIn(1, 20)
        remainingRepeats = targetRepeats

        val items = ayahs.map { ayah ->
            val localAyahFile = File(context.filesDir, "downloads/audio/${reciter.id}/ayah_${ayah.globalNumber}.mp3")
            val uri = if (localAyahFile.exists()) {
                Uri.fromFile(localAyahFile)
            } else {
                Uri.parse("${reciter.ayahBaseUrl}/${ayah.globalNumber}.mp3")
            }
            val extras = Bundle().apply {
                putInt("surahNumber", ayah.surahNumber)
                putInt("ayahNumber", ayah.ayahNumber)
                putInt("globalNumber", ayah.globalNumber)
                putString("surahName", ayah.surahNameAr)
            }
            MediaItem.Builder()
                .setUri(uri)
                .setMediaId("ayah_${ayah.globalNumber}")
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle("سورة ${ayah.surahNameAr} - الآية ${ayah.ayahNumber}")
                        .setArtist(reciter.name)
                        .setExtras(extras)
                        .build()
                )
                .build()
        }

        val safeStart = startIndex.coerceIn(0, items.lastIndex)
        val firstAyah = ayahs[safeStart]
        _state.update {
            it.copy(
                isRadioMode = false,
                radioStationName = "",
                reciter = reciter,
                surahNumber = firstAyah.surahNumber,
                surahName = firstAyah.surahNameAr,
                activeAyahNumber = firstAyah.ayahNumber,
                activeGlobalAyah = firstAyah.globalNumber,
                repeatCount = targetRepeats,
                currentRepeatIteration = 1,
                speed = speed,
                autoAdvance = autoAdvance,
                errorMessage = null,
                isBuffering = true
            )
        }

        if (ctrl != null) {
            ctrl.setMediaItems(items, safeStart, 0L)
            ctrl.playbackParameters = PlaybackParameters(speed, 1.0f) // preserves pitch
            ctrl.prepare()
            ctrl.play()
        } else {
            scope.launch {
                delay(400)
                controller?.let { c ->
                    c.setMediaItems(items, safeStart, 0L)
                    c.playbackParameters = PlaybackParameters(speed, 1.0f)
                    c.prepare()
                    c.play()
                }
            }
        }
    }

    fun playSurahFileOrStream(surahNumber: Int, surahName: String, reciter: Reciter, ayahs: List<Ayah>) {
        val localSurah = File(context.filesDir, "downloads/audio/${reciter.id}/surah_${surahNumber}.mp3")
        if (localSurah.exists()) {
            connect()
            val extras = Bundle().apply {
                putInt("surahNumber", surahNumber)
                putString("surahName", surahName)
            }
            val item = MediaItem.Builder()
                .setUri(Uri.fromFile(localSurah))
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle("سورة $surahName")
                        .setArtist("${reciter.name} (محملة)")
                        .setExtras(extras)
                        .build()
                )
                .build()
            _state.update {
                it.copy(
                    isRadioMode = false,
                    reciter = reciter,
                    surahNumber = surahNumber,
                    surahName = surahName,
                    activeAyahNumber = null,
                    errorMessage = null,
                    isBuffering = true
                )
            }
            controller?.apply {
                setMediaItem(item)
                playbackParameters = PlaybackParameters(_state.value.speed, 1.0f)
                prepare()
                play()
            }
        } else {
            playAyahRange(ayahs, 0, reciter)
        }
    }

    fun playRadioStation(station: RadioStation) {
        connect()
        currentQueueAyahs = emptyList()
        val item = MediaItem.Builder()
            .setUri(station.url)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(station.name)
                    .setArtist("بث مباشر - راديو القرآن الكريم")
                    .build()
            )
            .build()
        _state.update {
            it.copy(
                isRadioMode = true,
                radioStationName = station.name,
                activeAyahNumber = null,
                errorMessage = null,
                isBuffering = true
            )
        }
        val ctrl = controller
        if (ctrl != null) {
            ctrl.setMediaItem(item)
            ctrl.playbackParameters = PlaybackParameters(1.0f, 1.0f)
            ctrl.prepare()
            ctrl.play()
        } else {
            scope.launch {
                delay(400)
                controller?.apply {
                    setMediaItem(item)
                    prepare()
                    play()
                }
            }
        }
    }

    fun togglePlayPause() {
        val ctrl = controller ?: return
        if (ctrl.isPlaying) ctrl.pause() else ctrl.play()
    }

    fun stop() {
        controller?.stop()
        _state.update { it.copy(isPlaying = false, activeAyahNumber = null, isRadioMode = false) }
    }

    fun nextAyah() {
        controller?.seekToNextMediaItem()
    }

    fun previousAyah() {
        controller?.seekToPreviousMediaItem()
    }

    fun setSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.5f, 2.0f)
        _state.update { it.copy(speed = clamped) }
        controller?.playbackParameters = PlaybackParameters(clamped, 1.0f)
    }

    fun setRepeatCount(count: Int) {
        val clamped = count.coerceIn(1, 20)
        targetRepeats = clamped
        remainingRepeats = clamped
        _state.update { it.copy(repeatCount = clamped, currentRepeatIteration = 1) }
    }

    fun setAutoAdvance(enabled: Boolean) {
        _state.update { it.copy(autoAdvance = enabled) }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    fun release() {
        progressJob?.cancel()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        controller = null
    }
}
