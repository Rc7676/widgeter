package com.widgeter.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

/** Daily check-in showing the streak of your first habit. */
class HabitWidget : AppWidgetProvider() {

    companion object {
        const val ACTION_CHECKIN = "com.widgeter.app.HABIT_CHECKIN"
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
        if (intent.action != ACTION_CHECKIN) return
        Store.resolveHabitPrimary(context)?.let { Store.checkInHabitEntry(context, it.id) }
        val mgr = AppWidgetManager.getInstance(context)
        for (id in mgr.getAppWidgetIds(ComponentName(context, HabitWidget::class.java))) {
            render(context, mgr, id)
        }
    }

    private fun render(context: Context, mgr: AppWidgetManager, id: Int) {
        val views = RemoteViews(context.packageName, R.layout.habit_widget)
        val entry = Store.resolveHabitPrimary(context)
        if (entry == null) {
            views.setTextViewText(R.id.habit_streak, "0")
            views.setTextViewText(R.id.habit_sub, context.getString(R.string.habit_widget_empty))
            views.setImageViewResource(R.id.habit_check, R.drawable.ic_check_off)
            views.setOnClickPendingIntent(R.id.habit_check, openAppPendingIntent(context, 20000))
        } else {
            val done = Store.habitDoneTodayEntry(entry)
            views.setTextViewText(R.id.habit_streak, Store.habitLiveStreak(entry).toString())
            views.setTextViewText(
                R.id.habit_sub,
                context.getString(if (done) R.string.habit_done else R.string.habit_todo)
            )
            views.setImageViewResource(
                R.id.habit_check,
                if (done) R.drawable.ic_check_on else R.drawable.ic_check_off
            )
            val intent = Intent(context, HabitWidget::class.java).setAction(ACTION_CHECKIN)
            val pi = PendingIntent.getBroadcast(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.habit_check, pi)
        }
        mgr.updateAppWidget(id, views)
    }
}
