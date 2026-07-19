package com.widgeter.app

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.TextInputEditText
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/** Names a countdown widget and picks its target date, per instance. */
class CountdownConfigActivity : AppCompatActivity() {

    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var isConfigure = false
    private var targetEpochDay = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(Themes.styleFor(this))
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        setContentView(R.layout.activity_countdown_config)

        val root = findViewById<View>(R.id.config_root)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(top = bars.top, left = bars.left, right = bars.right, bottom = bars.bottom)
            insets
        }

        widgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        isConfigure = intent?.action == AppWidgetManager.ACTION_APPWIDGET_CONFIGURE

        val nameInput = findViewById<TextInputEditText>(R.id.cfg_name_input)
        val dateLabel = findViewById<TextView>(R.id.cfg_date_label)
        val btnDate = findViewById<MaterialButton>(R.id.cfg_date_btn)

        nameInput.setText(Store.getCountdownTitle(this, widgetId))
        targetEpochDay = Store.getCountdownDate(this, widgetId)

        fun refresh() {
            dateLabel.text = if (targetEpochDay > 0) {
                LocalDate.ofEpochDay(targetEpochDay)
                    .format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
            } else ""
        }
        refresh()

        btnDate.setOnClickListener {
            val initial = if (targetEpochDay > 0) {
                targetEpochDay * 24L * 60L * 60L * 1000L
            } else {
                MaterialDatePicker.todayInUtcMilliseconds()
            }
            val picker = MaterialDatePicker.Builder.datePicker()
                .setSelection(initial)
                .build()
            picker.addOnPositiveButtonClickListener { utcMillis ->
                targetEpochDay = Instant.ofEpochMilli(utcMillis)
                    .atZone(ZoneOffset.UTC).toLocalDate().toEpochDay()
                refresh()
            }
            picker.show(supportFragmentManager, "cd_picker")
        }

        findViewById<MaterialToolbar>(R.id.config_toolbar)
            .setNavigationOnClickListener { finish() }

        val save = findViewById<MaterialButton>(R.id.cfg_save)
        if (!isConfigure) save.setText(R.string.cfg_save_changes)
        save.setOnClickListener {
            val title = nameInput.text?.toString()?.trim().takeUnless { it.isNullOrEmpty() } ?: "Countdown"
            Store.setCountdownTitle(this, widgetId, title)
            Store.setCountdownDate(this, widgetId, targetEpochDay)

            CountdownWidget().onUpdate(
                this, AppWidgetManager.getInstance(this), intArrayOf(widgetId)
            )

            setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
            finish()
        }
    }
}
