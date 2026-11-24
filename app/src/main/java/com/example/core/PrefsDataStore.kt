package com.example.core

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "noor_preferences")

data class UserPreferences(
    val lastSurah: Int = 1,
    val lastAyah: Int = 1,
    val lastPage: Int = 1,
    val readerFontSize: Float = 24f,
    val readerLineSpacing: Float = 1.85f,
    val readerLetterSpacing: Float = 0.0f,
    val readerFontId: String = "amiri_quran",
    val readerThemeId: String = "ivory",
    val readerMode: String = "pages", // "pages" or "continuous"
    val darkModeSetting: String = "system", // "system", "light", "dark"
    val tajweedColorsEnabled: Boolean = false,
    val keepScreenOn: Boolean = true,
    val selectedReciterId: String = "ar.alafasy",
    val playbackSpeed: Float = 1.0f,
    val repeatCount: Int = 1,
    val autoAdvanceAyah: Boolean = true,
    val cityName: String = "مكة المكرمة",
    val cityLat: Double = 21.4225,
    val cityLon: Double = 39.8262,
    val cityTz: Double = 3.0,
    val calculationMethod: String = "UMM_AL_QURA",
    val asrMadhab: String = "SHAFII",
    val adhanFajr: Boolean = true,
    val adhanDhuhr: Boolean = true,
    val adhanAsr: Boolean = true,
    val adhanMaghrib: Boolean = true,
    val adhanIsha: Boolean = true,
    val hijriOffsetDays: Int = 0,
    val voiceCommandsEnabled: Boolean = false,
    val khatmahCount: Int = 0,
    val tasbeehTotal: Int = 0,
    val tasbeehCurrent: Int = 0,
    val tasbeehTarget: Int = 33,
    val selectedDhikr: String = "سُبْحَانَ اللَّهِ"
)

class PrefsDataStore(private val context: Context) {
    private object Keys {
        val LAST_SURAH = intPreferencesKey("last_surah")
        val LAST_AYAH = intPreferencesKey("last_ayah")
        val LAST_PAGE = intPreferencesKey("last_page")
        val FONT_SIZE = floatPreferencesKey("reader_font_size")
        val LINE_SPACING = floatPreferencesKey("reader_line_spacing")
        val LETTER_SPACING = floatPreferencesKey("reader_letter_spacing")
        val FONT_ID = stringPreferencesKey("reader_font_id")
        val THEME_ID = stringPreferencesKey("reader_theme_id")
        val READER_MODE = stringPreferencesKey("reader_mode")
        val DARK_MODE = stringPreferencesKey("dark_mode_setting")
        val TAJWEED_COLORS = booleanPreferencesKey("tajweed_colors")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val RECITER_ID = stringPreferencesKey("selected_reciter_id")
        val PLAYBACK_SPEED = floatPreferencesKey("playback_speed")
        val REPEAT_COUNT = intPreferencesKey("repeat_count")
        val AUTO_ADVANCE = booleanPreferencesKey("auto_advance_ayah")
        val CITY_NAME = stringPreferencesKey("city_name")
        val CITY_LAT = doublePreferencesKey("city_lat")
        val CITY_LON = doublePreferencesKey("city_lon")
        val CITY_TZ = doublePreferencesKey("city_tz")
        val CALC_METHOD = stringPreferencesKey("calc_method")
        val ASR_MADHAB = stringPreferencesKey("asr_madhab")
        val ADHAN_FAJR = booleanPreferencesKey("adhan_fajr")
        val ADHAN_DHUHR = booleanPreferencesKey("adhan_dhuhr")
        val ADHAN_ASR = booleanPreferencesKey("adhan_asr")
        val ADHAN_MAGHRIB = booleanPreferencesKey("adhan_maghrib")
        val ADHAN_ISHA = booleanPreferencesKey("adhan_isha")
        val HIJRI_OFFSET = intPreferencesKey("hijri_offset")
        val VOICE_COMMANDS = booleanPreferencesKey("voice_commands_enabled")
        val KHATMAH_COUNT = intPreferencesKey("khatmah_count")
        val TASBEEH_TOTAL = intPreferencesKey("tasbeeh_total")
        val TASBEEH_CURRENT = intPreferencesKey("tasbeeh_current")
        val TASBEEH_TARGET = intPreferencesKey("tasbeeh_target")
        val SELECTED_DHIKR = stringPreferencesKey("selected_dhikr")
    }

