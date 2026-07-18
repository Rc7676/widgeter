package com.widgeter.app

import android.annotation.SuppressLint
import android.graphics.Paint
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.checkbox.MaterialCheckBox
import java.util.Collections

/**
 * RecyclerView adapter for the in-app to-do list, with drag-to-reorder,
 * priority stripe, and due-date meta. Uses a mutable backing list so drag
 * moves are immediate; [submit] diffs external updates for animations.
 */
class TodoAdapter(
    private val onToggle: (TodoItem) -> Unit,
    private val onDelete: (TodoItem) -> Unit,
    private val onEdit: (TodoItem) -> Unit,
    private val onStartDrag: (RecyclerView.ViewHolder) -> Unit
) : RecyclerView.Adapter<TodoAdapter.VH>() {

    private val items = mutableListOf<TodoItem>()

    fun submit(newList: List<TodoItem>) {
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = items.size
            override fun getNewListSize() = newList.size
            override fun areItemsTheSame(o: Int, n: Int) = items[o].id == newList[n].id
            override fun areContentsTheSame(o: Int, n: Int) = items[o] == newList[n]
        })
        items.clear()
        items.addAll(newList)
        diff.dispatchUpdatesTo(this)
    }

    fun itemAt(position: Int): TodoItem = items[position]
    fun currentIds(): List<Long> = items.map { it.id }

    fun onItemMove(from: Int, to: Int) {
        Collections.swap(items, from, to)
        notifyItemMoved(from, to)
    }

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_todo, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val priority: View = itemView.findViewById(R.id.item_priority)
        private val check: MaterialCheckBox = itemView.findViewById(R.id.item_check)
        private val text: TextView = itemView.findViewById(R.id.item_text)
        private val meta: TextView = itemView.findViewById(R.id.item_meta)
        private val drag: ImageView = itemView.findViewById(R.id.item_drag)
        private val delete: ImageButton = itemView.findViewById(R.id.item_delete)

        @SuppressLint("ClickableViewAccessibility")
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

            val prioColor = when (item.priority) {
                1 -> R.color.prio_low
                2 -> R.color.prio_med
                3 -> R.color.prio_high
                else -> R.color.prio_none
            }
            priority.setBackgroundColor(ContextCompat.getColor(itemView.context, prioColor))

            val metaText = buildMeta(item)
            if (metaText.isNullOrEmpty()) {
                meta.visibility = View.GONE
            } else {
                meta.visibility = View.VISIBLE
                meta.text = metaText
            }

            val toggle = View.OnClickListener { onToggle(item) }
            itemView.setOnClickListener(toggle)
            check.setOnClickListener(toggle)
            itemView.setOnLongClickListener { onEdit(item); true }
            delete.setOnClickListener { onDelete(item) }
            drag.setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN) onStartDrag(this)
                false
            }
        }

        private fun buildMeta(item: TodoItem): String? {
            val parts = mutableListOf<String>()
            if (item.due > 0) parts.add(formatDue(item.due))
            when (item.priority) {
                1 -> parts.add(itemView.context.getString(R.string.prio_low))
                2 -> parts.add(itemView.context.getString(R.string.prio_med))
                3 -> parts.add(itemView.context.getString(R.string.prio_high))
            }
            return if (parts.isEmpty()) null else parts.joinToString("  •  ")
        }

        private fun formatDue(due: Long): String {
            val now = System.currentTimeMillis()
            return when {
                DateUtils.isToday(due) -> itemView.context.getString(R.string.due_today)
                DateUtils.isToday(due - DateUtils.DAY_IN_MILLIS) ->
                    itemView.context.getString(R.string.due_tomorrow)
                else -> DateUtils.formatDateTime(
                    itemView.context, due, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_MONTH
                )
            }.let { if (due < now && !DateUtils.isToday(due)) "⚠ $it" else it }
        }
    }
}
