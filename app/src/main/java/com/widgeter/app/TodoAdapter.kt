package com.widgeter.app

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.checkbox.MaterialCheckBox

/** RecyclerView adapter for the in-app to-do list, with DiffUtil animations. */
class TodoAdapter(
    private val onToggle: (TodoItem) -> Unit,
    private val onDelete: (TodoItem) -> Unit
) : ListAdapter<TodoItem, TodoAdapter.VH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<TodoItem>() {
            override fun areItemsTheSame(a: TodoItem, b: TodoItem) = a.id == b.id
            override fun areContentsTheSame(a: TodoItem, b: TodoItem) = a == b
        }
    }

    fun itemAt(position: Int): TodoItem = getItem(position)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_todo, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val check: MaterialCheckBox = itemView.findViewById(R.id.item_check)
        private val text: TextView = itemView.findViewById(R.id.item_text)
        private val delete: ImageButton = itemView.findViewById(R.id.item_delete)

        fun bind(item: TodoItem) {
            text.text = item.text
            check.isChecked = item.done
            if (item.done) {
                text.paintFlags = text.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                text.alpha = 0.55f
            } else {
                text.paintFlags = text.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                text.alpha = 1.0f
            }
            val toggle = View.OnClickListener { onToggle(item) }
            itemView.setOnClickListener(toggle)
            check.setOnClickListener(toggle)
            delete.setOnClickListener { onDelete(item) }
        }
    }
}
