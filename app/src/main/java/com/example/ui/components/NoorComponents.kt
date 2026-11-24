package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AudioPlayerState
import com.example.quran.ArabicNormalizer
import com.example.quran.Ayah
import com.example.quran.AyahCardRenderer
import com.example.quran.SababAlNuzul
import com.example.quran.WordMeaning
import com.example.ui.theme.AmiriQuranFamily
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldRoyal

@Composable
fun NoorCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = MaterialTheme.shapes.medium
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        content = content
    )
}

@Composable
fun OrnamentalSurahBanner(
    surahName: String,
    revelationType: String,
    ayahCount: Int,
    modifier: Modifier = Modifier
) {
    val revAr = if (revelationType.equals("Meccan", ignoreCase = true)) "مكية" else "مدنية"
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(EmeraldDeep, Color(0xFF145A45), EmeraldDeep)
                )
            )
            .border(1.5.dp, GoldRoyal, RoundedCornerShape(16.dp))
            .padding(vertical = 12.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val bannerTitle = if (surahName.contains("سُورَة") || surahName.startsWith("سورة")) surahName else "سُورَةُ $surahName"
            Text(
                text = bannerTitle,
                fontFamily = AmiriQuranFamily,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = GoldBright
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "$revAr • ${ArabicNormalizer.toArabicIndicDigits(ayahCount)} آية",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.88f)
            )
        }
    }
}

