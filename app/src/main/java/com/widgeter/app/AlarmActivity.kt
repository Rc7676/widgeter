package com.widgeter.app

import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import com.google.android.material.button.MaterialButton

/** Full-screen ringing screen shown when an alarm fires. */
class AlarmActivity : AppCompatActivity() {

    private var ringtone: Ringtone? = null
    private var alarmId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(Themes.styleFor(this))
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        setContentView(R.layout.activity_alarm)

        alarmId = intent?.getLongExtra("alarm_id", -1L) ?: -1L
        val entry = Store.findAlarm(this, alarmId)
        findViewById<TextView>(R.id.alarm_time).text =
            if (entry != null) String.format("%02d:%02d", entry.hour, entry.minute) else "--:--"
        findViewById<TextView>(R.id.alarm_label).text =
            entry?.label?.ifBlank { getString(R.string.alarm_title) } ?: getString(R.string.alarm_title)

        findViewById<MaterialButton>(R.id.alarm_dismiss).setOnClickListener { dismiss() }

        startRinging()
    }

    private fun startRinging() {
        try {
            val uri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ringtone = RingtoneManager.getRingtone(this, uri)?.apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) isLooping = true
                play()
            }
        } catch (e: Exception) {
            // Ringtone unavailable; the notification sound still plays.
        }
    }

    private fun dismiss() {
        try { ringtone?.stop() } catch (e: Exception) { }
        if (alarmId >= 0) NotificationManagerCompat.from(this).cancel(alarmId.toInt())
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        try { ringtone?.stop() } catch (e: Exception) { }
    }
}
