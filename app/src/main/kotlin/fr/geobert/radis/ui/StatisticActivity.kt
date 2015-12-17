package fr.geobert.radis.ui

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.support.v4.util.SimpleArrayMap
import android.util.Log
import android.view.View
import android.widget.RelativeLayout
import android.widget.TextView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.BarLineChartBase
import com.github.mikephil.charting.charts.Chart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.BarLineScatterCandleBubbleData
import com.github.mikephil.charting.data.BarLineScatterCandleBubbleDataSet
import com.github.mikephil.charting.data.ChartData
import com.github.mikephil.charting.data.DataSet
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.formatter.DefaultValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import fr.geobert.radis.BaseActivity
import fr.geobert.radis.R
import fr.geobert.radis.data.Account
import fr.geobert.radis.data.Operation
import fr.geobert.radis.data.Statistic
import fr.geobert.radis.db.AccountTable
import fr.geobert.radis.db.OperationTable
import fr.geobert.radis.tools.formatDate
import fr.geobert.radis.tools.formatDateLong
import fr.geobert.radis.tools.map
import fr.geobert.radis.tools.plusMonth
import fr.geobert.radis.tools.plusYear
import hirondelle.date4j.DateTime
import java.text.DateFormatSymbols
import java.text.NumberFormat
import java.util.*

class StatisticActivity : BaseActivity() {
    companion object {
        val STAT = "statistic"
    }

    val accountNameLbl: TextView by lazy { findViewById(R.id.chart_account_name) as TextView }
    val filterLbl: TextView by lazy { findViewById(R.id.filter_lbl) as TextView }
    val timeScaleLbl: TextView by lazy { findViewById(R.id.time_scale_lbl) as TextView }
    val chartCont: RelativeLayout by lazy { findViewById(R.id.chart) as RelativeLayout }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val extras = intent.extras
        val stat = extras.getParcelable<Statistic>(STAT)
        setContentView(R.layout.statistic_activity)
        setTitle(stat.name)

        setIconOnClick(View.OnClickListener { onBackPressed() })
        mToolbar.menu.clear()
        setIcon(R.drawable.ok_48)

        accountNameLbl.text = stat.accountName
        filterLbl.text = getString(stat.getFilterStr())
        val (start, end) = stat.createTimeRange()
        timeScaleLbl.text = "${start.formatDateLong()} ${getString(R.string.rarr)} ${end.formatDateLong()}"

