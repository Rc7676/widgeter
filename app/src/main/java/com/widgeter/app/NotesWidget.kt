package com.widgeter.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.text.format.DateUtils
import android.view.View
import android.widget.RemoteViews

/** A named note backed by an app collection item; each widget picks one. */
class NotesWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (id in appWidgetIds) render(context, appWidgetManager, id)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        for (id in appWidgetIds) Store.clearNoteTarget(context, id)
    }

    private fun render(context: Context, mgr: AppWidgetManager, id: Int) {
        val views = RemoteViews(context.packageName, R.layout.notes_widget)
        val entry = Store.resolveNoteForWidget(context, id)

        if (entry == null) {
            views.setTextViewText(R.id.notes_label, context.getString(R.string.notes_title))
            views.setTextViewText(R.id.notes_text, context.getString(R.string.widget_pick))
            views.setViewVisibility(R.id.notes_time, View.GONE)
            views.setOnClickPendingIntent(R.id.notes_root, editIntent(context, id))
        } else {
            views.setTextViewText(R.id.notes_label, entry.name)
            views.setTextViewText(
                R.id.notes_text,
                entry.text.ifBlank { context.getString(R.string.notes_empty) }
            )
            if (entry.text.isNotBlank() && entry.time > 0) {
                views.setViewVisibility(R.id.notes_time, View.VISIBLE)
                views.setTextViewText(
                    R.id.notes_time,
                    DateUtils.getRelativeTimeSpanString(
                        entry.time, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS
                    )
                )
            } else {
                views.setViewVisibility(R.id.notes_time, View.GONE)
            }
            views.setOnClickPendingIntent(R.id.notes_root, openAppPendingIntent(context, id + 60000))
        }
        mgr.updateAppWidget(id, views)
    }

    private fun editIntent(context: Context, id: Int): PendingIntent {
        val intent = Intent(context, NoteConfigActivity::class.java)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return PendingIntent.getActivity(
            context, id + 80000, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
