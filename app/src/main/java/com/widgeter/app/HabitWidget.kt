package com.widgeter.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

/** A daily check-in that tracks your current streak. */
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
        Store.checkInHabit(context)
        val mgr = AppWidgetManager.getInstance(context)
        for (id in mgr.getAppWidgetIds(ComponentName(context, HabitWidget::class.java))) {
            render(context, mgr, id)
        }
    }

    private fun render(context: Context, mgr: AppWidgetManager, id: Int) {
        val views = RemoteViews(context.packageName, R.layout.habit_widget)
        val streak = Store.habitStreak(context)
        val doneToday = Store.habitDoneToday(context)
        views.setTextViewText(R.id.habit_streak, streak.toString())
        views.setTextViewText(
            R.id.habit_sub,
            context.getString(
                if (doneToday) R.string.habit_done else R.string.habit_todo
            )
        )
        views.setImageViewResource(
            R.id.habit_check,
            if (doneToday) R.drawable.ic_check_on else R.drawable.ic_check_off
        )

        val intent = Intent(context, HabitWidget::class.java).setAction(ACTION_CHECKIN)
        val pi = PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.habit_check, pi)
        mgr.updateAppWidget(id, views)
    }
}
