package com.widgeter.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

/** List of app-managed counters with inline +/- and rename/delete. */
class CountersAdapter(
    private val onAdjust: (CounterEntry, Int) -> Unit,
    private val onRename: (CounterEntry) -> Unit,
    private val onDelete: (CounterEntry) -> Unit
) : RecyclerView.Adapter<CountersAdapter.VH>() {

    private val items = mutableListOf<CounterEntry>()

    fun submit(list: List<CounterEntry>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun getItemCount() = items.size

    fun itemAt(position: Int): CounterEntry = items[position]
    fun currentIds(): List<Long> = items.map { it.id }
    fun onItemMove(from: Int, to: Int) {
        java.util.Collections.swap(items, from, to)
        notifyItemMoved(from, to)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_counter, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dot: View = itemView.findViewById(R.id.item_color)
        private val name: TextView = itemView.findViewById(R.id.counter_name)
        private val value: TextView = itemView.findViewById(R.id.counter_value)
        private val minus: MaterialButton = itemView.findViewById(R.id.counter_minus)
        private val plus: MaterialButton = itemView.findViewById(R.id.counter_plus)
        private val edit: ImageButton = itemView.findViewById(R.id.counter_edit)
        private val delete: ImageButton = itemView.findViewById(R.id.counter_delete)

        fun bind(item: CounterEntry) {
            name.text = item.name
            value.text = item.value.toString()
            val col = Store.itemColor(itemView.context, item.color)
            if (col != 0) {
                dot.visibility = View.VISIBLE
                dot.backgroundTintList = android.content.res.ColorStateList.valueOf(col)
            } else dot.visibility = View.GONE
            minus.setOnClickListener { onAdjust(item, -1) }
            plus.setOnClickListener { onAdjust(item, 1) }
            edit.setOnClickListener { onRename(item) }
            delete.setOnClickListener { onDelete(item) }
        }
    }
}
