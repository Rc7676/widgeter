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
data class NoteEntry(val id: Long, val name: String, val text: String, val time: Long)
data class CounterEntry(val id: Long, val name: String, val value: Int, val step: Int)
data class CountdownEntry(val id: Long, val name: String, val date: Long) // date = epoch day, 0 = unset
data class HabitEntry(val id: Long, val name: String, val streak: Int, val last: Long) // last = epoch day
data class WaterEntry(val id: Long, val name: String, val count: Int, val goal: Int, val day: Long)

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
                NoteEntry(o.getLong("id"), o.optString("n", "Note"), o.optString("t", ""), o.optLong("ts", 0L))
            }
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    private fun saveNoteList(c: Context, list: List<NoteEntry>) {
        val arr = JSONArray()
        for (n in list) {
            arr.put(JSONObject().put("id", n.id).put("n", n.name).put("t", n.text).put("ts", n.time))
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
                CounterEntry(o.getLong("id"), o.optString("n", "Counter"), o.optInt("v", 0), o.optInt("s", 1))
            }
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    private fun saveCounterList(c: Context, list: List<CounterEntry>) {
        val arr = JSONArray()
        for (e in list) {
            arr.put(JSONObject().put("id", e.id).put("n", e.name).put("v", e.value).put("s", e.step))
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
                HabitEntry(o.getLong("id"), o.optString("n", "Habit"), o.optInt("s", 0), o.optLong("l", 0L))
            }
        } catch (e: Exception) { mutableListOf() }
    }

    private fun saveHabitEntries(c: Context, list: List<HabitEntry>) {
        val arr = JSONArray()
        for (e in list) arr.put(JSONObject().put("id", e.id).put("n", e.name).put("s", e.streak).put("l", e.last))
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
}
