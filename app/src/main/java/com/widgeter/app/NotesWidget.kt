package com.widgeter.app

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.text.format.DateUtils
import android.view.View
import android.widget.RemoteViews

/** Shows the saved note plus when it was last edited; tap to edit. */
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

            val time = Store.getNoteTime(context)
            if (note.isNotBlank() && time > 0) {
                views.setViewVisibility(R.id.notes_time, View.VISIBLE)
                views.setTextViewText(
                    R.id.notes_time,
                    DateUtils.getRelativeTimeSpanString(
                        time, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS
                    )
                )
            } else {
                views.setViewVisibility(R.id.notes_time, View.GONE)
            }

            views.setOnClickPendingIntent(R.id.notes_root, openAppPendingIntent(context))
            appWidgetManager.updateAppWidget(id, views)
        }
    }
}