    val preferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { p ->
        UserPreferences(
            lastSurah = p[Keys.LAST_SURAH] ?: 1,
            lastAyah = p[Keys.LAST_AYAH] ?: 1,
            lastPage = p[Keys.LAST_PAGE] ?: 1,
            readerFontSize = p[Keys.FONT_SIZE] ?: 24f,
            readerLineSpacing = p[Keys.LINE_SPACING] ?: 1.85f,
            readerLetterSpacing = p[Keys.LETTER_SPACING] ?: 0.0f,
            readerFontId = p[Keys.FONT_ID] ?: "amiri_quran",
            readerThemeId = p[Keys.THEME_ID] ?: "ivory",
            readerMode = p[Keys.READER_MODE] ?: "pages",
            darkModeSetting = p[Keys.DARK_MODE] ?: "system",
            tajweedColorsEnabled = p[Keys.TAJWEED_COLORS] ?: false,
            keepScreenOn = p[Keys.KEEP_SCREEN_ON] ?: true,
            selectedReciterId = p[Keys.RECITER_ID] ?: "ar.alafasy",
            playbackSpeed = p[Keys.PLAYBACK_SPEED] ?: 1.0f,
            repeatCount = p[Keys.REPEAT_COUNT] ?: 1,
            autoAdvanceAyah = p[Keys.AUTO_ADVANCE] ?: true,
            cityName = p[Keys.CITY_NAME] ?: "مكة المكرمة",
            cityLat = p[Keys.CITY_LAT] ?: 21.4225,
            cityLon = p[Keys.CITY_LON] ?: 39.8262,
            cityTz = p[Keys.CITY_TZ] ?: 3.0,
            calculationMethod = p[Keys.CALC_METHOD] ?: "UMM_AL_QURA",
            asrMadhab = p[Keys.ASR_MADHAB] ?: "SHAFII",
            adhanFajr = p[Keys.ADHAN_FAJR] ?: true,
            adhanDhuhr = p[Keys.ADHAN_DHUHR] ?: true,
            adhanAsr = p[Keys.ADHAN_ASR] ?: true,
            adhanMaghrib = p[Keys.ADHAN_MAGHRIB] ?: true,
            adhanIsha = p[Keys.ADHAN_ISHA] ?: true,
            hijriOffsetDays = p[Keys.HIJRI_OFFSET] ?: 0,
            voiceCommandsEnabled = p[Keys.VOICE_COMMANDS] ?: false,
            khatmahCount = p[Keys.KHATMAH_COUNT] ?: 0,
            tasbeehTotal = p[Keys.TASBEEH_TOTAL] ?: 0,
            tasbeehCurrent = p[Keys.TASBEEH_CURRENT] ?: 0,
            tasbeehTarget = p[Keys.TASBEEH_TARGET] ?: 33,
            selectedDhikr = p[Keys.SELECTED_DHIKR] ?: "سُبْحَانَ اللَّهِ"
        )
    }

    suspend fun updateLastPosition(surah: Int, ayah: Int, page: Int) {
        context.dataStore.edit {
            it[Keys.LAST_SURAH] = surah.coerceIn(1, 114)
            it[Keys.LAST_AYAH] = ayah.coerceAtLeast(1)
            it[Keys.LAST_PAGE] = page.coerceIn(1, 604)
        }
    }

