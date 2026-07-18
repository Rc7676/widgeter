package com.widgeter.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

/** A number with + and − buttons that live right on the home screen. */
class CounterWidget : AppWidgetProvider() {

    companion object {
        const val ACTION_INC = "com.widgeter.app.COUNTER_INC"
        const val ACTION_DEC = "com.widgeter.app.COUNTER_DEC"
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (id in appWidgetIds) render(context, appWidgetManager, id)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_INC -> Store.setCounter(context, Store.getCounter(context) + 1)
            ACTION_DEC -> Store.setCounter(context, Store.getCounter(context) - 1)
            else -> return
        }
        val mgr = AppWidgetManager.getInstance(context)
        val ids = mgr.getAppWidgetIds(ComponentName(context, CounterWidget::class.java))
        for (id in ids) render(context, mgr, id)
    }

    private fun render(context: Context, mgr: AppWidgetManager, id: Int) {
        val views = RemoteViews(context.packageName, R.layout.counter_widget)
        views.setTextViewText(R.id.counter_value, Store.getCounter(context).toString())
        views.setOnClickPendingIntent(R.id.counter_plus, buttonIntent(context, ACTION_INC, 1))
        views.setOnClickPendingIntent(R.id.counter_minus, buttonIntent(context, ACTION_DEC, 2))
        mgr.updateAppWidget(id, views)
    }

    private fun buttonIntent(context: Context, action: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, CounterWidget::class.java).setAction(action)
        return PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
