package com.widgeter.app

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/** Fires when a timer reaches zero: clears running state and notifies the user. */
class TimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Store.onTimerFinished(context)

        val notification = NotificationCompat.Builder(context, WidgeterApp.CHANNEL_TIMER)
            .setSmallIcon(R.drawable.ic_timer)
            .setContentTitle(context.getString(R.string.timer_done_title))
            .setContentText(context.getString(R.string.timer_done_text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(openAppPendingIntent(context, 9102))
            .build()
        try {
            NotificationManagerCompat.from(context).notify(9101, notification)
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS not granted; nothing more we can do.
        }

        val mgr = AppWidgetManager.getInstance(context)
        val ids = mgr.getAppWidgetIds(ComponentName(context, TimerWidget::class.java))
        if (ids.isNotEmpty()) TimerWidget().onUpdate(context, mgr, ids)
    }
}
