package com.widgeter.app

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.Bundle
import android.widget.RemoteViews
import java.util.Calendar

/** Time, date and a greeting. Switches to a compact layout when made small. */
class ClockWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (id in appWidgetIds) render(context, appWidgetManager, id)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        render(context, appWidgetManager, appWidgetId)
    }

    private fun render(context: Context, mgr: AppWidgetManager, id: Int) {
        val minHeight = mgr.getAppWidgetOptions(id)
            .getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
        val compact = minHeight in 1..119

        val views = if (compact) {
            RemoteViews(context.packageName, R.layout.clock_widget_compact)
        } else {
            RemoteViews(context.packageName, R.layout.clock_widget).apply {
                setTextViewText(R.id.clock_greeting, greeting())
            }
        }
        views.setOnClickPendingIntent(R.id.clock_root, openAppPendingIntent(context))
        mgr.updateAppWidget(id, views)
    }

    private fun greeting(): String {
        return when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..20 -> "Good evening"
            else -> "Good night"
        }
    }
}
