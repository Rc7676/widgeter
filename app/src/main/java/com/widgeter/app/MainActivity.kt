package com.widgeter.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

/** One-screen home for editing the note, counter and to-do list. */
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()
    private lateinit var adapter: TodoAdapter
    private lateinit var itemTouchHelper: ItemTouchHelper

    private lateinit var noteInput: TextInputEditText
    private lateinit var taskInput: TextInputEditText
    private lateinit var counterValue: TextView
    private lateinit var todoEmpty: TextView
    private lateinit var todoProgress: TextView
    private var noteInitialised = false

    companion object {
        const val ACTION_NEW_TASK = "com.widgeter.app.action.NEW_TASK"
        const val ACTION_NEW_NOTE = "com.widgeter.app.action.NEW_NOTE"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        applyInsets()

        setupToolbar()
        bindViews()
        setupNote()
        setupCounter()
        setupTodos()
        observeState()
        maybeShowOnboarding()
        handleShortcut(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShortcut(intent)
    }

    private fun setupToolbar() {
        findViewById<MaterialToolbar>(R.id.toolbar).apply {
            inflateMenu(R.menu.main_menu)
            setOnMenuItemClickListener { item ->
                if (item.itemId == R.id.action_settings) {
                    startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
                    true
                } else false
            }
        }
    }

    private fun maybeShowOnboarding() {
        if (Store.isOnboarded(this)) return
        Store.setOnboarded(this)
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.onboarding_title)
            .setMessage(R.string.onboarding_body)
            .setPositiveButton(R.string.onboarding_got_it, null)
            .show()
    }

    private fun handleShortcut(intent: Intent?) {
        when (intent?.action) {
            ACTION_NEW_NOTE -> focusAndOpenKeyboard(noteInput)
            ACTION_NEW_TASK -> focusAndOpenKeyboard(taskInput)
        }
    }

    private fun focusAndOpenKeyboard(view: View) {
        view.post {
            view.requestFocus()
            val imm = getSystemService(INPUT_METHOD_SERVICE)
                    as? android.view.inputmethod.InputMethodManager
            imm?.showSoftInput(view, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun applyInsets() {
        val appBar = findViewById<View>(R.id.app_bar)
        val scroll = findViewById<View>(R.id.content_scroll)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root)) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            appBar.updatePadding(top = bars.top)
            scroll.updatePadding(left = bars.left, right = bars.right, bottom = bars.bottom)
            insets
        }
    }

    private fun bindViews() {
        noteInput = findViewById(R.id.note_input)
        taskInput = findViewById(R.id.task_input)
        counterValue = findViewById(R.id.counter_value)
        todoEmpty = findViewById(R.id.todo_empty)
        todoProgress = findViewById(R.id.todo_progress)
    }

    private fun setupNote() {
        findViewById<MaterialButton>(R.id.btn_save_note).setOnClickListener {
            viewModel.setNote(noteInput.text?.toString().orEmpty())
            hideKeyboardFrom(noteInput)
            Snackbar.make(it, R.string.note_saved, Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun setupCounter() {
        findViewById<View>(R.id.counter_plus).setOnClickListener {
            it.performHapticClick()
            viewModel.adjustCounter(1)
        }
        findViewById<View>(R.id.counter_minus).setOnClickListener {
            it.performHapticClick()
            viewModel.adjustCounter(-1)
        }
        findViewById<MaterialButton>(R.id.btn_reset_counter).setOnClickListener {
            viewModel.resetCounter()
        }
    }

    private fun setupTodos() {
        adapter = TodoAdapter(
            onToggle = { viewModel.toggleTodo(it.id) },
            onDelete = { removeWithUndo(it) },
            onEdit = { showEditDialog(it) },
            onStartDrag = { vh -> itemTouchHelper.startDrag(vh) }
        )
        val list = findViewById<RecyclerView>(R.id.todo_recycler)
        list.layoutManager = LinearLayoutManager(this)
        list.adapter = adapter
        list.isNestedScrollingEnabled = false

        val callback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun isLongPressDragEnabled() = false

            override fun onMove(
                r: RecyclerView, v: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder
            ): Boolean {
                adapter.onItemMove(v.bindingAdapterPosition, t.bindingAdapterPosition)
                return true
            }

            override fun onSwiped(vh: RecyclerView.ViewHolder, direction: Int) {
                val pos = vh.bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    removeWithUndo(adapter.itemAt(pos))
                }
            }

            override fun clearView(r: RecyclerView, vh: RecyclerView.ViewHolder) {
                super.clearView(r, vh)
                viewModel.reorder(adapter.currentIds())
            }
        }
        itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(list)

        findViewById<MaterialButton>(R.id.btn_add_task).setOnClickListener {
            val text = taskInput.text?.toString().orEmpty()
            if (text.isNotBlank()) {
                viewModel.addTodo(text)
                taskInput.setText("")
            }
        }
        findViewById<MaterialButton>(R.id.btn_clear_completed).setOnClickListener {
            viewModel.clearCompleted()
        }
        findViewById<MaterialButton>(R.id.btn_sort_todos).setOnClickListener {
            viewModel.sortTodos()
        }
    }

    private fun removeWithUndo(item: TodoItem) {
        val index = adapter.currentIds().indexOf(item.id).coerceAtLeast(0)
        viewModel.deleteTodo(item.id)
        Snackbar.make(findViewById(R.id.root), R.string.task_deleted, Snackbar.LENGTH_LONG)
            .setAction(R.string.undo) { viewModel.restoreTodo(item, index) }
            .show()
    }

    private fun showEditDialog(item: TodoItem) {
        val view = layoutInflater.inflate(R.layout.dialog_edit_todo, null)
        val name = view.findViewById<TextInputEditText>(R.id.edit_name)
        val group = view.findViewById<MaterialButtonToggleGroup>(R.id.priority_group)
        val dueLabel = view.findViewById<TextView>(R.id.due_label)
        val btnDue = view.findViewById<MaterialButton>(R.id.btn_due)
        val btnClear = view.findViewById<MaterialButton>(R.id.btn_due_clear)

        name.setText(item.text)
        group.check(
            when (item.priority) {
                1 -> R.id.prio_low
                2 -> R.id.prio_med
                3 -> R.id.prio_high
                else -> R.id.prio_none
            }
        )

        var due = item.due
        fun refreshDue() {
            if (due > 0) {
                dueLabel.text = android.text.format.DateUtils.formatDateTime(
                    this, due,
                    android.text.format.DateUtils.FORMAT_SHOW_DATE or
                        android.text.format.DateUtils.FORMAT_ABBREV_MONTH
                )
                btnClear.visibility = View.VISIBLE
            } else {
                dueLabel.text = ""
                btnClear.visibility = View.GONE
            }
        }
        refreshDue()

        btnDue.setOnClickListener {
            val picker = MaterialDatePicker.Builder.datePicker()
                .setSelection(if (due > 0) due else MaterialDatePicker.todayInUtcMilliseconds())
                .build()
            picker.addOnPositiveButtonClickListener { selection ->
                due = selection
                refreshDue()
            }
            picker.show(supportFragmentManager, "due_picker")
        }
        btnClear.setOnClickListener {
            due = 0L
            refreshDue()
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.edit_title)
            .setView(view)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.save_generic) { _, _ ->
                val priority = when (group.checkedButtonId) {
                    R.id.prio_low -> 1
                    R.id.prio_med -> 2
                    R.id.prio_high -> 3
                    else -> 0
                }
                viewModel.updateTodo(item.id, name.text?.toString().orEmpty(), priority, due)
            }
            .show()
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state -> render(state) }
            }
        }
    }

    private fun render(state: AppState) {
        if (!noteInitialised) {
            noteInput.setText(state.note)
            noteInitialised = true
        }
        counterValue.text = state.counter.toString()

        adapter.submit(state.todos.toList())
        val hasTasks = state.todos.isNotEmpty()
        todoEmpty.visibility = if (hasTasks) View.GONE else View.VISIBLE
        findViewById<View>(R.id.todo_recycler).visibility =
            if (hasTasks) View.VISIBLE else View.GONE
        todoProgress.visibility = if (hasTasks) View.VISIBLE else View.GONE
        todoProgress.text = getString(R.string.todo_progress_fmt, state.doneCount, state.totalCount)
        findViewById<View>(R.id.btn_clear_completed).visibility =
            if (state.doneCount > 0) View.VISIBLE else View.GONE
    }

    private fun hideKeyboardFrom(view: View) {
        val imm = getSystemService(INPUT_METHOD_SERVICE)
                as? android.view.inputmethod.InputMethodManager
        imm?.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun View.performHapticClick() {
        performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
    }
}