    suspend fun updateReaderTypography(
        fontSize: Float? = null,
        lineSpacing: Float? = null,
        letterSpacing: Float? = null,
        fontId: String? = null,
        themeId: String? = null,
        readerMode: String? = null,
        tajweedColors: Boolean? = null,
        keepScreenOn: Boolean? = null
    ) {
        context.dataStore.edit { p ->
            fontSize?.let { p[Keys.FONT_SIZE] = it.coerceIn(14f, 48f) }
            lineSpacing?.let { p[Keys.LINE_SPACING] = it.coerceIn(1.3f, 2.8f) }
            letterSpacing?.let { p[Keys.LETTER_SPACING] = it.coerceIn(0f, 3f) }
            fontId?.let { p[Keys.FONT_ID] = it }
            themeId?.let { p[Keys.THEME_ID] = it }
            readerMode?.let { p[Keys.READER_MODE] = it }
            tajweedColors?.let { p[Keys.TAJWEED_COLORS] = it }
            keepScreenOn?.let { p[Keys.KEEP_SCREEN_ON] = it }
        }
    }

    suspend fun updateDarkMode(mode: String) {
        context.dataStore.edit { it[Keys.DARK_MODE] = mode }
    }

    suspend fun updateAudioSettings(
        reciterId: String? = null,
        speed: Float? = null,
        repeatCount: Int? = null,
        autoAdvance: Boolean? = null
    ) {
        context.dataStore.edit { p ->
            reciterId?.let { p[Keys.RECITER_ID] = it }
            speed?.let { p[Keys.PLAYBACK_SPEED] = it.coerceIn(0.5f, 2.0f) }
            repeatCount?.let { p[Keys.REPEAT_COUNT] = it.coerceIn(1, 20) }
            autoAdvance?.let { p[Keys.AUTO_ADVANCE] = it }
        }
    }

    suspend fun updateLocation(name: String, lat: Double, lon: Double, tz: Double) {
        context.dataStore.edit {
            it[Keys.CITY_NAME] = name
            it[Keys.CITY_LAT] = lat
            it[Keys.CITY_LON] = lon
            it[Keys.CITY_TZ] = tz
        }
    }

    suspend fun updatePrayerSettings(
        method: String? = null,
        madhab: String? = null,
        hijriOffset: Int? = null
    ) {
        context.dataStore.edit { p ->
            method?.let { p[Keys.CALC_METHOD] = it }
            madhab?.let { p[Keys.ASR_MADHAB] = it }
            hijriOffset?.let { p[Keys.HIJRI_OFFSET] = it.coerceIn(-2, 2) }
        }
    }

    suspend fun toggleAdhan(prayerKey: String, enabled: Boolean) {
        context.dataStore.edit { p ->
            when (prayerKey) {
                "fajr" -> p[Keys.ADHAN_FAJR] = enabled
                "dhuhr" -> p[Keys.ADHAN_DHUHR] = enabled
                "asr" -> p[Keys.ADHAN_ASR] = enabled
                "maghrib" -> p[Keys.ADHAN_MAGHRIB] = enabled
                "isha" -> p[Keys.ADHAN_ISHA] = enabled
            }
        }
    }

    suspend fun updateTasbeeh(current: Int, total: Int, target: Int? = null, dhikr: String? = null) {
        context.dataStore.edit { p ->
            p[Keys.TASBEEH_CURRENT] = current
            p[Keys.TASBEEH_TOTAL] = total
            target?.let { p[Keys.TASBEEH_TARGET] = it }
            dhikr?.let { p[Keys.SELECTED_DHIKR] = it }
        }
    }

    suspend fun incrementKhatmahCount() {
        context.dataStore.edit { p ->
            p[Keys.KHATMAH_COUNT] = (p[Keys.KHATMAH_COUNT] ?: 0) + 1
        }
    }

    suspend fun setKhatmahAndTasbeehFromCloud(
        lastSurah: Int,
        lastAyah: Int,
        lastPage: Int,
        khatmahCount: Int,
        tasbeehTotal: Int
    ) {
        context.dataStore.edit { p ->
            p[Keys.LAST_SURAH] = lastSurah.coerceIn(1, 114)
            p[Keys.LAST_AYAH] = lastAyah.coerceAtLeast(1)
            p[Keys.LAST_PAGE] = lastPage.coerceIn(1, 604)
            p[Keys.KHATMAH_COUNT] = khatmahCount.coerceAtLeast(0)
            p[Keys.TASBEEH_TOTAL] = tasbeehTotal.coerceAtLeast(0)
        }
    }

    suspend fun setVoiceCommandsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.VOICE_COMMANDS] = enabled }
    }
}
