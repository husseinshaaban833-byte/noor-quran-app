package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.quran.ArabicNormalizer
import com.example.ui.NoorViewModel
import com.example.ui.components.NoorCard
import com.example.ui.components.SurahNumberBadge
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldRoyal
import com.example.ui.theme.SuccessGreen
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ListenScreen(
    vm: NoorViewModel,
    onOpenReader: (Int) -> Unit
) {
    val prefs by vm.prefs.collectAsStateWithLifecycle()
    val audioState by vm.audioState.collectAsStateWithLifecycle()
    val downloads by vm.downloads.collectAsStateWithLifecycle()
    val activeDownloads by vm.activeDownloads.collectAsStateWithLifecycle()
    val isReady by vm.isQuranReady.collectAsStateWithLifecycle()

    val reciters = remember(isReady) { vm.getReciters() }
    val surahs = remember(isReady) { vm.getSurahs() }
    val currentReciter = remember(prefs.selectedReciterId, reciters) {
        reciters.firstOrNull { it.id == prefs.selectedReciterId } ?: reciters.firstOrNull()
    }
    var selectedTab by remember { mutableIntStateOf(0) }
    var styleFilter by remember { mutableStateOf("الكل") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("listen_screen")
    ) {
        // Active Audio Player Control Panel
        Surface(
            color = EmeraldDeep,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (audioState.isRadioMode) audioState.radioStationName
                            else "سورة ${audioState.surahName} ${audioState.activeAyahNumber?.let { "• الآية ${ArabicNormalizer.toArabicIndicDigits(it)}" } ?: ""}",
                            style = MaterialTheme.typography.titleLarge,
                            color = GoldBright
                        )
                        Text(
                            text = currentReciter?.let { "${it.name} (${it.style})" } ?: "اختر قارئًا",
                            style = MaterialTheme.typography.bodyMedium,
                            color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.85f)
                        )
                    }
                    Row {
                        IconButton(onClick = { vm.audioController.previousAyah() }, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Default.SkipNext, contentDescription = "الآية السابقة", tint = GoldBright)
                        }
                        IconButton(
                            onClick = {
                                if (audioState.isPlaying || audioState.isBuffering) {
                                    vm.audioController.togglePlayPause()
                                } else {
                                    vm.playSurah(prefs.lastSurah)
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("audio_play_pause_btn")
                        ) {
                            Icon(
                                imageVector = if (audioState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "تشغيل أو إيقاف",
                                tint = androidx.compose.ui.graphics.Color.White
                            )
                        }
                        IconButton(onClick = { vm.audioController.nextAyah() }, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Default.SkipPrevious, contentDescription = "الآية التالية", tint = GoldBright)
                        }
                        IconButton(onClick = { vm.audioController.stop() }, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Default.Stop, contentDescription = "إيقاف", tint = GoldBright)
                        }
                    }
                }

                // Speed & Repeat Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "السرعة: ${"%.2f".format(prefs.playbackSpeed)}x (يحافظ على طبقة الصوت)",
                        color = androidx.compose.ui.graphics.Color.White,
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = "التكرار: ${prefs.repeatCount}×",
                        color = GoldBright,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                Slider(
                    value = prefs.playbackSpeed,
                    onValueChange = { s -> vm.setPlaybackSpeed((s * 4f).roundToInt() / 4f) },
                    valueRange = 0.5f..2.0f
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(1, 3, 5, 10, 20).forEach { rep ->
                            FilterChip(
                                selected = prefs.repeatCount == rep,
                                onClick = { vm.setRepeatCount(rep) },
                                label = { Text("${rep}×", fontSize = 12.sp) }
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "تلقائي",
                            color = androidx.compose.ui.graphics.Color.White,
                            style = MaterialTheme.typography.labelMedium
                        )
                        Spacer(Modifier.width(4.dp))
                        Switch(
                            checked = prefs.autoAdvanceAyah,
                            onCheckedChange = { vm.setAutoAdvance(it) }
                        )
                    }
                }

                audioState.errorMessage?.let { err ->
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = err,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(10.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        val tabs = listOf("السور للاستماع", "القراء (${reciters.size})", "التنزيلات (${downloads.size})")
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { idx, title ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    text = { Text(title, maxLines = 1, fontSize = 13.sp) }
                )
            }
        }

        when (selectedTab) {
            0 -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(surahs, key = { it.number }) { surah ->
                    val dlId = "audio_${currentReciter?.id}_${surah.number}"
                    val isDownloaded = downloads.any { it.id == dlId }
                    val activeDl = activeDownloads[dlId]

                    NoorCard {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                SurahNumberBadge(surah.number)
                                Column {
                                    Text("سورة ${surah.nameAr}", style = MaterialTheme.typography.titleMedium)
                                    Text("${surah.ayahCount} آية", style = MaterialTheme.typography.bodyMedium)
                                    if (activeDl != null) {
                                        val progress = if (activeDl.totalBytes > 0) {
                                            (activeDl.bytesDownloaded.toFloat() / activeDl.totalBytes).coerceIn(0f, 1f)
                                        } else 0.3f
                                        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
                                    }
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isDownloaded) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = "محملة دون إنترنت",
                                        tint = SuccessGreen,
                                        modifier = Modifier.padding(horizontal = 6.dp)
                                    )
                                } else if (activeDl != null && activeDl.status == "DOWNLOADING") {
                                    IconButton(onClick = { vm.pauseDownload(dlId) }, modifier = Modifier.size(48.dp)) {
                                        Icon(Icons.Default.Pause, contentDescription = "إيقاف التنزيل مؤقتًا")
                                    }
                                } else {
                                    IconButton(
                                        onClick = {
                                            currentReciter?.let { rec ->
                                                vm.startSurahDownload(rec, surah.number, surah.nameAr)
                                            }
                                        },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(Icons.Default.CloudDownload, contentDescription = "تنزيل السورة")
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        vm.playSurah(surah.number)
                                        onOpenReader(surah.startPage)
                                    },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "تشغيل السورة", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }

            1 -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("الكل", "مرتل", "مجود", "معلم").forEach { style ->
                            FilterChip(
                                selected = styleFilter == style,
                                onClick = { styleFilter = style },
                                label = { Text(style) }
                            )
                        }
                    }
                }
                val filteredReciters = reciters.filter { styleFilter == "الكل" || it.style.contains(styleFilter) }
                items(filteredReciters, key = { it.id }) { reciter ->
                    val selected = reciter.id == currentReciter?.id
                    NoorCard(
                        onClick = { vm.selectReciter(reciter) },
                        containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(reciter.name, style = MaterialTheme.typography.titleMedium)
                                Text("أسلوب القراءة: ${reciter.style}", style = MaterialTheme.typography.bodyMedium)
                            }
                            if (selected) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "مختار", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }

            2 -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    val freeMb = vm.downloadRepository.getAvailableInternalStorageBytes() / (1024 * 1024)
                    NoorCard {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("المساحة المتاحة على الجهاز: $freeMb ميجابايت", style = MaterialTheme.typography.titleMedium)
                            val jalalaynReady = downloads.any { it.id == "tafsir_ar.jalalayn" }
                            val jalalaynActive = activeDownloads["tafsir_ar.jalalayn"]
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("تفسير الجلالين (حزمة كاملة دون إنترنت)", style = MaterialTheme.typography.titleMedium)
                                    Text(if (jalalaynReady) "مثبت ومُتحقق ببصمة SHA-256" else "متاح للتنزيل للاستخدام دون إنترنت", style = MaterialTheme.typography.bodyMedium)
                                }
                                if (jalalaynReady) {
                                    Icon(Icons.Default.Verified, contentDescription = "تم التحقق", tint = SuccessGreen)
                                } else if (jalalaynActive != null) {
                                    Text("جاري التحميل...", style = MaterialTheme.typography.labelLarge)
                                } else {
                                    Button(onClick = { vm.startTafsirDownload("ar.jalalayn", "تفسير الجلالين") }) {
                                        Text("تنزيل")
                                    }
                                }
                            }
                        }
                    }
                }

                if (downloads.isEmpty()) {
                    item {
                        NoorCard {
                            Text(
                                text = "لا توجد سور أو تفاسير محملة بعد. يمكنك تنزيل السور من تبويب «السور للاستماع» مع دعم الإيقاف المؤقت والاستئناف.",
                                modifier = Modifier.padding(18.dp)
                            )
                        }
                    }
                } else {
                    items(downloads, key = { it.id }) { item ->
                        NoorCard {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.title, style = MaterialTheme.typography.titleMedium)
                                    val sizeKb = item.sizeBytes / 1024
                                    Text("الحجم: $sizeKb كيلوبايت • SHA-256: ${item.sha256.take(12)}...", style = MaterialTheme.typography.labelMedium)
                                }
                                IconButton(onClick = { vm.deleteDownload(item) }, modifier = Modifier.size(48.dp)) {
                                    Icon(Icons.Default.Delete, contentDescription = "حذف التنزيل", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
