package fr.geobert.radis.ui.adapter

import android.os.Parcelable
import android.support.v4.app.FragmentActivity
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import fr.geobert.radis.R
import fr.geobert.radis.data.ExportCol

public class ExportColsAdapter(val ctx: FragmentActivity, cols: List<ExportCol>) :
        RecyclerView.Adapter<ExportColRowHolder>(), Iterable<ExportCol> {
    override fun iterator(): Iterator<ExportCol> = columns.iterator()

    private val columns: MutableList<ExportCol> = linkedListOf()
    public var selectedPos: Int = -1

    init {
        columns.addAll(cols)
    }

    override fun getItemCount(): Int = columns.count()

    override fun onBindViewHolder(holder: ExportColRowHolder, position: Int) {
        val item = columns.get(position)

        holder.label.text = item.label
        holder.toExport.isChecked = item.toExport

        holder.view.setBackgroundResource(if (position == selectedPos) R.drawable.line_selected_gradient
        else R.color.normal_bg)
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ExportColRowHolder? {
        val l = LayoutInflater.from(ctx).inflate(R.layout.export_col_row, parent, false)
        return ExportColRowHolder(l, this)
    }

    public fun moveItem(offset: Int) {
        val from = selectedPos
        val newPos = from + offset
        if (from == -1 || offset == 0 || newPos < 0 || newPos >= itemCount) {
            return
        }

        val item = columns.get(from)
        columns.removeAt(from)
        columns.add(newPos, item)
        notifyItemMoved(from, newPos)
        selectedPos += offset
    }

    fun getItem(position: Int): ExportCol {
        return columns.get(position)
    }

    fun toArray(): Array<ExportCol> {
        return columns.toTypedArray()
    }

    fun fromArray(cols: Array<Parcelable>) {
        columns.clear()
        cols.forEach { columns.add(it as ExportCol) }
    }
}
