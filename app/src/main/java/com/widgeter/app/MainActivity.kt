package com.widgeter.app

import android.graphics.Paint
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/** In-app screen to edit the note, reset the counter, and manage to-do items. */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupNote()
        setupCounter()
        setupTodo()
    }

    override fun onResume() {
        super.onResume()
        // Counter can change from the widget while the app is open.
        findViewById<TextView>(R.id.text_counter).text = Store.getCounter(this).toString()
    }

    private fun setupNote() {
        val editNote = findViewById<EditText>(R.id.edit_note)
        editNote.setText(Store.getNote(this))
        findViewById<Button>(R.id.btn_save_note).setOnClickListener {
            Store.setNote(this, editNote.text.toString())
            Widgets.refreshAll(this)
            Toast.makeText(this, R.string.note_saved, Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupCounter() {
        val counterText = findViewById<TextView>(R.id.text_counter)
        counterText.text = Store.getCounter(this).toString()
        findViewById<Button>(R.id.btn_reset_counter).setOnClickListener {
            Store.setCounter(this, 0)
            counterText.text = "0"
            Widgets.refreshAll(this)
        }
    }

    private fun setupTodo() {
        val editTask = findViewById<EditText>(R.id.edit_task)
        findViewById<Button>(R.id.btn_add_task).setOnClickListener {
            val text = editTask.text.toString().trim()
            if (text.isNotEmpty()) {
                val items = Store.getTodos(this)
                items.add(TodoItem(text, false))
                Store.saveTodos(this, items)
                editTask.setText("")
                renderTodos()
                Widgets.refreshAll(this)
            }
        }
        renderTodos()
    }

    private fun renderTodos() {
        val container = findViewById<LinearLayout>(R.id.todo_container)
        container.removeAllViews()
        val items = Store.getTodos(this)
        for ((index, item) in items.withIndex()) {
            val row = layoutInflater.inflate(R.layout.main_todo_row, container, false)
            val check = row.findViewById<ImageView>(R.id.row_check)
            val text = row.findViewById<TextView>(R.id.row_text)
            val delete = row.findViewById<ImageButton>(R.id.row_delete)

            text.text = item.text
            check.setImageResource(
                if (item.done) R.drawable.ic_check_on else R.drawable.ic_check_off
            )
            if (item.done) {
                text.paintFlags = text.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                text.setTextColor(0xFF9E9E9E.toInt())
            }

            val toggle = View.OnClickListener {
                val list = Store.getTodos(this)
                if (index < list.size) {
                    list[index] = list[index].copy(done = !list[index].done)
                    Store.saveTodos(this, list)
                    renderTodos()
                    Widgets.refreshAll(this)
                }
            }
            check.setOnClickListener(toggle)
            text.setOnClickListener(toggle)

            delete.setOnClickListener {
                val list = Store.getTodos(this)
                if (index < list.size) {
                    list.removeAt(index)
                    Store.saveTodos(this, list)
                    renderTodos()
                    Widgets.refreshAll(this)
                }
            }

            container.addView(row)
        }
    }
}
