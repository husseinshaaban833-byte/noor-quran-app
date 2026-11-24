package com.example.islamic

import android.os.Build
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.floor

data class HijriDateInfo(
    val day: Int,
    val month: Int,
    val year: Int,
    val monthNameAr: String,
    val formattedHijri: String,
    val formattedGregorian: String,
    val occasion: String?
)

object HijriCalendarHelper {
    private val hijriMonths = listOf(
        "محرم", "صفر", "ربيع الأول", "ربيع الآخر",
        "جمادى الأولى", "جمادى الآخرة", "رجب", "شعبان",
        "رمضان", "شوال", "ذو القعدة", "ذو الحجة"
    )

    fun today(offsetDays: Int = 0): HijriDateInfo {
        val (year, month, day) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val gDate = LocalDate.now().plusDays(offsetDays.toLong())
            val hijri = HijrahDate.from(gDate)
            Triple(
                hijri.get(ChronoField.YEAR),
                hijri.get(ChronoField.MONTH_OF_YEAR),
                hijri.get(ChronoField.DAY_OF_MONTH)
            )
        } else {
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, offsetDays) }
            gregorianToTabularHijri(
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH)
            )
        }
        val monthName = hijriMonths.getOrElse(month - 1) { "رمضان" }

        val gFormat = SimpleDateFormat("EEEE d MMMM yyyy", Locale.forLanguageTag("ar"))
        val gregorian = gFormat.format(Date())
        val hijriStr = "$day $monthName $year هـ"

        val occasion = detectOccasion(month, day)
        return HijriDateInfo(
            day = day,
            month = month,
            year = year,
            monthNameAr = monthName,
            formattedHijri = hijriStr,
            formattedGregorian = gregorian,
            occasion = occasion
        )
    }

    private fun gregorianToTabularHijri(year: Int, month: Int, day: Int): Triple<Int, Int, Int> {
        var y = year
        var m = month
        if (m < 3) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0).toInt()
        val b = 2 - a + floor(a / 4.0).toInt()
        val jd = floor(365.25 * (y + 4716)).toInt() + floor(30.6001 * (m + 1)).toInt() + day + b - 1524
        val l = jd - 1948440 + 10632
        val n = floor((l - 1) / 10631.0).toInt()
        val l2 = l - 10631 * n + 354
        val j = (floor((10985 - l2) / 5316.0).toInt()) * (floor((50 * l2) / 17719.0).toInt()) +
            (floor(l2 / 5670.0).toInt()) * (floor((43 * l2) / 15238.0).toInt())
        val l3 = l2 - (floor((30 - j) / 15.0).toInt()) * (floor((17719 * j) / 50.0).toInt()) -
            (floor(j / 16.0).toInt()) * (floor((15238 * j) / 43.0).toInt()) + 29
        val hMonth = floor((24 * l3) / 709.0).toInt()
        val hDay = l3 - floor((709 * hMonth) / 24.0).toInt()
        val hYear = 30 * n + j - 30
        return Triple(hYear, hMonth, hDay)
    }

    private fun detectOccasion(month: Int, day: Int): String? = when {
        month == 1 && day == 1 -> "رأس السنة الهجرية"
        month == 1 && day == 10 -> "يوم عاشوراء"
        month == 9 && day == 1 -> "غرة شهر رمضان المبارك"
        month == 9 && day in 21..29 -> "العشر الأواخر من رمضان"
        month == 10 && day == 1 -> "عيد الفطر المبارك"
        month == 12 && day in 1..9 -> "عشر ذي الحجة"
        month == 12 && day == 9 -> "يوم عرفة"
        month == 12 && day == 10 -> "عيد الأضحى المبارك"
        else -> null
    }
}
