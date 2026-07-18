package com.widgeter.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel

/** Thin UI-facing layer over [Repo]; survives configuration changes. */
class MainViewModel(app: Application) : AndroidViewModel(app) {

    init {
        Repo.init(app)
    }

    val state = Repo.state

    fun setNote(text: String) = Repo.setNote(text)
    fun adjustCounter(delta: Int) = Repo.adjustCounter(delta)
    fun resetCounter() = Repo.resetCounter()
    fun addTodo(text: String) = Repo.addTodo(text)
    fun toggleTodo(id: Long) = Repo.toggleTodo(id)
    fun deleteTodo(id: Long) = Repo.deleteTodo(id)
    fun restoreTodo(item: TodoItem, index: Int) = Repo.restoreTodo(item, index)
    fun clearCompleted() = Repo.clearCompleted()
}
