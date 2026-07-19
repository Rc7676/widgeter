package com.widgeter.app

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

/** Applies the saved light/dark mode and warms the data layer. */
class WidgeterApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(Store.getThemeMode(this))
        Repo.init(this)
    }
}
