package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.memorization.SpacedRepetitionEngine
import com.example.memorization.WordDiffType
import com.example.quran.ArabicNormalizer
import com.example.ui.NoorViewModel
import com.example.ui.components.NoorCard
import com.example.ui.theme.AmiriQuranFamily
import com.example.ui.theme.GoldRoyal
import com.example.ui.theme.SuccessGreen

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MemorizationScreen(
    vm: NoorViewModel,
    onOpenDuaKhatm: () -> Unit,
    onOpenPage: (Int) -> Unit
) {
    val context = LocalContext.current
    val isReady by vm.isQuranReady.collectAsStateWithLifecycle()
    val plans by vm.memorizationPlans.collectAsStateWithLifecycle()
    val attempts by vm.recitationAttempts.collectAsStateWithLifecycle()
    val khatmahPlans by vm.khatmahPlans.collectAsStateWithLifecycle()
    val lastResult by vm.lastRecitationResult.collectAsStateWithLifecycle()
    val showDuaPrompt by vm.showDuaKhatmPrompt.collectAsStateWithLifecycle()

    val surahs = remember(isReady) { vm.getSurahs() }
    var selectedTab by remember { mutableIntStateOf(0) }

    // Plan creation dialog state
    var showCreatePlan by remember { mutableStateOf(false) }
    var selectedSurahNum by remember { mutableIntStateOf(78) } // An-Naba default for memorization
    var dailyAyahInput by remember { mutableStateOf("5") }

    // Khatmah creation dialog state
    var showCreateKhatmah by remember { mutableStateOf(false) }
    var khatmahTitle by remember { mutableStateOf("ختمة شهرية") }
    var khatmahDays by remember { mutableStateOf("30") }

    // Recitation & Memorization test range
    var testSurahNum by remember { mutableIntStateOf(112) } // Al-Ikhlas default
    var fromAyahStr by remember { mutableStateOf("1") }
    var toAyahStr by remember { mutableStateOf("4") }
    var spokenInput by remember { mutableStateOf("") }
    var showMicConsent by remember { mutableStateOf(false) }
    var testMode by remember { mutableIntStateOf(0) } // 0: Hide all, 1: First word, 2: Order test
    var revealedKeys by remember { mutableStateOf(setOf<String>()) }
    var micNotice by remember { mutableStateOf<String?>(null) }

    val testSurah = remember(testSurahNum, isReady) { vm.getSurah(testSurahNum) }
    val fromAyah = (fromAyahStr.toIntOrNull() ?: 1).coerceIn(1, testSurah?.ayahCount ?: 1)
    val toAyah = (toAyahStr.toIntOrNull() ?: (testSurah?.ayahCount ?: 4)).coerceIn(fromAyah, testSurah?.ayahCount ?: 4)

    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { res ->
        val spoken = res.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (!spoken.isNullOrBlank()) {
            spokenInput = spoken
            vm.evaluateRecitation(testSurahNum, fromAyah, toAyah, spoken)
        } else {
            micNotice = "تعذر التأكد: لم يصل نص صوتي واضح. تأكد من الوضوح أو اكتب الآيات يدويًا للمقارنة."
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
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "رتّل الآيات المختارة للتسميع...")
                }
                speechLauncher.launch(intent)
            } catch (_: Exception) {
                micNotice = "خدمة التعرف الصوتي غير مدعومة على هذا الجهاز؛ يمكنك اختبار التسميع بالكتابة."
            }
        } else {
            micNotice = "تم رفض إذن الميكروفون. يمكنك استخدام اختبار التسميع الكتابي أو إخفاء الآيات."
        }
    }

    fun startSpeechRecitation() {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) {
            try {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-SA")
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "رتّل الآيات من ${testSurah?.nameAr ?: ""}...")
                }
                speechLauncher.launch(intent)
            } catch (_: Exception) {
                micNotice = "خدمة التعرف الصوتي غير مدعومة على هذا الجهاز."
            }
        } else {
            micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    if (showDuaPrompt) {
        AlertDialog(
            onDismissRequest = { vm.dismissDuaKhatmPrompt() },
            title = { Text("مبارك إتمام الختمة!") },
            text = { Text("تقبّل الله منك ختمة القرآن الكريم. هل تود فتح دعاء ختم القرآن الآن؟") },
            confirmButton = {
                Button(onClick = {
                    vm.dismissDuaKhatmPrompt()
                    onOpenDuaKhatm()
                }) { Text("قراءة دعاء الختم") }
            },
            dismissButton = {
                TextButton(onClick = { vm.dismissDuaKhatmPrompt() }) { Text("لاحقًا") }
            }
        )
    }

    if (showMicConsent) {
        AlertDialog(
            onDismissRequest = { showMicConsent = false },
            title = { Text("تنبيه الميكروفون والضوابط الشرعية") },
            text = {
                Text("يُستخدم الميكروفون أثناء التسميع لمطابقة الكلمات المحفوظة فقط مع النص القرآني الموثق، ولا يقوّم هذا الفحص التلقائي أحكام التجويد أو المخارج التي تُؤخذ بالمشافهة على شيخ متقن.")
            },
            confirmButton = {
                Button(onClick = {
                    showMicConsent = false
                    startSpeechRecitation()
                }) { Text("موافق وابدأ التسميع") }
            },
            dismissButton = {
                TextButton(onClick = { showMicConsent = false }) { Text("إلغاء") }
            }
        )
    }

    if (showCreatePlan) {
        val chosen = surahs.firstOrNull { it.number == selectedSurahNum } ?: surahs.firstOrNull()
        AlertDialog(
            onDismissRequest = { showCreatePlan = false },
            title = { Text("خطة حفظ جديدة") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("اختر السورة: ${chosen?.nameAr ?: ""}")
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(1, 67, 78, 112, 114).forEach { n ->
                            val sName = surahs.firstOrNull { it.number == n }?.nameAr ?: "$n"
                            FilterChip(
                                selected = selectedSurahNum == n,
                                onClick = { selectedSurahNum = n },
                                label = { Text(sName, fontSize = 11.sp) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = dailyAyahInput,
                        onValueChange = { if (it.all(Char::isDigit)) dailyAyahInput = it },
                        label = { Text("عدد الآيات اليومي") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    chosen?.let { vm.createMemorizationPlan(it, dailyAyahInput.toIntOrNull() ?: 5, 7) }
                    showCreatePlan = false
                }) { Text("إنشاء الخطة") }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePlan = false }) { Text("إلغاء") }
            }
        )
    }

    if (showCreateKhatmah) {
        AlertDialog(
            onDismissRequest = { showCreateKhatmah = false },
            title = { Text("مخطط ختمة جديدة") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = khatmahTitle,
                        onValueChange = { khatmahTitle = it },
                        label = { Text("عنوان الختمة") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = khatmahDays,
                        onValueChange = { if (it.all(Char::isDigit)) khatmahDays = it },
                        label = { Text("عدد الأيام المستهدف") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    vm.createKhatmahPlan(khatmahTitle, khatmahDays.toIntOrNull() ?: 30)
                    showCreateKhatmah = false
                }) { Text("بدء الختمة") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateKhatmah = false }) { Text("إلاء") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("memorization_screen")
    ) {
        val tabs = listOf("التسميع الصوتي", "خطط الحفظ", "اختبار تفاعلي", "الختمات")
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
            // 0: Voice & Text Recitation Checker
            0 -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    NoorCard {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("اختيار المقطع للتسميع", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(1, 103, 108, 112, 113, 114, 78, 67).forEach { sNum ->
                                    val s = surahs.firstOrNull { it.number == sNum }
                                    FilterChip(
                                        selected = testSurahNum == sNum,
                                        onClick = {
                                            testSurahNum = sNum
                                            fromAyahStr = "1"
                                            toAyahStr = "${s?.ayahCount?.coerceAtMost(7) ?: 4}"
                                            vm.clearRecitationResult()
                                        },
                                        label = { Text(s?.nameAr ?: "$sNum", fontSize = 12.sp) }
                                    )
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = fromAyahStr,
                                    onValueChange = { if (it.all(Char::isDigit)) fromAyahStr = it },
                                    label = { Text("من الآية") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = toAyahStr,
                                    onValueChange = { if (it.all(Char::isDigit)) toAyahStr = it },
                                    label = { Text("إلى الآية") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }
                            Button(
                                onClick = { showMicConsent = true },
                                modifier = Modifier.fillMaxWidth().testTag("start_voice_recitation_btn")
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("ابدأ التسميع بالصوت")
                            }
                            OutlinedTextField(
                                value = spokenInput,
                                onValueChange = { spokenInput = it },
                                label = { Text("أو اكتب ما تحفظه هنا للتحقق الدقيق") },
                                modifier = Modifier.fillMaxWidth().testTag("recitation_text_input"),
                                minLines = 2
                            )
                            OutlinedButton(
                                onClick = { vm.evaluateRecitation(testSurahNum, fromAyah, toAyah, spokenInput) },
                                modifier = Modifier.fillMaxWidth().testTag("evaluate_recitation_btn")
                            ) {
                                Text("فحص ومطابقة النص")
                            }
                            micNotice?.let { msg ->
                                Text(msg, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }

                lastResult?.let { res ->
                    item {
                        NoorCard {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = if (res.confident) "دقة المطابقة: ${res.accuracyPercent}%" else "تعذر التأكد",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = if (res.confident && res.accuracyPercent >= 80) SuccessGreen else MaterialTheme.colorScheme.error
                                )
                                Text(res.summaryMessage, style = MaterialTheme.typography.bodyMedium)
                                if (res.confident) {
                                    Text(
                                        text = "محذوف: ${res.omittedCount} • زائد: ${res.addedCount} • مبدّل: ${res.replacedCount} • تقديم/تأخير: ${res.reorderedCount}",
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        res.items.forEach { item ->
                                            val bg = when (item.type) {
                                                WordDiffType.MATCH -> SuccessGreen.copy(alpha = 0.16f)
                                                WordDiffType.OMITTED -> Color(0xFFB3261E).copy(alpha = 0.2f)
                                                WordDiffType.ADDED -> Color(0xFFD97706).copy(alpha = 0.2f)
                                                WordDiffType.REPLACED -> Color(0xFF9333EA).copy(alpha = 0.2f)
                                                WordDiffType.REORDERED -> Color(0xFF2563EB).copy(alpha = 0.2f)
                                            }
                                            val label = when (item.type) {
                                                WordDiffType.MATCH -> item.expectedWord.orEmpty()
                                                WordDiffType.OMITTED -> "ناقص: ${item.expectedWord.orEmpty()}"
                                                WordDiffType.ADDED -> "زائد: ${item.spokenWord.orEmpty()}"
                                                WordDiffType.REPLACED -> "${item.expectedWord} ≠ ${item.spokenWord}"
                                                WordDiffType.REORDERED -> "تقديم/تأخير: ${item.expectedWord}"
                                            }
                                            Surface(color = bg, shape = RoundedCornerShape(8.dp)) {
                                                Text(label, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 13.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (attempts.isNotEmpty()) {
                    item {
                        Text("سجل المحاولات السابقة", style = MaterialTheme.typography.titleMedium)
                    }
                    items(attempts.take(10), key = { it.id }) { att ->
                        NoorCard {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("سورة ${vm.getSurah(att.surahNumber)?.nameAr ?: att.surahNumber} (${att.fromAyah}-${att.toAyah})")
                                Text("${att.accuracyPercent}%", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }

            // 1: Spaced Repetition Memorization Plans
            1 -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Button(
                        onClick = { showCreatePlan = true },
                        modifier = Modifier.fillMaxWidth().testTag("create_memorization_plan_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("إنشاء خطة حفظ جديدة")
                    }
                }
                if (plans.isEmpty()) {
                    item {
                        NoorCard {
                            Text("لا توجد خطط حفظ بعد. أنشئ خطة جديدة لتتبع المراجعة المتباعدة يوميًا.", modifier = Modifier.padding(18.dp))
                        }
                    }
                } else {
                    items(plans, key = { it.id }) { plan ->
                        val pct = (plan.completedAyahs.toFloat() / plan.totalAyahs.coerceAtLeast(1)).coerceIn(0f, 1f)
                        NoorCard {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("سورة ${plan.surahName}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                        Text("الورد اليومي: ${plan.dailyAyahTarget} آيات • المحفوظ: ${plan.completedAyahs}/${plan.totalAyahs}", style = MaterialTheme.typography.bodyMedium)
                                    }
                                    IconButton(onClick = { vm.deleteMemorizationPlan(plan.id) }, modifier = Modifier.size(48.dp)) {
                                        Icon(Icons.Default.Delete, contentDescription = "حذف الخطة")
                                    }
                                }
                                LinearProgressIndicator(progress = { pct }, modifier = Modifier.fillMaxWidth(), color = GoldRoyal)
                                if (plan.status != "COMPLETED") {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(onClick = { vm.advanceMemorizationPlan(plan, 1) }, modifier = Modifier.weight(1f)) {
                                            Text("تحتاج مراجعة", fontSize = 12.sp)
                                        }
                                        Button(onClick = { vm.advanceMemorizationPlan(plan, 5) }, modifier = Modifier.weight(1f)) {
                                            Text("تم الإتقان", fontSize = 12.sp)
                                        }
                                    }
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen)
                                        Text("مكتملة بحمد الله • المراجعة كل ${plan.intervalDays} يوم", color = SuccessGreen)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2: Interactive Memorization Test
            2 -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("إخفاء الآيات", "الكلمة الأولى فقط", "ترتيب الآيات").forEachIndexed { idx, label ->
                            FilterChip(
                                selected = testMode == idx,
                                onClick = { testMode = idx; revealedKeys = emptySet() },
                                label = { Text(label) }
                            )
                        }
                    }
                }
                val ayahs = testSurah?.ayahs?.filter { it.ayahNumber in fromAyah..toAyah }.orEmpty()
                val displayAyahs = if (testMode == 2) ayahs.shuffled() else ayahs
                items(displayAyahs, key = { it.globalNumber }) { ayah ->
                    val isRevealed = ayah.key in revealedKeys
                    val words = ayah.textUthmani.split(" ")
                    val shownText = when {
                        isRevealed || testMode == 2 -> ayah.textUthmani
                        testMode == 1 -> "${words.firstOrNull().orEmpty()}  ••••••  ﴿${ArabicNormalizer.toArabicIndicDigits(ayah.ayahNumber)}﴾"
                        else -> "اضغط لإظهار الآية ﴿${ArabicNormalizer.toArabicIndicDigits(ayah.ayahNumber)}﴾"
                    }
                    NoorCard(onClick = {
                        revealedKeys = if (isRevealed) revealedKeys - ayah.key else revealedKeys + ayah.key
                    }) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = shownText,
                                fontFamily = AmiriQuranFamily,
                                fontSize = 20.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = if (isRevealed) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "إظهار أو إخفاء الآية"
                            )
                        }
                    }
                }
            }

            // 3: Khatmah Planner + Dua Khatm
            3 -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { showCreateKhatmah = true },
                            modifier = Modifier.weight(1f).testTag("create_khatmah_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("ختمة جديدة")
                        }
                        OutlinedButton(onClick = onOpenDuaKhatm, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.AutoStories, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("دعاء الختم")
                        }
                    }
                }
                items(khatmahPlans, key = { it.id }) { k ->
                    val calc = SpacedRepetitionEngine.calculateKhatmahWird(k.totalPages, k.pagesCompleted, k.targetDays)
                    val pct = (k.pagesCompleted.toFloat() / k.totalPages.coerceAtLeast(1)).coerceIn(0f, 1f)
                    NoorCard {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(k.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                IconButton(onClick = { vm.deleteKhatmahPlan(k.id) }, modifier = Modifier.size(48.dp)) {
                                    Icon(Icons.Default.Delete, contentDescription = "حذف الختمة")
                                }
                            }
                            Text(
                                text = if (k.completed) "أتممت هذه الختمة بحمد الله!" else "المقروء: ${k.pagesCompleted}/604 • الورد المقترح: ${calc.pagesPerDay} صفحة/يوم",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            LinearProgressIndicator(progress = { pct }, modifier = Modifier.fillMaxWidth(), color = GoldRoyal)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { vm.addKhatmahPages(k, calc.pagesPerDay.coerceAtLeast(4)) },
                                    enabled = !k.completed,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("أتممت ورد اليوم")
                                }
                                OutlinedButton(
                                    onClick = { onOpenPage((k.pagesCompleted + 1).coerceIn(1, 604)) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("افتح المصحف")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
