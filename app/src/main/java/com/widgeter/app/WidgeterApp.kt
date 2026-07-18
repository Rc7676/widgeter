package com.widgeter.app

import android.app.Application
import com.google.android.material.color.DynamicColors

/** Enables Material You dynamic color (Android 12+) and warms the data layer. */
class WidgeterApp : Application() {
    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
        Repo.init(this)
    }
}
