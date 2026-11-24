package com.example.quran

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

class QuranRepository(private val context: Context) {
    private val loadMutex = Mutex()

    @Volatile
    private var loaded = false

    private var surahsList: List<Surah> = emptyList()
    private var pagesList: List<MadinahPage> = emptyList()
    private var juzList: List<JuzInfo> = emptyList()
    private var hizbList: List<HizbInfo> = emptyList()
    private var allAyahsList: List<Ayah> = emptyList()
    private var muyassarMap: Map<String, String> = emptyMap()
    private var downloadedTafsirMap: MutableMap<String, Map<String, String>> = mutableMapOf()
    private var englishMap: Map<String, String> = emptyMap()
    private var wordMeaningsMap: Map<String, List<WordMeaning>> = emptyMap()
    private var asbabList: List<SababAlNuzul> = emptyList()
    private var asbabDisclaimer: String = ""
    private var virtuesList: List<SurahVirtueHadith> = emptyList()
    private var virtuesDisclaimer: String = ""
    private var duaKhatmData: DuaKhatm? = null

    suspend fun ensureLoaded() {
        if (loaded) return
        loadMutex.withLock {
            if (loaded) return
            withContext(Dispatchers.IO) {
                loadQuranFromAssets()
                loadTafsirAndTranslations()
                loadScholarlyAssets()
                loadDownloadedTafsirs()
                loaded = true
            }
        }
    }

    private fun loadQuranFromAssets() {
        val raw = context.assets.open("quran/quran_uthmani.json").bufferedReader().use { it.readText() }
        val jsonArray = JSONArray(raw)
        val surahs = ArrayList<Surah>(114)
        val allAyahs = ArrayList<Ayah>(6236)
        val pageBuckets = HashMap<Int, MutableList<Ayah>>(604)
        val juzFirstAyah = HashMap<Int, Ayah>(30)
        val hizbFirstAyah = HashMap<Int, Ayah>(60)

        for (i in 0 until jsonArray.length()) {
            val sObj = jsonArray.getJSONObject(i)
            val sNum = sObj.getInt("n")
            val nameAr = sObj.getString("ar")
            val nameEn = sObj.getString("en")
            val meaningEn = sObj.getString("tr")
            val revType = sObj.getString("rev")
            val ayahArr = sObj.getJSONArray("ayahs")
            val ayahs = ArrayList<Ayah>(ayahArr.length())

            for (j in 0 until ayahArr.length()) {
                val aObj = ayahArr.getJSONObject(j)
                val textUthmani = aObj.getString("t")
                val ayah = Ayah(
                    surahNumber = sNum,
                    surahNameAr = nameAr,
                    ayahNumber = aObj.getInt("a"),
                    globalNumber = aObj.getInt("g"),
                    pageNumber = aObj.getInt("p"),
                    juzNumber = aObj.getInt("j"),
                    hizbQuarter = aObj.getInt("h"),
                    textUthmani = textUthmani,
                    normalizedText = ArabicNormalizer.normalizeForSearch(textUthmani),
                    sajdahType = aObj.optInt("s", 0)
                )
                ayahs.add(ayah)
                allAyahs.add(ayah)
                pageBuckets.getOrPut(ayah.pageNumber) { mutableListOf() }.add(ayah)
                juzFirstAyah.putIfAbsent(ayah.juzNumber, ayah)
                val hizbNum = ((ayah.hizbQuarter - 1) / 4) + 1
                hizbFirstAyah.putIfAbsent(hizbNum, ayah)
            }

            surahs.add(
                Surah(
                    number = sNum,
                    nameAr = nameAr,
                    nameEn = nameEn,
                    meaningEn = meaningEn,
                    revelationType = revType,
                    ayahCount = ayahs.size,
                    startPage = ayahs.first().pageNumber,
                    endPage = ayahs.last().pageNumber,
                    startJuz = ayahs.first().juzNumber,
                    ayahs = ayahs
                )
            )
        }

        val pages = (1..604).map { pageNum ->
            val pAyahs = pageBuckets[pageNum].orEmpty()
            val first = pAyahs.firstOrNull()
            MadinahPage(
                pageNumber = pageNum,
                juzNumber = first?.juzNumber ?: 1,
                hizbNumber = first?.let { ((it.hizbQuarter - 1) / 4) + 1 } ?: 1,
                surahsOnPage = pAyahs.map { it.surahNameAr }.distinct(),
                ayahs = pAyahs
            )
        }

        val juzs = (1..30).mapNotNull { jNum ->
            juzFirstAyah[jNum]?.let {
                JuzInfo(jNum, it.surahNumber, it.surahNameAr, it.ayahNumber, it.pageNumber)
            }
        }

        val hizbs = (1..60).mapNotNull { hNum ->
            hizbFirstAyah[hNum]?.let {
                HizbInfo(hNum, it.juzNumber, it.surahNumber, it.surahNameAr, it.ayahNumber, it.pageNumber)
            }
        }

        surahsList = surahs
        allAyahsList = allAyahs
        pagesList = pages
        juzList = juzs
        hizbList = hizbs
    }

