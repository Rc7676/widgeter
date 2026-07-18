package com.widgeter.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews

/** A checklist you can tick off directly from the home screen. */
class TodoWidget : AppWidgetProvider() {

    companion object {
        const val ACTION_TOGGLE = "com.widgeter.app.TODO_TOGGLE"
        const val EXTRA_POS = "com.widgeter.app.EXTRA_POS"
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
        if (intent.action == ACTION_TOGGLE) {
            val pos = intent.getIntExtra(EXTRA_POS, -1)
            if (pos >= 0) {
                val items = Store.getTodos(context)
                if (pos < items.size) {
                    items[pos] = items[pos].copy(done = !items[pos].done)
                    Store.saveTodos(context, items)
                }
            }
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, TodoWidget::class.java))
            mgr.notifyAppWidgetViewDataChanged(ids, R.id.todo_list)
        }
    }

    private fun render(context: Context, mgr: AppWidgetManager, id: Int) {
        val views = RemoteViews(context.packageName, R.layout.todo_widget)

        val serviceIntent = Intent(context, TodoWidgetService::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
            // Unique data per widget so the list adapter is not cached across instances.
            data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
        }
        views.setRemoteAdapter(R.id.todo_list, serviceIntent)
        views.setEmptyView(R.id.todo_list, R.id.todo_empty)

        val toggleIntent = Intent(context, TodoWidget::class.java).setAction(ACTION_TOGGLE)
        val template = PendingIntent.getBroadcast(
            context, 0, toggleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        views.setPendingIntentTemplate(R.id.todo_list, template)

        mgr.updateAppWidget(id, views)
    }
}
