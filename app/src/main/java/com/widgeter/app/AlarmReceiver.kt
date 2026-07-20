package com.widgeter.app

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/** Fires at an alarm's time: shows a full-screen ringing screen and reschedules. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra("alarm_id", -1L)
        val entry = Store.findAlarm(context, id) ?: return

        val fullIntent = Intent(context, AlarmActivity::class.java)
            .putExtra("alarm_id", id)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val fullPi = PendingIntent.getActivity(
            context, id.toInt(), fullIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = entry.label.ifBlank { context.getString(R.string.alarm_title) }
        val notification = NotificationCompat.Builder(context, WidgeterApp.CHANNEL_ALARM)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle(title)
            .setContentText(String.format("%02d:%02d", entry.hour, entry.minute))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullPi, true)
            .setContentIntent(fullPi)
            .setAutoCancel(true)
            .setOngoing(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id.toInt(), notification)
        } catch (e: SecurityException) {
            // No notification permission; still try to launch the alarm screen.
            try { context.startActivity(fullIntent) } catch (e2: Exception) { }
        }

        // Daily repeat: schedule the next occurrence.
        if (entry.enabled) Store.scheduleAlarm(context, entry)
    }
}
