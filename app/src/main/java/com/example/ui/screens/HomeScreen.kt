package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.quran.ArabicNormalizer
import com.example.quran.AyahCardRenderer
import com.example.ui.NoorViewModel
import com.example.ui.components.NoorCard
import com.example.ui.theme.AmiriQuranFamily
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldRoyal
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    vm: NoorViewModel,
    onOpenMushaf: (Int) -> Unit,
    onOpenContinuous: (Int) -> Unit,
    onOpenTvMode: () -> Unit,
    onOpenListen: () -> Unit,
    onOpenMemorize: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenAi: () -> Unit,
    onOpenPrayer: () -> Unit,
    onOpenQibla: () -> Unit,
    onOpenAdhkar: () -> Unit,
    onOpenTasbeeh: () -> Unit,
    onOpenRadio: () -> Unit,
    onOpenVirtues: () -> Unit,
    onOpenDuaKhatm: () -> Unit,
    onOpenAsbab: () -> Unit,
    onVoiceCommandRecognized: (String) -> Unit
) {
    val context = LocalContext.current
    val prefs by vm.prefs.collectAsStateWithLifecycle()
    val prayerSchedule by vm.prayerSchedule.collectAsStateWithLifecycle()
    val hijriDate by vm.hijriDate.collectAsStateWithLifecycle()
    val khatmahPlans by vm.khatmahPlans.collectAsStateWithLifecycle()
    val isReady by vm.isQuranReady.collectAsStateWithLifecycle()

    val lastSurah = remember(isReady, prefs.lastSurah) { vm.getSurah(prefs.lastSurah) }
    val ayahOfDay = remember(isReady) { vm.getAyahOfTheDay() }
    val activeKhatmah = khatmahPlans.firstOrNull { !it.completed }

    var showVoiceConsent by remember { mutableStateOf(false) }
    var voiceNotice by remember { mutableStateOf<String?>(null) }

    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (!spoken.isNullOrBlank()) {
            onVoiceCommandRecognized(spoken)
        } else {
            voiceNotice = "لم يتم التقاط أمر صوتي واضح."
        }
    }

    val micPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            try {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-SA")
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "قل: افتح سورة الكهف، شغل سورة يس، أو البحث...")
                }
                speechLauncher.launch(intent)
            } catch (_: Exception) {
                voiceNotice = "خدمة التعرف الصوتي غير متوفرة على هذا الجهاز."
            }
        } else {
            voiceNotice = "يلزم منح إذن الميكروفون لاستخدام الأوامر الصوتية."
        }
    }

    fun launchVoiceCommand() {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) {
            try {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-SA")
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "قل: افتح سورة الكهف، شغل تلاوة، أذكار الصباح...")
                }
                speechLauncher.launch(intent)
            } catch (_: Exception) {
                voiceNotice = "خدمة التعرف الصوتي غير متوفرة على هذا الجهاز."
            }
        } else {
            micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    if (showVoiceConsent) {
        AlertDialog(
            onDismissRequest = { showVoiceConsent = false },
            title = { Text("الأوامر الصوتية العربية") },
            text = {
                Text("يُستخدم الميكروفون فقط عند الضغط لتنفيذ أوامر مثل «افتح سورة الكهف» أو «شغل سورة الملك» أو البحث الصوتي. لا يتم أي تسجيل في الخلفية.")
            },
            confirmButton = {
                Button(onClick = {
                    showVoiceConsent = false
                    launchVoiceCommand()
                }) { Text("موافق وتحدث") }
            },
            dismissButton = {
                TextButton(onClick = { showVoiceConsent = false }) { Text("إلغاء") }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Royal Hero Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(EmeraldDeep)
            ) {
                Image(
                    painter = painterResource(R.drawable.img_home_hero_banner),
                    contentDescription = "خلفية نور القرآن الإسلامية",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize(),
                    alpha = 0.35f
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(EmeraldDark.copy(alpha = 0.72f), EmeraldDeep.copy(alpha = 0.92f))
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "نور القرآن",
                                style = MaterialTheme.typography.displaySmall,
                                color = GoldBright
                            )
                            Text(
                                text = hijriDate.formattedHijri,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                            Text(
                                text = hijriDate.formattedGregorian,
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(
                                onClick = onOpenSearch,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.14f))
                                    .testTag("home_search_btn")
                            ) {
                                Icon(Icons.Default.Search, contentDescription = "بحث في القرآن", tint = GoldBright)
                            }
                            IconButton(
                                onClick = { showVoiceConsent = true },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.14f))
                                    .testTag("home_voice_btn")
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = "أمر صوتي", tint = GoldBright)
                            }
                        }
                    }

                    hijriDate.occasion?.let { occ ->
                        Surface(
                            color = GoldRoyal.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "مناسبة: $occ",
                                color = GoldBright,
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Next Prayer Countdown strip
                    Surface(
                        color = Color.White.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onOpenPrayer)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.AccessTime, contentDescription = null, tint = GoldBright)
                                Column {
                                    Text(
                                        text = "${prefs.cityName} • الصلاة القادمة: ${prayerSchedule.nextPrayer.nameAr}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color.White
                                    )
                                    val hrs = prayerSchedule.minutesUntilNext / 60
                                    val mins = prayerSchedule.minutesUntilNext % 60
                                    Text(
                                        text = "المتبقي $hrs ساعة و $mins دقيقة",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = GoldBright
                                    )
                                }
                            }
                            Text(
                                text = prayerSchedule.nextPrayer.formattedTime,
                                style = MaterialTheme.typography.titleLarge,
                                color = GoldBright
                            )
                        }
                    }
                }
            }
        }

        voiceNotice?.let { msg ->
            item {
                NoorCard {
                    Text(
                        text = msg,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // 2. Continue Reading Card
        item {
            NoorCard(
                onClick = { onOpenMushaf(prefs.lastPage) },
                modifier = Modifier.testTag("continue_reading_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "متابعة القراءة (آخر موضع)",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "سورة ${lastSurah?.nameAr ?: "الفاتحة"} • الآية ${ArabicNormalizer.toArabicIndicDigits(prefs.lastAyah)}",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "صفحة ${ArabicNormalizer.toArabicIndicDigits(prefs.lastPage)} من ٦٠٤",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp), horizontalAlignment = Alignment.End) {
                        Button(
                            onClick = { onOpenMushaf(prefs.lastPage) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("المصحف")
                        }
                        OutlinedButton(onClick = { onOpenContinuous(prefs.lastSurah) }) {
                            Text("قراءة مستمرة", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // 3. Daily Khatmah Progress (if active)
        if (activeKhatmah != null) {
            item {
                val pct = (activeKhatmah.pagesCompleted.toFloat() / activeKhatmah.totalPages.coerceAtLeast(1)).coerceIn(0f, 1f)
                NoorCard(onClick = onOpenMemorize) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(activeKhatmah.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            Text("${activeKhatmah.pagesCompleted} / 604 صفحة", style = MaterialTheme.typography.labelLarge)
                        }
                        LinearProgressIndicator(
                            progress = { pct },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = GoldRoyal
                        )
                    }
                }
            }
        }

        // 4. Ayah of the Day
        if (ayahOfDay != null) {
            item {
                NoorCard {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "آية اليوم",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "سورة ${ayahOfDay.surahNameAr} • ${ArabicNormalizer.toArabicIndicDigits(ayahOfDay.ayahNumber)}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Text(
                            text = "﴿ ${ayahOfDay.textUthmani} ﴾",
                            fontFamily = AmiriQuranFamily,
                            fontSize = 21.sp,
                            lineHeight = 38.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalButton(
                                onClick = { vm.playAyah(ayahOfDay, 1) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("استماع")
                            }
                            OutlinedButton(
                                onClick = {
                                    val tafsir = vm.getTafsirMuyassar(ayahOfDay.surahNumber, ayahOfDay.ayahNumber)
                                    AyahCardRenderer.shareAyahAsImage(context, ayahOfDay, tafsir, true)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("مشاركة كصورة")
                            }
                        }
                    }
                }
            }
        }

        // 5. Quick Shortcuts Grid
        item {
            Text(
                text = "الوصول السريع",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            val shortcuts = listOf(
                HomeShortcut("المصحف ٦٠٤", Icons.AutoMirrored.Filled.MenuBook, "shortcut_mushaf") { onOpenMushaf(prefs.lastPage) },
                HomeShortcut("الاستماع والتنزيل", Icons.Default.Headphones, "shortcut_listen", onOpenListen),
                HomeShortcut("الحفظ والتسميع", Icons.Default.School, "shortcut_memorize", onOpenMemorize),
                HomeShortcut("المساعد القرآني", Icons.Default.AutoAwesome, "shortcut_ai", onOpenAi),
                HomeShortcut("فضائل السور", Icons.Default.Favorite, "shortcut_virtues", onOpenVirtues),
                HomeShortcut("دعاء ختم القرآن", Icons.Default.AutoStories, "shortcut_dua_khatm", onOpenDuaKhatm),
                HomeShortcut("أسباب النزول", Icons.AutoMirrored.Filled.FormatListBulleted, "shortcut_asbab", onOpenAsbab),
                HomeShortcut("الأذكار", Icons.Default.Spa, "shortcut_adhkar", onOpenAdhkar),
                HomeShortcut("السبحة", Icons.Default.Spa, "shortcut_tasbeeh", onOpenTasbeeh),
                HomeShortcut("مواقيت الصلاة", Icons.Default.AccessTime, "shortcut_prayer", onOpenPrayer),
                HomeShortcut("اتجاه القبلة", Icons.Default.Explore, "shortcut_qibla", onOpenQibla),
                HomeShortcut("راديو القرآن", Icons.Default.Radio, "shortcut_radio", onOpenRadio),
                HomeShortcut("عرض التلفاز", Icons.Default.Tv, "shortcut_tv", onOpenTvMode),
                HomeShortcut("بحث قرآني", Icons.Default.Search, "shortcut_search", onOpenSearch)
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                maxItemsInEachRow = 2
            ) {
                shortcuts.forEach { item ->
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(onClick = item.onClick)
                            .testTag(item.tag),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class HomeShortcut(
    val title: String,
    val icon: ImageVector,
    val tag: String,
    val onClick: () -> Unit
)
