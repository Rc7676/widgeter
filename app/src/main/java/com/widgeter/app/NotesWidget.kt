package com.widgeter.app

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews

/** Shows the saved note; tapping opens the app to edit it. */
class NotesWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (id in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.notes_widget)
            val note = Store.getNote(context)
            views.setTextViewText(
                R.id.notes_text,
                note.ifBlank { context.getString(R.string.notes_empty) }
            )
            views.setOnClickPendingIntent(R.id.notes_root, openAppPendingIntent(context))
            appWidgetManager.updateAppWidget(id, views)
        }
    }
}
