package com.example.quran

data class Ayah(
    val surahNumber: Int,
    val surahNameAr: String,
    val ayahNumber: Int,
    val globalNumber: Int,
    val pageNumber: Int,
    val juzNumber: Int,
    val hizbQuarter: Int,
    val textUthmani: String,
    val normalizedText: String,
    val sajdahType: Int // 0 = none, 1 = recommended, 2 = obligatory
) {
    val key: String get() = "$surahNumber:$ayahNumber"
}

data class Surah(
    val number: Int,
    val nameAr: String,
    val nameEn: String,
    val meaningEn: String,
    val revelationType: String,
    val ayahCount: Int,
    val startPage: Int,
    val endPage: Int,
    val startJuz: Int,
    val ayahs: List<Ayah>
)

data class MadinahPage(
    val pageNumber: Int,
    val juzNumber: Int,
    val hizbNumber: Int,
    val surahsOnPage: List<String>,
    val ayahs: List<Ayah>
)

data class JuzInfo(
    val number: Int,
    val startSurahNumber: Int,
    val startSurahName: String,
    val startAyahNumber: Int,
    val startPage: Int
)

data class HizbInfo(
    val number: Int,
    val juzNumber: Int,
    val startSurahNumber: Int,
    val startSurahName: String,
    val startAyahNumber: Int,
    val startPage: Int
)

data class WordMeaning(
    val word: String,
    val meaning: String
)

data class SababAlNuzul(
    val surahNumber: Int,
    val ayahNumber: Int,
    val surahName: String,
    val reason: String,
    val source: String,
    val authenticity: String
)

data class SurahVirtueHadith(
    val id: String,
    val surahNumber: Int,
    val surahName: String,
    val ayahNumber: Int,
    val title: String,
    val timeCategory: String,
    val timeLabel: String,
    val hadithText: String,
    val narrator: String,
    val source: String,
    val grade: String,
    val scholarlyNote: String
)

data class DuaKhatmSection(
    val heading: String,
    val text: String
)

data class DuaKhatm(
    val title: String,
    val scholarlyNote: String,
    val sections: List<DuaKhatmSection>
)

data class QuranTopic(
    val id: String,
    val title: String,
    val description: String,
    val references: List<Pair<Int, Int>>
)

data class QuranIntegrityReport(
    val passed: Boolean,
    val surahCount: Int,
    val ayahCount: Int,
    val pageCount: Int,
    val checksumsMatched: Boolean,
    val errors: List<String>
)
