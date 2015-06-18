package fr.geobert.radis.ui.adapter

import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.CheckBox
import android.widget.TextView
import fr.geobert.radis.R

public class ExportColRowHolder(val view: View, val adapter: ExportColsAdapter) : RecyclerView.ViewHolder(view), View.OnClickListener {
    public val label: TextView = view.findViewById(R.id.col_name) as TextView
    public val toExport: CheckBox = view.findViewById(R.id.col_is_to_export_chk) as CheckBox

    init {
        view.setOnClickListener(this)
        toExport.setOnCheckedChangeListener { b, checked ->
            adapter.getItem(getAdapterPosition()).toExport = checked
        }
    }

    override fun onClick(p0: View) {
        val old = adapter.selectedPos
        val position = getAdapterPosition()
        adapter.selectedPos = position
        adapter.notifyItemChanged(position)
        adapter.notifyItemChanged(old)
    }

}
