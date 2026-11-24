package com.example.islamic

import java.util.Calendar
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

enum class CalculationMethod(
    val id: String,
    val labelAr: String,
    val fajrAngle: Double,
    val ishaAngle: Double,
    val ishaMinutesAfterMaghrib: Int? = null
) {
    UMM_AL_QURA("UMM_AL_QURA", "أم القرى (مكة المكرمة)", 18.5, 0.0, 90),
    EGYPTIAN("EGYPTIAN", "الهيئة المصرية العامة للمساحة", 19.5, 17.5),
    MWL("MWL", "رابطة العالم الإسلامي", 18.0, 17.0),
    ISNA("ISNA", "أمريكا الشمالية (ISNA)", 15.0, 15.0),
    KARACHI("KARACHI", "جامعة العلوم الإسلامية بكراتشي", 18.0, 18.0);

    companion object {
        fun fromId(id: String): CalculationMethod =
            entries.firstOrNull { it.id == id } ?: UMM_AL_QURA
    }
}

enum class AsrMadhab(val id: String, val labelAr: String, val shadowFactor: Int) {
    SHAFII("SHAFII", "الجمهور (الشافعي والمالكي والحنبلي)", 1),
    HANAFI("HANAFI", "المذهب الحنفي", 2);

    companion object {
        fun fromId(id: String): AsrMadhab =
            entries.firstOrNull { it.id == id } ?: SHAFII
    }
}

data class PrayerTimeEntry(
    val key: String,
    val nameAr: String,
    val minutesFromMidnight: Int,
    val formattedTime: String
)

data class DailyPrayerSchedule(
    val prayers: List<PrayerTimeEntry>,
    val nextPrayer: PrayerTimeEntry,
    val minutesUntilNext: Int
)

object PrayerTimesCalculator {
    fun calculate(
        year: Int,
        month: Int, // 1..12
        day: Int,
        lat: Double,
        lon: Double,
        timezoneHours: Double,
        method: CalculationMethod,
        madhab: AsrMadhab,
        nowMinutes: Int = currentMinutesOfDay()
    ): DailyPrayerSchedule {
        val jd = julianDay(year, month, day) - timezoneHours / 24.0
        val d = jd - 2451545.0

        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * sinDeg(g) + 0.020 * sinDeg(2 * g))
        val e = 23.439 - 0.00000036 * d

        val ra = atan2Deg(cosDeg(e) * sinDeg(l), cosDeg(l)) / 15.0
        val eqTime = q / 15.0 - fixHour(ra)
        val decl = asinDeg(sinDeg(e) * sinDeg(l))

        val dhuhrHour = fixHour(12.0 + timezoneHours - lon / 15.0 - eqTime)
        val sunriseHour = dhuhrHour - sunAngleTime(lat, decl, 0.833)
        val sunsetHour = dhuhrHour + sunAngleTime(lat, decl, 0.833)
        val fajrHour = dhuhrHour - sunAngleTime(lat, decl, method.fajrAngle)
        val asrAngle = -atanDeg(1.0 / (madhab.shadowFactor + tanDeg(kotlin.math.abs(lat - decl))))
        val asrHour = dhuhrHour + sunAngleTime(lat, decl, asrAngle)
        val ishaHour = if (method.ishaMinutesAfterMaghrib != null) {
            sunsetHour + method.ishaMinutesAfterMaghrib / 60.0
        } else {
            dhuhrHour + sunAngleTime(lat, decl, method.ishaAngle)
        }

        val entries = listOf(
            createEntry("fajr", "الفجر", fajrHour),
            createEntry("sunrise", "الشروق", sunriseHour),
            createEntry("dhuhr", "الظهر", dhuhrHour),
            createEntry("asr", "العصر", asrHour),
            createEntry("maghrib", "المغرب", sunsetHour),
            createEntry("isha", "العشاء", ishaHour)
        )

        val upcoming = entries.firstOrNull { it.key != "sunrise" && it.minutesFromMidnight > nowMinutes }
        val (nextPrayer, remaining) = if (upcoming != null) {
            upcoming to (upcoming.minutesFromMidnight - nowMinutes)
        } else {
            val fajr = entries.first()
            fajr to ((24 * 60 - nowMinutes) + fajr.minutesFromMidnight)
        }

        return DailyPrayerSchedule(
            prayers = entries,
            nextPrayer = nextPrayer,
            minutesUntilNext = remaining.coerceAtLeast(0)
        )
    }

    private fun createEntry(key: String, name: String, hourDecimal: Double): PrayerTimeEntry {
        val totalMinutes = ((fixHour(hourDecimal) * 60.0).toInt()).coerceIn(0, 1439)
        val h24 = totalMinutes / 60
        val m = totalMinutes % 60
        val period = if (h24 < 12) "ص" else "م"
        val h12 = when {
            h24 == 0 -> 12
            h24 > 12 -> h24 - 12
            else -> h24
        }
        val formatted = String.format("%d:%02d %s", h12, m, period)
        return PrayerTimeEntry(key, name, totalMinutes, formatted)
    }

    private fun sunAngleTime(lat: Double, decl: Double, angle: Double): Double {
        val num = -sinDeg(angle) - sinDeg(lat) * sinDeg(decl)
        val den = cosDeg(lat) * cosDeg(decl)
        val cosVal = (num / den).coerceIn(-0.9999, 0.9999)
        return acosDeg(cosVal) / 15.0
    }

    private fun julianDay(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    private fun currentMinutesOfDay(): Int {
        val cal = Calendar.getInstance()
        return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    }

    private fun degToRad(d: Double) = d * PI / 180.0
    private fun radToDeg(r: Double) = r * 180.0 / PI
    private fun sinDeg(d: Double) = sin(degToRad(d))
    private fun cosDeg(d: Double) = cos(degToRad(d))
    private fun tanDeg(d: Double) = tan(degToRad(d))
    private fun asinDeg(x: Double) = radToDeg(asin(x.coerceIn(-1.0, 1.0)))
    private fun acosDeg(x: Double) = radToDeg(acos(x.coerceIn(-1.0, 1.0)))
    private fun atanDeg(x: Double) = radToDeg(atan(x))
    private fun atan2Deg(y: Double, x: Double) = radToDeg(atan2(y, x))

    private fun fixAngle(a: Double): Double {
        val res = a % 360.0
        return if (res < 0) res + 360.0 else res
    }

    private fun fixHour(h: Double): Double {
        val res = h % 24.0
        return if (res < 0) res + 24.0 else res
    }
}
