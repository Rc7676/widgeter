package com.widgeter.app

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
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
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

/** One-screen home for editing the note, counter and to-do list. */
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()
    private lateinit var adapter: TodoAdapter

    private lateinit var noteInput: TextInputEditText
    private lateinit var counterValue: TextView
    private lateinit var todoEmpty: TextView
    private lateinit var todoProgress: TextView
    private var noteInitialised = false

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        applyInsets()

        bindViews()
        setupNote()
        setupCounter()
        setupTodos()
        observeState()
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
            onDelete = { removeWithUndo(it) }
        )
        val list = findViewById<RecyclerView>(R.id.todo_recycler)
        list.layoutManager = LinearLayoutManager(this)
        list.adapter = adapter
        list.isNestedScrollingEnabled = false

        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                r: RecyclerView, v: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder
            ) = false

            override fun onSwiped(vh: RecyclerView.ViewHolder, direction: Int) {
                val pos = vh.bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    removeWithUndo(adapter.itemAt(pos))
                }
            }
        }).attachToRecyclerView(list)

        val taskInput = findViewById<TextInputEditText>(R.id.task_input)
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
    }

    private fun removeWithUndo(item: TodoItem) {
        val index = adapter.currentList.indexOfFirst { it.id == item.id }.coerceAtLeast(0)
        viewModel.deleteTodo(item.id)
        Snackbar.make(findViewById(R.id.root), R.string.task_deleted, Snackbar.LENGTH_LONG)
            .setAction(R.string.undo) { viewModel.restoreTodo(item, index) }
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

        adapter.submitList(state.todos.toList())
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
