package com.widgeter.app

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

/** Names a note widget and edits its text, per instance. */
class NoteConfigActivity : AppCompatActivity() {

    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var isConfigure = false

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        setContentView(R.layout.activity_note_config)

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
        val textInput = findViewById<TextInputEditText>(R.id.cfg_text_input)

        nameInput.setText(Store.getNoteLabel(this, widgetId))
        textInput.setText(Store.getNoteText(this, widgetId))

        findViewById<MaterialToolbar>(R.id.config_toolbar)
            .setNavigationOnClickListener { finish() }

        val save = findViewById<MaterialButton>(R.id.cfg_save)
        if (!isConfigure) save.setText(R.string.cfg_save_changes)
        save.setOnClickListener {
            val label = nameInput.text?.toString()?.trim().takeUnless { it.isNullOrEmpty() } ?: "Note"
            val text = textInput.text?.toString().orEmpty()

            Store.setNoteLabel(this, widgetId, label)
            Store.setNoteText(this, widgetId, text)

            NotesWidget().onUpdate(
                this, AppWidgetManager.getInstance(this), intArrayOf(widgetId)
            )

            setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
            finish()
        }
    }
}