    private fun loadTafsirAndTranslations() {
        val muyRaw = context.assets.open("quran/tafsir_muyassar.json").bufferedReader().use { it.readText() }
        val muyJson = JSONObject(muyRaw)
        val mMap = HashMap<String, String>(6236)
        muyJson.keys().forEach { key -> mMap[key] = muyJson.getString(key) }
        muyassarMap = mMap

        val engRaw = context.assets.open("quran/translation_en.json").bufferedReader().use { it.readText() }
        val engJson = JSONObject(engRaw)
        val eMap = HashMap<String, String>(6236)
        engJson.keys().forEach { key -> eMap[key] = engJson.getString(key) }
        englishMap = eMap
    }

    private fun loadScholarlyAssets() {
        // Word meanings
        val wmRaw = context.assets.open("quran/word_meanings.json").bufferedReader().use { it.readText() }
        val wmJson = JSONObject(wmRaw)
        val wmMap = HashMap<String, List<WordMeaning>>()
        wmJson.keys().forEach { key ->
            val arr = wmJson.getJSONArray(key)
            val list = (0 until arr.length()).map { idx ->
                val obj = arr.getJSONObject(idx)
                WordMeaning(obj.getString("w"), obj.getString("m"))
            }
            wmMap[key] = list
        }
        wordMeaningsMap = wmMap

        // Asbab Al-Nuzul
        val asbabRaw = context.assets.open("quran/asbab_al_nuzul.json").bufferedReader().use { it.readText() }
        val asbabJson = JSONObject(asbabRaw)
        asbabDisclaimer = asbabJson.getString("disclaimer")
        val asbabArr = asbabJson.getJSONArray("entries")
        asbabList = (0 until asbabArr.length()).map { idx ->
            val o = asbabArr.getJSONObject(idx)
            SababAlNuzul(
                surahNumber = o.getInt("surahNumber"),
                ayahNumber = o.getInt("ayahNumber"),
                surahName = o.getString("surahName"),
                reason = o.getString("reason"),
                source = o.getString("source"),
                authenticity = o.getString("authenticity")
            )
        }

        // Surah Virtues Hadiths
        val virtRaw = context.assets.open("quran/surah_virtues_hadiths.json").bufferedReader().use { it.readText() }
        val virtJson = JSONObject(virtRaw)
        virtuesDisclaimer = virtJson.getString("disclaimer")
        val virtArr = virtJson.getJSONArray("items")
        virtuesList = (0 until virtArr.length()).map { idx ->
            val o = virtArr.getJSONObject(idx)
            SurahVirtueHadith(
                id = o.getString("id"),
                surahNumber = o.getInt("surahNumber"),
                surahName = o.getString("surahName"),
                ayahNumber = o.getInt("ayahNumber"),
                title = o.getString("title"),
                timeCategory = o.getString("timeCategory"),
                timeLabel = o.getString("timeLabel"),
                hadithText = o.getString("hadithText"),
                narrator = o.getString("narrator"),
                source = o.getString("source"),
                grade = o.getString("grade"),
                scholarlyNote = o.getString("scholarlyNote")
            )
        }

        // Dua Khatm
        val duaRaw = context.assets.open("quran/dua_khatm.json").bufferedReader().use { it.readText() }
        val duaJson = JSONObject(duaRaw)
        val secArr = duaJson.getJSONArray("sections")
        val sections = (0 until secArr.length()).map { idx ->
            val o = secArr.getJSONObject(idx)
            DuaKhatmSection(o.getString("heading"), o.getString("text"))
        }
        duaKhatmData = DuaKhatm(
            title = duaJson.getString("title"),
            scholarlyNote = duaJson.getString("scholarlyNote"),
            sections = sections
        )
    }

