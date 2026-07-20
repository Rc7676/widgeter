package com.widgeter.app

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews

/** Shows the current moon phase, its name and illuminated percentage. */
class MoonWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        for (id in ids) render(context, mgr, id)
    }

    private fun render(context: Context, mgr: AppWidgetManager, id: Int) {
        val views = RemoteViews(context.packageName, R.layout.moon_widget)
        views.setTextViewText(R.id.moon_emoji, Store.moonEmoji())
        views.setTextViewText(R.id.moon_name, Store.moonName())
        views.setTextViewText(R.id.moon_illum, context.getString(R.string.moon_illum_fmt, Store.moonIllumination()))
        views.setTextColor(R.id.moon_label, Store.widgetAccent(context))
        views.setOnClickPendingIntent(R.id.moon_root, openAppPendingIntent(context, 9400))
        mgr.updateAppWidget(id, views)
    }
}
