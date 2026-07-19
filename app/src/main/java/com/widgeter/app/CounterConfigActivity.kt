package com.widgeter.app

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.RadioGroup
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.radiobutton.MaterialRadioButton
import com.google.android.material.textfield.TextInputEditText

/** Picks which app counter a widget shows, or creates a new one. */
class CounterConfigActivity : AppCompatActivity() {

    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var isConfigure = false

    /** radio view id -> entry id, or null for the "New counter" choice. */
    private val choiceMap = HashMap<Int, Long?>()

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(Themes.styleFor(this))
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        setContentView(R.layout.activity_counter_config)

        val root = findViewById<View>(R.id.config_root)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(top = bars.top, left = bars.left, right = bars.right, bottom = bars.bottom)
            insets
        }

        widgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) { finish(); return }
        isConfigure = intent?.action == AppWidgetManager.ACTION_APPWIDGET_CONFIGURE

        val group = findViewById<RadioGroup>(R.id.cfg_choices)
        val newFields = findViewById<LinearLayout>(R.id.cfg_new_fields)

        // "New counter" option first.
        val newRadio = MaterialRadioButton(this).apply {
            id = View.generateViewId()
            text = getString(R.string.cfg_new_counter_opt)
            minHeight = dp(48)
        }
        group.addView(newRadio)
        choiceMap[newRadio.id] = null

        val currentTarget = Store.getCounterTarget(this, widgetId)
        var selectId = newRadio.id
        for (entry in Store.getCounterList(this)) {
            val rb = MaterialRadioButton(this).apply {
                id = View.generateViewId()
                text = "${entry.name}  ·  ${entry.value}"
                minHeight = dp(48)
            }
            group.addView(rb)
            choiceMap[rb.id] = entry.id
            if (entry.id == currentTarget) selectId = rb.id
        }

        group.setOnCheckedChangeListener { _, checkedId ->
            newFields.visibility = if (choiceMap[checkedId] == null) View.VISIBLE else View.GONE
        }
        group.check(selectId)

        findViewById<MaterialToolbar>(R.id.config_toolbar).setNavigationOnClickListener { finish() }

        val save = findViewById<MaterialButton>(R.id.cfg_save)
        if (!isConfigure) save.setText(R.string.cfg_save_changes)
        save.setOnClickListener {
            val entryId = choiceMap[group.checkedRadioButtonId]
            val target: Long = if (entryId == null) {
                val name = findViewById<TextInputEditText>(R.id.cfg_name_input)
                    .text?.toString()?.trim().takeUnless { it.isNullOrEmpty() } ?: "Counter"
                val step = findViewById<TextInputEditText>(R.id.cfg_step_input)
                    .text?.toString()?.trim()?.toIntOrNull()?.coerceAtLeast(1) ?: 1
                val created = Store.addCounterEntry(this, name)
                Store.renameCounterEntry(this, created.id, name, step)
                created.id
            } else {
                entryId
            }
            Store.setCounterTarget(this, widgetId, target)
            CounterWidget().onUpdate(this, AppWidgetManager.getInstance(this), intArrayOf(widgetId))
            setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
            finish()
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
