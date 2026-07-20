package com.widgeter.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

/** A clock for a second time zone; tap to cycle through world cities. */
class WorldClockWidget : AppWidgetProvider() {

    companion object {
        const val ACTION_CYCLE = "com.widgeter.app.WC_CYCLE"
    }

    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        for (id in ids) render(context, mgr, id)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action != ACTION_CYCLE) return
        Store.cycleWorldClock(context)
        val mgr = AppWidgetManager.getInstance(context)
        for (id in mgr.getAppWidgetIds(ComponentName(context, WorldClockWidget::class.java))) render(context, mgr, id)
    }

    private fun render(context: Context, mgr: AppWidgetManager, id: Int) {
        val zone = Store.worldZoneId(context)
        val views = RemoteViews(context.packageName, R.layout.worldclock_widget)
        views.setTextViewText(R.id.wc_city, Store.worldCity(context))
        views.setTextColor(R.id.wc_city, Store.widgetAccent(context))
        views.setString(R.id.wc_time, "setTimeZone", zone)
        views.setString(R.id.wc_date, "setTimeZone", zone)
        views.setOnClickPendingIntent(R.id.wc_root, pi(context))
        mgr.updateAppWidget(id, views)
    }

    private fun pi(context: Context) = PendingIntent.getBroadcast(
        context, 0,
        Intent(context, WorldClockWidget::class.java).setAction(ACTION_CYCLE),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}
