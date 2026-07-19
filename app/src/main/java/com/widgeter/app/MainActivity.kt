package com.widgeter.app

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
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
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
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

    companion object {
        const val ACTION_NEW_TASK = "com.widgeter.app.action.NEW_TASK"
        const val ACTION_NEW_NOTE = "com.widgeter.app.action.NEW_NOTE"
        const val EXTRA_OPEN_NOTE = "com.widgeter.app.OPEN_NOTE"
    }

    private val dateFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
    private val heroDateFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, MMM d")

    override fun onCreate(savedInstanceState: Bundle?) {
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
        setupMore()

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
        renderNotes(); renderCounters(); renderTasks(); renderMore()
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
        findViewById<View>(R.id.page_more_root).visibility =
            if (itemId == R.id.nav_more) View.VISIBLE else View.GONE

        toolbar.title = when (itemId) {
            R.id.nav_counters -> getString(R.string.section_counter)
            R.id.nav_tasks -> getString(R.string.tasks_title)
            R.id.nav_more -> getString(R.string.more_title)
            else -> getString(R.string.app_name) // Notes/home
        }
        when (itemId) {
            R.id.nav_counters -> renderCounters()
            R.id.nav_tasks -> renderTasks()
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
        findViewById<TextView>(R.id.stat_streak).text = Store.habitStreak(this).toString()
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
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.edit_note_title)
            .setView(view)
            .setNegativeButton(R.string.cancel, null)
            .setNeutralButton(R.string.delete) { _, _ -> deleteNoteWithUndo(entry) }
            .setPositiveButton(R.string.save_generic) { _, _ ->
                Store.updateNoteEntry(this, entry.id, name.text?.toString().orEmpty(), text.text?.toString().orEmpty())
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
        MaterialAlertDialogBuilder(this)
            .setTitle(if (entry == null) R.string.new_counter else R.string.rename)
            .setView(view)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.save_generic) { _, _ ->
                val nm = name.text?.toString().orEmpty()
                val st = step.text?.toString()?.trim()?.toIntOrNull()?.coerceAtLeast(1) ?: 1
                if (entry == null) {
                    val created = Store.addCounterEntry(this, nm)
                    Store.renameCounterEntry(this, created.id, nm, st)
                } else {
                    Store.renameCounterEntry(this, entry.id, nm, st)
                }
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

    // ---------------- More ----------------

    private fun setupMore() {
        findViewById<MaterialButton>(R.id.habit_checkin_btn).setOnClickListener {
            Store.checkInHabit(this); afterChange(); renderMore()
        }
        findViewById<MaterialButton>(R.id.water_plus_btn).setOnClickListener {
            Store.addWater(this, 1); afterChange(); renderMore()
        }
        findViewById<MaterialButton>(R.id.water_minus_btn).setOnClickListener {
            Store.addWater(this, -1); afterChange(); renderMore()
        }
        findViewById<MaterialButton>(R.id.countdown_add_btn).setOnClickListener { countdownDialog(null) }
        findViewById<MaterialButton>(R.id.more_settings_btn).setOnClickListener { openSettings() }
    }

    private fun renderMore() {
        val streak = Store.habitStreak(this)
        findViewById<TextView>(R.id.habit_streak_text).text =
            if (streak > 0) getString(R.string.habit_streak_fmt, streak) else getString(R.string.habit_none)
        findViewById<TextView>(R.id.water_count_text).text =
            "${Store.getWaterCount(this)}/${Store.getWaterGoal(this)}"

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
        Widgets.refreshAll(this)
    }
}
