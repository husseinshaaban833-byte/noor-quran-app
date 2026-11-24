package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val AmiriQuranFamily = FontFamily(Font(R.font.amiri_quran, FontWeight.Normal))
val AmiriFamily = FontFamily(Font(R.font.amiri, FontWeight.Normal))
val ScheherazadeFamily = FontFamily(Font(R.font.scheherazade_new, FontWeight.Normal))
val NotoNaskhFamily = FontFamily(Font(R.font.noto_naskh_arabic, FontWeight.Normal))
val CairoFamily = FontFamily(Font(R.font.cairo, FontWeight.Normal))

enum class ReaderFontOption(val id: String, val labelAr: String, val fontFamily: FontFamily) {
    AMIRI_QURAN("amiri_quran", "الخط العثماني (أميري قرآن)", AmiriQuranFamily),
    SCHEHERAZADE("scheherazade", "نسخ شهرزاد المشكول", ScheherazadeFamily),
    AMIRI("amiri", "خط النسخ الكلاسيكي (أميري)", AmiriFamily),
    NOTO_NASKH("noto_naskh", "خط النسخ الحديث (نوتو)", NotoNaskhFamily),
    CAIRO("cairo", "خط القاهرة الواضح", CairoFamily);

    companion object {
        fun fromId(id: String): ReaderFontOption =
            entries.firstOrNull { it.id == id } ?: AMIRI_QURAN
    }
}

val NoorTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = AmiriFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 42.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = CairoFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 34.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = CairoFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 30.sp
    ),
    titleLarge = TextStyle(
        fontFamily = CairoFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontFamily = CairoFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = NotoNaskhFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 26.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = NotoNaskhFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 22.sp
    ),
    labelLarge = TextStyle(
        fontFamily = CairoFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = CairoFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 18.sp
    )
)
