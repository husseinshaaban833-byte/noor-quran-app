package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.quran.QuranRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `app name matches localized Arabic Noor Al-Quran title`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("نور القرآن", appName)
    }

    @Test
    fun `bundled quran dataset passes 114 surahs, 6236 ayahs, 604 pages, and sha256 verification`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = QuranRepository(context)
        val report = repo.verifyIntegrity()

        assertTrue("Integrity errors: ${report.errors}", report.passed)
        assertEquals(114, report.surahCount)
        assertEquals(6236, report.ayahCount)
        assertEquals(604, report.pageCount)
        assertTrue(report.checksumsMatched)

        val fatiha = repo.getSurah(1)
        assertEquals("سُورَةُ ٱلْفَاتِحَةِ", fatiha?.nameAr)
        assertEquals(7, fatiha?.ayahCount)

        val searchResults = repo.searchQuran("الحمد لله رب العالمين")
        assertTrue(searchResults.any { it.surahNumber == 1 && it.ayahNumber == 2 })
    }
}
