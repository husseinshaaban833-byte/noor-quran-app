package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class NoorApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java) ?: return
            val playbackChannel = NotificationChannel(
                CHANNEL_PLAYBACK,
                getString(R.string.playback_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "عناصر التحكم بتلاوة القرآن الكريم وإذاعة القرآن"
            }
            val prayerChannel = NotificationChannel(
                CHANNEL_PRAYER,
                getString(R.string.prayer_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "تنبيهات مواقيت الصلاة وورد الحفظ"
            }
            manager.createNotificationChannel(playbackChannel)
            manager.createNotificationChannel(prayerChannel)
        }
    }

    companion object {
        const val CHANNEL_PLAYBACK = "noor_playback_channel"
        const val CHANNEL_PRAYER = "noor_prayer_channel"
    }
}
