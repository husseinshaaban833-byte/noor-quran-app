package com.example.memorization

import com.example.quran.ArabicNormalizer

enum class WordDiffType {
    MATCH,
    OMITTED,     // حذف
    ADDED,       // إضافة
    REPLACED,    // استبدال
    REORDERED    // تقديم وتأخير
}

data class WordAlignmentItem(
    val expectedWord: String?,
    val spokenWord: String?,
    val type: WordDiffType
)

data class RecitationAnalysisResult(
    val confident: Boolean,
    val accuracyPercent: Int,
    val omittedCount: Int,
    val addedCount: Int,
    val replacedCount: Int,
    val reorderedCount: Int,
    val items: List<WordAlignmentItem>,
    val summaryMessage: String
)

object RecitationAnalyzer {
    /**
     * Compares user's spoken/entered Arabic text against verified Quran text (normalized)
     * using dynamic programming word-level alignment.
     * Note: Detects text memorization differences only, NOT Tajweed rules.
     */
    fun analyze(expectedUthmaniText: String, spokenText: String): RecitationAnalysisResult {
        val expectedDisplayWords = expectedUthmaniText.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        val expectedNorm = expectedDisplayWords.map { ArabicNormalizer.normalizeForSearch(it) }.filter { it.isNotBlank() }
        val spokenNorm = ArabicNormalizer.normalizeForSearch(spokenText).split(Regex("\\s+")).filter { it.isNotBlank() }

        if (spokenNorm.isEmpty() || (expectedNorm.size >= 4 && spokenNorm.size < (expectedNorm.size * 0.25).toInt())) {
            return RecitationAnalysisResult(
                confident = false,
                accuracyPercent = 0,
                omittedCount = 0,
                addedCount = 0,
                replacedCount = 0,
                reorderedCount = 0,
                items = expectedDisplayWords.map { WordAlignmentItem(it, null, WordDiffType.OMITTED) },
                summaryMessage = "تعذر التأكد: النص المسموع غير مكتمل أو الصوت غير واضح بما يكفي للمقارنة."
            )
        }

        val n = expectedNorm.size
        val m = spokenNorm.size
        val dp = Array(n + 1) { IntArray(m + 1) }
        for (i in 0..n) dp[i][0] = i
        for (j in 0..m) dp[0][j] = j

        for (i in 1..n) {
            for (j in 1..m) {
                val cost = if (expectedNorm[i - 1] == spokenNorm[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // Omission
                    dp[i][j - 1] + 1,      // Addition
                    dp[i - 1][j - 1] + cost // Match or Replace
                )
            }
        }

        val rawItems = mutableListOf<WordAlignmentItem>()
        var i = n
        var j = m
        while (i > 0 || j > 0) {
            when {
                i > 0 && j > 0 && expectedNorm[i - 1] == spokenNorm[j - 1] && dp[i][j] == dp[i - 1][j - 1] -> {
                    rawItems.add(WordAlignmentItem(expectedDisplayWords[i - 1], spokenNorm[j - 1], WordDiffType.MATCH))
                    i--; j--
                }
                i > 0 && j > 0 && dp[i][j] == dp[i - 1][j - 1] + 1 -> {
                    rawItems.add(WordAlignmentItem(expectedDisplayWords[i - 1], spokenNorm[j - 1], WordDiffType.REPLACED))
                    i--; j--
                }
                i > 0 && dp[i][j] == dp[i - 1][j] + 1 -> {
                    rawItems.add(WordAlignmentItem(expectedDisplayWords[i - 1], null, WordDiffType.OMITTED))
                    i--
                }
                else -> {
                    rawItems.add(WordAlignmentItem(null, spokenNorm[j - 1], WordDiffType.ADDED))
                    j--
                }
            }
        }
        rawItems.reverse()

        // Detect adjacent swaps / reordering (تقديم وتأخير)
        var reordered = 0
        val finalItems = rawItems.toMutableList()
        val omittedSet = finalItems.mapNotNull {
            if (it.type == WordDiffType.OMITTED && it.expectedWord != null) {
                ArabicNormalizer.normalizeForSearch(it.expectedWord)
            } else null
        }.toSet()
        val addedSet = finalItems.mapNotNull {
            if (it.type == WordDiffType.ADDED) it.spokenWord else null
        }.toSet()
        val swappedWords = omittedSet intersect addedSet

        if (swappedWords.isNotEmpty()) {
            for (idx in finalItems.indices) {
                val item = finalItems[idx]
                val expNorm = item.expectedWord?.let { ArabicNormalizer.normalizeForSearch(it) }
                if (item.type == WordDiffType.OMITTED && expNorm in swappedWords) {
                    finalItems[idx] = item.copy(type = WordDiffType.REORDERED)
                    reordered++
                }
            }
        }

        val matches = finalItems.count { it.type == WordDiffType.MATCH }
        val omitted = finalItems.count { it.type == WordDiffType.OMITTED }
        val added = finalItems.count { it.type == WordDiffType.ADDED }
        val replaced = finalItems.count { it.type == WordDiffType.REPLACED }
        val accuracy = if (n == 0) 0 else ((matches.toFloat() / n) * 100).toInt().coerceIn(0, 100)

        val confident = accuracy >= 15 || matches >= 1
        val summary = if (!confident) {
            "تعذر التأكد: يرجى إعادة التسميع بصوت أوضح أو التحقق من السورة المختارة."
        } else {
            "نسبة مطابقة النص: $accuracy% • تنبيه: هذا الفحص لمطابقة حفظ الكلمات فقط ولا يغني عن التلقي والمشافهة لأحكام التجويد."
        }

        return RecitationAnalysisResult(
            confident = confident,
            accuracyPercent = accuracy,
            omittedCount = omitted,
            addedCount = added,
            replacedCount = replaced,
            reorderedCount = reordered,
            items = finalItems,
            summaryMessage = summary
        )
    }
}
