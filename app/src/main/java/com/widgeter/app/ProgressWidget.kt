package com.widgeter.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

/** Shows how much of the year / month / week / day has elapsed; tap to cycle scope. */
class ProgressWidget : AppWidgetProvider() {

    companion object {
        const val ACTION_CYCLE = "com.widgeter.app.PROG_CYCLE"
    }

    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        for (id in ids) render(context, mgr, id)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action != ACTION_CYCLE) return
        Store.cycleProgressScope(context)
        val mgr = AppWidgetManager.getInstance(context)
        for (id in mgr.getAppWidgetIds(ComponentName(context, ProgressWidget::class.java))) render(context, mgr, id)
    }

    private fun render(context: Context, mgr: AppWidgetManager, id: Int) {
        val pct = Store.progressPercent(context)
        val views = RemoteViews(context.packageName, R.layout.progress_widget)
        views.setTextViewText(R.id.prog_pct, "$pct%")
        views.setTextViewText(R.id.prog_scope, Store.progressScopeLabel(context))
        views.setProgressBar(R.id.prog_bar, 100, pct, false)
        views.setTextColor(R.id.prog_label, Store.widgetAccent(context))
        views.setOnClickPendingIntent(R.id.prog_root, pi(context))
        mgr.updateAppWidget(id, views)
    }

    private fun pi(context: Context) = PendingIntent.getBroadcast(
        context, 0,
        Intent(context, ProgressWidget::class.java).setAction(ACTION_CYCLE),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}
