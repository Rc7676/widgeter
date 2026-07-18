package com.widgeter.app

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.color.DynamicColors

/** Enables Material You dynamic color, applies the saved theme, warms the data layer. */
class WidgeterApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(Store.getThemeMode(this))
        DynamicColors.applyToActivitiesIfAvailable(this)
        Repo.init(this)
    }
}
