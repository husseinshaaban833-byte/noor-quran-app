package com.example.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.MainActivity
import com.example.quran.QuranRepository
import java.util.Calendar

class DailyAyahWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = QuranRepository(context)
        repo.ensureLoaded()
        val day = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        val ayah = repo.getAyahOfTheDay(day)

        provideContent {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(Color(0xFFFAF6EE))
                    .padding(14.dp)
                    .clickable(actionStartActivity<MainActivity>()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "آية اليوم • نور القرآن",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF0A3D2E)),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = GlanceModifier.height(6.dp))
                Text(
                    text = ayah?.let { "﴿ ${it.textUthmani} ﴾" } ?: "﴿ بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ ﴾",
                    style = TextStyle(color = ColorProvider(Color(0xFF17221E)), fontSize = 14.sp),
                    maxLines = 3
                )
                Spacer(modifier = GlanceModifier.height(4.dp))
                Text(
                    text = ayah?.let { "سورة ${it.surahNameAr} • الآية ${it.ayahNumber}" } ?: "الفاتحة",
                    style = TextStyle(color = ColorProvider(Color(0xFF8E6D24)), fontSize = 11.sp)
                )
            }
        }
    }
}

class DailyAyahWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DailyAyahWidget()
}
