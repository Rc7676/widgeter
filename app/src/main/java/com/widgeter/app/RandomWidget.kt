package com.widgeter.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

/** Coin flip / dice / random number; tap to roll, tap the mode to change it. */
class RandomWidget : AppWidgetProvider() {

    companion object {
        const val ACTION_ROLL = "com.widgeter.app.RND_ROLL"
        const val ACTION_MODE = "com.widgeter.app.RND_MODE"
    }

    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        for (id in ids) render(context, mgr, id)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_ROLL -> Store.rollRandom(context)
            ACTION_MODE -> Store.cycleRandomMode(context)
            else -> return
        }
        val mgr = AppWidgetManager.getInstance(context)
        for (id in mgr.getAppWidgetIds(ComponentName(context, RandomWidget::class.java))) render(context, mgr, id)
    }

    private fun render(context: Context, mgr: AppWidgetManager, id: Int) {
        val views = RemoteViews(context.packageName, R.layout.random_widget)
        views.setTextViewText(R.id.random_mode, Store.randomModeLabel(context))
        views.setTextViewText(R.id.random_result, Store.randomResult(context))
        views.setTextColor(R.id.random_label, Store.widgetAccent(context))
        views.setOnClickPendingIntent(R.id.random_result, pi(context, ACTION_ROLL, 1))
        views.setOnClickPendingIntent(R.id.random_mode, pi(context, ACTION_MODE, 2))
        mgr.updateAppWidget(id, views)
    }

    private fun pi(context: Context, action: String, code: Int): PendingIntent {
        val i = Intent(context, RandomWidget::class.java).setAction(action)
        return PendingIntent.getBroadcast(context, code, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
}
