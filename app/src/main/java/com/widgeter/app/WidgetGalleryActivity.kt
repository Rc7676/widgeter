package com.widgeter.app

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton

/** Browse all widgets and add them to the home screen (one tap where supported). */
class WidgetGalleryActivity : AppCompatActivity() {

    private data class WidgetInfo(val provider: Class<*>, val nameRes: Int, val descRes: Int)

    private val widgets = listOf(
        WidgetInfo(ClockWidget::class.java, R.string.clock_title, R.string.widget_desc_clock),
        WidgetInfo(NotesWidget::class.java, R.string.notes_title, R.string.widget_desc_notes),
        WidgetInfo(CounterWidget::class.java, R.string.counter_title, R.string.widget_desc_counter),
        WidgetInfo(TodoWidget::class.java, R.string.todo_title, R.string.widget_desc_todo),
        WidgetInfo(HabitWidget::class.java, R.string.habit_title, R.string.widget_desc_habit),
        WidgetInfo(WaterWidget::class.java, R.string.water_title, R.string.widget_desc_water),
        WidgetInfo(CountdownWidget::class.java, R.string.countdown_title, R.string.widget_desc_countdown),
        WidgetInfo(StopwatchWidget::class.java, R.string.stopwatch_title, R.string.widget_desc_stopwatch),
        WidgetInfo(TimerWidget::class.java, R.string.timer_title, R.string.widget_desc_timer),
        WidgetInfo(CalendarWidget::class.java, R.string.calendar_title, R.string.widget_desc_calendar)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(Themes.styleFor(this))
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_widgets)

        val root = findViewById<View>(R.id.gallery_root)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val b = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(top = b.top, left = b.left, right = b.right, bottom = b.bottom)
            insets
        }
        findViewById<MaterialToolbar>(R.id.gallery_toolbar).setNavigationOnClickListener { finish() }

        val container = findViewById<LinearLayout>(R.id.gallery_container)
        for (w in widgets) {
            val row = LayoutInflater.from(this).inflate(R.layout.item_gallery_row, container, false)
            row.findViewById<TextView>(R.id.gallery_name).setText(w.nameRes)
            row.findViewById<TextView>(R.id.gallery_desc).setText(w.descRes)
            row.findViewById<MaterialButton>(R.id.gallery_add).setOnClickListener { addWidget(w.provider) }
            container.addView(row)
        }
    }

    private fun addWidget(provider: Class<*>) {
        val mgr = AppWidgetManager.getInstance(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && mgr.isRequestPinAppWidgetSupported) {
            mgr.requestPinAppWidget(ComponentName(this, provider), null, null)
        } else {
            Toast.makeText(this, R.string.gallery_manual, Toast.LENGTH_LONG).show()
        }
    }
}
