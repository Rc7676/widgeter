package com.widgeter.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Re-schedules all enabled alarms after a reboot. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Store.rescheduleAllAlarms(context)
        }
    }
}