        val p = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT)
        try {
            val chart = createChartView(stat)

            chartCont.addView(chart, p)
            chart.invalidate()
        } catch(e: InputMismatchException) {
            val lbl = TextView(this)
            lbl.text = getString(R.string.no_statistic_data)
            chartCont.addView(lbl, p)
        }
    }

    // generate chart data

    private fun getColorsArray(id: Int): List<Int> =
            resources.getStringArray(id)?.map({ Color.parseColor(it) }) as List<Int>

    private val pos_colors: List<Int> by lazy(LazyThreadSafetyMode.NONE) { getColorsArray(R.array.positive_colors) }
    private val neg_colors: List<Int> by lazy(LazyThreadSafetyMode.NONE) { getColorsArray(R.array.negative_colors) }

    /**
     * get the ops list according to the time range, split by sum sign and group each by partFunc
     * @param stat the stat to analyse
     * @return a map with the group key and List(Operation)
     */
    private fun partOps(stat: Statistic): Pair<Map<String, List<Operation>>, Map<String, List<Operation>>> {
        val (startDate, endDate) = stat.createTimeRange()
        val ops = OperationTable.getOpsBetweenDate(this, startDate, endDate, stat.accountId).map({ Operation(it) })
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
                Statistic.THIRD_PARTY -> { o: Operation -> o.mThirdParty }
                Statistic.TAGS -> { o: Operation -> o.mTag }
                Statistic.MODE -> { o: Operation -> o.mMode }
                else -> { // Statistic.NO_FILTER
                    o: Operation ->
                    val g = GregorianCalendar()
                    g.timeInMillis = o.getDate()
                    when (stat.timePeriodType) {
                        Statistic.PERIOD_DAYS, Statistic.PERIOD_ABSOLUTE -> g.time.formatDate()
                        Statistic.PERIOD_MONTHES ->
                            if (Build.VERSION.SDK_INT >= 9) {
                                g.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault()) ?: ""
                            } else {
                                DateFormatSymbols().shortMonths?.get(g[Calendar.MONTH]) ?: ""
                            }
                        Statistic.PERIOD_YEARS -> g[Calendar.YEAR].toString()
                        else -> {
                            ""
                        }
                    }
                }
            }


    // group sums that represents less than 10% of the total in a "misc" category
    private fun cleanMap(m: Map<String, Long>, total: Long): Map<String, Long> {
        val limit = Math.abs(total * 0.01)
        val result: MutableMap<String, Long> = hashMapOf()
        val miscKey = getString(R.string.misc_chart_cat)
        m.forEach {
            Log.d("StatisticListFragment", "key: ${it.key} / value: ${it.value} / limit: $limit / total: $total / m.size: ${m.size}")
            if (Math.abs(it.value) < limit) {
                val p = result[miscKey]
                if (p != null) {
                    result.put(miscKey, p + it.value)
                } else {
                    result.put(miscKey, it.value)
                }
            } else {
                result.put(it.key, it.value)
            }
        }
        return result
    }

    private fun sumPerFilter(stat: Statistic): Pair<Map<String, Long>, Map<String, Long>> {
        // partition the list according to filterType
        fun sumMapOfList(m: Map<String, List<Operation>>) = m.mapValues { it.value.fold(0L) { s: Long, o: Operation -> s + o.mSum } }

        fun sumMap(m: Map<String, Long>) = m.values.fold(0L) { s: Long, l: Long -> s + l }
        val (pos, neg) = partOps(stat)
        val sumP = sumMapOfList(pos)
        val sumN = sumMapOfList(neg)
        val total = sumMap(sumP) + Math.abs(sumMap(sumN))
        return Pair(cleanMap(sumP, total), cleanMap(sumN, total))
    }

    private fun initNumFormat(stat: Statistic): NumberFormat {
        val cursor = AccountTable.fetchAccount(this, stat.accountId)
        cursor.moveToFirst()
        val account = Account(cursor)
        val numFormat = NumberFormat.getCurrencyInstance()
        numFormat.currency = Currency.getInstance(account.currency)
        numFormat.minimumFractionDigits = 2
        return numFormat
    }

    // for pie chart
    private fun createPieData(stat: Statistic): PieData {
        val yValues = ArrayList<Entry>()
        val xValues = ArrayList<String>()

        // fill the data
        val (pos, neg) = sumPerFilter(stat)
        var i: Int = 0
        val cols = ArrayList<Int>()
        fun construct(m: Map<String, Long>, colors: List<Int>) {
            m.forEach {
                val k = if (it.key.length > 0) it.key else getString(when (stat.filterType) {
                    1 -> R.string.no_tag
                    2 -> R.string.no_mode
                    else -> throw IllegalArgumentException("No values for filter only happens for tag and mode")
                }).format(getString(if (it.value > 0) R.string.pos_values else R.string.neg_values))
                xValues.add(k)
                yValues.add(Entry(Math.abs(it.value) / 100.0f, i++))
                cols.add(colors[(yValues.size - 1) % colors.size])

            }
        }
        construct(pos, pos_colors)
        construct(neg, neg_colors)
        val set = PieDataSet(yValues, "")

        for (c in ColorTemplate.COLORFUL_COLORS) cols.add(c)
        set.colors = cols
        val d = PieData(xValues, set)
        d.setValueFormatter(DefaultValueFormatter(2))
        return d
    }

    // for lines chart
    private fun createLineData(stat: Statistic): LineData {
        val (pos, neg) = partOps(stat)
        val data = LineData()
        fun createDataSet(yVals: ArrayList<Entry>, name: String, color: Int): LineDataSet {
            val dataSet = LineDataSet(yVals, name)
            dataSet.setDrawValues(false)
            dataSet.color = color
            dataSet.setDrawCircleHole(false)
            dataSet.setCircleColor(color)
            dataSet.circleSize = 3.0f
            return dataSet
        }
        return when (stat.filterType) {
            Statistic.NO_FILTER -> {
                fun construct(m: Map<String, List<Operation>>, colors: List<Int>, name: String) {
                    var i = 0
                    val yVals = ArrayList<Entry>()
                    m.toSortedMap().forEach {
                        val v = Math.abs(it.value.fold(0L) { i: Long, op: Operation ->
                            i + op.mSum
                        }) + 0.0f
                        yVals.add(Entry(v / 100.0f, i))
                        i += 1
                        data.addXValue(it.key)
                    }

                    val dataSet = createDataSet(yVals, name, colors[0])
                    data.addDataSet(dataSet)
                }
                construct(pos, pos_colors, getString(R.string.positive_values))
                construct(neg, neg_colors, getString(R.string.negative_values))
                data
            }
            else -> {
                fun findMinMax(): Pair<DateTime?, DateTime?> {
                    val posList = pos.flatMap { e -> e.value }
                    val negList = neg.flatMap { e -> e.value }

                    val posMin: DateTime? = posList.minBy { it.mDate }?.mDate
                    val negMin: DateTime? = negList.minBy { it.mDate }?.mDate
                    val posMax: DateTime? = posList.maxBy { it.mDate }?.mDate
                    val negMax: DateTime? = negList.maxBy { it.mDate }?.mDate

                    return Pair(if ((posMin?.compareTo(negMin) ?: 0) < 0) posMin else negMin,
                            if ((posMax?.compareTo(negMax) ?: 0) > 0) posMax else negMax)
                }

                fun addPeriodicity(stat: Statistic, date: DateTime): DateTime {
                    return when (stat.timeScaleType) {
                        Statistic.PERIOD_DAYS -> {
                            date.plusDays(1)
                        }
                        Statistic.PERIOD_MONTHES -> {
                            date.plusMonth(1)
                        }
                        Statistic.PERIOD_YEARS -> {
                            date.plusYear(1)
                        }
                        else -> {
                            throw IllegalArgumentException("Time scale should be day, month or year only")
                        }
                    }
                }

                val (minDate, maxDate) = findMinMax()

                if (minDate != null && maxDate != null) {
                    var min: DateTime = minDate
                    while (min.compareTo(maxDate) < 0) {
                        data.addXValue(min.formatDateLong())
                        min = addPeriodicity(stat, min)
                    }
                } else {
                    throw InputMismatchException("No data to display")
                }

                fun construct(m: Map<String, List<Operation>>, colors: List<Int>) {
                    var i = 0
                    m.forEach {
                        if (it.key.length > 0) {
                            val yVals = ArrayList<Entry>()
                            it.value.toSortedSet().forEach {
                                val v = Math.abs(it.mSum / 100.0f)
                                val idx = data.xVals.indexOf(it.mDate.formatDateLong())
                                yVals.add(Entry(v, idx))
                            }
                            val s = createDataSet(yVals, it.key, colors[i % colors.count()])
                            i += 1
                            data.addDataSet(s)
                        }
                    }
                }
                construct(pos, pos_colors)
                construct(neg, neg_colors)
                data
            }
        }
    }

    private fun filterName(stat: Statistic): String {
        val stId = when (stat.filterType) {
            Statistic.THIRD_PARTY -> R.string.third_parties
            Statistic.TAGS -> R.string.tags
            Statistic.MODE -> R.string.modes
            else -> R.string.time
        }
        return getString(stId)
    }

    // for bar and lines chart
    private fun createXYMultipleSeriesRenderer(stat: Statistic): BarData? {
        //        val renderer = XYMultipleSeriesRenderer()
        //        renderer.backgroundColor = ctx.resources?.getColor(android.R.color.transparent) ?: 0
        //        renderer.isApplyBackgroundColor = true
        //        renderer.labelsTextSize = 18f
        //        renderer.marginsColor = ctx.resources?.getColor(android.R.color.transparent) ?: 0
        //        renderer.yAxisMin = 0.0
        //        renderer.margins = intArrayOf(0, 0, 0, 0) // top, left, bottom, right
        //        renderer.isInScroll = false
        //        renderer.setPanEnabled(true, false)
        //        renderer.isZoomButtonsVisible = ZOOM_ENABLED
        //
        //        when (stat.chartType) {
        //            Statistic.CHART_BAR -> {
        //                renderer.isShowLegend = false
        //                renderer.setYLabelsAlign(Paint.Align.RIGHT)
        //                renderer.yTitle = ctx.getString(R.string.sum)
        //                renderer.xLabels = 0
        //                renderer.yLabels = 0
        //                renderer.xAxisMin = 0.0
        //                renderer.xAxisMax = 0.0
        //                renderer.barWidth = 70f
        //                renderer.barSpacing = 0.5
        //                renderer.xTitle = filterName(stat)
        //            }
        //            Statistic.CHART_LINE -> {
        //                renderer.isShowLegend = true
        //                renderer.pointSize = 5f
        //                renderer.setYLabelsAlign(Paint.Align.LEFT)
        //                renderer.isZoomEnabled = false
        //            }
        //        }
        //        return renderer
        return null
    }

    // for bar chart
    private fun createBarData(stat: Statistic): BarData {
        val xValues = ArrayList<String>()
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

        var xLabels: SimpleArrayMap<String, Int> = SimpleArrayMap()
        fun construct(m: Map<String, Long>, isPos: Boolean): BarDataSet {
            val yValues = ArrayList<BarEntry>()
            val colors = ArrayList<Int>()
            m.forEach {
                val lbl = if (it.key.length == 0) getString(R.string.no_lbl) else it.key
                val v: Float = Math.abs(it.value / 100.0f)

                val existingSeries: Int? = xLabels[lbl]
                if (existingSeries == null) {
                    val idx = yValues.count()
                    xLabels.put(lbl, idx)
                    xValues.add(lbl)
                    yValues.add(BarEntry(v, idx))
                } else {
                    yValues.add(BarEntry(v, existingSeries))
                }

                colors.add(if (isPos) pos_colors[0] else neg_colors[0])
            }
            val set = BarDataSet(yValues, "")
            set.colors = colors
            return set
        }

        val sets = listOf(construct(pos, isPos = true),
                construct(neg, isPos = false))
        return BarData(xValues, sets)
    }

    fun createChartView(stat: Statistic): Chart<out ChartData<out DataSet<out Entry>>> {
        fun lineAndBarConfig(c: BarLineChartBase<out BarLineScatterCandleBubbleData<out BarLineScatterCandleBubbleDataSet<out Entry>>>) {
            c.setPinchZoom(true)
            c.isDragEnabled = true
            c.isScaleXEnabled = true
            c.setTouchEnabled(true)
            c.setDescription("")
            c.legend.textColor = Color.WHITE
            c.setGridBackgroundColor(Color.TRANSPARENT)
            c.axisLeft.textColor = Color.WHITE
            c.axisRight.isEnabled = false
            c.xAxis.position = XAxis.XAxisPosition.BOTTOM
            c.xAxis.textColor = Color.WHITE
        }

        val chart = when (stat.chartType) {
            Statistic.CHART_PIE -> {
                val c = PieChart(this)
                c.data = createPieData(stat)
                c.setHoleColor(resources.getColor(R.color.normal_bg))
                c.legend.textColor = Color.WHITE
                c
            }
            Statistic.CHART_BAR -> {
                val c = BarChart(this)
                c.data = createBarData(stat)
                lineAndBarConfig(c)
                c.legend.isEnabled = false
                c
            }
            Statistic.CHART_LINE -> {
                val c = LineChart(this)
                c.data = createLineData(stat)
                lineAndBarConfig(c)
                c.legend.isWordWrapEnabled = true
                c
            }
            else -> {
                throw IllegalArgumentException("Unknown chart type")
            }
        }
        chart.setBackgroundColor(Color.TRANSPARENT)


        return chart
    }
}
