package com.widgeter.app

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The full observable state the UI renders. */
data class AppState(
    val note: String = "",
    val noteTime: Long = 0L,
    val counter: Int = 0,
    val todos: List<TodoItem> = emptyList()
) {
    val doneCount: Int get() = todos.count { it.done }
    val totalCount: Int get() = todos.size
}

/**
 * App-facing reactive facade over [Store]. Emits a fresh [AppState] whenever
 * data changes — including when a *widget* mutates prefs in this same process,
 * so the open app stays in sync automatically.
 */
object Repo {
    private lateinit var appContext: Context
    private lateinit var prefs: SharedPreferences

    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()

    private val listener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> reload() }

    fun init(context: Context) {
        if (::appContext.isInitialized) return
        appContext = context.applicationContext
        prefs = Store.prefs(appContext)
        prefs.registerOnSharedPreferenceChangeListener(listener)
        reload()
    }

    private fun reload() {
        _state.value = AppState(
            note = Store.getNote(appContext),
            noteTime = Store.getNoteTime(appContext),
            counter = Store.getCounter(appContext),
            todos = Store.getTodos(appContext)
        )
    }

    // ---- Note ----
    fun setNote(text: String) {
        Store.setNote(appContext, text)
        Widgets.refreshAll(appContext)
    }

    // ---- Counter ----
    fun adjustCounter(delta: Int) {
        Store.setCounter(appContext, Store.getCounter(appContext) + delta)
        Widgets.refreshAll(appContext)
    }

    fun resetCounter() {
        Store.setCounter(appContext, 0)
        Widgets.refreshAll(appContext)
    }

    // ---- To-dos ----
    fun addTodo(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        val items = Store.getTodos(appContext)
        items.add(TodoItem(Store.nextId(appContext), trimmed, false))
        Store.saveTodos(appContext, items)
        Widgets.refreshAll(appContext)
    }

    fun updateTodo(id: Long, text: String, priority: Int, due: Long) {
        val items = Store.getTodos(appContext)
        val idx = items.indexOfFirst { it.id == id }
        if (idx >= 0) {
            val trimmed = text.trim().ifEmpty { items[idx].text }
            items[idx] = items[idx].copy(text = trimmed, priority = priority, due = due)
            Store.saveTodos(appContext, items)
            Widgets.refreshAll(appContext)
        }
    }

    /** Reorders the stored list to match the given id order (from drag & drop). */
    fun reorder(orderedIds: List<Long>) {
        val items = Store.getTodos(appContext)
        val byId = items.associateBy { it.id }
        val reordered = orderedIds.mapNotNull { byId[it] }
        if (reordered.size == items.size) {
            Store.saveTodos(appContext, reordered)
            Widgets.refreshAll(appContext)
        }
    }

    /** Sorts: unfinished first, then higher priority, then earlier due date. */
    fun sortTodos() {
        val items = Store.getTodos(appContext)
        val sorted = items.sortedWith(
            compareBy<TodoItem> { it.done }
                .thenByDescending { it.priority }
                .thenBy { if (it.due == 0L) Long.MAX_VALUE else it.due }
        )
        Store.saveTodos(appContext, sorted)
        Widgets.refreshAll(appContext)
    }

    fun toggleTodo(id: Long) {
        val items = Store.getTodos(appContext)
        val idx = items.indexOfFirst { it.id == id }
        if (idx >= 0) {
            items[idx] = items[idx].copy(done = !items[idx].done)
            Store.saveTodos(appContext, items)
            Widgets.refreshAll(appContext)
        }
    }

    fun deleteTodo(id: Long) {
        val items = Store.getTodos(appContext)
        if (items.removeAll { it.id == id }) {
            Store.saveTodos(appContext, items)
            Widgets.refreshAll(appContext)
        }
    }

    /** Re-inserts a previously deleted item at its old position (for Undo). */
    fun restoreTodo(item: TodoItem, index: Int) {
        val items = Store.getTodos(appContext)
        val at = index.coerceIn(0, items.size)
        items.add(at, item)
        Store.saveTodos(appContext, items)
        Widgets.refreshAll(appContext)
    }

    fun clearCompleted() {
        val items = Store.getTodos(appContext)
        if (items.removeAll { it.done }) {
            Store.saveTodos(appContext, items)
            Widgets.refreshAll(appContext)
        }
    }
}
