package com.widgeter.app

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Chronometer
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/** Multi-page home: Notes, Counters, Tasks and More, via bottom navigation. */
class MainActivity : AppCompatActivity() {

    private lateinit var notesAdapter: NotesAdapter
    private lateinit var countersAdapter: CountersAdapter
    private lateinit var todoAdapter: TodoAdapter
    private lateinit var itemTouchHelper: ItemTouchHelper
    private lateinit var toolbar: MaterialToolbar
    private lateinit var taskInput: TextInputEditText
    private lateinit var nav: BottomNavigationView
    private var appliedTheme = 0

    companion object {
        const val ACTION_NEW_TASK = "com.widgeter.app.action.NEW_TASK"
        const val ACTION_NEW_NOTE = "com.widgeter.app.action.NEW_NOTE"
        const val EXTRA_OPEN_NOTE = "com.widgeter.app.OPEN_NOTE"
    }

    private val dateFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
    private val heroDateFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, MMM d")

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(Themes.styleFor(this))
        appliedTheme = Store.getColorTheme(this)
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        applyInsets()

        toolbar = findViewById(R.id.toolbar)
        toolbar.inflateMenu(R.menu.main_menu)
        toolbar.setOnMenuItemClickListener {
            if (it.itemId == R.id.action_settings) { openSettings(); true } else false
        }

        setupNotes()
        setupCounters()
        setupTasks()
        setupClock()
        setupMore()
        requestNotifPermission()

        nav = findViewById(R.id.bottom_nav)
        nav.setOnItemSelectedListener { showPage(it.itemId); true }
        nav.selectedItemId = R.id.nav_notes

