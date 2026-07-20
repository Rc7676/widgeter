package com.widgeter.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate

/** Applies the saved light/dark mode, sets up notification channels, warms the data layer. */
class WidgeterApp : Application() {

    companion object {
        const val CHANNEL_TIMER = "timer"
        const val CHANNEL_ALARM = "alarm"
    }

    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(Store.getThemeMode(this))
        createChannels()
        Repo.init(this)
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(NotificationManager::class.java)

        val timer = NotificationChannel(
            CHANNEL_TIMER, getString(R.string.channel_timer), NotificationManager.IMPORTANCE_HIGH
        ).apply { description = getString(R.string.channel_timer_desc) }
        nm.createNotificationChannel(timer)

        val alarmAttrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val alarm = NotificationChannel(
            CHANNEL_ALARM, getString(R.string.channel_alarm), NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = getString(R.string.channel_alarm_desc)
            setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), alarmAttrs)
            enableVibration(true)
        }
        nm.createNotificationChannel(alarm)
    }
}
