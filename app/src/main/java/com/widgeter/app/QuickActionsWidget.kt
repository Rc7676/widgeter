package com.widgeter.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

/** Home-screen shortcuts that deep-link into the app: new note / task / counter. */
class QuickActionsWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        for (id in ids) render(context, mgr, id)
    }

    private fun render(context: Context, mgr: AppWidgetManager, id: Int) {
        val views = RemoteViews(context.packageName, R.layout.quickactions_widget)
        views.setTextColor(R.id.qa_label, Store.widgetAccent(context))
        views.setOnClickPendingIntent(R.id.qa_note, act(context, MainActivity.ACTION_NEW_NOTE, 1))
        views.setOnClickPendingIntent(R.id.qa_task, act(context, MainActivity.ACTION_NEW_TASK, 2))
        views.setOnClickPendingIntent(R.id.qa_counter, act(context, MainActivity.ACTION_NEW_COUNTER, 3))
        mgr.updateAppWidget(id, views)
    }

    private fun act(context: Context, action: String, code: Int): PendingIntent {
        val i = Intent(context, MainActivity::class.java)
            .setAction(action)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(context, code, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
}
