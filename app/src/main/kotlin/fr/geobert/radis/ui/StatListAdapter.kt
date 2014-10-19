package fr.geobert.radis.ui

import android.app.Activity
import android.content.Context
import android.database.Cursor
import android.os.Build
import fr.geobert.radis.R
import org.achartengine.chart.BarChart.Type
import org.achartengine.chart.PointStyle
import android.view.View
import android.widget.TextView
import android.widget.ImageButton
import android.support.v4.widget.SimpleCursorAdapter
import fr.geobert.radis.db.StatisticTable
import android.support.v4.widget.CursorAdapter
import android.graphics.Color
import fr.geobert.radis.data.Statistic
import android.support.v4.app.FragmentActivity
import fr.geobert.radis.ui.editor.StatisticEditor
import android.view.ViewGroup
import java.util.Calendar
import fr.geobert.radis.data.Operation
import fr.geobert.radis.db.OperationTable
import fr.geobert.radis.tools.map
import java.util.GregorianCalendar
import fr.geobert.radis.tools.Formater
import java.util.Locale
import java.text.DateFormatSymbols
import java.text.NumberFormat
import fr.geobert.radis.db.AccountTable
import fr.geobert.radis.data.Account
import java.util.Currency
import org.achartengine.renderer.DefaultRenderer
import org.achartengine.model.CategorySeries
import org.achartengine.renderer.SimpleSeriesRenderer
import org.achartengine.renderer.XYMultipleSeriesRenderer
import org.achartengine.model.XYMultipleSeriesDataset
import org.achartengine.renderer.XYSeriesRenderer
import org.achartengine.model.TimeSeries
import android.graphics.Paint
import android.support.v4.util.SimpleArrayMap
import org.achartengine.model.XYSeries
import org.achartengine.GraphicalView
import org.achartengine.ChartFactory
import android.widget.ImageView
import android.text.format.DateFormat

class StatListAdapter(val ctx: Activity, cursor: Cursor) :
        SimpleCursorAdapter(ctx, R.layout.statistic_row, cursor, array(StatisticTable.KEY_STAT_NAME, StatisticTable.KEY_STAT_ACCOUNT_NAME),
                intArray(R.id.chart_name, R.id.chart_account_name), CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER) {
    class StatRowHolder(v: View) {
        val nameLbl: TextView = v.findViewById(R.id.chart_name) as TextView
        val accountNameLbl: TextView = v.findViewById(R.id.chart_account_name) as TextView
        val trashBtn: ImageButton = v.findViewById(R.id.chart_delete) as ImageButton
        val editBtn: ImageButton = v.findViewById(R.id.edit_chart) as ImageButton
        val chartType: ImageView = v.findViewById(R.id.chart_type) as ImageView
        val timeScale: TextView = v.findViewById(R.id.time_scale) as TextView
        val filterName: TextView = v.findViewById(R.id.filter_lbl) as TextView
        var stat: Statistic? = null
    }

    {
        setViewBinder(StatViewBinder())
    }



    inner class StatViewBinder : SimpleCursorAdapter.ViewBinder {
        override fun setViewValue(v: View?, c: Cursor?, colIdx: Int): Boolean {
            val stat = Statistic(c as Cursor)
            when (c.getColumnName(colIdx)) {
                StatisticTable.KEY_STAT_NAME -> (v as TextView).setText(stat.name)
                StatisticTable.KEY_STAT_ACCOUNT_NAME -> {
                    val holder = (v?.getParent() as View).getTag() as StatRowHolder
                    holder.accountNameLbl.setText(stat.accountName)
                    holder.trashBtn.setOnClickListener {
                        DeleteStatConfirmationDiag(stat.id).show((ctx as FragmentActivity).getSupportFragmentManager(),
                                "")
                    }

                    holder.editBtn.setOnClickListener { StatisticEditor.callMeForResult(ctx, stat.id) }

                    holder.chartType.setImageResource(when (stat.chartType) {
                        Statistic.CHART_PIE -> R.drawable.stat_pie
                        Statistic.CHART_BAR -> R.drawable.stat_bar
                        else -> R.drawable.stat_graph
                    })

                    holder.filterName.setText(stat.getFilterStr())

                    val (start, end) = stat.createTimeRange()
                    val f = Formater.getFullDateFormater()
                    holder.timeScale.setText("${f.format(start)} ${ctx.getString(R.string.rarr)} ${f.format(end)}")
                    holder.stat = stat

                    //                    holder.graph.removeAllViews()
                    //                    val chart = createChartView(stat)
                    //                    chart.setBackgroundColor(ctx.getResources()?.getColor(android.R.color.transparent) ?: 0)
                    //                    holder.graph.addView(chart)
                }
            }
            return true
        }
    }

    override fun newView(p1: Context?, p2: Cursor?, p3: ViewGroup?): View {
        val row = super.newView(p1, p2, p3) as View
        val h = StatRowHolder(row)
        row.setTag(h)
        return row
    }


}