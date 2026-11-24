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
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.MainActivity
import com.example.core.PrefsDataStore
import com.example.islamic.AsrMadhab
import com.example.islamic.CalculationMethod
import com.example.islamic.HijriCalendarHelper
import com.example.islamic.PrayerTimesCalculator
import kotlinx.coroutines.flow.first
import java.util.Calendar

class PrayerTimesWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = PrefsDataStore(context).preferencesFlow.first()
        val cal = Calendar.getInstance()
        val schedule = PrayerTimesCalculator.calculate(
            year = cal.get(Calendar.YEAR),
            month = cal.get(Calendar.MONTH) + 1,
            day = cal.get(Calendar.DAY_OF_MONTH),
            lat = prefs.cityLat,
            lon = prefs.cityLon,
            timezoneHours = prefs.cityTz,
            method = CalculationMethod.fromId(prefs.calculationMethod),
            madhab = AsrMadhab.fromId(prefs.asrMadhab)
        )
        val hijri = HijriCalendarHelper.today(prefs.hijriOffsetDays)

        provideContent {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(Color(0xFF0A3D2E))
                    .padding(12.dp)
                    .clickable(actionStartActivity<MainActivity>()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(modifier = GlanceModifier.fillMaxWidth()) {
                    Text(
                        text = "${prefs.cityName} • ${hijri.formattedHijri}",
                        style = TextStyle(color = ColorProvider(Color(0xFFE6CA82)), fontSize = 12.sp)
                    )
                }
                Spacer(modifier = GlanceModifier.height(6.dp))
                Text(
                    text = "الصلاة القادمة: ${schedule.nextPrayer.nameAr} — ${schedule.nextPrayer.formattedTime}",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFFFAF6EE)),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = GlanceModifier.height(6.dp))
                Row(modifier = GlanceModifier.fillMaxWidth()) {
                    schedule.prayers.filter { it.key != "sunrise" }.forEach { entry ->
                        Column(
                            modifier = GlanceModifier.defaultWeight(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = entry.nameAr,
                                style = TextStyle(color = ColorProvider(Color(0xFFD7EDE4)), fontSize = 11.sp)
                            )
                            Text(
                                text = entry.formattedTime.replace(" ص", "").replace(" م", ""),
                                style = TextStyle(color = ColorProvider(Color.White), fontSize = 11.sp)
                            )
                        }
                    }
                }
            }
        }
    }
}

class PrayerTimesWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PrayerTimesWidget()
}
