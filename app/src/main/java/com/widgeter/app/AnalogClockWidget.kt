package com.widgeter.app

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews

/** A self-ticking analog clock with a theme-aware face and hands. */
class AnalogClockWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        for (id in ids) render(context, mgr, id)
    }

    private fun render(context: Context, mgr: AppWidgetManager, id: Int) {
        val views = RemoteViews(context.packageName, R.layout.analogclock_widget)
        views.setOnClickPendingIntent(R.id.analog_root, openAppPendingIntent(context, 9600))
        mgr.updateAppWidget(id, views)
    }
}
