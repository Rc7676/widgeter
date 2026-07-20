package com.widgeter.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate

/** Applies the saved light/dark mode, sets up notifications, warms the data layer. */
class WidgeterApp : Application() {

    companion object {
        const val CHANNEL_TIMER = "timer"
    }

    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(Store.getThemeMode(this))
        createTimerChannel()
        Repo.init(this)
    }

    private fun createTimerChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_TIMER,
                getString(R.string.channel_timer),
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = getString(R.string.channel_timer_desc) }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}
