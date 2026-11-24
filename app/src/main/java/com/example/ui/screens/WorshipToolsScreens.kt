package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.islamic.AsrMadhab
import com.example.islamic.CalculationMethod
import com.example.islamic.QiblaCalculator
import com.example.ui.NoorViewModel
import com.example.ui.components.NoorCard
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldRoyal
import com.example.ui.theme.SuccessGreen
import kotlinx.coroutines.launch

@Composable
fun MoreHubScreen(
    onOpenAi: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenVirtues: () -> Unit,
    onOpenDuaKhatm: () -> Unit,
    onOpenAsbab: () -> Unit,
    onOpenPrayer: () -> Unit,
    onOpenQibla: () -> Unit,
    onOpenAdhkar: () -> Unit,
    onOpenTasbeeh: () -> Unit,
    onOpenRadio: () -> Unit,
    onOpenTvMode: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val entries = listOf(
        MoreHubItem("المساعد القرآني الذكي (AI)", "استفسارات التفسير والمعاني مع المراجع", Icons.Default.AutoAwesome, "more_ai", onOpenAi),
        MoreHubItem("البحث القرآني الشامل", "بحث فوري في الآيات والتفسير والبحث الصوتي", Icons.Default.Search, "more_search", onOpenSearch),
        MoreHubItem("فضائل السور وأوقات القراءة", "أحاديث صحيحة مصنفة حسب الأوقات والصلوات", Icons.Default.Favorite, "more_virtues", onOpenVirtues),
        MoreHubItem("دعاء ختم القرآن الكريم", "النص الكامل بالتشكيل مع الضابط العلمي", Icons.Default.AutoStories, "more_dua_khatm", onOpenDuaKhatm),
        MoreHubItem("أسباب النزول ومكتبة التفسير", "روايات أسباب النزول الموثقة والتفاسير", Icons.AutoMirrored.Filled.FormatListBulleted, "more_asbab", onOpenAsbab),
        MoreHubItem("مواقيت الصلاة والأذان", "الحساب الفلكي المعتمد وتنبيهات الأذان", Icons.Default.AccessTime, "more_prayer", onOpenPrayer),
        MoreHubItem("اتجاه القبلة", "بوصلة دقيقة مع تنبيه المعايرة", Icons.Default.Explore, "more_qibla", onOpenQibla),
        MoreHubItem("الأذكار (حصن المسلم)", "أذكار الصباح والمساء والنوم والصلاة", Icons.Default.Spa, "more_adhkar", onOpenAdhkar),
        MoreHubItem("السبحة الإلكترونية", "تسبيح مع الاهتزاز وسجل الأذكار", Icons.Default.Spa, "more_tasbeeh", onOpenTasbeeh),
        MoreHubItem("راديو القرآن الكريم", "١٢ إذاعة مباشرة للتلاوات والفتاوى", Icons.Default.Radio, "more_radio", onOpenRadio),
        MoreHubItem("عرض المصحف على التلفاز", "وضع القراءة الكبيرة والشاشات الخارجية", Icons.Default.Tv, "more_tv", onOpenTvMode),
        MoreHubItem("النسخ الاحتياطي والمزامنة", "حساب Google الاختياري أو تصدير JSON محلي", Icons.Default.Backup, "more_backup", onOpenBackup),
        MoreHubItem("الإعدادات والمصادر", "المظهر والخطوط والتقويم الهجري وتوثيق النص", Icons.Default.Settings, "more_settings", onOpenSettings)
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("more_hub_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "المكتبة والأدوات الإسلامية",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        items(entries, key = { it.tag }) { item ->
            NoorCard(onClick = item.onClick, modifier = Modifier.testTag(item.tag)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(item.icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.title, style = MaterialTheme.typography.titleMedium)
                        Text(item.subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

private data class MoreHubItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val tag: String,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PrayerTimesScreen(vm: NoorViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs by vm.prefs.collectAsStateWithLifecycle()
    val schedule by vm.prayerSchedule.collectAsStateWithLifecycle()
    val cities = remember { vm.getCities() }
    val scope = rememberCoroutineScope()
    var showCityPicker by remember { mutableStateOf(false) }

    val locPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) vm.detectCoarseLocation(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("مواقيت الصلاة والأذان") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "رجوع")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("prayer_times_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                NoorCard(containerColor = EmeraldDeep) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("المدينة الحالية: ${prefs.cityName}", style = MaterialTheme.typography.titleLarge, color = GoldBright)
                        Text("الصلاة القادمة: ${schedule.nextPrayer.nameAr} (${schedule.nextPrayer.formattedTime})", style = MaterialTheme.typography.titleMedium, color = Color.White)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { showCityPicker = !showCityPicker }) {
                                Text("اختيار المدينة", color = GoldBright)
                            }
                            Button(onClick = {
                                val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                if (granted) vm.detectCoarseLocation(context)
                                else locPermLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                            }) {
                                Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("تحديد تلقائي")
                            }
                        }
                    }
                }
            }

            if (showCityPicker) {
                item {
                    NoorCard {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("اختر من المدن المتاحة دون إنترنت:", style = MaterialTheme.typography.titleMedium)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                cities.forEach { c ->
                                    FilterChip(
                                        selected = prefs.cityName == c.name,
                                        onClick = {
                                            vm.selectCity(c)
                                            showCityPicker = false
                                        },
                                        label = { Text(c.name, fontSize = 12.sp) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            items(schedule.prayers, key = { it.key }) { entry ->
                val isNext = entry.key == schedule.nextPrayer.key
                val adhanOn = when (entry.key) {
                    "fajr" -> prefs.adhanFajr
                    "dhuhr" -> prefs.adhanDhuhr
                    "asr" -> prefs.adhanAsr
                    "maghrib" -> prefs.adhanMaghrib
                    "isha" -> prefs.adhanIsha
                    else -> false
                }
                NoorCard(
                    containerColor = if (isNext) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(entry.nameAr, style = MaterialTheme.typography.titleMedium)
                            Text(entry.formattedTime, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                        }
                        if (entry.key != "sunrise") {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("تنبيه", style = MaterialTheme.typography.labelMedium)
                                Spacer(Modifier.width(6.dp))
                                Switch(
                                    checked = adhanOn,
                                    onCheckedChange = { enabled ->
                                        scope.launch { vm.prefsStore.toggleAdhan(entry.key, enabled) }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            item {
                NoorCard {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("طريقة الحساب الفلكي:", style = MaterialTheme.typography.titleMedium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            CalculationMethod.entries.forEach { method ->
                                FilterChip(
                                    selected = prefs.calculationMethod == method.id,
                                    onClick = { scope.launch { vm.prefsStore.updatePrayerSettings(method = method.id) } },
                                    label = { Text(method.labelAr, fontSize = 12.sp) }
                                )
                            }
                        }
                        Text("مذهب العصر:", style = MaterialTheme.typography.titleMedium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            AsrMadhab.entries.forEach { m ->
                                FilterChip(
                                    selected = prefs.asrMadhab == m.id,
                                    onClick = { scope.launch { vm.prefsStore.updatePrayerSettings(madhab = m.id) } },
                                    label = { Text(m.labelAr, fontSize = 12.sp) }
                                )
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
fun QiblaScreen(vm: NoorViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs by vm.prefs.collectAsStateWithLifecycle()
    val qiblaBearing = remember(prefs.cityLat, prefs.cityLon) { vm.getQiblaBearing().toFloat() }
    var azimuth by remember { mutableFloatStateOf(0f) }
    var lowAccuracy by remember { mutableStateOf(false) }
    var hasSensors by remember { mutableStateOf(true) }

    DisposableEffect(context) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val accel = sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val mag = sm?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        if (sm == null || accel == null || mag == null) {
            hasSensors = false
            onDispose { }
        } else {
            val gravity = FloatArray(3)
            val geomag = FloatArray(3)
            val rMat = FloatArray(9)
            val iMat = FloatArray(9)
            val orient = FloatArray(3)
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    val alpha = 0.25f
                    if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                        for (k in 0..2) gravity[k] = alpha * event.values[k] + (1 - alpha) * gravity[k]
                    } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                        for (k in 0..2) geomag[k] = alpha * event.values[k] + (1 - alpha) * geomag[k]
                    }
                    if (SensorManager.getRotationMatrix(rMat, iMat, gravity, geomag)) {
                        SensorManager.getOrientation(rMat, orient)
                        val deg = ((Math.toDegrees(orient[0].toDouble()) + 360.0) % 360.0).toFloat()
                        azimuth = deg
                    }
                }
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                    lowAccuracy = accuracy <= SensorManager.SENSOR_STATUS_ACCURACY_LOW
                }
            }
            sm.registerListener(listener, accel, SensorManager.SENSOR_DELAY_UI)
            sm.registerListener(listener, mag, SensorManager.SENSOR_DELAY_UI)
            onDispose { sm.unregisterListener(listener) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("اتجاه القبلة") },
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
                .padding(20.dp)
                .testTag("qibla_screen"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            NoorCard {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("الموقع: ${prefs.cityName}", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "زاوية القبلة من الشمال الحقيقي: ${qiblaBearing.toInt()}° (${QiblaCalculator.directionLabelAr(qiblaBearing.toDouble())})",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Box(
                modifier = Modifier.size(260.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val radius = size.minDimension / 2f
                    drawCircle(color = EmeraldDeep, radius = radius)
                    drawCircle(color = GoldRoyal, radius = radius - 10f, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 6f))
                    val targetAngle = (qiblaBearing - azimuth + 360f) % 360f
                    rotate(degrees = targetAngle) {
                        val arrow = Path().apply {
                            moveTo(center.x, center.y - radius + 28f)
                            lineTo(center.x - 24f, center.y + 18f)
                            lineTo(center.x + 24f, center.y + 18f)
                            close()
                        }
                        drawPath(arrow, color = GoldBright)
                        drawCircle(color = Color.White, radius = 14f, center = center)
                    }
                }
            }

            if (!hasSensors || lowAccuracy) {
                NoorCard(containerColor = MaterialTheme.colorScheme.errorContainer) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                        Text(
                            text = if (!hasSensors) "هذا الجهاز لا يحتوي على مستشعر مغناطيسي للبوصلة؛ يمكنك الاعتماد على الزاوية المحسوبة (${qiblaBearing.toInt()}° من الشمال)."
                            else "دقة البوصلة منخفضة أو يوجد تشويش مغناطيسي؛ حرّك الهاتف على شكل الرقم 8 للمعايرة.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AdhkarScreen(vm: NoorViewModel, onBack: () -> Unit) {
    val isReady by vm.isQuranReady.collectAsStateWithLifecycle()
    val progress by vm.adhkarProgress.collectAsStateWithLifecycle()
    val adhkar = remember(isReady) { vm.getAdhkar() }
    val categories = remember(adhkar) { listOf("الكل") + adhkar.map { it.category }.distinct() }
    var selectedCat by remember { mutableStateOf("الكل") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("الأذكار (حصن المسلم)") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "رجوع")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).testTag("adhkar_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = selectedCat == cat,
                            onClick = { selectedCat = cat },
                            label = { Text(cat) }
                        )
                    }
                }
            }
            val list = adhkar.filter { selectedCat == "الكل" || it.category == selectedCat }
            items(list, key = { it.id }) { item ->
                val count = progress.firstOrNull { it.dhikrId == item.id }?.currentCount ?: 0
                val done = count >= item.repeat
                NoorCard(
                    onClick = { vm.advanceDhikr(item) },
                    containerColor = if (done) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(item.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            Text("$count / ${item.repeat}", style = MaterialTheme.typography.labelLarge, color = if (done) SuccessGreen else GoldRoyal)
                        }
                        Text(item.text, style = MaterialTheme.typography.bodyLarge, lineHeight = 28.sp)
                        Text("المصدر: ${item.source}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TasbeehScreen(vm: NoorViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs by vm.prefs.collectAsStateWithLifecycle()
    val logs by vm.tasbeehLogs.collectAsStateWithLifecycle()
    var hapticEnabled by remember { mutableStateOf(true) }

    val dhikrChoices = listOf(
        "سُبْحَانَ اللَّهِ",
        "الْحَمْدُ لِلَّهِ",
        "اللَّهُ أَكْبَرُ",
        "لَا إِلَهَ إِلَّا اللَّهُ",
        "أَسْتَغْفِرُ اللَّهَ",
        "اللَّهُمَّ صَلِّ عَلَى مُحَمَّدٍ"
    )

    fun vibrateStep() {
        if (!hapticEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vmgr = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vmgr?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(25L, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v?.vibrate(VibrationEffect.createOneShot(25L, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    v?.vibrate(25L)
                }
            }
        } catch (_: Exception) {
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("السبحة الإلكترونية") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(onClick = { vm.resetTasbeehCurrent() }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.Refresh, contentDescription = "تصفير الجلسة")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).testTag("tasbeeh_screen"),
            contentPadding = PaddingValues(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    dhikrChoices.forEach { d ->
                        FilterChip(
                            selected = prefs.selectedDhikr == d,
                            onClick = { vm.setTasbeehSettings(d, prefs.tasbeehTarget) },
                            label = { Text(d, fontSize = 12.sp) }
                        )
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    listOf(33, 99, 100, 1000).forEach { target ->
                        FilterChip(
                            selected = prefs.tasbeehTarget == target,
                            onClick = { vm.setTasbeehSettings(prefs.selectedDhikr, target) },
                            label = { Text("$target") }
                        )
                    }
                    Switch(checked = hapticEnabled, onCheckedChange = { hapticEnabled = it })
                    Text("اهتزاز", fontSize = 12.sp)
                }
            }
            item {
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .clip(CircleShape)
                        .background(EmeraldDeep)
                        .clickable {
                            vibrateStep()
                            vm.incrementTasbeeh()
                        }
                        .testTag("tasbeeh_tap_circle"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${prefs.tasbeehCurrent}",
                            fontSize = 54.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldBright
                        )
                        Text(
                            text = "من ${prefs.tasbeehTarget}",
                            color = Color.White.copy(alpha = 0.85f),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = prefs.selectedDhikr,
                            color = GoldRoyal,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("الإجمالي الكلي: ${prefs.tasbeehTotal}", style = MaterialTheme.typography.titleMedium)
            }
            if (logs.isNotEmpty()) {
                item {
                    Text("سجل الجلسات الأخيرة", style = MaterialTheme.typography.titleMedium, modifier = Modifier.fillMaxWidth())
                }
                items(logs.take(10), key = { it.id }) { log ->
                    NoorCard {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(log.dhikr)
                            Text("${log.count} تسبيحة", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadioScreen(vm: NoorViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val stations = remember { vm.getRadioStations() }
    val audioState by vm.audioState.collectAsStateWithLifecycle()

    val isOnline = remember {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val net = cm?.activeNetwork
        val caps = cm?.getNetworkCapabilities(net)
        caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("راديو القرآن الكريم") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "رجوع")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).testTag("radio_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (!isOnline) {
                item {
                    NoorCard(containerColor = MaterialTheme.colorScheme.errorContainer) {
                        Text(
                            text = "لا يوجد اتصال بالإنترنت حاليًا. يتطلب البث الإذاعي المباشر اتصالاً بالشبكة.",
                            modifier = Modifier.padding(14.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
            items(stations, key = { it.id }) { station ->
                val isCurrent = audioState.isRadioMode && audioState.radioStationName == station.name
                NoorCard(
                    containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(station.name, style = MaterialTheme.typography.titleMedium)
                            Text(station.category, style = MaterialTheme.typography.bodyMedium)
                        }
                        IconButton(
                            onClick = {
                                if (isCurrent && audioState.isPlaying) vm.audioController.stop()
                                else vm.audioController.playRadioStation(station)
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = if (isCurrent && audioState.isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = "تشغيل الإذاعة"
                            )
                        }
                    }
                }
            }
        }
    }
}
