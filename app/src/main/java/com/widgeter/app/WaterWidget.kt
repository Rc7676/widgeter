package com.widgeter.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

/** Daily water tracker showing your first tracker (resets each day). */
class WaterWidget : AppWidgetProvider() {

    companion object {
        const val ACTION_ADD = "com.widgeter.app.WATER_ADD"
        const val ACTION_SUB = "com.widgeter.app.WATER_SUB"
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
        val delta = when (intent.action) {
            ACTION_ADD -> 1
            ACTION_SUB -> -1
            else -> return
        }
        Store.resolveWaterPrimary(context)?.let { Store.adjustWaterEntry(context, it.id, delta) }
        val mgr = AppWidgetManager.getInstance(context)
        for (id in mgr.getAppWidgetIds(ComponentName(context, WaterWidget::class.java))) {
            render(context, mgr, id)
        }
    }

    private fun render(context: Context, mgr: AppWidgetManager, id: Int) {
        val views = RemoteViews(context.packageName, R.layout.water_widget)
        val entry = Store.resolveWaterPrimary(context)
        if (entry == null) {
            views.setTextViewText(R.id.water_value, "0/8")
            views.setOnClickPendingIntent(R.id.water_plus, openAppPendingIntent(context, 21000))
            views.setOnClickPendingIntent(R.id.water_minus, openAppPendingIntent(context, 21001))
        } else {
            views.setTextViewText(R.id.water_value, "${Store.waterEntryCount(entry)}/${entry.goal}")
            views.setOnClickPendingIntent(R.id.water_plus, intent(context, ACTION_ADD, 1))
            views.setOnClickPendingIntent(R.id.water_minus, intent(context, ACTION_SUB, 2))
        }
        mgr.updateAppWidget(id, views)
    }

    private fun intent(context: Context, action: String, code: Int): PendingIntent {
        val i = Intent(context, WaterWidget::class.java).setAction(action)
        return PendingIntent.getBroadcast(
            context, code, i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
