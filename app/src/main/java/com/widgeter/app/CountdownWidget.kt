package com.widgeter.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.abs

/** Counts the days until (or since) a target date. One per instance. */
class CountdownWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (id in appWidgetIds) render(context, appWidgetManager, id)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        for (id in appWidgetIds) Store.deleteCountdown(context, id)
    }

    private fun render(context: Context, mgr: AppWidgetManager, id: Int) {
        val views = RemoteViews(context.packageName, R.layout.countdown_widget)
        views.setTextViewText(R.id.countdown_title, Store.getCountdownTitle(context, id))

        val target = Store.getCountdownDate(context, id)
        if (target == 0L) {
            views.setTextViewText(R.id.countdown_number, "—")
            views.setTextViewText(R.id.countdown_unit, context.getString(R.string.countdown_set))
            views.setViewVisibility(R.id.countdown_date, View.GONE)
        } else {
            val days = target - Store.todayEpochDay()
            views.setTextViewText(
                R.id.countdown_number,
                if (days == 0L) "🎉" else abs(days).toString()
            )
            views.setTextViewText(
                R.id.countdown_unit,
                context.getString(
                    when {
                        days > 0 -> R.string.countdown_until
                        days == 0L -> R.string.countdown_today
                        else -> R.string.countdown_ago
                    }
                )
            )
            views.setViewVisibility(R.id.countdown_date, View.VISIBLE)
            views.setTextViewText(
                R.id.countdown_date,
                LocalDate.ofEpochDay(target).format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
            )
        }

        views.setOnClickPendingIntent(R.id.countdown_root, editIntent(context, id))
        mgr.updateAppWidget(id, views)
    }

    private fun editIntent(context: Context, id: Int): PendingIntent {
        val intent = Intent(context, CountdownConfigActivity::class.java)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return PendingIntent.getActivity(
            context, id + 70000, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
