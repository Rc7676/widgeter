package com.widgeter.app

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.widget.RemoteViews

/** Shows the current battery level with a bar and charging status; tap to refresh. */
class BatteryWidget : AppWidgetProvider() {

    companion object {
        const val ACTION_REFRESH = "com.widgeter.app.BATTERY_REFRESH"
    }

    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        for (id in ids) render(context, mgr, id)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action != ACTION_REFRESH) return
        val mgr = AppWidgetManager.getInstance(context)
        for (id in mgr.getAppWidgetIds(ComponentName(context, BatteryWidget::class.java))) render(context, mgr, id)
    }

    private fun render(context: Context, mgr: AppWidgetManager, id: Int) {
        val bm = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = bm?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = bm?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val pct = if (level >= 0 && scale > 0) (level * 100 / scale) else -1
        val status = bm?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL

        val views = RemoteViews(context.packageName, R.layout.battery_widget)
        views.setTextViewText(R.id.battery_pct, if (pct >= 0) "$pct%" else "—")
        views.setProgressBar(R.id.battery_bar, 100, pct.coerceAtLeast(0), false)
        views.setTextViewText(
            R.id.battery_status,
            when {
                pct < 0 -> context.getString(R.string.battery_hint)
                charging -> context.getString(R.string.battery_charging)
                pct <= 15 -> context.getString(R.string.battery_low)
                else -> context.getString(R.string.battery_ok)
            }
        )
        views.setTextColor(R.id.battery_label, Store.widgetAccent(context))
        views.setOnClickPendingIntent(R.id.battery_root, pi(context))
        mgr.updateAppWidget(id, views)
    }

    private fun pi(context: Context) = android.app.PendingIntent.getBroadcast(
        context, 0,
        Intent(context, BatteryWidget::class.java).setAction(ACTION_REFRESH),
        android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
    )
}
