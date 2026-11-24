package com.example.quran

object ArabicNormalizer {
    private val DIACRITICS_REGEX = Regex("[\\u0610-\\u061A\\u064B-\\u065F\\u0670\\u06D6-\\u06ED\\u0640]")
    private val NON_ARABIC_PUNCT = Regex("[^\\u0621-\\u064A\\s0-9]")

    /**
     * Normalizes Arabic text for search and recitation matching ONLY.
     * Never used to alter displayed Uthmani Quranic script.
     */
    fun normalizeForSearch(input: String): String {
        if (input.isBlank()) return ""
        val withoutTashkeel = DIACRITICS_REGEX.replace(input, "")
        val normalizedChars = StringBuilder(withoutTashkeel.length)
        for (ch in withoutTashkeel) {
            when (ch) {
                'أ', 'إ', 'آ', 'ٱ', 'ٲ', 'ٳ' -> normalizedChars.append('ا')
                'ة' -> normalizedChars.append('ه')
                'ى', 'ی' -> normalizedChars.append('ي')
                'ؤ' -> normalizedChars.append('و')
                'ئ' -> normalizedChars.append('ي')
                else -> normalizedChars.append(ch)
            }
        }
        return NON_ARABIC_PUNCT.replace(normalizedChars.toString(), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    fun toArabicIndicDigits(number: Int): String {
        val digits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
        return number.toString().map { ch ->
            if (ch in '0'..'9') digits[ch - '0'] else ch
        }.joinToString("")
    }
}