@Composable
fun SurahNumberBadge(number: Int) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .border(1.dp, GoldRoyal.copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = ArabicNormalizer.toArabicIndicDigits(number),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
fun MiniAudioPlayerBar(
    audioState: AudioPlayerState,
    onTogglePlay: () -> Unit,
    onStop: () -> Unit,
    onOpenPlayer: () -> Unit
) {
    if (!audioState.isPlaying && !audioState.isBuffering && audioState.activeAyahNumber == null && !audioState.isRadioMode) {
        return
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenPlayer)
            .testTag("mini_audio_bar"),
        color = EmeraldDeep,
        tonalElevation = 6.dp
    ) {
        Column {
            if (audioState.isBuffering) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color = GoldBright,
                    trackColor = EmeraldDeep
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    val audioTitle = if (audioState.surahName.contains("سُورَة") || audioState.surahName.startsWith("سورة")) {
                        audioState.surahName
                    } else "سورة ${audioState.surahName}"
                    Text(
                        text = if (audioState.isRadioMode) audioState.radioStationName
                        else "$audioTitle ${audioState.activeAyahNumber?.let { "• آية $it" } ?: ""}",
                        style = MaterialTheme.typography.titleMedium,
                        color = GoldBright,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (audioState.isRadioMode) "بث إذاعي مباشر"
                        else "${audioState.reciter?.name ?: ""} • سرعة ${audioState.speed}x",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = onTogglePlay, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = if (audioState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (audioState.isPlaying) "إيقاف مؤقت" else "تشغيل",
                        tint = Color.White
                    )
                }
                IconButton(onClick = onStop, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "إيقاف",
                        tint = GoldBright
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AyahActionsBottomSheet(
    ayah: Ayah,
    tafsirMuyassar: String,
    extraTafsir: String?,
    wordMeanings: List<WordMeaning>,
    englishTranslation: String,
    sababAlNuzul: SababAlNuzul?,
    onDismiss: () -> Unit,
    onPlayAyah: (Int) -> Unit,
    onSaveBookmark: (String) -> Unit,
    onCreateMemorizationPlan: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var showBookmarkInput by remember { mutableStateOf(false) }
    var bookmarkLabel by remember { mutableStateOf("سورة ${ayah.surahNameAr} • الآية ${ayah.ayahNumber}") }
    var statusNotice by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val sheetSurah = if (ayah.surahNameAr.contains("سُورَة") || ayah.surahNameAr.startsWith("سورة")) ayah.surahNameAr else "سُورَةُ ${ayah.surahNameAr}"
            Text(
                text = "$sheetSurah • الآية ${ArabicNormalizer.toArabicIndicDigits(ayah.ayahNumber)} (صفحة ${ayah.pageNumber})",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            ) {
                Text(
                    text = "﴿ ${ayah.textUthmani} ﴾",
                    fontFamily = AmiriQuranFamily,
                    fontSize = 22.sp,
                    lineHeight = 40.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AssistChip(
                    onClick = { onPlayAyah(1) },
                    label = { Text("استماع") },
                    leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null) }
                )
                AssistChip(
                    onClick = { onPlayAyah(3) },
                    label = { Text("تكرار ٣ مرات") },
                    leadingIcon = { Icon(Icons.Default.Repeat, contentDescription = null) }
                )
                AssistChip(
                    onClick = { showBookmarkInput = !showBookmarkInput },
                    label = { Text("علامة مرجعية") },
                    leadingIcon = { Icon(Icons.Default.BookmarkAdd, contentDescription = null) }
                )
                AssistChip(
                    onClick = {
                        onCreateMemorizationPlan()
                        statusNotice = "تمت إضافة السورة إلى خطط الحفظ"
                    },
                    label = { Text("خطة حفظ") },
                    leadingIcon = { Icon(Icons.Default.School, contentDescription = null) }
                )
                AssistChip(
                    onClick = {
                        clipboard.setText(AnnotatedString("﴿${ayah.textUthmani}﴾ [${ayah.surahNameAr}: ${ayah.ayahNumber}]"))
                        statusNotice = "تم نسخ نص الآية الموثق"
                    },
                    label = { Text("نسخ موثق") },
                    leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) }
                )
                AssistChip(
                    onClick = { AyahCardRenderer.shareAyahAsImage(context, ayah, tafsirMuyassar, true) },
                    label = { Text("مشاركة كصورة (زمردي)") },
                    leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) }
                )
                AssistChip(
                    onClick = { AyahCardRenderer.shareAyahAsImage(context, ayah, tafsirMuyassar, false) },
                    label = { Text("بطاقة عاجية") },
                    leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) }
                )
            }

            if (showBookmarkInput) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = bookmarkLabel,
                        onValueChange = { bookmarkLabel = it },
                        label = { Text("اسم العلامة") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Button(onClick = {
                        onSaveBookmark(bookmarkLabel)
                        showBookmarkInput = false
                        statusNotice = "تم حفظ العلامة المرجعية"
                    }) {
                        Text("حفظ")
                    }
                }
            }

            statusNotice?.let { notice ->
                Text(
                    text = notice,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            HorizontalDivider()

            val tabs = listOf("التفسير", "معاني الكلمات", "أسباب النزول", "الترجمة")
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { idx, title ->
                    Tab(
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx },
                        text = { Text(title, maxLines = 1, fontSize = 12.sp) }
                    )
                }
            }

            when (selectedTab) {
                0 -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "التفسير الميسر (مجمع الملك فهد):",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = tafsirMuyassar.ifBlank { "التفسير غير متوفر لهذه الآية." },
                            style = MaterialTheme.typography.bodyLarge
                        )
                        if (!extraTafsir.isNullOrBlank()) {
                            HorizontalDivider()
                            Text(
                                text = "تفسير إضافي محمّل:",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(text = extraTafsir, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                1 -> {
                    if (wordMeanings.isEmpty()) {
                        Text(
                            text = "لا توجد مفردات مختارة مسجلة لهذه الآية تحديدًا؛ راجع التفسير الميسر لبيان المعنى الإجمالي.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        wordMeanings.forEach { wm ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(wm.word, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                    Text(wm.meaning, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
                2 -> {
                    if (sababAlNuzul == null) {
                        Text(
                            text = "لم يثبت سبب نزول خاص منصوص عليه لهذه الآية في الروايات المعتمدة.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "المصدر: ${sababAlNuzul.source} (${sababAlNuzul.authenticity})",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(text = sababAlNuzul.reason, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
                3 -> {
                    Text(
                        text = "Sahih International (English):",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = englishTranslation.ifBlank { "Translation unavailable." },
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}
