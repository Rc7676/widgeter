package com.widgeter.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

/** Shows a quote; tap to shuffle to another. */
class QuoteWidget : AppWidgetProvider() {

    companion object {
        const val ACTION_SHUFFLE = "com.widgeter.app.QUOTE_SHUFFLE"
    }

    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        for (id in ids) render(context, mgr, id)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action != ACTION_SHUFFLE) return
        Store.shuffleQuote(context)
        val mgr = AppWidgetManager.getInstance(context)
        for (id in mgr.getAppWidgetIds(ComponentName(context, QuoteWidget::class.java))) render(context, mgr, id)
    }

    private fun render(context: Context, mgr: AppWidgetManager, id: Int) {
        val views = RemoteViews(context.packageName, R.layout.quote_widget)
        val (text, author) = Quotes.list[Store.quoteIndex(context)]
        views.setTextViewText(R.id.quote_text, "“$text”")
        views.setTextViewText(R.id.quote_author, "— $author")
        views.setTextColor(R.id.quote_label, Store.widgetAccent(context))
        val i = Intent(context, QuoteWidget::class.java).setAction(ACTION_SHUFFLE)
        val pi = PendingIntent.getBroadcast(context, 0, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        views.setOnClickPendingIntent(R.id.quote_root, pi)
        mgr.updateAppWidget(id, views)
    }
}
