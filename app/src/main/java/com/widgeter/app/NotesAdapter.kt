package com.widgeter.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/** List of app-managed notes. */
class NotesAdapter(
    private val onOpen: (NoteEntry) -> Unit,
    private val onDelete: (NoteEntry) -> Unit
) : RecyclerView.Adapter<NotesAdapter.VH>() {

    private val items = mutableListOf<NoteEntry>()

    fun submit(list: List<NoteEntry>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun getItemCount() = items.size

    fun itemAt(position: Int): NoteEntry = items[position]
    fun currentIds(): List<Long> = items.map { it.id }
    fun onItemMove(from: Int, to: Int) {
        java.util.Collections.swap(items, from, to)
        notifyItemMoved(from, to)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_note, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dot: View = itemView.findViewById(R.id.item_color)
        private val name: TextView = itemView.findViewById(R.id.note_name)
        private val preview: TextView = itemView.findViewById(R.id.note_preview)
        private val delete: ImageButton = itemView.findViewById(R.id.note_delete)

        fun bind(item: NoteEntry) {
            name.text = item.name
            val text = item.text.trim()
            preview.text = text.ifEmpty { itemView.context.getString(R.string.note_empty_preview) }
            val col = Store.itemColor(itemView.context, item.color)
            if (col != 0) {
                dot.visibility = View.VISIBLE
                dot.backgroundTintList = android.content.res.ColorStateList.valueOf(col)
            } else dot.visibility = View.GONE
            itemView.setOnClickListener { onOpen(item) }
            delete.setOnClickListener { onDelete(item) }
        }
    }
}
