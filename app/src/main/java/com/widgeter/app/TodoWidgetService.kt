package com.widgeter.app

import android.content.Context
import android.content.Intent
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
        // Dim completed tasks (strikethrough isn't remotable, so we grey them out).
        views.setTextColor(
            R.id.todo_row_text,
            if (item.done) 0xFF9E9E9E.toInt() else 0xFF1C1B1F.toInt()
        )

        val fillIn = Intent().putExtra(TodoWidget.EXTRA_POS, position)
        views.setOnClickFillInIntent(R.id.todo_row, fillIn)
        return views
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = position.toLong()
    override fun hasStableIds(): Boolean = false
}
