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
import android.widget.LinearLayout
import android.support.v4.widget.SimpleCursorAdapter
import fr.geobert.radis.db.StatisticTable
import android.support.v4.widget.CursorAdapter
import android.graphics.Color
import fr.geobert.radis.data.Statistic
import android.support.v4.app.FragmentActivity
import fr.geobert.radis.ui.editor.StatisticEditor
import android.view.ViewGroup
import fr.geobert.radis.tools.Tools
import java.util.Date
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

class StatListAdapter(val ctx: Activity, cursor: Cursor) :
        SimpleCursorAdapter(ctx, R.layout.statistic_row, cursor, array(StatisticTable.KEY_STAT_NAME, StatisticTable.KEY_STAT_ACCOUNT_NAME),
                intArray(R.id.chart_name, R.id.chart_account_name), CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER) {
    class StatRowHolder(v: View) {
        val nameLbl: TextView = v.findViewById(R.id.chart_name) as TextView
        val accountNameLbl: TextView = v.findViewById(R.id.chart_account_name) as TextView
        val trashBtn: ImageButton = v.findViewById(R.id.chart_delete) as ImageButton
        val editBtn: ImageButton = v.findViewById(R.id.edit_chart) as ImageButton
        val graph: LinearLayout = v.findViewById(R.id.chart) as LinearLayout
    }

    {
        setViewBinder(StatViewBinder())
    }

    private fun getColorsArray(id: Int): List<Int> =
            ctx.getResources()?.getStringArray(id)?.map({ Color.parseColor(it) }) as List<Int>

    private val pos_colors: List<Int> = getColorsArray(R.array.positive_colors)
    private val neg_colors: List<Int> = getColorsArray(R.array.negative_colors)

    inner class StatViewBinder : SimpleCursorAdapter.ViewBinder {
        override fun setViewValue(v: View?, c: Cursor?, colIdx: Int): Boolean {
            val stat = Statistic(c as Cursor)
            when (c.getColumnName(colIdx)) {
                StatisticTable.KEY_STAT_NAME -> (v as TextView).setText(stat.name)
                StatisticTable.KEY_STAT_ACCOUNT_NAME -> {
                    (v as TextView).setText(stat.accountName)
                    val holder = (v.getParent() as View).getTag() as StatRowHolder

                    holder.trashBtn.setOnClickListener {
                        DeleteStatConfirmationDiag(stat.id).show((ctx as FragmentActivity).getSupportFragmentManager(),
                                "")
                    }

                    holder.editBtn.setOnClickListener { StatisticEditor.callMeForResult(ctx, stat.id) }
                    holder.graph.removeAllViews()
                    val chart = createChartView(stat)
                    chart.setBackgroundColor(ctx.getResources()?.getColor(android.R.color.transparent) ?: 0)
                    holder.graph.addView(chart)
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

    /**
     * create a time range according to statistic's configuration
     * @param stat
     * @return (startDate, endDate)
     */
    private fun createTimeRange(stat: Statistic): Pair<Date, Date> {
        fun createXLastRange(field: Int): Pair<Date, Date> {
            val endDate = Tools.createClearedCalendar()
            val startDate = Tools.createClearedCalendar()
            startDate.add(field, -stat.xLast)
            return Pair(startDate.getTime(), endDate.getTime())
        }

        return when (stat.timeScaleType) {
            Statistic.PERIOD_DAYS -> createXLastRange(Calendar.DAY_OF_MONTH)
            Statistic.PERIOD_MONTHES -> createXLastRange(Calendar.MONTH)
            Statistic.PERIOD_YEARS -> createXLastRange(Calendar.YEAR)
            Statistic.PERIOD_ABSOLUTE -> Pair(stat.startDate, stat.endDate)
            else -> {
                throw NoWhenBranchMatchedException()
            }
        }
    }

    /**
     * get the ops list according to the time range and group by partFunc
     * @param stat the stat to analyse
     * @return a map with the group key and List(Operation)
     */
    private fun partOps(stat: Statistic): Pair<Map<String, List<Operation>>, Map<String, List<Operation>>> {
        val (startDate, endDate) = createTimeRange(stat)
        val ops = OperationTable.getOpsBetweenDate(ctx, startDate, endDate, stat.accountId).map({ Operation(it) })
        val (pos, neg) = ops.partition { it.mSum > 0 }
        return Pair(pos.groupBy { partFunc(stat)(it) }, neg.groupBy { partFunc(stat)(it) })
    }

    /**
     * get the partitioning function according to filterType
     * @param stat the stat to analyse
     * @return the partitioning function
     */
    private fun partFunc(stat: Statistic): (Operation) -> String =
            when (stat.filterType) {
                Statistic.THIRD_PARTY -> {(o: Operation) -> o.mThirdParty ?: "" }
                Statistic.TAGS -> {(o: Operation) -> o.mTag ?: "" }
                Statistic.MODE -> {(o: Operation) -> o.mMode ?: "" }
                else -> { // Statistic.NO_FILTER
                    (o: Operation) ->
                    val g = GregorianCalendar()
                    g.setTimeInMillis(o.getDate())
                    when (stat.timeScaleType) {
                        Statistic.PERIOD_DAYS, Statistic.PERIOD_ABSOLUTE -> Formater.getFullDateFormater().format(g.getTime())
                        Statistic.PERIOD_MONTHES ->
                            if (Build.VERSION.SDK_INT >= 9) {
                                g.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault() as Locale) ?: ""
                            } else {
                                DateFormatSymbols().getShortMonths()?.get(g[Calendar.MONTH]) ?: ""
                            }
                        Statistic.PERIOD_YEARS -> g[Calendar.YEAR].toString()
                        else -> {
                            ""
                        }
                    }
                }
            }


    private fun sumPerFilter(stat: Statistic): Pair<Map<String, Long>, Map<String, Long>> {
        // partition the list according to filterType
        //partOps(stat).mapValues(_.foldLeft(0l)((s: Long, o: Operation) => s + o.mSum))
        fun sumMap(m: Map<String, List<Operation>>) = m.mapValues { it.value.fold(0L) {(s: Long, o: Operation) -> s + o.mSum } }
        val (pos, neg) = partOps(stat)
        return Pair(sumMap(pos), sumMap(neg))
    }

    private fun initNumFormat(stat: Statistic): NumberFormat {
        val cursor = AccountTable.fetchAccount(ctx, stat.accountId)
        cursor.moveToFirst()
        val account = Account(cursor)
        val numFormat = NumberFormat.getCurrencyInstance()
        numFormat.setCurrency(Currency.getInstance(account.currency))
        numFormat.setMinimumFractionDigits(2)
        return numFormat
    }

    // for pie chart
    private fun createCategorySeries(stat: Statistic): Pair<CategorySeries, DefaultRenderer> {
        val data = CategorySeries("")
        val renderer = DefaultRenderer()
        renderer.setStartAngle(180f)
        renderer.setDisplayValues(true)
        renderer.setLegendTextSize(16f)
        renderer.setLabelsTextSize(16f)
        renderer.setLabelsColor(Color.BLACK)
        renderer.setShowLegend(true)
        renderer.setInScroll(true)
        renderer.setPanEnabled(false)
        renderer.setZoomButtonsVisible(true)

        // fill the data
        val (pos, neg) = sumPerFilter(stat)
        fun construct(m: Map<String, Long>, colors: List<Int>) {
            m.forEach {
                data.add(it.key, it.value / 100.0)
                val r = SimpleSeriesRenderer()
                r.setColor(colors[(data.getItemCount() - 1) % colors.size])
                r.setChartValuesFormat(initNumFormat(stat))
                renderer.addSeriesRenderer(r)
            }
        }
        construct(pos, pos_colors)
        construct(neg, neg_colors)
        if (data.getItemCount() > 10) {
            renderer.setShowLabels(false)
        }
        return Pair(data, renderer)
    }

    // for lines chart
    private fun createLineXYDataSet(stat: Statistic): Pair<XYMultipleSeriesDataset, XYMultipleSeriesRenderer> {
        val data = XYMultipleSeriesDataset()
        val renderer = createXYMultipleSeriesRenderer(stat)
        renderer.setInScroll(true)
        renderer.setZoomButtonsVisible(true)

        fun createXYSeriesRenderer(colors: List<Int>): XYSeriesRenderer {
            val r = XYSeriesRenderer()
            r.setColor(colors[(data.getSeriesCount() - 1) % colors.size])
            r.setChartValuesFormat(initNumFormat(stat))
            r.setDisplayChartValues(false) // otherwise, crash in achartengine
            r.setPointStyle(PointStyle.CIRCLE)
            r.setFillPoints(true)
            r.setLineWidth(2f)
            return r
        }

        when (stat.filterType) {
            Statistic.NO_FILTER -> {
                val s = TimeSeries("")
                val (pos, neg) = partOps(stat)
                fun construct(m: Map<String, List<Operation>>, colors: List<Int>) {
                    m.forEach {
                        val v = Math.abs(it.value.fold(0L) {(i: Long, op: Operation) -> i + op.mSum }) + 0.0
                        renderer.setYAxisMax(Math.max(v + 1.0, renderer.getYAxisMax()))
                        renderer.addYTextLabel(v, Formater.getSumFormater().format(v))
                        s.add(it.value[0].getDateObj(), v)
                    }
                    data.addSeries(s)
                    renderer.addSeriesRenderer(createXYSeriesRenderer(colors))
                }
                construct(pos, pos_colors)
                construct(neg, neg_colors)
            }
            else -> {
                val (pos, neg) = partOps(stat)
                fun construct(m: Map<String, List<Operation>>, colors: List<Int>) {
                    m.forEach {
                        val s = TimeSeries(it.key)
                        it.value.forEach {
                            val v = Math.abs(it.mSum / 100.0)
                            renderer.setYAxisMax(Math.max(v + 1, renderer.getYAxisMax()))
                            renderer.addYTextLabel(v, it.getSumStr())
                            s.add(it.getDateObj(), v)
                        }
                        data.addSeries(s)
                        renderer.addSeriesRenderer(createXYSeriesRenderer(colors))
                    }
                }
                construct(pos, pos_colors)
                construct(neg, neg_colors)
            }
        }
        return Pair(data, renderer)
    }

    private fun filterName(stat: Statistic): String {
        val stId = when (stat.filterType) {
            Statistic.THIRD_PARTY -> R.string.third_parties
            Statistic.TAGS -> R.string.tags
            Statistic.MODE -> R.string.modes
            else -> R.string.time
        }
        return ctx.getString(stId)
    }

    // for bar and lines chart
    private fun createXYMultipleSeriesRenderer(stat: Statistic): XYMultipleSeriesRenderer {
        val renderer = XYMultipleSeriesRenderer()
        renderer.setBackgroundColor(ctx.getResources()?.getColor(android.R.color.transparent) ?: 0)
        renderer.setApplyBackgroundColor(true)
        renderer.setLabelsTextSize(18f)
        renderer.setMarginsColor(ctx.getResources()?.getColor(android.R.color.transparent) ?: 0)
        renderer.setYAxisMin(0.0)
        renderer.setMargins(intArray(0, 0, 0, 0)) // top, left, bottom, right
        renderer.setInScroll(true)
        renderer.setPanEnabled(true, false)
        renderer.setZoomButtonsVisible(true)

        when (stat.chartType) {
            Statistic.CHART_BAR -> {
                renderer.setShowLegend(false)
                renderer.setYLabelsAlign(Paint.Align.RIGHT)
                renderer.setYTitle(ctx.getString(R.string.sum))
                renderer.setXLabels(0)
                renderer.setYLabels(0)
                renderer.setXAxisMin(0.0)
                renderer.setXAxisMax(0.0)
                renderer.setBarWidth(70f)
                renderer.setBarSpacing(0.5)
                renderer.setXTitle(filterName(stat))
            }
            Statistic.CHART_LINE -> {
                renderer.setShowLegend(true)
                renderer.setPointSize(5f)
                renderer.setYLabelsAlign(Paint.Align.LEFT)
                renderer.setZoomEnabled(false)
            }
        }
        return renderer
    }

    // for bar chart
    private fun createBarXYDataSet(stat: Statistic): Pair<XYMultipleSeriesDataset, XYMultipleSeriesRenderer> {
        val data = XYMultipleSeriesDataset()
        val renderer = createXYMultipleSeriesRenderer(stat)
        var (tmpPos, tmpNeg) = sumPerFilter(stat)
        val pos: MutableMap<String, Long> = hashMapOf()
        pos.putAll(tmpPos)
        val neg: MutableMap<String, Long> = hashMapOf()
        neg.putAll(tmpNeg)
        pos.forEach {
            if (!neg.contains(it.key)) neg.put(it.key, 0L)
        }
        neg.forEach {
            if (!pos.contains(it.key)) pos.put(it.key, 0L)
        }

        var xLabels: SimpleArrayMap<String, Double> = SimpleArrayMap()
        fun construct(m: Map<String, Long>, isPos: Boolean) {
            val s2 = XYSeries("")
            m.forEach {
                val lbl = if (it.key.length == 0) ctx.getString(R.string.no_lbl) else it.key
                val v: Double = Math.abs(it.value / 100.0)
                renderer.setYAxisMax(Math.max(v, renderer.getYAxisMax()))

                val existingSeries: Double? = xLabels[lbl]
                if (existingSeries == null) {
                    val idx = data.getSeriesCount() * 0.5 + 0.5
                    renderer.addXTextLabel(idx, lbl)
                    xLabels.put(lbl, idx)
                    s2.add(idx, v)
                } else {
                    s2.add(existingSeries, v)
                }
                data.addSeries(s2)

                val r = XYSeriesRenderer()
                r.setColor(if (isPos) pos_colors[0] else neg_colors[0])
                r.setChartValuesFormat(initNumFormat(stat))
                r.setDisplayChartValues(true)
                r.setChartValuesTextSize(16f)
                renderer.addSeriesRenderer(r)
            }

            renderer.setXAxisMax(renderer.getXAxisMax() + 1)
        }
        construct(pos, isPos = true)
        construct(neg, isPos = false)
        renderer.setRange(doubleArray(0.0, renderer.getXAxisMax(), 0.0, renderer.getYAxisMax() * 1.1))
        return Pair(data, renderer)
    }

    private fun createChartView(stat: Statistic): GraphicalView =
            when (stat.chartType) {
                Statistic.CHART_PIE -> {
                    val (dataSet, renderer) = createCategorySeries(stat)
                    ChartFactory.getPieChartView(ctx, dataSet, renderer) as GraphicalView
                }
                Statistic.CHART_BAR -> {
                    val (dataSet, renderer) = createBarXYDataSet(stat)
                    ChartFactory.getBarChartView(ctx, dataSet, renderer, Type.STACKED) as GraphicalView
                }
                else -> {
                    //Statistic.CHART_LINE
                    val (dataSet, renderer) = createLineXYDataSet(stat)
                    val format = when (stat.timeScaleType) {
                        Statistic.PERIOD_DAYS, Statistic.PERIOD_ABSOLUTE -> "dd/MM/yy"
                        Statistic.PERIOD_MONTHES -> "MM/yy"
                        else -> "yy"
                    }
                    ChartFactory.getTimeChartView(ctx, dataSet, renderer, format) as GraphicalView
                }
            }

}