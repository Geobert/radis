package fr.geobert.radis.ui.adapter

import android.support.v7.widget.RecyclerView
import android.database.Cursor
import fr.geobert.radis.tools.map
import fr.geobert.radis.data.Statistic
import android.view.ViewGroup
import android.view.LayoutInflater
import fr.geobert.radis.ui.DeleteStatConfirmationDiag
import fr.geobert.radis.R
import fr.geobert.radis.ui.editor.StatisticEditor
import fr.geobert.radis.ui.StatisticsListFragment
import fr.geobert.radis.tools.formatDate

public class StatisticAdapter(cursor: Cursor, val ctx: StatisticsListFragment) : RecyclerView.Adapter<StatRowHolder>() {
    var statistics = cursor.map<Statistic> { Statistic(it) }

    override fun onCreateViewHolder(viewGroup: ViewGroup, p1: Int): StatRowHolder {
        val l = LayoutInflater.from(viewGroup.getContext()).inflate(fr.geobert.radis.R.layout.statistic_row,
                viewGroup, false)
        return StatRowHolder(l, ctx)
    }

    override fun onBindViewHolder(holder: StatRowHolder, pos: Int) {
        val stat = statisticAt(pos)

        holder.nameLbl.setText(stat.name)
        holder.accountNameLbl.setText(stat.accountName)
        holder.trashBtn.setOnClickListener {
            DeleteStatConfirmationDiag(stat.id).show(ctx.getActivity().getSupportFragmentManager(), "")
        }

        holder.editBtn.setOnClickListener { StatisticEditor.callMeForResult(ctx.getActivity(), stat.id) }

        holder.chartType.setImageResource(when (stat.chartType) {
            Statistic.CHART_PIE -> R.drawable.stat_pie
            Statistic.CHART_BAR -> R.drawable.stat_bar
            else -> R.drawable.stat_graph
        })

        holder.filterName.setText(stat.getFilterStr())

        val (start, end) = stat.createTimeRange()
        holder.timeScale.setText("${start.formatDate()} ${ctx.getString(R.string.rarr)} ${end.formatDate()}")
        //        holder.stat = stat
    }

    fun statisticAt(pos: Int) = statistics.elementAt(pos)

    override fun getItemCount(): Int = statistics.count()

    fun swapCursor(c: Cursor) {
        statistics = c.map<Statistic> { Statistic(it) }
        notifyDataSetChanged()
    }
}