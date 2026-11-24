package com.example.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.quran.ArabicNormalizer
import com.example.ui.NoorViewModel
import com.example.ui.components.MiniAudioPlayerBar
import com.example.ui.screens.AdhkarScreen
import com.example.ui.screens.AiAssistantScreen
import com.example.ui.screens.AsbabAndTafsirScreen
import com.example.ui.screens.BackupAndAccountScreen
import com.example.ui.screens.ContinuousReaderScreen
import com.example.ui.screens.DuaKhatmScreen
import com.example.ui.screens.GlobalSearchScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ListenScreen
import com.example.ui.screens.MemorizationScreen
import com.example.ui.screens.MoreHubScreen
import com.example.ui.screens.MushafPageReaderScreen
import com.example.ui.screens.PrayerTimesScreen
import com.example.ui.screens.QiblaScreen
import com.example.ui.screens.QuranIndexScreen
import com.example.ui.screens.RadioScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SurahVirtuesScreen
import com.example.ui.screens.TasbeehScreen
import com.example.ui.screens.TvReaderScreen

object NoorRoutes {
    const val HOME = "home"
    const val QURAN = "quran"
    const val LISTEN = "listen"
    const val MEMORIZE = "memorize"
    const val MORE = "more"
    const val READER_PAGES = "reader_pages"
    const val READER_CONTINUOUS = "reader_continuous"
    const val TV_MODE = "tv_mode"
    const val SEARCH = "search"
    const val AI = "ai"
    const val PRAYER = "prayer"
    const val QIBLA = "qibla"
    const val ADHKAR = "adhkar"
    const val TASBEEH = "tasbeeh"
    const val RADIO = "radio"
    const val VIRTUES = "virtues"
    const val DUA_KHATM = "dua_khatm"
    const val ASBAB = "asbab"
    const val BACKUP = "backup"
    const val SETTINGS = "settings"
}

private data class BottomTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val tag: String
)

private val mainTabs = listOf(
    BottomTab(NoorRoutes.HOME, "الرئيسية", Icons.Default.Home, "tab_home"),
    BottomTab(NoorRoutes.QURAN, "المصحف", Icons.AutoMirrored.Filled.MenuBook, "tab_quran"),
    BottomTab(NoorRoutes.LISTEN, "الاستماع", Icons.Default.Headphones, "tab_listen"),
    BottomTab(NoorRoutes.MEMORIZE, "الحفظ", Icons.Default.School, "tab_memorize"),
    BottomTab(NoorRoutes.MORE, "المزيد", Icons.Default.MoreHoriz, "tab_more")
)

