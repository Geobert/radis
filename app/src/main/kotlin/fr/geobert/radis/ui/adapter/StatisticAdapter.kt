package fr.geobert.radis.ui.adapter

import android.database.Cursor
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import fr.geobert.radis.R
import fr.geobert.radis.data.Statistic
import fr.geobert.radis.tools.formatDateLong
import fr.geobert.radis.tools.map
import fr.geobert.radis.ui.DeleteStatConfirmationDiag
import fr.geobert.radis.ui.StatisticsListFragment
import fr.geobert.radis.ui.editor.StatisticEditor

public class StatisticAdapter(cursor: Cursor, val ctx: StatisticsListFragment) : RecyclerView.Adapter<StatRowHolder>() {
    var statistics = cursor.map { Statistic(it) }

    override fun onCreateViewHolder(viewGroup: ViewGroup, p1: Int): StatRowHolder {
        val l = LayoutInflater.from(viewGroup.context).inflate(fr.geobert.radis.R.layout.statistic_row,
                viewGroup, false)
        return StatRowHolder(l, ctx)
    }

    override fun onBindViewHolder(holder: StatRowHolder, pos: Int) {
        val stat = statisticAt(pos)

        holder.nameLbl.text = stat.name
        holder.accountNameLbl.text = stat.accountName
        holder.trashBtn.setOnClickListener {
            DeleteStatConfirmationDiag(stat.id).show(ctx.activity.supportFragmentManager, "")
        }

        holder.editBtn.setOnClickListener { StatisticEditor.callMeForResult(ctx.activity, stat.id) }

        holder.chartType.setImageResource(when (stat.chartType) {
            Statistic.CHART_PIE -> R.drawable.stat_pie
            Statistic.CHART_BAR -> R.drawable.stat_bar
            else -> R.drawable.stat_graph
        })

        holder.filterName.setText(stat.getFilterStr())

        val (start, end) = stat.createTimeRange()
        holder.timeScale.text = "${start.formatDateLong()} ${ctx.getString(R.string.rarr)} ${end.formatDateLong()}"
        holder.stat = stat
    }

    fun statisticAt(pos: Int) = statistics.elementAt(pos)

    override fun getItemCount(): Int = statistics.count()

    fun swapCursor(c: Cursor) {
        statistics = c.map { Statistic(it) }
        notifyDataSetChanged()
    }
}
