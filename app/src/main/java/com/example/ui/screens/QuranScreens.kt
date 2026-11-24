package com.example.ui.screens

import android.app.Activity
import android.app.Presentation
import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.view.Display
import android.view.Gravity
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.quran.ArabicNormalizer
import com.example.quran.Ayah
import com.example.quran.TajweedHighlighter
import com.example.ui.NoorViewModel
import com.example.ui.components.AyahActionsBottomSheet
import com.example.ui.components.NoorCard
import com.example.ui.components.OrnamentalSurahBanner
import com.example.ui.components.SurahNumberBadge
import com.example.ui.theme.AmiriQuranFamily
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldRoyal
import com.example.ui.theme.IvoryBackground
import com.example.ui.theme.ReaderFontOption
import com.example.ui.theme.ReaderThemeMode
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranIndexScreen(
    vm: NoorViewModel,
    onOpenPage: (Int) -> Unit,
    onOpenContinuous: (Int) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenTvMode: () -> Unit
) {
    val isReady by vm.isQuranReady.collectAsStateWithLifecycle()
    val bookmarks by vm.bookmarks.collectAsStateWithLifecycle()
    val prefs by vm.prefs.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    var pageInput by remember { mutableStateOf("") }

    val surahs = remember(isReady) { vm.getSurahs() }
    val juzs = remember(isReady) { vm.getJuzList() }
    val hizbs = remember(isReady) { vm.getHizbList() }
    val topics = remember(isReady) { vm.getTopics() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("quran_index_screen")
    ) {
        Surface(tonalElevation = 2.dp, color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "المصحف الشريف",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = onOpenSearch, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Default.Search, contentDescription = "بحث")
                        }
                        IconButton(onClick = onOpenTvMode, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Default.Tv, contentDescription = "وضع التلفاز")
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = pageInput,
                        onValueChange = { if (it.length <= 3 && it.all(Char::isDigit)) pageInput = it },
                        label = { Text("انتقال لصفحة (١-٦٠٤)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = {
                            pageInput.toIntOrNull()?.let { p -> onOpenPage(p.coerceIn(1, 604)) }
                        }),
                        modifier = Modifier.weight(1f)
                    )
                    Button(onClick = {
                        val p = pageInput.toIntOrNull()?.coerceIn(1, 604) ?: prefs.lastPage
                        onOpenPage(p)
                    }) {
                        Text("فتح")
                    }
                    OutlinedButton(onClick = { onOpenContinuous(prefs.lastSurah) }) {
                        Text("مستمر")
                    }
                }
            }
        }

        val tabs = listOf("السور", "الأجزاء", "الأحزاب", "المواضيع", "العلامات")
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
                    val revAr = if (surah.revelationType.equals("Meccan", true)) "مكية" else "مدنية"
                    NoorCard(
                        onClick = {
                            vm.openSurah(surah.number)
                            onOpenPage(surah.startPage)
                        },
                        modifier = Modifier.testTag("surah_item_${surah.number}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                SurahNumberBadge(surah.number)
                                Column {
                                    Text(
                                        text = "سورة ${surah.nameAr}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "$revAr • ${ArabicNormalizer.toArabicIndicDigits(surah.ayahCount)} آية • صفحة ${surah.startPage}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Text(
                                text = "قراءة مستمرة",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        vm.openSurah(surah.number)
                                        onOpenContinuous(surah.number)
                                    }
                                    .padding(8.dp)
                            )
                        }
                    }
                }
            }
            1 -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(juzs, key = { it.number }) { juz ->
                    NoorCard(onClick = { onOpenPage(juz.startPage) }) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("الجزء ${ArabicNormalizer.toArabicIndicDigits(juz.number)}", style = MaterialTheme.typography.titleMedium)
                                Text("يبدأ بسورة ${juz.startSurahName} • آية ${juz.startAyahNumber}", style = MaterialTheme.typography.bodyMedium)
                            }
                            Text("صفحة ${juz.startPage}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
            2 -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(hizbs, key = { it.number }) { hizb ->
                    NoorCard(onClick = { onOpenPage(hizb.startPage) }) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("الحزب ${ArabicNormalizer.toArabicIndicDigits(hizb.number)} (الجزء ${hizb.juzNumber})", style = MaterialTheme.typography.titleMedium)
                                Text("سورة ${hizb.startSurahName} • آية ${hizb.startAyahNumber}", style = MaterialTheme.typography.bodyMedium)
                            }
                            Text("صفحة ${hizb.startPage}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
            3 -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(topics, key = { it.id }) { topic ->
                    NoorCard {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(topic.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            Text(topic.description, style = MaterialTheme.typography.bodyMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                topic.references.take(4).forEach { (s, a) ->
                                    AssistChip(
                                        onClick = {
                                            vm.openSurah(s, a)
                                            vm.getSurah(s)?.ayahs?.getOrNull(a - 1)?.let { onOpenPage(it.pageNumber) }
                                        },
                                        label = { Text("$s:$a") }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            4 -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (bookmarks.isEmpty()) {
                    item {
                        NoorCard {
                            Text(
                                text = "لا توجد علامات مرجعية محفوظة بعد. اضغط على أي آية أثناء القراءة لحفظ علامة.",
                                modifier = Modifier.padding(20.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    items(bookmarks, key = { it.id }) { bm ->
                        NoorCard(onClick = {
                            vm.openSurah(bm.surahNumber, bm.ayahNumber)
                            onOpenPage(bm.pageNumber)
                        }) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.Bookmark, contentDescription = null, tint = GoldRoyal)
                                    Column {
                                        Text(bm.label, style = MaterialTheme.typography.titleMedium)
                                        Text("صفحة ${bm.pageNumber}", style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                                IconButton(onClick = { vm.deleteBookmark(bm.id) }, modifier = Modifier.size(48.dp)) {
                                    Icon(Icons.Default.Delete, contentDescription = "حذف العلامة")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MushafPageReaderScreen(
    vm: NoorViewModel,
    initialPage: Int,
    onBack: () -> Unit,
    onSwitchToContinuous: (Int) -> Unit,
    onOpenTvMode: () -> Unit
) {
    val context = LocalContext.current
    val prefs by vm.prefs.collectAsStateWithLifecycle()
    val audioState by vm.audioState.collectAsStateWithLifecycle()
    val isReady by vm.isQuranReady.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    KeepScreenOnEffect(enabled = prefs.keepScreenOn)

    val pagerState = rememberPagerState(
        initialPage = (initialPage - 1).coerceIn(0, 603),
        pageCount = { 604 }
    )
    var selectedAyah by remember { mutableStateOf<Ayah?>(null) }

    LaunchedEffect(pagerState.currentPage, isReady) {
        if (isReady) {
            vm.openPage(pagerState.currentPage + 1)
        }
    }

    Scaffold(
        topBar = {
            val pageData = remember(pagerState.currentPage, isReady) { vm.getPage(pagerState.currentPage + 1) }
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = pageData?.surahsOnPage?.joinToString(" • ") ?: "المصحف الشريف",
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1
                        )
                        Text(
                            text = "صفحة ${ArabicNormalizer.toArabicIndicDigits(pagerState.currentPage + 1)} • الجزء ${pageData?.juzNumber ?: 1} • الحزب ${pageData?.hizbNumber ?: 1}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        pageData?.ayahs?.firstOrNull()?.let { vm.playAyah(it, 1) }
                    }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "تلاوة الصفحة")
                    }
                    IconButton(onClick = onOpenTvMode, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.Tv, contentDescription = "عرض التلفاز")
                    }
                    TextButtonOrChip(
                        label = "مستمر",
                        onClick = {
                            val surahNum = pageData?.ayahs?.firstOrNull()?.surahNumber ?: prefs.lastSurah
                            onSwitchToContinuous(surahNum)
                        }
                    )
                }
            )
        }
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("mushaf_pager")
        ) { pageIndex ->
            val pageData = remember(pageIndex, isReady) { vm.getPage(pageIndex + 1) }
            val ayahs = pageData?.ayahs.orEmpty()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(IvoryBackground)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .border(2.dp, GoldRoyal.copy(alpha = 0.65f), RoundedCornerShape(18.dp)),
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFFFDF9F0)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ayahs.forEachIndexed { idx, ayah ->
                            if (ayah.ayahNumber == 1) {
                                val surahObj = vm.getSurah(ayah.surahNumber)
                                OrnamentalSurahBanner(
                                    surahName = ayah.surahNameAr,
                                    revelationType = surahObj?.revelationType ?: "Meccan",
                                    ayahCount = surahObj?.ayahCount ?: 0
                                )
                                if (ayah.surahNumber != 1 && ayah.surahNumber != 9) {
                                    Text(
                                        text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                                        fontFamily = AmiriQuranFamily,
                                        fontSize = 21.sp,
                                        color = EmeraldDeep,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                    )
                                }
                            }
                            val isActiveAudio = audioState.activeGlobalAyah == ayah.globalNumber
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        vm.saveLastReadAyah(ayah)
                                        selectedAyah = ayah
                                    },
                                color = if (isActiveAudio) GoldRoyal.copy(alpha = 0.22f) else Color.Transparent
                            ) {
                                Text(
                                    text = "${ayah.textUthmani}  ﴿${ArabicNormalizer.toArabicIndicDigits(ayah.ayahNumber)}﴾" +
                                        if (ayah.sajdahType > 0) " ۩" else "",
                                    fontFamily = AmiriQuranFamily,
                                    fontSize = 23.sp,
                                    lineHeight = 44.sp,
                                    color = Color(0xFF16201C),
                                    textAlign = TextAlign.Justify,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                )
                            }
                            if (idx < ayahs.lastIndex) {
                                HorizontalDivider(color = GoldRoyal.copy(alpha = 0.14f))
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { scope.launch { pagerState.animateScrollToPage((pagerState.currentPage - 1).coerceAtLeast(0)) } },
                        enabled = pagerState.currentPage > 0,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "الصفحة السابقة")
                    }
                    Text(
                        text = "صفحة ${ArabicNormalizer.toArabicIndicDigits(pageIndex + 1)} / ٦٠٤",
                        style = MaterialTheme.typography.labelLarge,
                        color = EmeraldDeep
                    )
                    IconButton(
                        onClick = { scope.launch { pagerState.animateScrollToPage((pagerState.currentPage + 1).coerceAtMost(603)) } },
                        enabled = pagerState.currentPage < 603,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "الصفحة التالية")
                    }
                }
            }
        }
    }

    selectedAyah?.let { ayah ->
        val extraTafsir = vm.getDownloadedTafsir("ar.jalalayn", ayah.surahNumber, ayah.ayahNumber)
        AyahActionsBottomSheet(
            ayah = ayah,
            tafsirMuyassar = vm.getTafsirMuyassar(ayah.surahNumber, ayah.ayahNumber),
            extraTafsir = extraTafsir,
            wordMeanings = vm.getWordMeanings(ayah.surahNumber, ayah.ayahNumber),
            englishTranslation = vm.getEnglishTranslation(ayah.surahNumber, ayah.ayahNumber),
            sababAlNuzul = vm.getAsbabForAyah(ayah.surahNumber, ayah.ayahNumber),
            onDismiss = { selectedAyah = null },
            onPlayAyah = { repeat -> vm.playAyah(ayah, repeat) },
            onSaveBookmark = { label -> vm.addBookmark(ayah, label) },
            onCreateMemorizationPlan = {
                vm.getSurah(ayah.surahNumber)?.let { s -> vm.createMemorizationPlan(s, 5, 7) }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ContinuousReaderScreen(
    vm: NoorViewModel,
    surahNumber: Int,
    onBack: () -> Unit,
    onSwitchToPages: (Int) -> Unit
) {
    val prefs by vm.prefs.collectAsStateWithLifecycle()
    val audioState by vm.audioState.collectAsStateWithLifecycle()
    val isReady by vm.isQuranReady.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    KeepScreenOnEffect(enabled = prefs.keepScreenOn)

    val surah = remember(surahNumber, isReady) { vm.getSurah(surahNumber) }
    val readerTheme = ReaderThemeMode.fromId(prefs.readerThemeId)
    val readerFont = ReaderFontOption.fromId(prefs.readerFontId)
    var showAppearancePanel by remember { mutableStateOf(false) }
    var selectedAyah by remember { mutableStateOf<Ayah?>(null) }

    val listState = rememberLazyListState()
    LaunchedEffect(audioState.activeAyahNumber, audioState.surahNumber) {
        if (audioState.surahNumber == surahNumber && audioState.activeAyahNumber != null) {
            val targetIdx = (audioState.activeAyahNumber!! - 1).coerceAtLeast(0)
            listState.animateScrollToItem(targetIdx)
        }
    }

    Scaffold(
        containerColor = readerTheme.bgColor,
        topBar = {
            TopAppBar(
                title = {
                    Text("سورة ${surah?.nameAr ?: ""}", style = MaterialTheme.typography.titleLarge)
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(onClick = { showAppearancePanel = !showAppearancePanel }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.FormatSize, contentDescription = "إعدادات الخط والخلفية")
                    }
                    IconButton(onClick = {
                        surah?.let { vm.playSurah(it.number) }
                    }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "تشغيل السورة")
                    }
                    TextButtonOrChip(
                        label = "صفحات",
                        onClick = { onSwitchToPages(surah?.startPage ?: prefs.lastPage) }
                    )
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(readerTheme.bgColor)
        ) {
            if (showAppearancePanel) {
                Surface(
                    tonalElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("تخصيص القراءة المستمرة", style = MaterialTheme.typography.titleMedium)
                        Text("حجم الخط: ${prefs.readerFontSize.toInt()} sp")
                        Slider(
                            value = prefs.readerFontSize,
                            onValueChange = { size ->
                                scope.launch { vm.prefsStore.updateReaderTypography(fontSize = size) }
                            },
                            valueRange = 14f..48f
                        )
                        Text("تباعد الأسطر: ${"%.2f".format(prefs.readerLineSpacing)}")
                        Slider(
                            value = prefs.readerLineSpacing,
                            onValueChange = { ls ->
                                scope.launch { vm.prefsStore.updateReaderTypography(lineSpacing = ls) }
                            },
                            valueRange = 1.3f..2.6f
                        )
                        Text("نوع الخط العربي:")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ReaderFontOption.entries.forEach { opt ->
                                FilterChip(
                                    selected = opt.id == prefs.readerFontId,
                                    onClick = { scope.launch { vm.prefsStore.updateReaderTypography(fontId = opt.id) } },
                                    label = { Text(opt.labelAr, fontSize = 12.sp) }
                                )
                            }
                        }
                        Text("لون الخلفية:")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ReaderThemeMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = mode.id == prefs.readerThemeId,
                                    onClick = { scope.launch { vm.prefsStore.updateReaderTypography(themeId = mode.id) } },
                                    label = { Text(mode.labelAr, fontSize = 12.sp) }
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("تلوين علامات التجويد (في الوضع المستمر)")
                            Switch(
                                checked = prefs.tajweedColorsEnabled,
                                onCheckedChange = { enabled ->
                                    scope.launch { vm.prefsStore.updateReaderTypography(tajweedColors = enabled) }
                                }
                            )
                        }
                    }
                }
            }

            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (surah != null) {
                    item {
                        OrnamentalSurahBanner(surah.nameAr, surah.revelationType, surah.ayahCount)
                        if (surah.number != 1 && surah.number != 9) {
                            Text(
                                text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                                fontFamily = readerFont.fontFamily,
                                fontSize = (prefs.readerFontSize + 2f).sp,
                                color = readerTheme.accentColor,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                            )
                        }
                    }

                    items(surah.ayahs, key = { it.globalNumber }) { ayah ->
                        val isPlayingThis = audioState.activeGlobalAyah == ayah.globalNumber
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isPlayingThis) GoldRoyal.copy(alpha = 0.2f) else Color.Transparent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    vm.saveLastReadAyah(ayah)
                                    selectedAyah = ayah
                                }
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                val lineHeightSp = (prefs.readerFontSize * prefs.readerLineSpacing).sp
                                if (prefs.tajweedColorsEnabled) {
                                    val annotated = remember(ayah.textUthmani, readerTheme.textColor) {
                                        TajweedHighlighter.highlight(
                                            "${ayah.textUthmani}  ﴿${ArabicNormalizer.toArabicIndicDigits(ayah.ayahNumber)}﴾",
                                            readerTheme.textColor
                                        )
                                    }
                                    Text(
                                        text = annotated,
                                        fontFamily = readerFont.fontFamily,
                                        fontSize = prefs.readerFontSize.sp,
                                        lineHeight = lineHeightSp,
                                        letterSpacing = prefs.readerLetterSpacing.sp
                                    )
                                } else {
                                    Text(
                                        text = "${ayah.textUthmani}  ﴿${ArabicNormalizer.toArabicIndicDigits(ayah.ayahNumber)}﴾",
                                        fontFamily = readerFont.fontFamily,
                                        fontSize = prefs.readerFontSize.sp,
                                        lineHeight = lineHeightSp,
                                        letterSpacing = prefs.readerLetterSpacing.sp,
                                        color = readerTheme.textColor
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    selectedAyah?.let { ayah ->
        AyahActionsBottomSheet(
            ayah = ayah,
            tafsirMuyassar = vm.getTafsirMuyassar(ayah.surahNumber, ayah.ayahNumber),
            extraTafsir = vm.getDownloadedTafsir("ar.jalalayn", ayah.surahNumber, ayah.ayahNumber),
            wordMeanings = vm.getWordMeanings(ayah.surahNumber, ayah.ayahNumber),
            englishTranslation = vm.getEnglishTranslation(ayah.surahNumber, ayah.ayahNumber),
            sababAlNuzul = vm.getAsbabForAyah(ayah.surahNumber, ayah.ayahNumber),
            onDismiss = { selectedAyah = null },
            onPlayAyah = { repeat -> vm.playAyah(ayah, repeat) },
            onSaveBookmark = { label -> vm.addBookmark(ayah, label) },
            onCreateMemorizationPlan = {
                vm.getSurah(ayah.surahNumber)?.let { s -> vm.createMemorizationPlan(s, 5, 7) }
            }
        )
    }
}

@Composable
fun TvReaderScreen(
    vm: NoorViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs by vm.prefs.collectAsStateWithLifecycle()
    val audioState by vm.audioState.collectAsStateWithLifecycle()
    val isReady by vm.isQuranReady.collectAsStateWithLifecycle()
    var pageNum by remember { mutableIntStateOf(prefs.lastPage) }

    KeepScreenOnEffect(enabled = true)

    val pageData = remember(pageNum, isReady) { vm.getPage(pageNum) }
    val externalDisplays = remember {
        val dm = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
        dm?.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION)?.toList().orEmpty()
    }

    DisposableEffect(externalDisplays, pageNum) {
        val presentation = externalDisplays.firstOrNull()?.let { display ->
            TvQuranPresentation(context, display).apply {
                show()
                updateContent(
                    title = "نور القرآن — صفحة $pageNum",
                    body = pageData?.ayahs?.joinToString("  ") {
                        "${it.textUthmani} ﴿${ArabicNormalizer.toArabicIndicDigits(it.ayahNumber)}﴾"
                    }.orEmpty()
                )
            }
        }
        onDispose { presentation?.dismiss() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(EmeraldDark)
            .padding(24.dp)
            .testTag("tv_reader_screen")
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "خروج من وضع التلفاز", tint = GoldBright)
                    }
                    Column {
                        Text(
                            text = "عرض التلفاز • ${pageData?.surahsOnPage?.joinToString("، ") ?: ""}",
                            style = MaterialTheme.typography.titleLarge,
                            color = GoldBright
                        )
                        Text(
                            text = if (externalDisplays.isNotEmpty()) "متصل بشاشة خارجية" else "وضع القراءة على الشاشة الكبيرة (بدون مشتتات)",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                    }
                }
                IconButton(onClick = {
                    if (audioState.isPlaying) vm.audioController.togglePlayPause()
                    else pageData?.ayahs?.firstOrNull()?.let { vm.playAyah(it, 1) }
                }, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = if (audioState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "تشغيل أو إيقاف التلاوة",
                        tint = GoldBright
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                val combined = pageData?.ayahs?.joinToString("   ") {
                    "${it.textUthmani} ﴿${ArabicNormalizer.toArabicIndicDigits(it.ayahNumber)}﴾"
                }.orEmpty()
                Text(
                    text = combined,
                    fontFamily = AmiriQuranFamily,
                    fontSize = 32.sp,
                    lineHeight = 58.sp,
                    color = Color(0xFFFAF6EE),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { if (pageNum > 1) { pageNum--; vm.openPage(pageNum) } },
                    enabled = pageNum > 1
                ) {
                    Text("الصفحة السابقة", color = GoldBright)
                }
                Text(
                    text = "صفحة ${ArabicNormalizer.toArabicIndicDigits(pageNum)} / ٦٠٤",
                    color = GoldBright,
                    style = MaterialTheme.typography.titleMedium
                )
                OutlinedButton(
                    onClick = { if (pageNum < 604) { pageNum++; vm.openPage(pageNum) } },
                    enabled = pageNum < 604
                ) {
                    Text("الصفحة التالية", color = GoldBright)
                }
            }
        }
    }
}

private class TvQuranPresentation(
    context: Context,
    display: Display
) : Presentation(context, display) {
    private var titleView: TextView? = null
    private var bodyView: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(android.graphics.Color.parseColor("#06291F"))
            setPadding(64, 48, 64, 48)
            gravity = Gravity.CENTER
        }
        titleView = TextView(context).apply {
            textSize = 24f
            setTextColor(android.graphics.Color.parseColor("#E6CA82"))
            gravity = Gravity.CENTER
        }
        bodyView = TextView(context).apply {
            textSize = 36f
            setTextColor(android.graphics.Color.parseColor("#FAF6EE"))
            gravity = Gravity.CENTER
            setLineSpacing(24f, 1.2f)
        }
        layout.addView(titleView)
        layout.addView(bodyView)
        setContentView(layout)
    }

    fun updateContent(title: String, body: String) {
        titleView?.text = title
        bodyView?.text = body
    }
}

@Composable
private fun TextButtonOrChip(label: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(end = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun KeepScreenOnEffect(enabled: Boolean) {
    val context = LocalContext.current
    DisposableEffect(enabled) {
        val window = (context as? Activity)?.window
        if (enabled) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}