    fun loadDownloadedTafsirs() {
        val dir = File(context.filesDir, "downloads/tafsir")
        if (!dir.exists()) return
        dir.listFiles()?.filter { it.extension == "json" }?.forEach { file ->
            try {
                val id = file.nameWithoutExtension
                val json = JSONObject(file.readText())
                val map = HashMap<String, String>(6236)
                json.keys().forEach { k -> map[k] = json.getString(k) }
                downloadedTafsirMap[id] = map
            } catch (_: Exception) {
            }
        }
    }

    fun getSurahs(): List<Surah> = surahsList
    fun getSurah(number: Int): Surah? = surahsList.getOrNull(number - 1)
    fun getPage(pageNumber: Int): MadinahPage? = pagesList.getOrNull(pageNumber - 1)
    fun getAllPages(): List<MadinahPage> = pagesList
    fun getJuzList(): List<JuzInfo> = juzList
    fun getHizbList(): List<HizbInfo> = hizbList
    fun getAllAyahs(): List<Ayah> = allAyahsList

    fun getAyah(surahNumber: Int, ayahNumber: Int): Ayah? =
        getSurah(surahNumber)?.ayahs?.getOrNull(ayahNumber - 1)

    fun getAyahOfTheDay(dayOfYear: Int): Ayah? {
        val highlights = listOf(
            2 to 255, 2 to 286, 3 to 190, 13 to 28, 18 to 10, 24 to 35,
            39 to 53, 55 to 60, 59 to 22, 65 to 3, 94 to 5, 112 to 1
        )
        val pair = highlights[dayOfYear % highlights.size]
        return getAyah(pair.first, pair.second)
    }

    fun getTafsirMuyassar(surahNumber: Int, ayahNumber: Int): String =
        muyassarMap["$surahNumber:$ayahNumber"].orEmpty()

    fun getDownloadedTafsir(editionId: String, surahNumber: Int, ayahNumber: Int): String? =
        downloadedTafsirMap[editionId]?.get("$surahNumber:$ayahNumber")

    fun getAvailableDownloadedTafsirs(): Set<String> = downloadedTafsirMap.keys.toSet()

    fun getEnglishTranslation(surahNumber: Int, ayahNumber: Int): String =
        englishMap["$surahNumber:$ayahNumber"].orEmpty()

    fun getWordMeanings(surahNumber: Int, ayahNumber: Int): List<WordMeaning> =
        wordMeaningsMap["$surahNumber:$ayahNumber"].orEmpty()

    fun getAsbabForAyah(surahNumber: Int, ayahNumber: Int): SababAlNuzul? =
        asbabList.firstOrNull { it.surahNumber == surahNumber && it.ayahNumber == ayahNumber }

    fun getAllAsbab(): List<SababAlNuzul> = asbabList
    fun getAsbabDisclaimer(): String = asbabDisclaimer
    fun getSurahVirtues(): List<SurahVirtueHadith> = virtuesList
    fun getVirtuesDisclaimer(): String = virtuesDisclaimer
    fun getDuaKhatm(): DuaKhatm? = duaKhatmData

    fun getCuratedTopics(): List<QuranTopic> = listOf(
        QuranTopic(
            "tawheed", "التوحيد وعظمة الله", "آيات في وحدانية الله وأسمائه الحسنى",
            listOf(1 to 2, 2 to 255, 59 to 22, 59 to 23, 112 to 1)
        ),
        QuranTopic(
            "dua", "الأدعية القرآنية", "جوامع الدعاء من القرآن الكريم",
            listOf(1 to 6, 2 to 201, 2 to 286, 3 to 8, 18 to 10, 25 to 74)
        ),
        QuranTopic(
            "patience", "الصبر والتوكل والفرج", "آيات السكينة والطمأنينة والتوكل على الله",
            listOf(2 to 153, 13 to 28, 39 to 53, 65 to 3, 94 to 5)
        ),
        QuranTopic(
            "ethics", "بر الوالدين والأخلاق", "الوصايا القرآنية في الإحسان والعدل والبر",
            listOf(17 to 23, 17 to 24, 31 to 14, 49 to 12, 49 to 13)
        ),
        QuranTopic(
            "sajdah", "مواضع السجود في القرآن", "آيات السجدة في المصحف الشريف",
            allAyahsList.filter { it.sajdahType > 0 }.map { it.surahNumber to it.ayahNumber }
        )
    )