@Composable
fun NoorAppScaffold(vm: NoorViewModel) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val audioState by vm.audioState.collectAsStateWithLifecycle()

    val showChrome = currentRoute in mainTabs.map { it.route }

    fun navigateTopTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun handleVoiceCommand(spoken: String) {
        val norm = ArabicNormalizer.normalizeForSearch(spoken)
        val matchedSurah = vm.getSurahs().firstOrNull { s ->
            norm.contains(ArabicNormalizer.normalizeForSearch(s.nameAr))
        }
        when {
            norm.contains("دعاء") || norm.contains("ختم") -> navController.navigate(NoorRoutes.DUA_KHATM)
            norm.contains("اذكار") || norm.contains("ذكر") -> navController.navigate(NoorRoutes.ADHKAR)
            norm.contains("قبلة") -> navController.navigate(NoorRoutes.QIBLA)
            norm.contains("صلاة") || norm.contains("مواقيت") -> navController.navigate(NoorRoutes.PRAYER)
            norm.contains("راديو") || norm.contains("اذاعة") -> navController.navigate(NoorRoutes.RADIO)
            norm.contains("شغل") && matchedSurah != null -> {
                vm.playSurah(matchedSurah.number)
                navigateTopTab(NoorRoutes.LISTEN)
            }
            matchedSurah != null -> {
                vm.openSurah(matchedSurah.number)
                navController.navigate("${NoorRoutes.READER_PAGES}/${matchedSurah.startPage}")
            }
            else -> {
                vm.updateSearchQuery(spoken, true)
                navController.navigate(NoorRoutes.SEARCH)
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = if (showChrome) WindowInsets.statusBars else WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (showChrome && !isWideScreen) {
                    Column {
                        MiniAudioPlayerBar(
                            audioState = audioState,
                            onTogglePlay = { vm.audioController.togglePlayPause() },
                            onStop = { vm.audioController.stop() },
                            onOpenPlayer = { navigateTopTab(NoorRoutes.LISTEN) }
                        )
                        NavigationBar(
                            windowInsets = WindowInsets.navigationBars,
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            mainTabs.forEach { tab ->
                                NavigationBarItem(
                                    selected = currentRoute == tab.route,
                                    onClick = { navigateTopTab(tab.route) },
                                    icon = { Icon(tab.icon, contentDescription = tab.label) },
                                    label = { Text(tab.label, maxLines = 1) },
                                    modifier = Modifier.testTag(tab.tag)
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (showChrome && isWideScreen) {
                    NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
                        mainTabs.forEach { tab ->
                            NavigationRailItem(
                                selected = currentRoute == tab.route,
                                onClick = { navigateTopTab(tab.route) },
                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag(tab.tag)
                            )
                        }
                    }
                }
                Box(modifier = Modifier.weight(1f)) {
                    NoorNavHost(
                        navController = navController,
                        vm = vm,
                        onNavigateTopTab = ::navigateTopTab,
                        onVoiceCommand = ::handleVoiceCommand
                    )
                }
            }
        }
    }
}

@Composable
private fun NoorNavHost(
    navController: NavHostController,
    vm: NoorViewModel,
    onNavigateTopTab: (String) -> Unit,
    onVoiceCommand: (String) -> Unit
) {
    NavHost(navController = navController, startDestination = NoorRoutes.HOME) {
        composable(NoorRoutes.HOME) {
            HomeScreen(
                vm = vm,
                onOpenMushaf = { page -> navController.navigate("${NoorRoutes.READER_PAGES}/$page") },
                onOpenContinuous = { surah -> navController.navigate("${NoorRoutes.READER_CONTINUOUS}/$surah") },
                onOpenTvMode = { navController.navigate(NoorRoutes.TV_MODE) },
                onOpenListen = { onNavigateTopTab(NoorRoutes.LISTEN) },
                onOpenMemorize = { onNavigateTopTab(NoorRoutes.MEMORIZE) },
                onOpenSearch = { navController.navigate(NoorRoutes.SEARCH) },
                onOpenAi = { navController.navigate(NoorRoutes.AI) },
                onOpenPrayer = { navController.navigate(NoorRoutes.PRAYER) },
                onOpenQibla = { navController.navigate(NoorRoutes.QIBLA) },
                onOpenAdhkar = { navController.navigate(NoorRoutes.ADHKAR) },
                onOpenTasbeeh = { navController.navigate(NoorRoutes.TASBEEH) },
                onOpenRadio = { navController.navigate(NoorRoutes.RADIO) },
                onOpenVirtues = { navController.navigate(NoorRoutes.VIRTUES) },
                onOpenDuaKhatm = { navController.navigate(NoorRoutes.DUA_KHATM) },
                onOpenAsbab = { navController.navigate(NoorRoutes.ASBAB) },
                onVoiceCommandRecognized = onVoiceCommand
            )
        }
        composable(NoorRoutes.QURAN) {
            QuranIndexScreen(
                vm = vm,
                onOpenPage = { page -> navController.navigate("${NoorRoutes.READER_PAGES}/$page") },
                onOpenContinuous = { surah -> navController.navigate("${NoorRoutes.READER_CONTINUOUS}/$surah") },
                onOpenSearch = { navController.navigate(NoorRoutes.SEARCH) },
                onOpenTvMode = { navController.navigate(NoorRoutes.TV_MODE) }
            )
        }
        composable(NoorRoutes.LISTEN) {
            ListenScreen(
                vm = vm,
                onOpenReader = { page -> navController.navigate("${NoorRoutes.READER_PAGES}/$page") }
            )
        }
        composable(NoorRoutes.MEMORIZE) {
            MemorizationScreen(
                vm = vm,
                onOpenDuaKhatm = { navController.navigate(NoorRoutes.DUA_KHATM) },
                onOpenPage = { page -> navController.navigate("${NoorRoutes.READER_PAGES}/$page") }
            )
        }
        composable(NoorRoutes.MORE) {
            MoreHubScreen(
                onOpenAi = { navController.navigate(NoorRoutes.AI) },
                onOpenSearch = { navController.navigate(NoorRoutes.SEARCH) },
                onOpenVirtues = { navController.navigate(NoorRoutes.VIRTUES) },
                onOpenDuaKhatm = { navController.navigate(NoorRoutes.DUA_KHATM) },
                onOpenAsbab = { navController.navigate(NoorRoutes.ASBAB) },
                onOpenPrayer = { navController.navigate(NoorRoutes.PRAYER) },
                onOpenQibla = { navController.navigate(NoorRoutes.QIBLA) },
                onOpenAdhkar = { navController.navigate(NoorRoutes.ADHKAR) },
                onOpenTasbeeh = { navController.navigate(NoorRoutes.TASBEEH) },
                onOpenRadio = { navController.navigate(NoorRoutes.RADIO) },
                onOpenTvMode = { navController.navigate(NoorRoutes.TV_MODE) },
                onOpenBackup = { navController.navigate(NoorRoutes.BACKUP) },
                onOpenSettings = { navController.navigate(NoorRoutes.SETTINGS) }
            )
        }
        composable(
            route = "${NoorRoutes.READER_PAGES}/{page}",
            arguments = listOf(navArgument("page") { type = NavType.IntType })
        ) { entry ->
            val page = entry.arguments?.getInt("page") ?: 1
            MushafPageReaderScreen(
                vm = vm,
                initialPage = page,
                onBack = { navController.popBackStack() },
                onSwitchToContinuous = { surah ->
                    navController.navigate("${NoorRoutes.READER_CONTINUOUS}/$surah") {
                        popUpTo("${NoorRoutes.READER_PAGES}/{page}") { inclusive = true }
                    }
                },
                onOpenTvMode = { navController.navigate(NoorRoutes.TV_MODE) }
            )
        }
        composable(
            route = "${NoorRoutes.READER_CONTINUOUS}/{surah}",
            arguments = listOf(navArgument("surah") { type = NavType.IntType })
        ) { entry ->
            val surah = entry.arguments?.getInt("surah") ?: 1
            ContinuousReaderScreen(
                vm = vm,
                surahNumber = surah,
                onBack = { navController.popBackStack() },
                onSwitchToPages = { page ->
                    navController.navigate("${NoorRoutes.READER_PAGES}/$page") {
                        popUpTo("${NoorRoutes.READER_CONTINUOUS}/{surah}") { inclusive = true }
                    }
                }
            )
        }
        composable(NoorRoutes.TV_MODE) {
            TvReaderScreen(vm = vm, onBack = { navController.popBackStack() })
        }
        composable(NoorRoutes.SEARCH) {
            GlobalSearchScreen(
                vm = vm,
                onBack = { navController.popBackStack() },
                onOpenAyahInMushaf = { s, a, page ->
                    vm.openSurah(s, a)
                    navController.navigate("${NoorRoutes.READER_PAGES}/$page")
                }
            )
        }
        composable(NoorRoutes.AI) {
            AiAssistantScreen(
                vm = vm,
                onBack = { navController.popBackStack() },
                onJumpToAyah = { s, a ->
                    vm.openSurah(s, a)
                    val page = vm.getSurah(s)?.ayahs?.getOrNull(a - 1)?.pageNumber ?: 1
                    navController.navigate("${NoorRoutes.READER_PAGES}/$page")
                }
            )
        }
        composable(NoorRoutes.PRAYER) {
            PrayerTimesScreen(vm = vm, onBack = { navController.popBackStack() })
        }
        composable(NoorRoutes.QIBLA) {
            QiblaScreen(vm = vm, onBack = { navController.popBackStack() })
        }
        composable(NoorRoutes.ADHKAR) {
            AdhkarScreen(vm = vm, onBack = { navController.popBackStack() })
        }
        composable(NoorRoutes.TASBEEH) {
            TasbeehScreen(vm = vm, onBack = { navController.popBackStack() })
        }
        composable(NoorRoutes.RADIO) {
            RadioScreen(vm = vm, onBack = { navController.popBackStack() })
        }
        composable(NoorRoutes.VIRTUES) {
            SurahVirtuesScreen(
                vm = vm,
                onBack = { navController.popBackStack() },
                onJumpToSurah = { s, a ->
                    vm.openSurah(s, a)
                    val page = vm.getSurah(s)?.ayahs?.getOrNull(a - 1)?.pageNumber ?: 1
                    navController.navigate("${NoorRoutes.READER_PAGES}/$page")
                }
            )
        }
        composable(NoorRoutes.DUA_KHATM) {
            DuaKhatmScreen(vm = vm, onBack = { navController.popBackStack() })
        }
        composable(NoorRoutes.ASBAB) {
            AsbabAndTafsirScreen(
                vm = vm,
                onBack = { navController.popBackStack() },
                onJumpToSurah = { s, a ->
                    vm.openSurah(s, a)
                    val page = vm.getSurah(s)?.ayahs?.getOrNull(a - 1)?.pageNumber ?: 1
                    navController.navigate("${NoorRoutes.READER_PAGES}/$page")
                }
            )
        }
        composable(NoorRoutes.BACKUP) {
            BackupAndAccountScreen(vm = vm, onBack = { navController.popBackStack() })
        }
        composable(NoorRoutes.SETTINGS) {
            SettingsScreen(vm = vm, onBack = { navController.popBackStack() })
        }
    }
}
