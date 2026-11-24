package com.example.quran

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import com.example.ui.theme.TajweedGhunnah
import com.example.ui.theme.TajweedIdgham
import com.example.ui.theme.TajweedIkhfa
import com.example.ui.theme.TajweedMadd
import com.example.ui.theme.TajweedQalqalah

/**
 * Highlights common Tajweed markers in Continuous Reader mode ONLY,
 * preserving every single Unicode character of the verified Uthmani text.
 */
object TajweedHighlighter {
    private val qalqalahLetters = setOf('ق', 'ط', 'ب', 'ج', 'د')
    private const val SUKUN = '\u0652'
    private const val UTHMANI_SUKUN = '\u06E1'
    private const val SHADDA = '\u0651'
    private const val MADDAH_ABOVE = '\u0653'
    private const val SMALL_HIGH_MEEM = '\u06E2'

    fun highlight(text: String, defaultColor: Color): AnnotatedString {
        return buildAnnotatedString {
            append(text)
            addStyle(SpanStyle(color = defaultColor), 0, text.length)
            var i = 0
            while (i < text.length) {
                val ch = text[i]
                val next1 = text.getOrNull(i + 1)
                val next2 = text.getOrNull(i + 2)

                // Maddah sign
                if (next1 == MADDAH_ABOVE || next2 == MADDAH_ABOVE) {
                    val end = (i + 3).coerceAtMost(text.length)
                    addStyle(SpanStyle(color = TajweedMadd), i, end)
                }

                // Ghunnah on Noon / Meem Mushaddadah
                if ((ch == 'ن' || ch == 'م') && next1 == SHADDA) {
                    val end = (i + 3).coerceAtMost(text.length)
                    addStyle(SpanStyle(color = TajweedGhunnah), i, end)
                }

                // Qalqalah (Qaf, Ta, Ba, Jeem, Dal with Sukun or Uthmani Sukun)
                if (ch in qalqalahLetters && (next1 == SUKUN || next1 == UTHMANI_SUKUN)) {
                    val end = (i + 2).coerceAtMost(text.length)
                    addStyle(SpanStyle(color = TajweedQalqalah), i, end)
                }

                // Iqlab (small high meem)
                if (next1 == SMALL_HIGH_MEEM || next2 == SMALL_HIGH_MEEM) {
                    val end = (i + 3).coerceAtMost(text.length)
                    addStyle(SpanStyle(color = TajweedIkhfa), i, end)
                }

                // Noon Sakinah / Tanween indicator for Idgham/Ikhfa awareness
                if (ch == 'ن' && (next1 == SUKUN || next1 == UTHMANI_SUKUN)) {
                    val end = (i + 2).coerceAtMost(text.length)
                    addStyle(SpanStyle(color = TajweedIdgham), i, end)
                }
                i++
            }
        }
    }
}
