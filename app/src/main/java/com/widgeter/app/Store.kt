package com.widgeter.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import org.json.JSONArray
import org.json.JSONObject

/** A single to-do entry with a stable id (so toggles never hit the wrong row). */
data class TodoItem(val id: Long, val text: String, val done: Boolean)

/** Low-level persistent state for the app + widgets, backed by SharedPreferences. */
object Store {
    private const val PREFS = "widgeter_prefs"
    private const val KEY_NOTE = "note_text"
    private const val KEY_NOTE_TIME = "note_time"
    private const val KEY_COUNTER = "counter_value"
    private const val KEY_TODO = "todo_json"
    private const val KEY_SEQ = "todo_seq"
    private const val KEY_THEME = "theme_mode"
    private const val KEY_ONBOARDED = "onboarded"

    fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // ---- Note ----
    fun getNote(c: Context): String = prefs(c).getString(KEY_NOTE, "") ?: ""
    fun getNoteTime(c: Context): Long = prefs(c).getLong(KEY_NOTE_TIME, 0L)
    fun setNote(c: Context, value: String) {
        prefs(c).edit()
            .putString(KEY_NOTE, value)
            .putLong(KEY_NOTE_TIME, System.currentTimeMillis())
            .apply()
    }

    // ---- Preferences ----
    /** AppCompatDelegate night mode: -1 follow system, 1 light, 2 dark. */
    fun getThemeMode(c: Context): Int = prefs(c).getInt(KEY_THEME, -1)
    fun setThemeMode(c: Context, mode: Int) =
        prefs(c).edit().putInt(KEY_THEME, mode).apply()

    fun isOnboarded(c: Context): Boolean = prefs(c).getBoolean(KEY_ONBOARDED, false)
    fun setOnboarded(c: Context) = prefs(c).edit().putBoolean(KEY_ONBOARDED, true).apply()

    // ---- Counter (legacy global, = the app's "primary" counter) ----
    fun getCounter(c: Context): Int = prefs(c).getInt(KEY_COUNTER, 0)
    fun setCounter(c: Context, value: Int) =
        prefs(c).edit().putInt(KEY_COUNTER, value).apply()

    /**
     * Per-widget-instance data. DEFAULT_ID (0) maps to the legacy global note/
     * counter that the in-app screen edits; real widget ids (>=1) get their own
     * independent data, falling back to the global value on first read so
     * widgets placed before configuration existed keep working.
     */
    const val DEFAULT_ID = 0

    fun getCounterValue(c: Context, id: Int): Int {
        if (id == DEFAULT_ID) return getCounter(c)
        val p = prefs(c)
        val key = "cnt_val_$id"
        return if (p.contains(key)) p.getInt(key, 0) else getCounter(c)
    }

    fun setCounterValue(c: Context, id: Int, value: Int) {
        if (id == DEFAULT_ID) setCounter(c, value)
        else prefs(c).edit().putInt("cnt_val_$id", value).apply()
    }

    fun getCounterLabel(c: Context, id: Int): String =
        if (id == DEFAULT_ID) "Counter"
        else prefs(c).getString("cnt_lbl_$id", "Counter") ?: "Counter"

    fun setCounterLabel(c: Context, id: Int, label: String) {
        if (id != DEFAULT_ID) prefs(c).edit().putString("cnt_lbl_$id", label).apply()
    }

    fun getCounterStep(c: Context, id: Int): Int =
        if (id == DEFAULT_ID) 1 else prefs(c).getInt("cnt_step_$id", 1)

    fun setCounterStep(c: Context, id: Int, step: Int) {
        if (id != DEFAULT_ID) prefs(c).edit().putInt("cnt_step_$id", step.coerceAtLeast(1)).apply()
    }

    fun deleteCounter(c: Context, id: Int) {
        prefs(c).edit().remove("cnt_val_$id").remove("cnt_lbl_$id").remove("cnt_step_$id").apply()
    }

    fun getNoteText(c: Context, id: Int): String {
        if (id == DEFAULT_ID) return getNote(c)
        val p = prefs(c)
        val key = "note_txt_$id"
        return if (p.contains(key)) p.getString(key, "") ?: "" else getNote(c)
    }

    fun getNoteTimeFor(c: Context, id: Int): Long =
        if (id == DEFAULT_ID) getNoteTime(c) else prefs(c).getLong("note_time_$id", 0L)

    fun setNoteText(c: Context, id: Int, text: String) {
        if (id == DEFAULT_ID) {
            setNote(c, text)
        } else {
            prefs(c).edit()
                .putString("note_txt_$id", text)
                .putLong("note_time_$id", System.currentTimeMillis())
                .apply()
        }
    }

    fun getNoteLabel(c: Context, id: Int): String =
        if (id == DEFAULT_ID) "Note"
        else prefs(c).getString("note_lbl_$id", "Note") ?: "Note"

    fun setNoteLabel(c: Context, id: Int, label: String) {
        if (id != DEFAULT_ID) prefs(c).edit().putString("note_lbl_$id", label).apply()
    }

    fun deleteNote(c: Context, id: Int) {
        prefs(c).edit().remove("note_txt_$id").remove("note_time_$id").remove("note_lbl_$id").apply()
    }

    // ---- Ids ----
    fun nextId(c: Context): Long {
        val next = prefs(c).getLong(KEY_SEQ, 1L)
        prefs(c).edit().putLong(KEY_SEQ, next + 1).apply()
        return next
    }

    // ---- To-do list ----
    fun getTodos(c: Context): MutableList<TodoItem> {
        val raw = prefs(c).getString(KEY_TODO, null) ?: return mutableListOf()
        return try {
            val arr = JSONArray(raw)
            var needsMigration = false
            var seq = prefs(c).getLong(KEY_SEQ, 1L)
            val list = MutableList(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                var id = o.optLong("id", 0L)
                if (id == 0L) {
                    id = seq++
                    needsMigration = true
                }
                TodoItem(id, o.getString("t"), o.optBoolean("d", false))
            }
            if (needsMigration) {
                prefs(c).edit().putLong(KEY_SEQ, seq).apply()
                saveTodos(c, list)
            }
            list
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    fun saveTodos(c: Context, items: List<TodoItem>) {
        val arr = JSONArray()
        for (item in items) {
            arr.put(
                JSONObject()
                    .put("id", item.id)
                    .put("t", item.text)
                    .put("d", item.done)
            )
        }
        prefs(c).edit().putString(KEY_TODO, arr.toString()).apply()
    }
}

/** Opens the main app screen when a widget is tapped. */
fun openAppPendingIntent(context: Context, requestCode: Int = 0): PendingIntent {
    val intent = Intent(context, MainActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    return PendingIntent.getActivity(
        context, requestCode, intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}

/** Pushes fresh data to every widget instance on the home screen. */
object Widgets {
    fun refreshAll(context: Context) {
        val mgr = AppWidgetManager.getInstance(context)
        val providers = listOf(
            NotesWidget::class.java,
            CounterWidget::class.java,
            ClockWidget::class.java,
            TodoWidget::class.java
        )
        for (cls in providers) {
            val ids = mgr.getAppWidgetIds(ComponentName(context, cls))
            if (ids.isNotEmpty()) {
                context.sendBroadcast(
                    Intent(context, cls).apply {
                        action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                    }
                )
            }
        }
        val todoIds = mgr.getAppWidgetIds(ComponentName(context, TodoWidget::class.java))
        if (todoIds.isNotEmpty()) {
            mgr.notifyAppWidgetViewDataChanged(todoIds, R.id.todo_list)
        }
    }
}