        maybeOnboard()
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        if (appliedTheme != Store.getColorTheme(this)) { recreate(); return }
        renderNotes(); renderCounters(); renderTasks(); renderClock(); renderMore()
        // Refresh home-screen widgets so info widgets (battery, moon, progress,
        // day info, goal, …) reflect the latest state when returning to the app.
        Widgets.refreshEverything(this)
    }

    private fun handleIntent(intent: Intent?) {
        val openNoteId = intent?.getLongExtra(EXTRA_OPEN_NOTE, 0L) ?: 0L
        when {
            intent?.action == ACTION_NEW_TASK -> {
                nav.selectedItemId = R.id.nav_tasks
                taskInput.post {
                    taskInput.requestFocus()
                    showKeyboard(taskInput)
                }
            }
            intent?.action == ACTION_NEW_NOTE -> {
                nav.selectedItemId = R.id.nav_notes
                val entry = Store.addNoteEntry(this, getString(R.string.notes_title))
                afterChange(); renderNotes(); editNoteDialog(entry)
            }
            openNoteId != 0L -> {
                nav.selectedItemId = R.id.nav_notes
                Store.findNote(this, openNoteId)?.let { editNoteDialog(it) }
            }
        }
    }

    private fun maybeOnboard() {
        if (Store.isOnboarded(this)) return
        Store.setOnboarded(this)
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.onboarding_title)
            .setMessage(R.string.onboarding_body)
            .setPositiveButton(R.string.onboarding_got_it, null)
            .show()
    }

    private fun showKeyboard(view: View) {
        (getSystemService(INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager)
            ?.showSoftInput(view, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
    }

    private fun applyInsets() {
        val bar = findViewById<View>(R.id.toolbar)
        val nav = findViewById<View>(R.id.bottom_nav)
        val content = findViewById<View>(R.id.content)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root)) { _, insets ->
            val b = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            bar.updatePadding(top = b.top)
            nav.updatePadding(bottom = b.bottom)
            content.updatePadding(left = b.left, right = b.right)
            insets
        }
    }

    private fun openSettings() = startActivity(Intent(this, SettingsActivity::class.java))

    private fun showPage(itemId: Int) {
        findViewById<View>(R.id.page_notes_root).visibility =
            if (itemId == R.id.nav_notes) View.VISIBLE else View.GONE
        findViewById<View>(R.id.page_counters_root).visibility =
            if (itemId == R.id.nav_counters) View.VISIBLE else View.GONE
        findViewById<View>(R.id.page_tasks_root).visibility =
            if (itemId == R.id.nav_tasks) View.VISIBLE else View.GONE
        findViewById<View>(R.id.page_clock_root).visibility =
            if (itemId == R.id.nav_clock) View.VISIBLE else View.GONE
        findViewById<View>(R.id.page_more_root).visibility =
            if (itemId == R.id.nav_more) View.VISIBLE else View.GONE

        toolbar.title = when (itemId) {
            R.id.nav_counters -> getString(R.string.section_counter)
            R.id.nav_tasks -> getString(R.string.tasks_title)
            R.id.nav_clock -> getString(R.string.clock_title)
            R.id.nav_more -> getString(R.string.more_title)
            else -> getString(R.string.app_name) // Notes/home
        }
        when (itemId) {
            R.id.nav_counters -> renderCounters()
            R.id.nav_tasks -> renderTasks()
            R.id.nav_clock -> renderClock()
            R.id.nav_more -> renderMore()
            else -> renderNotes()
        }
    }

    // ---------------- Notes ----------------

    private fun setupNotes() {
        notesAdapter = NotesAdapter(
            onOpen = { editNoteDialog(it) },
            onDelete = { deleteNoteWithUndo(it) }
        )
        val rv = findViewById<RecyclerView>(R.id.notes_recycler)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = notesAdapter
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
            override fun onMove(r: RecyclerView, v: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder): Boolean {
                notesAdapter.onItemMove(v.bindingAdapterPosition, t.bindingAdapterPosition); return true
            }
            override fun onSwiped(vh: RecyclerView.ViewHolder, dir: Int) {}
            override fun clearView(r: RecyclerView, vh: RecyclerView.ViewHolder) {
                super.clearView(r, vh); Store.reorderNotes(this@MainActivity, notesAdapter.currentIds()); afterChange()
            }
        }).attachToRecyclerView(rv)
        findViewById<View>(R.id.notes_fab).setOnClickListener {
            val entry = Store.addNoteEntry(this, getString(R.string.notes_title))
            afterChange(); renderNotes()
            editNoteDialog(entry)
        }
    }

    private fun renderNotes() {
        val list = Store.getNoteList(this)
        notesAdapter.submit(list)
        findViewById<View>(R.id.notes_recycler).visibility =
            if (list.isEmpty()) View.GONE else View.VISIBLE
        findViewById<View>(R.id.notes_empty).visibility =
            if (list.isEmpty()) View.VISIBLE else View.GONE

        findViewById<TextView>(R.id.hero_greeting).text = greeting()
        findViewById<TextView>(R.id.hero_date).text = LocalDate.now().format(heroDateFmt)
        findViewById<TextView>(R.id.stat_notes).text = list.size.toString()
        findViewById<TextView>(R.id.stat_counters).text = Store.getCounterList(this).size.toString()
        findViewById<TextView>(R.id.stat_tasks).text = Store.getTodos(this).count { !it.done }.toString()
        findViewById<TextView>(R.id.stat_streak).text = Store.topHabitStreak(this).toString()
    }

    private fun greeting(): String = when (LocalTime.now().hour) {
        in 5..11 -> "Good morning 👋"
        in 12..16 -> "Good afternoon 👋"
        in 17..20 -> "Good evening 👋"
        else -> "Good night 🌙"
    }

    private fun editNoteDialog(entry: NoteEntry) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_note, null)
        val name = view.findViewById<TextInputEditText>(R.id.dlg_name)
        val text = view.findViewById<TextInputEditText>(R.id.dlg_text)
        name.setText(entry.name)
        text.setText(entry.text)
        val colorPick = buildColorPicker(view.findViewById(R.id.dlg_colors), entry.color)
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.edit_note_title)
            .setView(view)
            .setNegativeButton(R.string.cancel, null)
            .setNeutralButton(R.string.delete) { _, _ -> deleteNoteWithUndo(entry) }
            .setPositiveButton(R.string.save_generic) { _, _ ->
                Store.updateNoteEntry(this, entry.id, name.text?.toString().orEmpty(), text.text?.toString().orEmpty())
                Store.setNoteColor(this, entry.id, colorPick())
                afterChange(); renderNotes()
            }
            .show()
    }

    private fun deleteNoteWithUndo(entry: NoteEntry) {
        val index = Store.getNoteList(this).indexOfFirst { it.id == entry.id }.coerceAtLeast(0)
        Store.removeNoteEntry(this, entry.id); afterChange(); renderNotes()
        Snackbar.make(findViewById(R.id.root), R.string.item_deleted, Snackbar.LENGTH_LONG)
            .setAction(R.string.undo) { Store.restoreNoteEntry(this, entry, index); afterChange(); renderNotes() }
            .show()
    }

    // ---------------- Counters ----------------

    private fun setupCounters() {
        countersAdapter = CountersAdapter(
            onAdjust = { entry, dir -> Store.adjustCounterEntry(this, entry.id, dir); afterChange(); renderCounters() },
            onRename = { renameCounterDialog(it) },
            onDelete = { deleteCounterWithUndo(it) }
        )
        val rv = findViewById<RecyclerView>(R.id.counters_recycler)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = countersAdapter
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
            override fun onMove(r: RecyclerView, v: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder): Boolean {
                countersAdapter.onItemMove(v.bindingAdapterPosition, t.bindingAdapterPosition); return true
            }
            override fun onSwiped(vh: RecyclerView.ViewHolder, dir: Int) {}
            override fun clearView(r: RecyclerView, vh: RecyclerView.ViewHolder) {
                super.clearView(r, vh); Store.reorderCounters(this@MainActivity, countersAdapter.currentIds()); afterChange()
            }
        }).attachToRecyclerView(rv)
        findViewById<View>(R.id.counters_fab).setOnClickListener { renameCounterDialog(null) }
    }

    private fun renderCounters() {
        val list = Store.getCounterList(this)
        countersAdapter.submit(list)
        findViewById<View>(R.id.counters_empty).visibility =
            if (list.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun deleteCounterWithUndo(entry: CounterEntry) {
        val index = Store.getCounterList(this).indexOfFirst { it.id == entry.id }.coerceAtLeast(0)
        Store.removeCounterEntry(this, entry.id); afterChange(); renderCounters()
        Snackbar.make(findViewById(R.id.root), R.string.item_deleted, Snackbar.LENGTH_LONG)
            .setAction(R.string.undo) { Store.restoreCounterEntry(this, entry, index); afterChange(); renderCounters() }
            .show()
    }

    private fun renameCounterDialog(entry: CounterEntry?) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_counter, null)
        val name = view.findViewById<TextInputEditText>(R.id.dlg_name)
        val step = view.findViewById<TextInputEditText>(R.id.dlg_step)
        name.setText(entry?.name ?: "")
        step.setText((entry?.step ?: 1).toString())
        val colorPick = buildColorPicker(view.findViewById(R.id.dlg_colors), entry?.color ?: 0)
        MaterialAlertDialogBuilder(this)
            .setTitle(if (entry == null) R.string.new_counter else R.string.rename)
            .setView(view)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.save_generic) { _, _ ->
                val nm = name.text?.toString().orEmpty()
                val st = step.text?.toString()?.trim()?.toIntOrNull()?.coerceAtLeast(1) ?: 1
                val targetId = if (entry == null) Store.addCounterEntry(this, nm).id else entry.id
                Store.renameCounterEntry(this, targetId, nm, st)
                Store.setCounterColor(this, targetId, colorPick())
                afterChange(); renderCounters()
            }
            .show()
    }

    // ---------------- Tasks ----------------

    private fun setupTasks() {
        todoAdapter = TodoAdapter(
            onToggle = { Repo.toggleTodo(it.id); renderTasks() },
            onDelete = { removeTaskWithUndo(it) },
            onEdit = { showTaskEditDialog(it) },
            onStartDrag = { vh -> itemTouchHelper.startDrag(vh) }
        )
        val rv = findViewById<RecyclerView>(R.id.todo_recycler)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = todoAdapter

        val callback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun isLongPressDragEnabled() = false
            override fun onMove(r: RecyclerView, v: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder): Boolean {
                todoAdapter.onItemMove(v.bindingAdapterPosition, t.bindingAdapterPosition)
                return true
            }
            override fun onSwiped(vh: RecyclerView.ViewHolder, direction: Int) {
                val pos = vh.bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) removeTaskWithUndo(todoAdapter.itemAt(pos))
            }
            override fun clearView(r: RecyclerView, vh: RecyclerView.ViewHolder) {
                super.clearView(r, vh)
                Repo.reorder(todoAdapter.currentIds()); renderTasks()
            }
        }
        itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(rv)

        taskInput = findViewById(R.id.task_input)
        findViewById<MaterialButton>(R.id.btn_add_task).setOnClickListener {
            val t = taskInput.text?.toString().orEmpty()
            if (t.isNotBlank()) { Repo.addTodo(t); taskInput.setText(""); renderTasks() }
        }
        findViewById<MaterialButton>(R.id.btn_sort_todos).setOnClickListener { Repo.sortTodos(); renderTasks() }
        findViewById<MaterialButton>(R.id.btn_clear_completed).setOnClickListener { Repo.clearCompleted(); renderTasks() }
    }

    private fun renderTasks() {
        val list = Store.getTodos(this)
        todoAdapter.submit(list.toList())
        val has = list.isNotEmpty()
        findViewById<View>(R.id.todo_recycler).visibility = if (has) View.VISIBLE else View.GONE
        findViewById<View>(R.id.todo_empty).visibility = if (has) View.GONE else View.VISIBLE
        val done = list.count { it.done }
        val progress = findViewById<TextView>(R.id.todo_progress)
        progress.visibility = if (has) View.VISIBLE else View.GONE
        progress.text = getString(R.string.todo_progress_fmt, done, list.size)
        findViewById<View>(R.id.btn_clear_completed).visibility = if (done > 0) View.VISIBLE else View.GONE
    }

    private fun removeTaskWithUndo(item: TodoItem) {
        val index = todoAdapter.currentIds().indexOf(item.id).coerceAtLeast(0)
        Repo.deleteTodo(item.id); renderTasks()
        Snackbar.make(findViewById(R.id.root), R.string.task_deleted, Snackbar.LENGTH_LONG)
            .setAction(R.string.undo) { Repo.restoreTodo(item, index); renderTasks() }
            .show()
    }

    private fun showTaskEditDialog(item: TodoItem) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_todo, null)
        val name = view.findViewById<TextInputEditText>(R.id.edit_name)
        val group = view.findViewById<MaterialButtonToggleGroup>(R.id.priority_group)
        val dueLabel = view.findViewById<TextView>(R.id.due_label)
        val btnDue = view.findViewById<MaterialButton>(R.id.btn_due)
        val btnClear = view.findViewById<MaterialButton>(R.id.btn_due_clear)

        name.setText(item.text)
        group.check(
            when (item.priority) { 1 -> R.id.prio_low; 2 -> R.id.prio_med; 3 -> R.id.prio_high; else -> R.id.prio_none }
        )
        var due = item.due
        fun refresh() {
            if (due > 0) {
                dueLabel.text = android.text.format.DateUtils.formatDateTime(
                    this, due,
                    android.text.format.DateUtils.FORMAT_SHOW_DATE or android.text.format.DateUtils.FORMAT_ABBREV_MONTH
                )
                btnClear.visibility = View.VISIBLE
            } else { dueLabel.text = ""; btnClear.visibility = View.GONE }
        }
        refresh()
        btnDue.setOnClickListener {
            val picker = MaterialDatePicker.Builder.datePicker()
                .setSelection(if (due > 0) due else MaterialDatePicker.todayInUtcMilliseconds())
                .build()
            picker.addOnPositiveButtonClickListener { due = it; refresh() }
            picker.show(supportFragmentManager, "due")
        }
        btnClear.setOnClickListener { due = 0L; refresh() }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.edit_title)
            .setView(view)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.save_generic) { _, _ ->
                val priority = when (group.checkedButtonId) {
                    R.id.prio_low -> 1; R.id.prio_med -> 2; R.id.prio_high -> 3; else -> 0
                }
                Repo.updateTodo(item.id, name.text?.toString().orEmpty(), priority, due); renderTasks()
            }
            .show()
    }

    // ---------------- Clock (stopwatch + timer) ----------------

    private fun setupClock() {
        findViewById<MaterialButton>(R.id.sw_toggle_btn).setOnClickListener {
            if (Store.stopwatchRunning(this)) Store.pauseStopwatch(this) else Store.startStopwatch(this)
            refreshWidget(StopwatchWidget::class.java); renderClock()
        }
        findViewById<MaterialButton>(R.id.sw_reset_btn).setOnClickListener {
            Store.resetStopwatch(this); refreshWidget(StopwatchWidget::class.java); renderClock()
        }
        findViewById<MaterialButton>(R.id.t_toggle_btn).setOnClickListener {
            if (Store.timerRunning(this)) Store.pauseTimer(this) else Store.startTimer(this)
            refreshWidget(TimerWidget::class.java); renderClock()
        }
        findViewById<MaterialButton>(R.id.t_reset_btn).setOnClickListener {
            Store.resetTimer(this); refreshWidget(TimerWidget::class.java); renderClock()
        }
        val presets = mapOf(R.id.preset_1 to 1L, R.id.preset_5 to 5L, R.id.preset_10 to 10L, R.id.preset_25 to 25L)
        for ((viewId, minutes) in presets) {
            findViewById<MaterialButton>(viewId).setOnClickListener {
                Store.setTimerDuration(this, minutes * 60_000L)
                Store.resetTimer(this)
                refreshWidget(TimerWidget::class.java); renderClock()
            }
        }
        findViewById<MaterialButton>(R.id.alarm_add_btn).setOnClickListener { showAlarmTimePicker(null) }
    }

    private fun renderAlarms() {
        val container = findViewById<LinearLayout>(R.id.alarms_container)
        container.removeAllViews()
        val list = Store.getAlarms(this)
        findViewById<View>(R.id.alarms_empty).visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        for (a in list) {
            val row = LayoutInflater.from(this).inflate(R.layout.item_alarm_row, container, false)
            row.findViewById<TextView>(R.id.alarm_time_row).text = String.format("%02d:%02d", a.hour, a.minute)
            row.findViewById<TextView>(R.id.alarm_label_row).text =
                a.label.ifBlank { getString(R.string.alarm_daily) }
            val sw = row.findViewById<MaterialSwitch>(R.id.alarm_switch)
            sw.isChecked = a.enabled
            sw.setOnClickListener { Store.setAlarmEnabled(this, a.id, sw.isChecked); renderAlarms() }
            row.findViewById<View>(R.id.alarm_tap).setOnClickListener { showAlarmTimePicker(a) }
            row.findViewById<ImageButton>(R.id.alarm_delete_row).setOnClickListener {
                Store.removeAlarm(this, a.id); renderAlarms()
            }
            container.addView(row)
        }
    }

    private fun showAlarmTimePicker(existing: AlarmEntry?) {
        val is24 = android.text.format.DateFormat.is24HourFormat(this)
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(if (is24) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H)
            .setHour(existing?.hour ?: 8)
            .setMinute(existing?.minute ?: 0)
            .setTitleText(R.string.alarm_set)
            .build()
        picker.addOnPositiveButtonClickListener {
            val h = picker.hour
            val m = picker.minute
            if (existing == null) Store.addAlarm(this, h, m, "")
            else Store.updateAlarm(this, existing.id, h, m, existing.label, existing.enabled)
            renderAlarms()
        }
        picker.show(supportFragmentManager, "alarm_time")
    }

    private fun renderClock() {
        val swRunning = Store.stopwatchRunning(this)
        findViewById<Chronometer>(R.id.sw_chrono_app).apply {
            isCountDown = false
            base = Store.stopwatchBase(this@MainActivity)
            if (swRunning) start() else stop()
        }
        findViewById<MaterialButton>(R.id.sw_toggle_btn).apply {
            setText(if (swRunning) R.string.pause else R.string.start)
            setIconResource(if (swRunning) R.drawable.ic_pause else R.drawable.ic_play)
        }

        val tRunning = Store.timerRunning(this)
        findViewById<Chronometer>(R.id.t_chrono_app).apply {
            isCountDown = true
            base = Store.timerBase(this@MainActivity)
            if (tRunning) start() else stop()
        }
        findViewById<MaterialButton>(R.id.t_toggle_btn).apply {
            setText(if (tRunning) R.string.pause else R.string.start)
            setIconResource(if (tRunning) R.drawable.ic_pause else R.drawable.ic_play)
        }

        renderAlarms()
    }

    private fun refreshWidget(cls: Class<*>) {
        val mgr = AppWidgetManager.getInstance(this)
        val ids = mgr.getAppWidgetIds(ComponentName(this, cls))
        if (ids.isNotEmpty()) sendBroadcast(Intent(this, cls).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        })
    }

    private fun requestNotifPermission() {
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 42)
        }
    }

    // ---------------- More ----------------

    private fun setupMore() {
        findViewById<MaterialButton>(R.id.habit_add_btn).setOnClickListener { habitDialog(null) }
        findViewById<MaterialButton>(R.id.water_add_btn).setOnClickListener { waterDialog(null) }
        findViewById<MaterialButton>(R.id.countdown_add_btn).setOnClickListener { countdownDialog(null) }
        findViewById<MaterialButton>(R.id.more_widgets_btn).setOnClickListener {
            startActivity(Intent(this, WidgetGalleryActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.more_settings_btn).setOnClickListener { openSettings() }
    }

    private fun renderMore() {
        renderHabits()
        renderWaters()
        renderCountdowns()
    }

    // ---- Habits ----
    private fun renderHabits() {
        val container = findViewById<LinearLayout>(R.id.habits_container)
        container.removeAllViews()
        val list = Store.getHabitEntries(this)
        findViewById<View>(R.id.habits_empty).visibility =
            if (list.isEmpty()) View.VISIBLE else View.GONE
        for (h in list) {
            val row = LayoutInflater.from(this).inflate(R.layout.item_habit_row, container, false)
            row.findViewById<TextView>(R.id.habit_row_name).text = h.name
            val hCol = Store.itemColor(this, h.color)
            row.findViewById<View>(R.id.item_color).apply {
                if (hCol != 0) {
                    visibility = View.VISIBLE
                    backgroundTintList = android.content.res.ColorStateList.valueOf(hCol)
                } else visibility = View.GONE
            }
            val streak = Store.habitLiveStreak(h)
            row.findViewById<TextView>(R.id.habit_row_sub).text = when {
                Store.habitDoneTodayEntry(h) -> getString(R.string.habit_done_row, streak)
                streak > 0 -> getString(R.string.habit_streak_row, streak)
                else -> getString(R.string.habit_none_row)
            }
            row.findViewById<MaterialButton>(R.id.habit_row_checkin).setOnClickListener {
                Store.checkInHabitEntry(this, h.id); afterChange(); renderMore(); refreshHeroStreak()
            }
            row.setOnClickListener { habitDialog(h) }
            row.findViewById<ImageButton>(R.id.habit_row_delete).setOnClickListener { deleteHabitWithUndo(h) }
            container.addView(row)
        }
    }

    private fun habitDialog(entry: HabitEntry?) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_name, null)
        val name = view.findViewById<TextInputEditText>(R.id.dlg_name)
        name.setText(entry?.name ?: "")
        val colorPick = buildColorPicker(view.findViewById(R.id.dlg_colors), entry?.color ?: 0)
        MaterialAlertDialogBuilder(this)
            .setTitle(if (entry == null) R.string.new_habit else R.string.rename)
            .setView(view)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.save_generic) { _, _ ->
                val nm = name.text?.toString().orEmpty()
                val targetId = if (entry == null) Store.addHabitEntry(this, nm).id else entry.id
                Store.renameHabitEntry(this, targetId, nm)
                Store.setHabitColor(this, targetId, colorPick())
                afterChange(); renderMore(); refreshHeroStreak()
            }
            .show()
    }

    private fun deleteHabitWithUndo(entry: HabitEntry) {
        val index = Store.getHabitEntries(this).indexOfFirst { it.id == entry.id }.coerceAtLeast(0)
        Store.removeHabitEntry(this, entry.id); afterChange(); renderMore(); refreshHeroStreak()
        Snackbar.make(findViewById(R.id.root), R.string.item_deleted, Snackbar.LENGTH_LONG)
            .setAction(R.string.undo) { Store.restoreHabitEntry(this, entry, index); afterChange(); renderMore(); refreshHeroStreak() }
            .show()
    }

    // ---- Water ----
    private fun renderWaters() {
        val container = findViewById<LinearLayout>(R.id.waters_container)
        container.removeAllViews()
        val list = Store.getWaterEntries(this)
        findViewById<View>(R.id.waters_empty).visibility =
            if (list.isEmpty()) View.VISIBLE else View.GONE
        for (w in list) {
            val row = LayoutInflater.from(this).inflate(R.layout.item_water_row, container, false)
            row.findViewById<TextView>(R.id.water_row_name).text = w.name
            row.findViewById<TextView>(R.id.water_row_sub).text =
                getString(R.string.water_row_sub, Store.waterEntryCount(w), w.goal)
            row.findViewById<MaterialButton>(R.id.water_row_minus).setOnClickListener {
                Store.adjustWaterEntry(this, w.id, -1); afterChange(); renderWaters()
            }
            row.findViewById<MaterialButton>(R.id.water_row_plus).setOnClickListener {
                Store.adjustWaterEntry(this, w.id, 1); afterChange(); renderWaters()
            }
            row.setOnClickListener { waterDialog(w) }
            row.findViewById<ImageButton>(R.id.water_row_delete).setOnClickListener { deleteWaterWithUndo(w) }
            container.addView(row)
        }
    }

    private fun waterDialog(entry: WaterEntry?) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_water, null)
        val name = view.findViewById<TextInputEditText>(R.id.dlg_name)
        val goal = view.findViewById<TextInputEditText>(R.id.dlg_goal)
        name.setText(entry?.name ?: "")
        goal.setText((entry?.goal ?: 8).toString())
        MaterialAlertDialogBuilder(this)
            .setTitle(if (entry == null) R.string.new_water else R.string.rename)
            .setView(view)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.save_generic) { _, _ ->
                val nm = name.text?.toString().orEmpty()
                val g = goal.text?.toString()?.trim()?.toIntOrNull()?.coerceAtLeast(1) ?: 8
                if (entry == null) Store.addWaterEntry(this, nm, g) else Store.renameWaterEntry(this, entry.id, nm, g)
                afterChange(); renderWaters()
            }
            .show()
    }

    private fun deleteWaterWithUndo(entry: WaterEntry) {
        val index = Store.getWaterEntries(this).indexOfFirst { it.id == entry.id }.coerceAtLeast(0)
        Store.removeWaterEntry(this, entry.id); afterChange(); renderWaters()
        Snackbar.make(findViewById(R.id.root), R.string.item_deleted, Snackbar.LENGTH_LONG)
            .setAction(R.string.undo) { Store.restoreWaterEntry(this, entry, index); afterChange(); renderWaters() }
            .show()
    }

    private fun refreshHeroStreak() {
        findViewById<TextView>(R.id.stat_streak).text = Store.topHabitStreak(this).toString()
    }

    // ---- Countdowns ----
    private fun renderCountdowns() {
        val container = findViewById<LinearLayout>(R.id.countdowns_container)
        container.removeAllViews()
        val list = Store.getCountdownList(this)
        findViewById<View>(R.id.countdowns_empty).visibility =
            if (list.isEmpty()) View.VISIBLE else View.GONE
        for (cd in list) {
            val row = LayoutInflater.from(this).inflate(R.layout.item_countdown_row, container, false)
            row.findViewById<TextView>(R.id.cd_row_name).text = cd.name
            row.findViewById<TextView>(R.id.cd_row_days).text = countdownSubtitle(cd)
            row.setOnClickListener { countdownDialog(cd) }
            row.findViewById<ImageButton>(R.id.cd_row_delete).setOnClickListener {
                deleteCountdownWithUndo(cd)
            }
            container.addView(row)
        }
    }

    private fun deleteCountdownWithUndo(cd: CountdownEntry) {
        val index = Store.getCountdownList(this).indexOfFirst { it.id == cd.id }.coerceAtLeast(0)
        Store.removeCountdownEntry(this, cd.id); afterChange(); renderMore()
        Snackbar.make(findViewById(R.id.root), R.string.item_deleted, Snackbar.LENGTH_LONG)
            .setAction(R.string.undo) { Store.restoreCountdownEntry(this, cd, index); afterChange(); renderMore() }
            .show()
    }

    private fun countdownSubtitle(cd: CountdownEntry): String {
        if (cd.date == 0L) return getString(R.string.countdown_set)
        val days = cd.date - Store.todayEpochDay()
        val dateStr = LocalDate.ofEpochDay(cd.date).format(dateFmt)
        return when {
            days > 0 -> "$days ${getString(R.string.countdown_until)} · $dateStr"
            days == 0L -> "${getString(R.string.countdown_today)} · $dateStr"
            else -> "${-days} ${getString(R.string.countdown_ago)} · $dateStr"
        }
    }

    private fun countdownDialog(entry: CountdownEntry?) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_countdown, null)
        val name = view.findViewById<TextInputEditText>(R.id.dlg_name)
        val dateLabel = view.findViewById<TextView>(R.id.dlg_date_label)
        val btnDate = view.findViewById<MaterialButton>(R.id.dlg_date_btn)
        name.setText(entry?.name ?: "")
        var date = entry?.date ?: 0L
        fun refresh() { dateLabel.text = if (date > 0) LocalDate.ofEpochDay(date).format(dateFmt) else "" }
        refresh()
        btnDate.setOnClickListener {
            val initial = if (date > 0) date * 24L * 60L * 60L * 1000L else MaterialDatePicker.todayInUtcMilliseconds()
            val picker = MaterialDatePicker.Builder.datePicker().setSelection(initial).build()
            picker.addOnPositiveButtonClickListener {
                date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay(); refresh()
            }
            picker.show(supportFragmentManager, "cd")
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(if (entry == null) R.string.new_countdown else R.string.rename)
            .setView(view)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.save_generic) { _, _ ->
                val nm = name.text?.toString().orEmpty()
                if (entry == null) {
                    val created = Store.addCountdownEntry(this, nm)
                    Store.updateCountdownEntry(this, created.id, nm, date)
                } else {
                    Store.updateCountdownEntry(this, entry.id, nm, date)
                }
                afterChange(); renderMore()
            }
            .show()
    }

    private fun afterChange() {
        Widgets.refreshEverything(this)
    }

    /** Builds a row of color swatches (0 = none, 1..6 colors) into [container].
     *  Returns a getter for the currently selected index. */
    private fun buildColorPicker(container: LinearLayout, current: Int): () -> Int {
        var selected = current
        val ring = androidx.core.content.ContextCompat.getColor(this, R.color.on_surface)
        val outline = androidx.core.content.ContextCompat.getColor(this, R.color.outline)
        fun rebuild() {
            container.removeAllViews()
            for (idx in 0..Store.itemColorRes.size) {
                val swatch = View(this)
                val d = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    if (idx == 0) {
                        setColor(0x00000000)
                        setStroke(dp(2), outline)
                    } else {
                        setColor(Store.itemColor(this@MainActivity, idx))
                    }
                    if (idx == selected) setStroke(dp(3), ring)
                }
                swatch.background = d
                swatch.setOnClickListener { selected = idx; rebuild() }
                val lp = LinearLayout.LayoutParams(dp(34), dp(34)).apply { marginEnd = dp(10) }
                container.addView(swatch, lp)
            }
        }
        rebuild()
        return { selected }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
