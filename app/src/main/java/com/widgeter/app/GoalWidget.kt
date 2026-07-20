package com.widgeter.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

/** A savings / goal tracker: add or remove toward a target with a progress bar. */
class GoalWidget : AppWidgetProvider() {

    companion object {
        const val ACTION_PLUS = "com.widgeter.app.GOAL_PLUS"
        const val ACTION_MINUS = "com.widgeter.app.GOAL_MINUS"
        const val ACTION_TARGET = "com.widgeter.app.GOAL_TARGET"
    }

    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        for (id in ids) render(context, mgr, id)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_PLUS -> Store.bumpGoal(context, Store.goalStep(context))
            ACTION_MINUS -> Store.bumpGoal(context, -Store.goalStep(context))
            ACTION_TARGET -> Store.cycleGoalTarget(context)
            else -> return
        }
        val mgr = AppWidgetManager.getInstance(context)
        for (id in mgr.getAppWidgetIds(ComponentName(context, GoalWidget::class.java))) render(context, mgr, id)
    }

    private fun render(context: Context, mgr: AppWidgetManager, id: Int) {
        val views = RemoteViews(context.packageName, R.layout.goal_widget)
        views.setTextViewText(R.id.goal_current, Store.goalCurrent(context).toString())
        views.setTextViewText(R.id.goal_target, context.getString(R.string.goal_target_fmt, Store.goalTarget(context)))
        views.setProgressBar(R.id.goal_bar, 100, Store.goalPercent(context), false)
        views.setTextColor(R.id.goal_label, Store.widgetAccent(context))
        views.setOnClickPendingIntent(R.id.goal_plus, pi(context, ACTION_PLUS, 1))
        views.setOnClickPendingIntent(R.id.goal_minus, pi(context, ACTION_MINUS, 2))
        views.setOnClickPendingIntent(R.id.goal_target, pi(context, ACTION_TARGET, 3))
        mgr.updateAppWidget(id, views)
    }

    private fun pi(context: Context, action: String, code: Int): PendingIntent {
        val i = Intent(context, GoalWidget::class.java).setAction(action)
        return PendingIntent.getBroadcast(context, code, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
}
