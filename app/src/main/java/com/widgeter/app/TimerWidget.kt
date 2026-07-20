package com.widgeter.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

/** A self-ticking countdown timer on the home screen. */
class TimerWidget : AppWidgetProvider() {

    companion object {
        const val ACTION_TOGGLE = "com.widgeter.app.T_TOGGLE"
        const val ACTION_RESET = "com.widgeter.app.T_RESET"
    }

    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        for (id in ids) render(context, mgr, id)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_TOGGLE -> if (Store.timerRunning(context)) Store.pauseTimer(context) else Store.startTimer(context)
            ACTION_RESET -> Store.resetTimer(context)
            else -> return
        }
        val mgr = AppWidgetManager.getInstance(context)
        for (id in mgr.getAppWidgetIds(ComponentName(context, TimerWidget::class.java))) render(context, mgr, id)
    }

    private fun render(context: Context, mgr: AppWidgetManager, id: Int) {
        val views = RemoteViews(context.packageName, R.layout.timer_widget)
        val running = Store.timerRunning(context)
        views.setChronometerCountDown(R.id.t_chrono, true)
        views.setChronometer(R.id.t_chrono, Store.timerBase(context), null, running)
        views.setImageViewResource(R.id.t_toggle, if (running) R.drawable.ic_pause else R.drawable.ic_play)
        views.setOnClickPendingIntent(R.id.t_toggle, pi(context, ACTION_TOGGLE, 1))
        views.setOnClickPendingIntent(R.id.t_reset, pi(context, ACTION_RESET, 2))
        views.setOnClickPendingIntent(R.id.t_chrono, openAppPendingIntent(context, 9200))
        mgr.updateAppWidget(id, views)
    }

    private fun pi(context: Context, action: String, code: Int): PendingIntent {
        val i = Intent(context, TimerWidget::class.java).setAction(action)
        return PendingIntent.getBroadcast(context, code, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
}
