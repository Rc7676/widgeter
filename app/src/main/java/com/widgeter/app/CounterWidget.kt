package com.widgeter.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

/** A named counter backed by an app collection item; each widget picks one. */
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

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        for (id in appWidgetIds) Store.clearCounterTarget(context, id)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action
        if (action != ACTION_INC && action != ACTION_DEC) return
        val id = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID
        )
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) return
        Store.resolveCounterForWidget(context, id)?.let { entry ->
            Store.adjustCounterEntry(context, entry.id, if (action == ACTION_INC) 1 else -1)
        }
        render(context, AppWidgetManager.getInstance(context), id)
    }

    private fun render(context: Context, mgr: AppWidgetManager, id: Int) {
        val views = RemoteViews(context.packageName, R.layout.counter_widget)
        val entry = Store.resolveCounterForWidget(context, id)
        if (entry == null) {
            views.setTextViewText(R.id.counter_label, context.getString(R.string.widget_pick))
            views.setTextViewText(R.id.counter_value, "–")
        } else {
            views.setTextViewText(R.id.counter_label, entry.name)
            views.setTextViewText(R.id.counter_value, entry.value.toString())
        }
        views.setOnClickPendingIntent(R.id.counter_plus, buttonIntent(context, ACTION_INC, id))
        views.setOnClickPendingIntent(R.id.counter_minus, buttonIntent(context, ACTION_DEC, id))
        views.setOnClickPendingIntent(R.id.counter_label, editIntent(context, id))
        mgr.updateAppWidget(id, views)
    }

    private fun buttonIntent(context: Context, action: String, id: Int): PendingIntent {
        val intent = Intent(context, CounterWidget::class.java)
            .setAction(action)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
        val requestCode = id * 2 + if (action == ACTION_INC) 0 else 1
        return PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun editIntent(context: Context, id: Int): PendingIntent {
        val intent = Intent(context, CounterConfigActivity::class.java)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return PendingIntent.getActivity(
            context, id * 2 + 90000, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
