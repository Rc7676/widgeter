package com.widgeter.app

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

/** Shows today's date detail: weekday, ISO week number, day-of-year and days left. */
class DayInfoWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        for (id in ids) render(context, mgr, id)
    }

    private fun render(context: Context, mgr: AppWidgetManager, id: Int) {
        val today = LocalDate.now()
        val week = today.get(WeekFields.ISO.weekOfWeekBasedYear())
        val dayOfYear = today.dayOfYear
        val lengthOfYear = today.lengthOfYear()
        val left = lengthOfYear - dayOfYear

        val views = RemoteViews(context.packageName, R.layout.dayinfo_widget)
        views.setTextViewText(R.id.day_num, today.dayOfMonth.toString())
        views.setTextViewText(
            R.id.day_weekday,
            today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
        )
        views.setTextViewText(R.id.day_month, today.format(DateTimeFormatter.ofPattern("MMMM yyyy")))
        views.setTextViewText(R.id.day_week, context.getString(R.string.dayinfo_week_fmt, week))
        views.setTextViewText(
            R.id.day_detail,
            context.getString(R.string.dayinfo_detail_fmt, dayOfYear, lengthOfYear, left)
        )
        views.setTextColor(R.id.day_num, Store.widgetAccent(context))
        views.setOnClickPendingIntent(R.id.day_root, openAppPendingIntent(context, 9500))
        mgr.updateAppWidget(id, views)
    }
}
