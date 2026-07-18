package com.widgeter.app

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import android.widget.RemoteViews
import android.widget.RemoteViewsService

/** Feeds to-do rows into the checklist widget's ListView. */
class TodoWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory =
        TodoViewsFactory(applicationContext)
}

private class TodoViewsFactory(
    private val context: Context
) : RemoteViewsService.RemoteViewsFactory {

    private var items: List<TodoItem> = emptyList()

    override fun onCreate() {}

    override fun onDataSetChanged() {
        items = Store.getTodos(context)
    }

    override fun onDestroy() {}

    override fun getCount(): Int = items.size

    override fun getViewAt(position: Int): RemoteViews {
        val item = items[position]
        val views = RemoteViews(context.packageName, R.layout.todo_row)
        views.setTextViewText(R.id.todo_row_text, item.text)
        views.setImageViewResource(
            R.id.todo_row_check,
            if (item.done) R.drawable.ic_check_on else R.drawable.ic_check_off
        )
        // getColor resolves day/night automatically for the current config.
        views.setTextColor(
            R.id.todo_row_text,
            ContextCompat.getColor(
                context,
                if (item.done) R.color.widget_on_card_muted else R.color.widget_on_card
            )
        )

        val fillIn = Intent().putExtra(TodoWidget.EXTRA_ID, item.id)
        views.setOnClickFillInIntent(R.id.todo_row, fillIn)
        return views
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = items[position].id
    override fun hasStableIds(): Boolean = true
}
