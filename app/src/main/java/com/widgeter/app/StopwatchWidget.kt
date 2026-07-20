package com.widgeter.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

/** A self-ticking stopwatch on the home screen. */
class StopwatchWidget : AppWidgetProvider() {

    companion object {
        const val ACTION_TOGGLE = "com.widgeter.app.SW_TOGGLE"
        const val ACTION_RESET = "com.widgeter.app.SW_RESET"
    }

    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        for (id in ids) render(context, mgr, id)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_TOGGLE -> if (Store.stopwatchRunning(context)) Store.pauseStopwatch(context) else Store.startStopwatch(context)
            ACTION_RESET -> Store.resetStopwatch(context)
            else -> return
        }
        val mgr = AppWidgetManager.getInstance(context)
        for (id in mgr.getAppWidgetIds(ComponentName(context, StopwatchWidget::class.java))) render(context, mgr, id)
    }

    private fun render(context: Context, mgr: AppWidgetManager, id: Int) {
        val views = RemoteViews(context.packageName, R.layout.stopwatch_widget)
        val running = Store.stopwatchRunning(context)
        views.setChronometerCountDown(R.id.sw_chrono, false)
        views.setChronometer(R.id.sw_chrono, Store.stopwatchBase(context), null, running)
        views.setImageViewResource(R.id.sw_toggle, if (running) R.drawable.ic_pause else R.drawable.ic_play)
        views.setOnClickPendingIntent(R.id.sw_toggle, pi(context, ACTION_TOGGLE, 1))
        views.setOnClickPendingIntent(R.id.sw_reset, pi(context, ACTION_RESET, 2))
        views.setTextColor(R.id.sw_label, Store.widgetAccent(context))
        mgr.updateAppWidget(id, views)
    }

    private fun pi(context: Context, action: String, code: Int): PendingIntent {
        val i = Intent(context, StopwatchWidget::class.java).setAction(action)
        return PendingIntent.getBroadcast(context, code, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
}
