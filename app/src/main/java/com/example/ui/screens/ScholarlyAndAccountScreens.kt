package com.example.ui.screens

import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cloud.onGoogleSignInClicked
import com.example.cloud.signOutGoogle
import com.example.quran.ArabicNormalizer
import com.example.ui.AiUiState
import com.example.ui.NoorViewModel
import com.example.ui.components.NoorCard
import com.example.ui.theme.AmiriQuranFamily
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.GoldBright
import com.example.ui.theme.SuccessGreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SurahVirtuesScreen(
    vm: NoorViewModel,
    onBack: () -> Unit,
    onJumpToSurah: (Int, Int) -> Unit
) {
    val isReady by vm.isQuranReady.collectAsStateWithLifecycle()
    val items = remember(isReady) { vm.getSurahVirtues() }
    val disclaimer = remember(isReady) { vm.getVirtuesDisclaimer() }
    var filterCat by remember { mutableStateOf("all") }

    val cats = listOf(
        "all" to "الكل",
        "friday" to "يوم الجمعة",
        "sleep" to "عند النوم",
        "after_prayer" to "بعد الصلاة",
        "morning_evening" to "الصباح والمساء",
        "general" to "فضل عام"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("فضائل السور وأوقات القراءة") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "رجوع")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).testTag("virtues_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                NoorCard(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                    Text(
                        text = disclaimer,
                        modifier = Modifier.padding(14.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    cats.forEach { (id, label) ->
                        FilterChip(
                            selected = filterCat == id,
                            onClick = { filterCat = id },
                            label = { Text(label) }
                        )
                    }
                }
            }
            val filtered = items.filter { filterCat == "all" || it.timeCategory == filterCat }
            items(filtered, key = { it.id }) { h ->
                NoorCard {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(h.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            Text(h.timeLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                        }
                        Text(h.hadithText, style = MaterialTheme.typography.bodyLarge, lineHeight = 26.sp)
                        Text(
                            text = "الراوي: ${h.narrator} • المصدر: ${h.source} (${h.grade})",
                            style = MaterialTheme.typography.labelLarge,
                            color = SuccessGreen
                        )
                        if (h.scholarlyNote.isNotBlank()) {
                            Text(h.scholarlyNote, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Button(onClick = { onJumpToSurah(h.surahNumber, h.ayahNumber) }) {
                            Text("قراءة سورة ${h.surahName} الآن")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuaKhatmScreen(vm: NoorViewModel, onBack: () -> Unit) {
    val isReady by vm.isQuranReady.collectAsStateWithLifecycle()
    val prefs by vm.prefs.collectAsStateWithLifecycle()
    val dua = remember(isReady) { vm.getDuaKhatm() }
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("دعاء ختم القرآن الكريم") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val allText = dua?.sections?.joinToString("\n\n") { it.text }.orEmpty()
                        clipboard.setText(AnnotatedString(allText))
                        copied = true
                    }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "نسخ الدعاء")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).testTag("dua_khatm_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (dua != null) {
                item {
                    NoorCard(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("عدد ختماتك المكتملة: ${prefs.khatmahCount}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            Text(dua.scholarlyNote, style = MaterialTheme.typography.bodyMedium)
                            if (copied) Text("تم نسخ نص الدعاء", color = SuccessGreen, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
                items(dua.sections) { sec ->
                    NoorCard {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(sec.heading, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            Text(sec.text, fontFamily = AmiriQuranFamily, fontSize = 20.sp, lineHeight = 36.sp)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AsbabAndTafsirScreen(
    vm: NoorViewModel,
    onBack: () -> Unit,
    onJumpToSurah: (Int, Int) -> Unit
) {
    val isReady by vm.isQuranReady.collectAsStateWithLifecycle()
    val asbab = remember(isReady) { vm.getAllAsbab() }
    val disclaimer = remember(isReady) { vm.quranRepository.getAsbabDisclaimer() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("أسباب النزول والتفسير") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "رجوع")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).testTag("asbab_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (disclaimer.isNotBlank()) {
                item {
                    NoorCard(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                        Text(disclaimer, modifier = Modifier.padding(14.dp), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            items(asbab) { item ->
                NoorCard(onClick = { onJumpToSurah(item.surahNumber, item.ayahNumber) }) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("سورة ${item.surahName} • آية ${item.ayahNumber}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            Text(item.authenticity, style = MaterialTheme.typography.labelLarge, color = SuccessGreen)
                        }
                        Text(item.reason, style = MaterialTheme.typography.bodyLarge)
                        Text("المصدر: ${item.source}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalSearchScreen(
    vm: NoorViewModel,
    onBack: () -> Unit,
    onOpenAyahInMushaf: (Int, Int, Int) -> Unit
) {
    val query by vm.searchQuery.collectAsStateWithLifecycle()
    val results by vm.searchResults.collectAsStateWithLifecycle()
    var searchTafsir by remember { mutableStateOf(true) }

    val voiceSearchLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { res ->
        val spoken = res.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (!spoken.isNullOrBlank()) {
            vm.updateSearchQuery(spoken, searchTafsir)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("البحث القرآني الشامل") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "رجوع")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .testTag("global_search_screen"),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { vm.updateSearchQuery(it, searchTafsir) },
                    label = { Text("ابحث بكلمة أو رقم (مثل 2:255 أو الكهف)") },
                    modifier = Modifier.weight(1f).testTag("search_input"),
                    singleLine = true
                )
                IconButton(
                    onClick = {
                        try {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-SA")
                            }
                            voiceSearchLauncher.launch(intent)
                        } catch (_: Exception) {
                        }
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.Mic, contentDescription = "بحث صوتي")
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("تضمين البحث في التفسير والترجمة")
                Switch(checked = searchTafsir, onCheckedChange = {
                    searchTafsir = it
                    vm.updateSearchQuery(query, it)
                })
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(results, key = { it.globalNumber }) { ayah ->
                    NoorCard(onClick = { onOpenAyahInMushaf(ayah.surahNumber, ayah.ayahNumber, ayah.pageNumber) }) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "سورة ${ayah.surahNameAr} • الآية ${ArabicNormalizer.toArabicIndicDigits(ayah.ayahNumber)} (صفحة ${ayah.pageNumber})",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(ayah.textUthmani, fontFamily = AmiriQuranFamily, fontSize = 19.sp, lineHeight = 34.sp)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AiAssistantScreen(
    vm: NoorViewModel,
    onBack: () -> Unit,
    onJumpToAyah: (Int, Int) -> Unit
) {
    val aiState by vm.aiState.collectAsStateWithLifecycle()
    var question by remember { mutableStateOf("") }

    val quickPrompts = listOf(
        "ما معنى الصمد في سورة الإخلاص؟",
        "اشرح لي آية الكرسي باختصار",
        "ما سبب نزول سورة الكوثر؟",
        "أرشدني إلى آيات السكينة والتوكل"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("المساعد القرآني الذكي") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "رجوع")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .testTag("ai_assistant_screen"),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            NoorCard(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                Text(
                    text = "تنبيه علمي: يعتمد المساعد على المراجع القرآنية الموثقة محليًا (RAG)، ولا يُصدر فتاوى شرعية مستقلة ولا يُنشئ نصوصًا قرآنية.",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                quickPrompts.forEach { q ->
                    AssistChip(onClick = { question = q; vm.askAiAssistant(q) }, label = { Text(q, fontSize = 11.sp) })
                }
            }

            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                when (val s = aiState) {
                    is AiUiState.Idle -> item {
                        NoorCard {
                            Text("اسأل عن تفسير آية، معنى مفردة قرآنية، أو سياق نزول موثق.", modifier = Modifier.padding(16.dp))
                        }
                    }
                    is AiUiState.Loading -> item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Text("جاري البحث في المراجع الموثقة وتحضير الإجابة...")
                        }
                    }
                    is AiUiState.Streaming -> item {
                        NoorCard {
                            Text(s.text, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    is AiUiState.Success -> {
                        item {
                            NoorCard {
                                Text(s.text, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                        if (s.citations.isNotEmpty()) {
                            item {
                                Text("المراجع القرآنية المرتبطة:", style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(6.dp))
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    s.citations.forEach { cit ->
                                        AssistChip(
                                            onClick = {
                                                if (cit.surahNumber != null && cit.ayahNumber != null) {
                                                    onJumpToAyah(cit.surahNumber, cit.ayahNumber)
                                                }
                                            },
                                            label = { Text(cit.title) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    is AiUiState.Error -> item {
                        NoorCard(containerColor = MaterialTheme.colorScheme.errorContainer) {
                            Text(s.message, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = question,
                    onValueChange = { question = it },
                    label = { Text("اكتب سؤالك هنا...") },
                    modifier = Modifier.weight(1f).testTag("ai_query_input"),
                    maxLines = 3
                )
                IconButton(
                    onClick = { vm.askAiAssistant(question) },
                    modifier = Modifier.size(48.dp).testTag("ai_send_btn")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "إرسال", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupAndAccountScreen(vm: NoorViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val user by vm.currentUser.collectAsStateWithLifecycle()
    val cloudState by vm.cloudState.collectAsStateWithLifecycle()
    val credentialManager = remember { CredentialManager.create(context) }

    var authError by remember { mutableStateOf<String?>(null) }
    var localStatus by remember { mutableStateOf<String?>(null) }
    var confirmDeleteCloud by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val json = vm.cloudSyncRepository.exportLocalBackupJson()
                context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                localStatus = "تم تصدير النسخة الاحتياطية المحلية بنجاح."
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val raw = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty()
                val res = vm.cloudSyncRepository.importLocalBackupJson(raw)
                localStatus = res.fold(
                    onSuccess = { "تم استيراد $it عنصر بنجاح." },
                    onFailure = { "فشل استيراد الملف: ${it.localizedMessage}" }
                )
            }
        }
    }

    if (confirmDeleteCloud) {
        AlertDialog(
            onDismissRequest = { confirmDeleteCloud = false },
            title = { Text("تأكيد حذف البيانات السحابية") },
            text = { Text("سيتم حذف جميع بياناتك المخزنة في السحابة (العلامات وخطط الحفظ والملف السحابي) مع الإبقاء على بيانات جهازك الحالية.") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = { confirmDeleteCloud = false; vm.deleteCloudData() }
                ) { Text("حذف نهائي") }
            },
            dismissButton = { TextButton(onClick = { confirmDeleteCloud = false }) { Text("إلغاء") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("النسخ الاحتياطي والمزامنة") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "رجوع")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).testTag("backup_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                NoorCard {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("المزامنة السحابية الاختيارية", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        Text("يعمل التطبيق بالكامل دون تسجيل دخول. يمكنك ربط حساب Google للمزامنة الاختيارية بين أجهزتك.", style = MaterialTheme.typography.bodyMedium)
                        if (user == null) {
                            Button(
                                onClick = {
                                    onGoogleSignInClicked(
                                        context = context,
                                        credentialManager = credentialManager,
                                        scope = scope,
                                        onAuthSuccess = { authError = null },
                                        onAuthError = { authError = it }
                                    )
                                },
                                modifier = Modifier.fillMaxWidth().testTag("google_signin_btn")
                            ) {
                                Text("تسجيل الدخول بحساب Google")
                            }
                        } else {
                            Text("مسجل الدخول: ${user?.displayName ?: user?.email ?: user?.uid}", fontWeight = FontWeight.Bold, color = SuccessGreen)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { vm.backupToCloud() }, enabled = !cloudState.isBusy, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("رفع الآن")
                                }
                                OutlinedButton(onClick = { vm.restoreFromCloud() }, enabled = !cloudState.isBusy, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("استعادة")
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { confirmDeleteCloud = true }, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("حذف السحابة")
                                }
                                OutlinedButton(
                                    onClick = { signOutGoogle(credentialManager, scope) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("تسجيل الخروج")
                                }
                            }
                        }
                        authError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        cloudState.statusMessage?.let { Text(it, color = SuccessGreen) }
                        cloudState.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    }
                }
            }

            item {
                NoorCard {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("النسخ الاحتياطي المحلي (ملف JSON)", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { exportLauncher.launch("noor_alquran_backup.json") }, modifier = Modifier.weight(1f)) {
                                Text("تصدير ملف")
                            }
                            OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json")) }, modifier = Modifier.weight(1f)) {
                                Text("استيراد ملف")
                            }
                        }
                        localStatus?.let { Text(it, color = SuccessGreen) }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(vm: NoorViewModel, onBack: () -> Unit) {
    val prefs by vm.prefs.collectAsStateWithLifecycle()
    val integrity by vm.integrityReport.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var showSourcesDialog by remember { mutableStateOf(false) }

    if (showSourcesDialog) {
        AlertDialog(
            onDismissRequest = { showSourcesDialog = false },
            title = { Text("مصادر المحتوى الشرعي والتراخيص") },
            text = {
                Text(
                    """
                    • النص القرآني بالرسم العثماني: مجمع الملك فهد لطباعة المصحف الشريف / مصحف المدينة عبر Tanzil / AlQuran Cloud، محفوظ دون تعديل ومحقق بالبصمة SHA-256.
                    • التفسير الميسر: مجمع الملك فهد لطباعة المصحف الشريف.
                    • الترجمة الإنجليزية: Sahih International.
                    • الأحاديث النبوية: من الصحيحين (البخاري ومسلم) والسنن الموثقة مع العزو للكتاب والرقم.
                    • الأذكار: حصن المسلم للشيخ سعيد بن علي بن وهف القحطاني.
                    • التلاوات والإذاعات: مكتبة MP3Quran و Islamic Network (للاستخدام غير التجاري المجاني).
                    """.trimIndent()
                )
            },
            confirmButton = { Button(onClick = { showSourcesDialog = false }) { Text("إغلاق") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("الإعدادات والمصادر") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "رجوع")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).testTag("settings_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                NoorCard {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("مظهر التطبيق العام", style = MaterialTheme.typography.titleMedium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("system" to "تلقائي", "light" to "نهاري", "dark" to "ليلي").forEach { (id, label) ->
                                FilterChip(
                                    selected = prefs.darkModeSetting == id,
                                    onClick = { scope.launch { vm.prefsStore.updateDarkMode(id) } },
                                    label = { Text(label) }
                                )
                            }
                        }
                        HorizontalDivider()
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("إبقاء الشاشة مضاءة أثناء القراءة")
                            Switch(
                                checked = prefs.keepScreenOn,
                                onCheckedChange = { scope.launch { vm.prefsStore.updateReaderTypography(keepScreenOn = it) } }
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("تفعيل الأوامر الصوتية العربية")
                            Switch(
                                checked = prefs.voiceCommandsEnabled,
                                onCheckedChange = { scope.launch { vm.prefsStore.setVoiceCommandsEnabled(it) } }
                            )
                        }
                    }
                }
            }

            item {
                NoorCard {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("تعديل التاريخ الهجري (حسب الرؤية الشرعية):", style = MaterialTheme.typography.titleMedium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            (-2..2).forEach { offset ->
                                FilterChip(
                                    selected = prefs.hijriOffsetDays == offset,
                                    onClick = { scope.launch { vm.prefsStore.updatePrayerSettings(hijriOffset = offset) } },
                                    label = { Text(if (offset > 0) "+$offset" else "$offset") }
                                )
                            }
                        }
                    }
                }
            }

            item {
                NoorCard {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Verified, contentDescription = null, tint = SuccessGreen)
                            Text("تقرير سلامة النص القرآني الموثق", style = MaterialTheme.typography.titleMedium)
                        }
                        integrity?.let { rep ->
                            Text(
                                text = "الحالة: ${if (rep.passed) "سليم ومتحقق ١٠٠٪" else "يوجد تنبيه"}\nالسور: ${rep.surahCount} • الآيات: ${rep.ayahCount} • الصفحات: ${rep.pageCount}\nالتحقق من البصمة SHA-256: ${if (rep.checksumsMatched) "متطابقة" else "غير متطابقة"}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { vm.runIntegrityVerification() }, modifier = Modifier.weight(1f)) {
                                Text("إعادة الفحص")
                            }
                            Button(onClick = { showSourcesDialog = true }, modifier = Modifier.weight(1f)) {
                                Text("المصادر والتراخيص")
                            }
                        }
                    }
                }
            }
        }
    }
}
