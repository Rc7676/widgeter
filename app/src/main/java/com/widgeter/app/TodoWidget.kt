package com.widgeter.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.RemoteViews

/** A checklist you can tick off directly from the home screen. */
class TodoWidget : AppWidgetProvider() {

    companion object {
        const val ACTION_TOGGLE = "com.widgeter.app.TODO_TOGGLE"
        const val EXTRA_ID = "com.widgeter.app.EXTRA_ID"
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
            val itemId = intent.getLongExtra(EXTRA_ID, -1L)
            if (itemId >= 0) {
                val items = Store.getTodos(context)
                val idx = items.indexOfFirst { it.id == itemId }
                if (idx >= 0) {
                    items[idx] = items[idx].copy(done = !items[idx].done)
                    Store.saveTodos(context, items)
                }
            }
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, TodoWidget::class.java))
            mgr.notifyAppWidgetViewDataChanged(ids, R.id.todo_list)
            for (id in ids) render(context, mgr, id)
        }
    }

    private fun render(context: Context, mgr: AppWidgetManager, id: Int) {
        val views = RemoteViews(context.packageName, R.layout.todo_widget)

        val todos = Store.getTodos(context)
        val done = todos.count { it.done }
        if (todos.isEmpty()) {
            views.setViewVisibility(R.id.todo_progress, View.GONE)
        } else {
            views.setViewVisibility(R.id.todo_progress, View.VISIBLE)
            views.setTextViewText(R.id.todo_progress, "$done/${todos.size}")
        }

        val serviceIntent = Intent(context, TodoWidgetService::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
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

        // Header "+" and empty state both open the app to add tasks.
        views.setOnClickPendingIntent(R.id.todo_add, openAppPendingIntent(context, 10))
        views.setOnClickPendingIntent(R.id.todo_empty, openAppPendingIntent(context, 11))

        mgr.updateAppWidget(id, views)
    }
}
