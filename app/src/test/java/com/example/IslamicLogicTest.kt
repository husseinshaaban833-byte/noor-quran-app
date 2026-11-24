package com.example

import com.example.data.local.MemorizationPlanEntity
import com.example.islamic.AsrMadhab
import com.example.islamic.CalculationMethod
import com.example.islamic.PrayerTimesCalculator
import com.example.islamic.QiblaCalculator
import com.example.memorization.RecitationAnalyzer
import com.example.memorization.SpacedRepetitionEngine
import com.example.quran.ArabicNormalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IslamicLogicTest {

    @Test
    fun `arabic normalizer removes diacritics without breaking word match`() {
        val uthmani = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ"
        val normalized = ArabicNormalizer.normalizeForSearch(uthmani)
        assertTrue(normalized.contains("بسم"))
        assertTrue(normalized.contains("الله"))
        assertTrue(normalized.contains("الرحيم"))
    }

    @Test
    fun `prayer times calculation produces chronological valid schedule for Makkah`() {
        val schedule = PrayerTimesCalculator.calculate(
            year = 2026,
            month = 11,
            day = 24,
            lat = 21.4225,
            lon = 39.8262,
            timezoneHours = 3.0,
            method = CalculationMethod.UMM_AL_QURA,
            madhab = AsrMadhab.SHAFII,
            nowMinutes = 10 * 60
        )
        assertEquals(6, schedule.prayers.size)
        val fajr = schedule.prayers[0].minutesFromMidnight
        val sunrise = schedule.prayers[1].minutesFromMidnight
        val dhuhr = schedule.prayers[2].minutesFromMidnight
        val asr = schedule.prayers[3].minutesFromMidnight
        val maghrib = schedule.prayers[4].minutesFromMidnight
        val isha = schedule.prayers[5].minutesFromMidnight
        assertTrue(fajr < sunrise)
        assertTrue(sunrise < dhuhr)
        assertTrue(dhuhr < asr)
        assertTrue(asr < maghrib)
        assertTrue(maghrib < isha)
    }

    @Test
    fun `qibla bearing for Riyadh points southwest around 240-250 degrees`() {
        val bearing = QiblaCalculator.calculateBearingDegrees(24.7136, 46.6753)
        assertTrue("Expected SW bearing for Riyadh, got $bearing", bearing in 235.0..255.0)
    }

    @Test
    fun `spaced repetition increases interval on mastery and resets on hard`() {
        val plan = MemorizationPlanEntity(
            surahNumber = 112,
            surahName = "الإخلاص",
            dailyAyahTarget = 4,
            completedAyahs = 0,
            totalAyahs = 4,
            intervalDays = 6,
            easeFactor = 2.5f
        )
        val mastered = SpacedRepetitionEngine.scheduleNextReview(plan, quality = 5)
        assertTrue(mastered.intervalDays > 6)

        val needsReview = SpacedRepetitionEngine.scheduleNextReview(plan, quality = 1)
        assertEquals(1, needsReview.intervalDays)
    }

    @Test
    fun `recitation analyzer detects exact match, omission, and low confidence`() {
        val expected = "قُلْ هُوَ اللَّهُ أَحَدٌ اللَّهُ الصَّمَدُ"
        val exact = RecitationAnalyzer.analyze(expected, "قل هو الله احد الله الصمد")
        assertTrue(exact.confident)
        assertEquals(100, exact.accuracyPercent)
        assertEquals(0, exact.omittedCount)

        val omitted = RecitationAnalyzer.analyze(expected, "قل هو الله الله الصمد")
        assertTrue(omitted.confident)
        assertEquals(1, omitted.omittedCount)

        val unclear = RecitationAnalyzer.analyze(expected, "")
        assertFalse(unclear.confident)
        assertTrue(unclear.summaryMessage.contains("تعذر التأكد"))
    }
}