    fun searchQuran(query: String, searchTafsir: Boolean = true, limit: Int = 80): List<Ayah> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return emptyList()

        // Support direct reference "18:10" or "سورة الكهف"
        val refMatch = Regex("^(\\d{1,3}):(\\d{1,3})$").matchEntire(trimmed)
        if (refMatch != null) {
            val s = refMatch.groupValues[1].toIntOrNull()
            val a = refMatch.groupValues[2].toIntOrNull()
            if (s != null && a != null) {
                getAyah(s, a)?.let { return listOf(it) }
            }
        }

        val normQuery = ArabicNormalizer.normalizeForSearch(trimmed)
        if (normQuery.isEmpty()) return emptyList()

        val results = ArrayList<Ayah>()
        val seen = HashSet<String>()

        // 1. Match Surah names first
        surahsList.filter {
            ArabicNormalizer.normalizeForSearch(it.nameAr).contains(normQuery) ||
                it.nameEn.contains(trimmed, ignoreCase = true)
        }.forEach { surah ->
            surah.ayahs.firstOrNull()?.let { first ->
                if (seen.add(first.key)) results.add(first)
            }
        }

        // 2. Match Uthmani Quran text (normalized without altering original text)
        for (ayah in allAyahsList) {
            if (results.size >= limit) break
            if (ayah.normalizedText.contains(normQuery) && seen.add(ayah.key)) {
                results.add(ayah)
            }
        }

        // 3. Match Tafsir / English translation if requested
        if (searchTafsir && results.size < limit) {
            for (ayah in allAyahsList) {
                if (results.size >= limit) break
                val tafsirNorm = ArabicNormalizer.normalizeForSearch(muyassarMap[ayah.key].orEmpty())
                val eng = englishMap[ayah.key].orEmpty()
                if ((tafsirNorm.contains(normQuery) || eng.contains(trimmed, ignoreCase = true)) &&
                    seen.add(ayah.key)
                ) {
                    results.add(ayah)
                }
            }
        }
        return results
    }

    suspend fun verifyIntegrity(): QuranIntegrityReport = withContext(Dispatchers.IO) {
        ensureLoaded()
        val errors = mutableListOf<String>()

        if (surahsList.size != 114) {
            errors.add("عدد السور ${surahsList.size} لا يساوي 114")
        }
        if (allAyahsList.size != 6236) {
            errors.add("إجمالي الآيات ${allAyahsList.size} لا يساوي 6236")
        }
        if (pagesList.size != 604 || pagesList.any { it.ayahs.isEmpty() }) {
            errors.add("خلل في صفحات مصحف المدينة الـ 604")
        }

        var prevPage = 1
        for ((index, surah) in surahsList.withIndex()) {
            if (surah.number != index + 1) {
                errors.add("ترتيب السورة ${surah.number} غير مطابق")
            }
            for ((aIdx, ayah) in surah.ayahs.withIndex()) {
                if (ayah.ayahNumber != aIdx + 1) {
                    errors.add("خلل في تسلسل آيات سورة ${surah.number}")
                }
                if (ayah.pageNumber < prevPage) {
                    errors.add("تراجع رقم الصفحة عند ${ayah.key}")
                }
                prevPage = ayah.pageNumber
            }
        }

        val checksumsMatched = verifyAssetChecksums(errors)
        QuranIntegrityReport(
            passed = errors.isEmpty() && checksumsMatched,
            surahCount = surahsList.size,
            ayahCount = allAyahsList.size,
            pageCount = pagesList.size,
            checksumsMatched = checksumsMatched,
            errors = errors
        )
    }

    private fun verifyAssetChecksums(errors: MutableList<String>): Boolean {
        return try {
            val raw = context.assets.open("quran/checksums.sha256").bufferedReader().use { it.readText() }
            val json = JSONObject(raw)
            var allOk = true
            json.keys().forEach { fileName ->
                val expected = json.getString(fileName)
                val bytes = context.assets.open("quran/$fileName").use { it.readBytes() }
                val actual = sha256Hex(bytes)
                if (expected != actual) {
                    allOk = false
                    errors.add("عدم تطابق البصمة للملف $fileName")
                }
            }
            allOk
        } catch (e: Exception) {
            errors.add("تعذر قراءة checksums.sha256: ${e.message}")
            false
        }
    }

    companion object {
        fun sha256Hex(bytes: ByteArray): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
            return digest.joinToString("") { "%02x".format(it) }
        }
    }
}
