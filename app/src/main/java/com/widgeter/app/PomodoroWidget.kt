package com.widgeter.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

/** A Pomodoro focus timer: 25 min focus / 5 min break, tap to run or skip phases. */
class PomodoroWidget : AppWidgetProvider() {

    companion object {
        const val ACTION_TOGGLE = "com.widgeter.app.POMO_TOGGLE"
        const val ACTION_NEXT = "com.widgeter.app.POMO_NEXT"
    }

    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        for (id in ids) render(context, mgr, id)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_TOGGLE -> Store.togglePomo(context)
            ACTION_NEXT -> Store.nextPomoPhase(context)
            else -> return
        }
        val mgr = AppWidgetManager.getInstance(context)
        for (id in mgr.getAppWidgetIds(ComponentName(context, PomodoroWidget::class.java))) render(context, mgr, id)
    }

    private fun render(context: Context, mgr: AppWidgetManager, id: Int) {
        val views = RemoteViews(context.packageName, R.layout.pomodoro_widget)
        val running = Store.pomoRunning(context)
        views.setChronometerCountDown(R.id.pomo_chrono, true)
        views.setChronometer(R.id.pomo_chrono, Store.pomoBase(context), null, running)
        views.setTextViewText(R.id.pomo_phase, Store.pomoPhaseLabel(context))
        views.setTextViewText(R.id.pomo_count, "🍅 ${Store.pomoCount(context)}")
        views.setImageViewResource(R.id.pomo_toggle, if (running) R.drawable.ic_pause else R.drawable.ic_play)
        views.setTextColor(R.id.pomo_label, Store.widgetAccent(context))
        views.setOnClickPendingIntent(R.id.pomo_toggle, pi(context, ACTION_TOGGLE, 1))
        views.setOnClickPendingIntent(R.id.pomo_next, pi(context, ACTION_NEXT, 2))
        mgr.updateAppWidget(id, views)
    }

    private fun pi(context: Context, action: String, code: Int): PendingIntent {
        val i = Intent(context, PomodoroWidget::class.java).setAction(action)
        return PendingIntent.getBroadcast(context, code, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
}
