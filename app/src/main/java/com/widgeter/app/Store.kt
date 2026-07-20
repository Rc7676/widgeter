package com.widgeter.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/** App-managed collection entries (the in-app pages). */
data class NoteEntry(val id: Long, val name: String, val text: String, val time: Long, val color: Int = 0)
data class CounterEntry(val id: Long, val name: String, val value: Int, val step: Int, val color: Int = 0)
data class CountdownEntry(val id: Long, val name: String, val date: Long) // date = epoch day, 0 = unset
data class HabitEntry(val id: Long, val name: String, val streak: Int, val last: Long, val color: Int = 0) // last = epoch day
data class WaterEntry(val id: Long, val name: String, val count: Int, val goal: Int, val day: Long)
data class AlarmEntry(val id: Long, val hour: Int, val minute: Int, val label: String, val enabled: Boolean)

/** A single to-do entry with a stable id (so toggles never hit the wrong row). */
data class TodoItem(
    val id: Long,
    val text: String,
    val done: Boolean,
    val priority: Int = 0,   // 0 none, 1 low, 2 medium, 3 high
    val due: Long = 0L       // epoch millis; 0 = no due date
)

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

    /** Accent theme index: 0 Purple, 1 Red, 2 Blue, 3 Yellow, 4 Black. */
    fun getColorTheme(c: Context): Int = prefs(c).getInt("color_theme", 0)
    fun setColorTheme(c: Context, index: Int) =
        prefs(c).edit().putInt("color_theme", index).apply()

    /** Widget accent color for the current theme (day/night aware). */
    fun widgetAccent(c: Context): Int =
        c.getColor(Themes.swatchColors[getColorTheme(c).coerceIn(0, Themes.swatchColors.size - 1)])

    /** Gradient background drawable for the current theme (for gradient widgets). */
    fun widgetGradRes(c: Context): Int = when (getColorTheme(c)) {
        1 -> R.drawable.widget_grad_red
        2 -> R.drawable.widget_grad_blue
        3 -> R.drawable.widget_grad_yellow
        4 -> R.drawable.widget_grad_black
        else -> R.drawable.widget_clock_bg
    }

    /** Text color on the theme gradient — dark for Yellow (contrast), white otherwise. */
    fun widgetOnGrad(c: Context): Int =
        if (getColorTheme(c) == 3) 0xFF241A00.toInt() else 0xFFFFFFFF.toInt()

    // ---- Backup / restore (all preferences, typed) ----
    fun exportJson(c: Context): String {
        val o = JSONObject()
        for ((k, v) in prefs(c).all) {
            val e = JSONObject()
            when (v) {
                is Boolean -> e.put("t", "b").put("v", v)
                is Int -> e.put("t", "i").put("v", v)
                is Long -> e.put("t", "l").put("v", v)
                is Float -> e.put("t", "f").put("v", v.toDouble())
                is String -> e.put("t", "s").put("v", v)
                else -> continue
            }
            o.put(k, e)
        }
        return JSONObject().put("widgeter_backup", 1).put("data", o).toString()
    }

    /** Returns true on success. */
    fun importJson(c: Context, json: String): Boolean {
        return try {
            val root = JSONObject(json)
            val o = root.optJSONObject("data") ?: return false
            val ed = prefs(c).edit()
            ed.clear()
            val keys = o.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val e = o.getJSONObject(k)
                when (e.getString("t")) {
                    "b" -> ed.putBoolean(k, e.getBoolean("v"))
                    "i" -> ed.putInt(k, e.getInt("v"))
                    "l" -> ed.putLong(k, e.getLong("v"))
                    "f" -> ed.putFloat(k, e.getDouble("v").toFloat())
                    "s" -> ed.putString(k, e.getString("v"))
                }
            }
            ed.apply()
            true
        } catch (e: Exception) {
            false
        }
    }

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

    private fun today(): Long = LocalDate.now().toEpochDay()

    // ---- Habit / streak (single shared habit) ----
    fun getHabitLast(c: Context): Long = prefs(c).getLong("habit_last", 0L)

    fun habitDoneToday(c: Context): Boolean = getHabitLast(c) == today()

    /** Streak that is still "alive" (counts only if done today or yesterday). */
    fun habitStreak(c: Context): Int {
        val last = getHabitLast(c)
        val t = today()
        val stored = prefs(c).getInt("habit_streak", 0)
        return if (last == t || last == t - 1) stored else 0
    }

    fun checkInHabit(c: Context) {
        val t = today()
        val last = getHabitLast(c)
        if (last == t) return
        val newStreak = if (last == t - 1) prefs(c).getInt("habit_streak", 0) + 1 else 1
        prefs(c).edit().putInt("habit_streak", newStreak).putLong("habit_last", t).apply()
    }

    // ---- Water (resets each day) ----
    fun getWaterGoal(c: Context): Int = prefs(c).getInt("water_goal", 8)

    fun getWaterCount(c: Context): Int =
        if (prefs(c).getLong("water_day", 0L) == today()) prefs(c).getInt("water_count", 0) else 0

    fun addWater(c: Context, delta: Int) {
        val next = (getWaterCount(c) + delta).coerceIn(0, 99)
        prefs(c).edit().putInt("water_count", next).putLong("water_day", today()).apply()
    }

    // ---- Countdown (per widget instance) ----
    fun getCountdownTitle(c: Context, id: Int): String =
        prefs(c).getString("cd_title_$id", "Countdown") ?: "Countdown"

    fun setCountdownTitle(c: Context, id: Int, title: String) =
        prefs(c).edit().putString("cd_title_$id", title).apply()

    /** Target date as epoch-day; 0 = not set. */
    fun getCountdownDate(c: Context, id: Int): Long = prefs(c).getLong("cd_date_$id", 0L)

    fun setCountdownDate(c: Context, id: Int, epochDay: Long) =
        prefs(c).edit().putLong("cd_date_$id", epochDay).apply()

    fun deleteCountdown(c: Context, id: Int) {
        prefs(c).edit().remove("cd_title_$id").remove("cd_date_$id").apply()
    }

    fun todayEpochDay(): Long = today()

    // =====================================================================
    // App-managed collections (the in-app pages). Independent of the
    // home-screen widget instances, which keep their own per-instance data.
    // =====================================================================

    private const val KEY_NOTES = "notes_list"
    private const val KEY_COUNTERS = "counters_list"
    private const val KEY_COUNTDOWNS = "countdowns_list"

    // ---- Notes collection ----
    fun getNoteList(c: Context): MutableList<NoteEntry> {
        val raw = prefs(c).getString(KEY_NOTES, null) ?: return mutableListOf()
        return try {
            val arr = JSONArray(raw)
            MutableList(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                NoteEntry(o.getLong("id"), o.optString("n", "Note"), o.optString("t", ""), o.optLong("ts", 0L), o.optInt("col", 0))
            }
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    private fun saveNoteList(c: Context, list: List<NoteEntry>) {
        val arr = JSONArray()
        for (n in list) {
            arr.put(JSONObject().put("id", n.id).put("n", n.name).put("t", n.text).put("ts", n.time).put("col", n.color))
        }
        prefs(c).edit().putString(KEY_NOTES, arr.toString()).apply()
    }

    fun addNoteEntry(c: Context, name: String): NoteEntry {
        val list = getNoteList(c)
        val entry = NoteEntry(nextId(c), name.ifBlank { "Note" }, "", System.currentTimeMillis())
        list.add(entry)
        saveNoteList(c, list)
        return entry
    }

    fun updateNoteEntry(c: Context, id: Long, name: String, text: String) {
        val list = getNoteList(c)
        val idx = list.indexOfFirst { it.id == id }
        if (idx >= 0) {
            list[idx] = list[idx].copy(name = name.ifBlank { "Note" }, text = text, time = System.currentTimeMillis())
            saveNoteList(c, list)
        }
    }

    fun removeNoteEntry(c: Context, id: Long) {
        val list = getNoteList(c)
        if (list.removeAll { it.id == id }) saveNoteList(c, list)
    }

    // ---- Counters collection ----
    fun getCounterList(c: Context): MutableList<CounterEntry> {
        val raw = prefs(c).getString(KEY_COUNTERS, null) ?: return mutableListOf()
        return try {
            val arr = JSONArray(raw)
            MutableList(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                CounterEntry(o.getLong("id"), o.optString("n", "Counter"), o.optInt("v", 0), o.optInt("s", 1), o.optInt("col", 0))
            }
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    private fun saveCounterList(c: Context, list: List<CounterEntry>) {
        val arr = JSONArray()
        for (e in list) {
            arr.put(JSONObject().put("id", e.id).put("n", e.name).put("v", e.value).put("s", e.step).put("col", e.color))
        }
        prefs(c).edit().putString(KEY_COUNTERS, arr.toString()).apply()
    }

    fun addCounterEntry(c: Context, name: String): CounterEntry {
        val list = getCounterList(c)
        val entry = CounterEntry(nextId(c), name.ifBlank { "Counter" }, 0, 1)
        list.add(entry)
        saveCounterList(c, list)
        return entry
    }

    fun renameCounterEntry(c: Context, id: Long, name: String, step: Int) {
        val list = getCounterList(c)
        val idx = list.indexOfFirst { it.id == id }
        if (idx >= 0) {
            list[idx] = list[idx].copy(name = name.ifBlank { "Counter" }, step = step.coerceAtLeast(1))
            saveCounterList(c, list)
        }
    }

    fun adjustCounterEntry(c: Context, id: Long, deltaSteps: Int) {
        val list = getCounterList(c)
        val idx = list.indexOfFirst { it.id == id }
        if (idx >= 0) {
            val e = list[idx]
            list[idx] = e.copy(value = e.value + e.step * deltaSteps)
            saveCounterList(c, list)
        }
    }

    fun removeCounterEntry(c: Context, id: Long) {
        val list = getCounterList(c)
        if (list.removeAll { it.id == id }) saveCounterList(c, list)
    }

    // ---- Countdowns collection ----
    fun getCountdownList(c: Context): MutableList<CountdownEntry> {
        val raw = prefs(c).getString(KEY_COUNTDOWNS, null) ?: return mutableListOf()
        return try {
            val arr = JSONArray(raw)
            MutableList(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                CountdownEntry(o.getLong("id"), o.optString("n", "Countdown"), o.optLong("d", 0L))
            }
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    private fun saveCountdownList(c: Context, list: List<CountdownEntry>) {
        val arr = JSONArray()
        for (e in list) {
            arr.put(JSONObject().put("id", e.id).put("n", e.name).put("d", e.date))
        }
        prefs(c).edit().putString(KEY_COUNTDOWNS, arr.toString()).apply()
    }

    fun addCountdownEntry(c: Context, name: String): CountdownEntry {
        val list = getCountdownList(c)
        val entry = CountdownEntry(nextId(c), name.ifBlank { "Countdown" }, 0L)
        list.add(entry)
        saveCountdownList(c, list)
        return entry
    }

    fun updateCountdownEntry(c: Context, id: Long, name: String, date: Long) {
        val list = getCountdownList(c)
        val idx = list.indexOfFirst { it.id == id }
        if (idx >= 0) {
            list[idx] = list[idx].copy(name = name.ifBlank { "Countdown" }, date = date)
            saveCountdownList(c, list)
        }
    }

    fun removeCountdownEntry(c: Context, id: Long) {
        val list = getCountdownList(c)
        if (list.removeAll { it.id == id }) saveCountdownList(c, list)
    }

    /** Re-inserts a deleted entry at a position, for Undo. */
    fun restoreNoteEntry(c: Context, entry: NoteEntry, index: Int) {
        val list = getNoteList(c)
        list.add(index.coerceIn(0, list.size), entry)
        saveNoteList(c, list)
    }

    fun restoreCounterEntry(c: Context, entry: CounterEntry, index: Int) {
        val list = getCounterList(c)
        list.add(index.coerceIn(0, list.size), entry)
        saveCounterList(c, list)
    }

    fun restoreCountdownEntry(c: Context, entry: CountdownEntry, index: Int) {
        val list = getCountdownList(c)
        list.add(index.coerceIn(0, list.size), entry)
        saveCountdownList(c, list)
    }

    private const val KEY_HABITS = "habits_list"
    private const val KEY_WATERS = "waters_list"

    // ---- Habits collection ----
    fun getHabitEntries(c: Context): MutableList<HabitEntry> {
        val raw = prefs(c).getString(KEY_HABITS, null) ?: return mutableListOf()
        return try {
            val arr = JSONArray(raw)
            MutableList(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                HabitEntry(o.getLong("id"), o.optString("n", "Habit"), o.optInt("s", 0), o.optLong("l", 0L), o.optInt("col", 0))
            }
        } catch (e: Exception) { mutableListOf() }
    }

    private fun saveHabitEntries(c: Context, list: List<HabitEntry>) {
        val arr = JSONArray()
        for (e in list) arr.put(JSONObject().put("id", e.id).put("n", e.name).put("s", e.streak).put("l", e.last).put("col", e.color))
        prefs(c).edit().putString(KEY_HABITS, arr.toString()).apply()
    }

    fun addHabitEntry(c: Context, name: String): HabitEntry {
        val list = getHabitEntries(c)
        val entry = HabitEntry(nextId(c), name.ifBlank { "Habit" }, 0, 0L)
        list.add(entry); saveHabitEntries(c, list); return entry
    }

    fun renameHabitEntry(c: Context, id: Long, name: String) {
        val list = getHabitEntries(c)
        val idx = list.indexOfFirst { it.id == id }
        if (idx >= 0) { list[idx] = list[idx].copy(name = name.ifBlank { "Habit" }); saveHabitEntries(c, list) }
    }

    fun removeHabitEntry(c: Context, id: Long) {
        val list = getHabitEntries(c)
        if (list.removeAll { it.id == id }) saveHabitEntries(c, list)
    }

    fun restoreHabitEntry(c: Context, entry: HabitEntry, index: Int) {
        val list = getHabitEntries(c); list.add(index.coerceIn(0, list.size), entry); saveHabitEntries(c, list)
    }

    fun checkInHabitEntry(c: Context, id: Long) {
        val list = getHabitEntries(c)
        val idx = list.indexOfFirst { it.id == id }
        if (idx < 0) return
        val e = list[idx]; val t = today()
        if (e.last == t) return
        val newStreak = if (e.last == t - 1) e.streak + 1 else 1
        list[idx] = e.copy(streak = newStreak, last = t); saveHabitEntries(c, list)
    }

    fun habitLiveStreak(entry: HabitEntry): Int {
        val t = today()
        return if (entry.last == t || entry.last == t - 1) entry.streak else 0
    }

    fun habitDoneTodayEntry(entry: HabitEntry): Boolean = entry.last == today()

    fun topHabitStreak(c: Context): Int = getHabitEntries(c).maxOfOrNull { habitLiveStreak(it) } ?: 0

    /** First habit for widgets, migrating the old single habit if present. */
    fun resolveHabitPrimary(c: Context): HabitEntry? {
        val list = getHabitEntries(c)
        if (list.isNotEmpty()) return list.first()
        val legacyLast = getHabitLast(c)
        val legacyStreak = prefs(c).getInt("habit_streak", 0)
        if (legacyLast > 0 || legacyStreak > 0) {
            val e = HabitEntry(nextId(c), "Habit", legacyStreak, legacyLast)
            saveHabitEntries(c, mutableListOf(e)); return e
        }
        return null
    }

    // ---- Waters collection ----
    fun getWaterEntries(c: Context): MutableList<WaterEntry> {
        val raw = prefs(c).getString(KEY_WATERS, null) ?: return mutableListOf()
        return try {
            val arr = JSONArray(raw)
            MutableList(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                WaterEntry(o.getLong("id"), o.optString("n", "Water"), o.optInt("c", 0), o.optInt("g", 8), o.optLong("d", 0L))
            }
        } catch (e: Exception) { mutableListOf() }
    }

    private fun saveWaterEntries(c: Context, list: List<WaterEntry>) {
        val arr = JSONArray()
        for (e in list) arr.put(JSONObject().put("id", e.id).put("n", e.name).put("c", e.count).put("g", e.goal).put("d", e.day))
        prefs(c).edit().putString(KEY_WATERS, arr.toString()).apply()
    }

    fun addWaterEntry(c: Context, name: String, goal: Int): WaterEntry {
        val list = getWaterEntries(c)
        val entry = WaterEntry(nextId(c), name.ifBlank { "Water" }, 0, goal.coerceAtLeast(1), today())
        list.add(entry); saveWaterEntries(c, list); return entry
    }

    fun renameWaterEntry(c: Context, id: Long, name: String, goal: Int) {
        val list = getWaterEntries(c)
        val idx = list.indexOfFirst { it.id == id }
        if (idx >= 0) { list[idx] = list[idx].copy(name = name.ifBlank { "Water" }, goal = goal.coerceAtLeast(1)); saveWaterEntries(c, list) }
    }

    fun removeWaterEntry(c: Context, id: Long) {
        val list = getWaterEntries(c)
        if (list.removeAll { it.id == id }) saveWaterEntries(c, list)
    }

    fun restoreWaterEntry(c: Context, entry: WaterEntry, index: Int) {
        val list = getWaterEntries(c); list.add(index.coerceIn(0, list.size), entry); saveWaterEntries(c, list)
    }

    fun adjustWaterEntry(c: Context, id: Long, delta: Int) {
        val list = getWaterEntries(c)
        val idx = list.indexOfFirst { it.id == id }
        if (idx < 0) return
        val e = list[idx]; val t = today()
        val base = if (e.day == t) e.count else 0
        list[idx] = e.copy(count = (base + delta).coerceIn(0, 99), day = t); saveWaterEntries(c, list)
    }

    fun waterEntryCount(entry: WaterEntry): Int = if (entry.day == today()) entry.count else 0

    /** First water tracker for widgets, migrating the old single tracker if present. */
    fun resolveWaterPrimary(c: Context): WaterEntry? {
        val list = getWaterEntries(c)
        if (list.isNotEmpty()) return list.first()
        val legacyCount = prefs(c).getInt("water_count", 0)
        val legacyDay = prefs(c).getLong("water_day", 0L)
        if (legacyDay > 0 || legacyCount > 0) {
            val e = WaterEntry(nextId(c), "Water", legacyCount, getWaterGoal(c), legacyDay)
            saveWaterEntries(c, mutableListOf(e)); return e
        }
        return null
    }

    // ===================== Alarms =====================
    private const val KEY_ALARMS = "alarms_list"

    fun getAlarms(c: Context): MutableList<AlarmEntry> {
        val raw = prefs(c).getString(KEY_ALARMS, null) ?: return mutableListOf()
        return try {
            val arr = JSONArray(raw)
            MutableList(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                AlarmEntry(o.getLong("id"), o.optInt("h", 8), o.optInt("m", 0), o.optString("lbl", ""), o.optBoolean("en", true))
            }
        } catch (e: Exception) { mutableListOf() }
    }

    private fun saveAlarms(c: Context, list: List<AlarmEntry>) {
        val arr = JSONArray()
        for (a in list) arr.put(JSONObject().put("id", a.id).put("h", a.hour).put("m", a.minute).put("lbl", a.label).put("en", a.enabled))
        prefs(c).edit().putString(KEY_ALARMS, arr.toString()).apply()
    }

    fun addAlarm(c: Context, hour: Int, minute: Int, label: String): AlarmEntry {
        val list = getAlarms(c)
        val entry = AlarmEntry(nextId(c), hour, minute, label, true)
        list.add(entry)
        list.sortWith(compareBy({ it.hour }, { it.minute }))
        saveAlarms(c, list)
        scheduleAlarm(c, entry)
        return entry
    }

    fun updateAlarm(c: Context, id: Long, hour: Int, minute: Int, label: String, enabled: Boolean) {
        val list = getAlarms(c)
        val idx = list.indexOfFirst { it.id == id }
        if (idx < 0) return
        val updated = list[idx].copy(hour = hour, minute = minute, label = label, enabled = enabled)
        list[idx] = updated
        list.sortWith(compareBy({ it.hour }, { it.minute }))
        saveAlarms(c, list)
        if (enabled) scheduleAlarm(c, updated) else cancelAlarm(c, id)
    }

    fun setAlarmEnabled(c: Context, id: Long, enabled: Boolean) {
        val list = getAlarms(c)
        val idx = list.indexOfFirst { it.id == id }
        if (idx < 0) return
        list[idx] = list[idx].copy(enabled = enabled)
        saveAlarms(c, list)
        if (enabled) scheduleAlarm(c, list[idx]) else cancelAlarm(c, id)
    }

    fun removeAlarm(c: Context, id: Long) {
        val list = getAlarms(c)
        if (list.removeAll { it.id == id }) { saveAlarms(c, list); cancelAlarm(c, id) }
    }

    fun findAlarm(c: Context, id: Long): AlarmEntry? = getAlarms(c).firstOrNull { it.id == id }

    fun alarmNextTrigger(hour: Int, minute: Int): Long {
        val now = java.util.Calendar.getInstance()
        val t = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, hour)
            set(java.util.Calendar.MINUTE, minute)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        if (t.timeInMillis <= now.timeInMillis) t.add(java.util.Calendar.DAY_OF_YEAR, 1)
        return t.timeInMillis
    }

    private fun alarmFirePI(c: Context, id: Long): PendingIntent {
        val intent = Intent(c, AlarmReceiver::class.java).putExtra("alarm_id", id)
        return PendingIntent.getBroadcast(c, id.toInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    fun scheduleAlarm(c: Context, entry: AlarmEntry) {
        if (!entry.enabled) return
        val am = c.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        val trigger = alarmNextTrigger(entry.hour, entry.minute)
        val show = openAppPendingIntent(c, 5000 + entry.id.toInt())
        try {
            am.setAlarmClock(android.app.AlarmManager.AlarmClockInfo(trigger, show), alarmFirePI(c, entry.id))
        } catch (e: SecurityException) {
            am.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, trigger, alarmFirePI(c, entry.id))
        }
    }

    fun cancelAlarm(c: Context, id: Long) {
        val am = c.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        am.cancel(alarmFirePI(c, id))
    }

    fun rescheduleAllAlarms(c: Context) {
        for (a in getAlarms(c)) if (a.enabled) scheduleAlarm(c, a) else cancelAlarm(c, a.id)
    }

    // ===================== Stopwatch =====================
    fun stopwatchRunning(c: Context): Boolean = prefs(c).getBoolean("sw_running", false)

    /** Chronometer base (elapsedRealtime reference): running counts up from it. */
    fun stopwatchBase(c: Context): Long {
        return if (stopwatchRunning(c)) prefs(c).getLong("sw_base", android.os.SystemClock.elapsedRealtime())
        else android.os.SystemClock.elapsedRealtime() - prefs(c).getLong("sw_accum", 0L)
    }

    fun startStopwatch(c: Context) {
        if (stopwatchRunning(c)) return
        val base = android.os.SystemClock.elapsedRealtime() - prefs(c).getLong("sw_accum", 0L)
        prefs(c).edit().putLong("sw_base", base).putBoolean("sw_running", true).apply()
    }

    fun pauseStopwatch(c: Context) {
        if (!stopwatchRunning(c)) return
        val accum = android.os.SystemClock.elapsedRealtime() - prefs(c).getLong("sw_base", 0L)
        prefs(c).edit().putLong("sw_accum", accum).putBoolean("sw_running", false).apply()
    }

    fun resetStopwatch(c: Context) {
        prefs(c).edit().putBoolean("sw_running", false).putLong("sw_accum", 0L).putLong("sw_base", 0L).apply()
    }

    // ===================== Timer =====================
    fun timerRunning(c: Context): Boolean = prefs(c).getBoolean("t_running", false)
    fun timerDuration(c: Context): Long = prefs(c).getLong("t_dur", 5 * 60_000L)
    fun timerRemaining(c: Context): Long {
        return if (timerRunning(c)) (prefs(c).getLong("t_end", 0L) - android.os.SystemClock.elapsedRealtime()).coerceAtLeast(0L)
        else prefs(c).getLong("t_remain", timerDuration(c))
    }

    /** Chronometer base for count-down (the elapsedRealtime the timer hits zero). */
    fun timerBase(c: Context): Long {
        return if (timerRunning(c)) prefs(c).getLong("t_end", 0L)
        else android.os.SystemClock.elapsedRealtime() + timerRemaining(c)
    }

    fun setTimerDuration(c: Context, ms: Long) {
        val d = ms.coerceAtLeast(1000L)
        prefs(c).edit().putLong("t_dur", d).apply()
        if (!timerRunning(c)) prefs(c).edit().putLong("t_remain", d).apply()
    }

    fun startTimer(c: Context) {
        if (timerRunning(c)) return
        var remain = prefs(c).getLong("t_remain", timerDuration(c))
        if (remain <= 0L) remain = timerDuration(c)
        val end = android.os.SystemClock.elapsedRealtime() + remain
        prefs(c).edit().putLong("t_end", end).putBoolean("t_running", true).apply()
        scheduleTimerAlarm(c, end)
    }

    fun pauseTimer(c: Context) {
        if (!timerRunning(c)) return
        val remain = (prefs(c).getLong("t_end", 0L) - android.os.SystemClock.elapsedRealtime()).coerceAtLeast(0L)
        prefs(c).edit().putLong("t_remain", remain).putBoolean("t_running", false).apply()
        cancelTimerAlarm(c)
    }

    fun resetTimer(c: Context) {
        prefs(c).edit().putBoolean("t_running", false).putLong("t_remain", timerDuration(c)).apply()
        cancelTimerAlarm(c)
    }

    /** Called by TimerReceiver when the countdown reaches zero. */
    fun onTimerFinished(c: Context) {
        prefs(c).edit().putBoolean("t_running", false).putLong("t_remain", 0L).apply()
    }

    private fun timerAlarmIntent(c: Context): PendingIntent {
        val intent = Intent(c, TimerReceiver::class.java)
        return PendingIntent.getBroadcast(
            c, 9100, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun scheduleTimerAlarm(c: Context, triggerElapsed: Long) {
        val am = c.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        try {
            am.setExactAndAllowWhileIdle(
                android.app.AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerElapsed, timerAlarmIntent(c)
            )
        } catch (e: SecurityException) {
            am.setAndAllowWhileIdle(
                android.app.AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerElapsed, timerAlarmIntent(c)
            )
        }
    }

    private fun cancelTimerAlarm(c: Context) {
        val am = c.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        am.cancel(timerAlarmIntent(c))
    }

    // ===================== Pomodoro =====================
    const val POMO_FOCUS_MS = 25 * 60_000L
    const val POMO_BREAK_MS = 5 * 60_000L

    /** 0 = focus, 1 = break. */
    fun pomoPhase(c: Context): Int = prefs(c).getInt("pomo_phase", 0)
    fun pomoOnBreak(c: Context): Boolean = pomoPhase(c) == 1
    fun pomoDuration(c: Context): Long = if (pomoOnBreak(c)) POMO_BREAK_MS else POMO_FOCUS_MS
    fun pomoRunning(c: Context): Boolean = prefs(c).getBoolean("pomo_running", false)
    fun pomoCount(c: Context): Int = prefs(c).getInt("pomo_count", 0)
    fun pomoPhaseLabel(c: Context): String = if (pomoOnBreak(c)) "Break" else "Focus"

    fun pomoRemaining(c: Context): Long {
        return if (pomoRunning(c))
            (prefs(c).getLong("pomo_end", 0L) - android.os.SystemClock.elapsedRealtime()).coerceAtLeast(0L)
        else prefs(c).getLong("pomo_remain", pomoDuration(c))
    }

    /** Chronometer base for count-down (elapsedRealtime the phase hits zero). */
    fun pomoBase(c: Context): Long {
        return if (pomoRunning(c)) prefs(c).getLong("pomo_end", 0L)
        else android.os.SystemClock.elapsedRealtime() + pomoRemaining(c)
    }

    fun startPomo(c: Context) {
        if (pomoRunning(c)) return
        var remain = prefs(c).getLong("pomo_remain", pomoDuration(c))
        if (remain <= 0L) remain = pomoDuration(c)
        val end = android.os.SystemClock.elapsedRealtime() + remain
        prefs(c).edit().putLong("pomo_end", end).putBoolean("pomo_running", true).apply()
    }

    fun pausePomo(c: Context) {
        if (!pomoRunning(c)) return
        val remain = (prefs(c).getLong("pomo_end", 0L) - android.os.SystemClock.elapsedRealtime()).coerceAtLeast(0L)
        prefs(c).edit().putLong("pomo_remain", remain).putBoolean("pomo_running", false).apply()
    }

    fun togglePomo(c: Context) { if (pomoRunning(c)) pausePomo(c) else startPomo(c) }

    /** Skip to the next phase: focus -> break (counts a session), break -> focus. */
    fun nextPomoPhase(c: Context) {
        val ed = prefs(c).edit()
        if (!pomoOnBreak(c)) {
            ed.putInt("pomo_count", pomoCount(c) + 1).putInt("pomo_phase", 1)
        } else {
            ed.putInt("pomo_phase", 0)
        }
        ed.putBoolean("pomo_running", false).remove("pomo_remain").apply()
    }

    /** Reset the current phase's countdown (keeps the phase and session count). */
    fun resetPomo(c: Context) {
        prefs(c).edit().putBoolean("pomo_running", false).putLong("pomo_remain", pomoDuration(c)).apply()
    }

    // ===================== Quote of the day =====================
    fun quoteIndex(c: Context): Int {
        val stored = prefs(c).getInt("quote_idx", -1)
        return if (stored in Quotes.list.indices) stored
        else LocalDate.now().dayOfYear % Quotes.list.size
    }

    fun shuffleQuote(c: Context) {
        prefs(c).edit().putInt("quote_idx", (0 until Quotes.list.size).random()).apply()
    }

    // ===================== Random / decision =====================
    fun randomMode(c: Context): Int = prefs(c).getInt("rnd_mode", 1) // 0 coin, 1 d6, 2 d20, 3 percent

    fun cycleRandomMode(c: Context) {
        prefs(c).edit().putInt("rnd_mode", (randomMode(c) + 1) % 4).putString("rnd_res", "?").apply()
    }

    fun randomResult(c: Context): String = prefs(c).getString("rnd_res", "?") ?: "?"

    fun randomModeLabel(c: Context): String = when (randomMode(c)) {
        0 -> "Coin"; 2 -> "d20"; 3 -> "0–100"; else -> "d6"
    }

    fun rollRandom(c: Context) {
        val r = when (randomMode(c)) {
            0 -> if ((0..1).random() == 0) "Heads" else "Tails"
            2 -> (1..20).random().toString()
            3 -> (0..100).random().toString()
            else -> (1..6).random().toString()
        }
        prefs(c).edit().putString("rnd_res", r).apply()
    }

    // ===================== World clock =====================
    /** City label to IANA time-zone id, cycled by tapping the widget. */
    val worldZones = listOf(
        "Los Angeles" to "America/Los_Angeles",
        "New York" to "America/New_York",
        "London" to "Europe/London",
        "Paris" to "Europe/Paris",
        "Dubai" to "Asia/Dubai",
        "Mumbai" to "Asia/Kolkata",
        "Singapore" to "Asia/Singapore",
        "Tokyo" to "Asia/Tokyo",
        "Sydney" to "Australia/Sydney"
    )

    fun worldClockIndex(c: Context): Int =
        prefs(c).getInt("wc_idx", 0).coerceIn(0, worldZones.size - 1)

    fun cycleWorldClock(c: Context) {
        prefs(c).edit().putInt("wc_idx", (worldClockIndex(c) + 1) % worldZones.size).apply()
    }

    fun worldZoneId(c: Context): String = worldZones[worldClockIndex(c)].second
    fun worldCity(c: Context): String = worldZones[worldClockIndex(c)].first

    // ===================== Moon phase =====================
    private const val SYNODIC_MONTH = 29.53058867
    // Reference new moon: 2000-01-06 18:14 UTC.
    private const val REF_NEW_MOON_MS = 947182440000L

    /** Age of the moon in the current cycle, 0..SYNODIC_MONTH days. */
    private fun moonAgeDays(): Double {
        val days = (System.currentTimeMillis() - REF_NEW_MOON_MS) / 86_400_000.0
        val age = days % SYNODIC_MONTH
        return if (age < 0) age + SYNODIC_MONTH else age
    }

    /** 0 = new, 1 = waxing crescent … 4 = full … 7 = waning crescent. */
    fun moonPhaseIndex(): Int {
        val frac = moonAgeDays() / SYNODIC_MONTH
        return (Math.round(frac * 8).toInt()) % 8
    }

    fun moonEmoji(): String = when (moonPhaseIndex()) {
        0 -> "🌑"; 1 -> "🌒"; 2 -> "🌓"; 3 -> "🌔"; 4 -> "🌕"; 5 -> "🌖"; 6 -> "🌗"; else -> "🌘"
    }

    fun moonName(): String = when (moonPhaseIndex()) {
        0 -> "New moon"; 1 -> "Waxing crescent"; 2 -> "First quarter"; 3 -> "Waxing gibbous"
        4 -> "Full moon"; 5 -> "Waning gibbous"; 6 -> "Last quarter"; else -> "Waning crescent"
    }

    /** Illuminated fraction of the disc, 0..100. */
    fun moonIllumination(): Int {
        val frac = moonAgeDays() / SYNODIC_MONTH
        return Math.round((1 - Math.cos(2 * Math.PI * frac)) / 2 * 100).toInt()
    }

    // ===================== Progress (year / month / week / day) =====================
    /** 0 = year, 1 = month, 2 = week, 3 = day. */
    fun progressScope(c: Context): Int = prefs(c).getInt("prog_scope", 0).coerceIn(0, 3)

    fun cycleProgressScope(c: Context) {
        prefs(c).edit().putInt("prog_scope", (progressScope(c) + 1) % 4).apply()
    }

    fun progressScopeLabel(c: Context): String = when (progressScope(c)) {
        1 -> "Month"; 2 -> "Week"; 3 -> "Day"; else -> "Year"
    }

    /** Fraction 0..100 of the current scope that has elapsed. */
    fun progressPercent(c: Context): Int {
        val zone = java.time.ZoneId.systemDefault()
        val now = java.time.ZonedDateTime.now(zone)
        val start: java.time.ZonedDateTime
        val end: java.time.ZonedDateTime
        when (progressScope(c)) {
            1 -> { // month
                start = now.toLocalDate().withDayOfMonth(1).atStartOfDay(zone)
                end = start.plusMonths(1)
            }
            2 -> { // week (Monday start)
                val monday = now.toLocalDate().with(java.time.DayOfWeek.MONDAY)
                start = monday.atStartOfDay(zone)
                end = start.plusWeeks(1)
            }
            3 -> { // day
                start = now.toLocalDate().atStartOfDay(zone)
                end = start.plusDays(1)
            }
            else -> { // year
                start = java.time.LocalDate.of(now.year, 1, 1).atStartOfDay(zone)
                end = start.plusYears(1)
            }
        }
        val total = java.time.Duration.between(start, end).seconds.coerceAtLeast(1)
        val done = java.time.Duration.between(start, now).seconds.coerceIn(0, total)
        return (done * 100 / total).toInt()
    }

    // ---- Per-item color accents ----
    val itemColorRes = intArrayOf(
        R.color.item_c1, R.color.item_c2, R.color.item_c3,
        R.color.item_c4, R.color.item_c5, R.color.item_c6
    )

    /** Resolves a 1-based color index to a color int; 0 (or out of range) = none. */
    fun itemColor(c: Context, index: Int): Int =
        if (index in 1..itemColorRes.size) c.getColor(itemColorRes[index - 1]) else 0

    fun setNoteColor(c: Context, id: Long, color: Int) {
        val list = getNoteList(c); val i = list.indexOfFirst { it.id == id }
        if (i >= 0) { list[i] = list[i].copy(color = color); saveNoteList(c, list) }
    }

    fun setCounterColor(c: Context, id: Long, color: Int) {
        val list = getCounterList(c); val i = list.indexOfFirst { it.id == id }
        if (i >= 0) { list[i] = list[i].copy(color = color); saveCounterList(c, list) }
    }

    fun setHabitColor(c: Context, id: Long, color: Int) {
        val list = getHabitEntries(c); val i = list.indexOfFirst { it.id == id }
        if (i >= 0) { list[i] = list[i].copy(color = color); saveHabitEntries(c, list) }
    }

    fun reorderNotes(c: Context, orderedIds: List<Long>) {
        val list = getNoteList(c); val byId = list.associateBy { it.id }
        val reordered = orderedIds.mapNotNull { byId[it] }
        if (reordered.size == list.size) saveNoteList(c, reordered)
    }

    fun reorderCounters(c: Context, orderedIds: List<Long>) {
        val list = getCounterList(c); val byId = list.associateBy { it.id }
        val reordered = orderedIds.mapNotNull { byId[it] }
        if (reordered.size == list.size) saveCounterList(c, reordered)
    }

    fun findCounter(c: Context, id: Long): CounterEntry? = getCounterList(c).firstOrNull { it.id == id }
    fun findNote(c: Context, id: Long): NoteEntry? = getNoteList(c).firstOrNull { it.id == id }

    fun setCounterEntryValue(c: Context, id: Long, value: Int) {
        val list = getCounterList(c)
        val idx = list.indexOfFirst { it.id == id }
        if (idx >= 0) {
            list[idx] = list[idx].copy(value = value)
            saveCounterList(c, list)
        }
    }

    // ---- Widget → collection item mapping ----
    fun getCounterTarget(c: Context, widgetId: Int): Long = prefs(c).getLong("wtgt_cnt_$widgetId", 0L)
    fun setCounterTarget(c: Context, widgetId: Int, entryId: Long) =
        prefs(c).edit().putLong("wtgt_cnt_$widgetId", entryId).apply()
    fun clearCounterTarget(c: Context, widgetId: Int) =
        prefs(c).edit().remove("wtgt_cnt_$widgetId").apply()

    fun getNoteTarget(c: Context, widgetId: Int): Long = prefs(c).getLong("wtgt_note_$widgetId", 0L)
    fun setNoteTarget(c: Context, widgetId: Int, entryId: Long) =
        prefs(c).edit().putLong("wtgt_note_$widgetId", entryId).apply()
    fun clearNoteTarget(c: Context, widgetId: Int) =
        prefs(c).edit().remove("wtgt_note_$widgetId").apply()

    /** Resolves the counter a widget shows, migrating legacy per-widget data on first use. */
    fun resolveCounterForWidget(c: Context, widgetId: Int): CounterEntry? {
        val target = getCounterTarget(c, widgetId)
        if (target != 0L) return findCounter(c, target) // may be null if deleted in-app
        val entry = addCounterEntry(c, getCounterLabel(c, widgetId))
        renameCounterEntry(c, entry.id, getCounterLabel(c, widgetId), getCounterStep(c, widgetId))
        setCounterEntryValue(c, entry.id, getCounterValue(c, widgetId))
        setCounterTarget(c, widgetId, entry.id)
        return findCounter(c, entry.id)
    }

    /** Resolves the note a widget shows, migrating legacy per-widget data on first use. */
    fun resolveNoteForWidget(c: Context, widgetId: Int): NoteEntry? {
        val target = getNoteTarget(c, widgetId)
        if (target != 0L) return findNote(c, target)
        val entry = addNoteEntry(c, getNoteLabel(c, widgetId))
        updateNoteEntry(c, entry.id, getNoteLabel(c, widgetId), getNoteText(c, widgetId))
        setNoteTarget(c, widgetId, entry.id)
        return findNote(c, entry.id)
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
                TodoItem(
                    id = id,
                    text = o.getString("t"),
                    done = o.optBoolean("d", false),
                    priority = o.optInt("p", 0),
                    due = o.optLong("due", 0L)
                )
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
                    .put("p", item.priority)
                    .put("due", item.due)
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

    /** Re-render every widget type (used when the accent theme changes). */
    fun refreshEverything(context: Context) {
        val mgr = AppWidgetManager.getInstance(context)
        val providers = listOf(
            NotesWidget::class.java, CounterWidget::class.java, ClockWidget::class.java,
            TodoWidget::class.java, HabitWidget::class.java, WaterWidget::class.java,
            CountdownWidget::class.java, StopwatchWidget::class.java, TimerWidget::class.java,
            CalendarWidget::class.java, QuoteWidget::class.java, RandomWidget::class.java,
            BatteryWidget::class.java, WorldClockWidget::class.java, PomodoroWidget::class.java,
            ProgressWidget::class.java, MoonWidget::class.java
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
    }
}
