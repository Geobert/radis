package fr.geobert.radis.ui

import android.app.Activity
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.support.v4.app.LoaderManager.LoaderCallbacks
import android.support.v4.content.CursorLoader
import android.support.v4.content.Loader
import android.support.v4.util.SimpleArrayMap
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import fr.geobert.radis.BaseFragment
import fr.geobert.radis.R
import fr.geobert.radis.data.Account
import fr.geobert.radis.data.Operation
import fr.geobert.radis.data.Statistic
import fr.geobert.radis.db.AccountTable
import fr.geobert.radis.db.DbContentProvider
import fr.geobert.radis.db.OperationTable
import fr.geobert.radis.db.StatisticTable
import fr.geobert.radis.service.OnRefreshReceiver
import fr.geobert.radis.tools.*
import fr.geobert.radis.ui.adapter.StatisticAdapter
import fr.geobert.radis.ui.editor.StatisticEditor
import org.achartengine.ChartFactory
import org.achartengine.chart.BarChart.Type
import org.achartengine.chart.PointStyle
import org.achartengine.model.CategorySeries
import org.achartengine.model.TimeSeries
import org.achartengine.model.XYMultipleSeriesDataset
import org.achartengine.model.XYSeries
import org.achartengine.renderer.DefaultRenderer
import org.achartengine.renderer.SimpleSeriesRenderer
import org.achartengine.renderer.XYMultipleSeriesRenderer
import org.achartengine.renderer.XYSeriesRenderer
import java.text.DateFormatSymbols
import java.text.NumberFormat
import java.util.Calendar
import java.util.Currency
import java.util.GregorianCalendar
import java.util.Locale
import kotlin.properties.Delegates

class StatisticsListFragment : BaseFragment(), LoaderCallbacks<Cursor> {
    val ctx: FragmentActivity by lazy(LazyThreadSafetyMode.NONE) { activity }
    private val STAT_LOADER = 2000
    private var mContainer: View by Delegates.notNull()
    private var mList: RecyclerView by Delegates.notNull()
    private val mEmptyView: View by lazy(LazyThreadSafetyMode.NONE) { mContainer.findViewById(R.id.empty_textview) }
    private var mAdapter: StatisticAdapter? = null
    private var mLoader: Loader<Cursor>? = null
    private val ZOOM_ENABLED = true
    private val mOnRefreshReceiver by lazy(LazyThreadSafetyMode.NONE) { OnRefreshReceiver(this) }

    override fun setupIcon() = setIcon(R.drawable.stat_48)

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)

        val v = inflater?.inflate(R.layout.statistics_list_fragment, container, false) as View
        mContainer = v
        mList = v.findViewById(R.id.operation_list) as RecyclerView
        mList.layoutManager = android.support.v7.widget.LinearLayoutManager(activity)
        mList.setHasFixedSize(true)
        mList.itemAnimator = DefaultItemAnimator()

        setupIcon()
        setMenu(R.menu.operations_list_menu)

        mActivity.registerReceiver(mOnRefreshReceiver, IntentFilter(Tools.INTENT_REFRESH_STAT))
        return v
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mActivity.unregisterReceiver(mOnRefreshReceiver)
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean =
            when (item?.itemId) {
                R.id.create_operation -> {
                    StatisticEditor.callMeForResult(ctx)
                    true
                }
                else ->
                    false
            }

    override fun onResume(): Unit {
        super.onResume()
        fetchStats()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            fetchStats()
        }
    }

    override fun onOperationEditorResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            fetchStats()
        }
    }

    private fun fetchStats() {
        when (mLoader) {
            null ->
                ctx.supportLoaderManager?.initLoader(2000, Bundle(), this)
            else ->
                ctx.supportLoaderManager?.restartLoader(2000, Bundle(), this)
        }
    }

    override fun updateDisplay(intent: Intent?) {
        fetchStats()
    }

    override fun onCreateLoader(p1: Int, p2: Bundle?): Loader<Cursor>? {
        mLoader = CursorLoader(ctx, DbContentProvider.STATS_URI, StatisticTable.STAT_COLS, null, null, null)
        return mLoader
    }

    override fun onLoaderReset(p1: Loader<Cursor>?): Unit {
        mLoader?.reset()
    }

    override fun onLoadFinished(loader: Loader<Cursor>?, cursor: Cursor): Unit {
        if (mAdapter == null) {
            mAdapter = StatisticAdapter(cursor, this)
            mList.adapter = mAdapter
        } else {
            mAdapter?.swapCursor(cursor)
        }
        if (mAdapter?.itemCount == 0) {
            mList.visibility = View.GONE
            mEmptyView.visibility = View.VISIBLE
        } else {
            mList.visibility = View.VISIBLE
            mEmptyView.visibility = View.GONE
        }
    }

    // generate chart data

    private fun getColorsArray(id: Int): List<Int> =
            ctx.resources?.getStringArray(id)?.map({ Color.parseColor(it) }) as List<Int>

    private val pos_colors: List<Int> by lazy(LazyThreadSafetyMode.NONE) { getColorsArray(R.array.positive_colors) }
    private val neg_colors: List<Int> by lazy(LazyThreadSafetyMode.NONE) { getColorsArray(R.array.negative_colors) }

    /**
     * get the ops list according to the time range, split by sum sign and group each by partFunc
     * @param stat the stat to analyse
     * @return a map with the group key and List(Operation)
     */
    private fun partOps(stat: Statistic): Pair<Map<String, List<Operation>>, Map<String, List<Operation>>> {
        val (startDate, endDate) = stat.createTimeRange()
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
                Statistic.THIRD_PARTY -> { o: Operation -> o.mThirdParty }
                Statistic.TAGS -> { o: Operation -> o.mTag }
                Statistic.MODE -> { o: Operation -> o.mMode }
                else -> { // Statistic.NO_FILTER
                    o: Operation ->
                    val g = GregorianCalendar()
                    g.timeInMillis = o.getDate()
                    when (stat.timeScaleType) {
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
        val limit = Math.abs(total * 0.05)
        val result: MutableMap<String, Long> = hashMapOf()
        val miscKey = ctx.getString(R.string.misc_chart_cat)
        m.forEach {
            Log.d("StatisticListFragment", "key: ${it.key} / value: ${it.value} / limit: $limit / total: $total / m.size: ${m.size()}")
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

        fun sumMap(m: Map<String, Long>) = m.values().fold(0L) { s: Long, l: Long -> s + l }
        val (pos, neg) = partOps(stat)
        val sumP = sumMapOfList(pos)
        val sumN = sumMapOfList(neg)
        val total = sumMap(sumP) + Math.abs(sumMap(sumN))
        return Pair(cleanMap(sumP, total), cleanMap(sumN, total))
    }

    private fun initNumFormat(stat: Statistic): NumberFormat {
        val cursor = AccountTable.fetchAccount(ctx, stat.accountId)
        cursor.moveToFirst()
        val account = Account(cursor)
        val numFormat = NumberFormat.getCurrencyInstance()
        numFormat.currency = Currency.getInstance(account.currency)
        numFormat.minimumFractionDigits = 2
        return numFormat
    }

    // for pie chart
    private fun createCategorySeries(stat: Statistic): Pair<CategorySeries, DefaultRenderer> {
        val data = CategorySeries("")
        val renderer = DefaultRenderer()
        renderer.startAngle = 180f
        renderer.isDisplayValues = true
        renderer.legendTextSize = 16f
        renderer.labelsTextSize = 16f
        renderer.labelsColor = Color.WHITE
        renderer.isShowLegend = true
        renderer.isInScroll = false
        renderer.isPanEnabled = true
        renderer.isZoomButtonsVisible = ZOOM_ENABLED

        // fill the data
        val (pos, neg) = sumPerFilter(stat)
        fun construct(m: Map<String, Long>, colors: List<Int>) {
            m.forEach {
                data.add(it.key, Math.abs(it.value) / 100.0)
                val r = SimpleSeriesRenderer()
                r.color = colors[(data.itemCount - 1) % colors.size()]
                r.chartValuesFormat = initNumFormat(stat)
                renderer.addSeriesRenderer(r)
            }
        }
        construct(pos, pos_colors)
        construct(neg, neg_colors)
        if (data.itemCount > 10) {
            renderer.isShowLabels = false
        }
        return Pair(data, renderer)
    }

    // for lines chart
    private fun createLineXYDataSet(stat: Statistic): Pair<XYMultipleSeriesDataset, XYMultipleSeriesRenderer> {
        val data = XYMultipleSeriesDataset()
        val renderer = createXYMultipleSeriesRenderer(stat)
        renderer.isInScroll = false
        renderer.isZoomButtonsVisible = ZOOM_ENABLED

        fun createXYSeriesRenderer(colors: List<Int>): XYSeriesRenderer {
            val r = XYSeriesRenderer()
            r.color = colors[(data.seriesCount - 1) % colors.size()]
            r.chartValuesFormat = initNumFormat(stat)
            r.isDisplayChartValues = false // otherwise, crash in achartengine
            r.pointStyle = PointStyle.CIRCLE
            r.isFillPoints = true
            r.lineWidth = 2f
            return r
        }

        val (pos, neg) = partOps(stat)
        when (stat.filterType) {
            Statistic.NO_FILTER -> {
                val s = TimeSeries("")
                fun construct(m: Map<String, List<Operation>>, colors: List<Int>) {
                    m.forEach {
                        val v = Math.abs(it.value.fold(0L) { i: Long, op: Operation -> i + op.mSum }) + 0.0
                        renderer.yAxisMax = Math.max(v + 1.0, renderer.yAxisMax)
                        renderer.addYTextLabel(v, v.formatSum())
                        s.add(it.value[0].getDateObj(), v)
                    }
                    data.addSeries(s)
                    renderer.addSeriesRenderer(createXYSeriesRenderer(colors))
                }
                construct(pos, pos_colors)
                construct(neg, neg_colors)
            }
            else -> {
                fun construct(m: Map<String, List<Operation>>, colors: List<Int>) {
                    m.forEach {
                        val s = TimeSeries(it.key)
                        it.value.forEach {
                            val v = Math.abs(it.mSum / 100.0)
                            renderer.yAxisMax = Math.max(v + 1, renderer.yAxisMax)
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
        renderer.backgroundColor = ctx.resources?.getColor(android.R.color.transparent) ?: 0
        renderer.isApplyBackgroundColor = true
        renderer.labelsTextSize = 18f
        renderer.marginsColor = ctx.resources?.getColor(android.R.color.transparent) ?: 0
        renderer.yAxisMin = 0.0
        renderer.margins = intArrayOf(0, 0, 0, 0) // top, left, bottom, right
        renderer.isInScroll = false
        renderer.setPanEnabled(true, false)
        renderer.isZoomButtonsVisible = ZOOM_ENABLED

        when (stat.chartType) {
            Statistic.CHART_BAR -> {
                renderer.isShowLegend = false
                renderer.setYLabelsAlign(Paint.Align.RIGHT)
                renderer.yTitle = ctx.getString(R.string.sum)
                renderer.xLabels = 0
                renderer.yLabels = 0
                renderer.xAxisMin = 0.0
                renderer.xAxisMax = 0.0
                renderer.barWidth = 70f
                renderer.barSpacing = 0.5
                renderer.xTitle = filterName(stat)
            }
            Statistic.CHART_LINE -> {
                renderer.isShowLegend = true
                renderer.pointSize = 5f
                renderer.setYLabelsAlign(Paint.Align.LEFT)
                renderer.isZoomEnabled = false
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
                val lbl = if (it.key.length() == 0) ctx.getString(R.string.no_lbl) else it.key
                val v: Double = Math.abs(it.value / 100.0)
                renderer.yAxisMax = Math.max(v, renderer.yAxisMax)

                val existingSeries: Double? = xLabels[lbl]
                if (existingSeries == null) {
                    val idx = data.seriesCount * 0.5 + 0.5
                    renderer.addXTextLabel(idx, lbl)
                    xLabels.put(lbl, idx)
                    s2.add(idx, v)
                } else {
                    s2.add(existingSeries, v)
                }
                data.addSeries(s2)

                val r = XYSeriesRenderer()
                r.color = if (isPos) pos_colors[0] else neg_colors[0]
                r.chartValuesFormat = initNumFormat(stat)
                r.isDisplayChartValues = true
                r.chartValuesTextSize = 16f
                renderer.addSeriesRenderer(r)
            }

            renderer.xAxisMax = renderer.xAxisMax + 1
        }
        construct(pos, isPos = true)
        construct(neg, isPos = false)
        renderer.setRange(doubleArrayOf(0.0, renderer.xAxisMax, 0.0, renderer.yAxisMax * 1.1))
        return Pair(data, renderer)
    }

    fun createChartView(stat: Statistic): Intent? {
        val intent = when (stat.chartType) {
            Statistic.CHART_PIE -> {
                val (dataSet, renderer) = createCategorySeries(stat)
                //                    ChartFactory.getPieChartView(ctx, dataSet, renderer) as GraphicalView
                ChartFactory.getPieChartIntent(ctx, dataSet, renderer, stat.name).setClass(ctx, StatisticActivity::class.java)
            }
            Statistic.CHART_BAR -> {
                val (dataSet, renderer) = createBarXYDataSet(stat)
                ChartFactory.getBarChartIntent(ctx, dataSet, renderer, Type.STACKED, stat.name).setClass(ctx, StatisticActivity::class.java)
            }
            else -> {
                //Statistic.CHART_LINE
                val (dataSet, renderer) = createLineXYDataSet(stat)
                val format = when (stat.timeScaleType) {
                    Statistic.PERIOD_DAYS, Statistic.PERIOD_ABSOLUTE -> "dd/MM/yy"
                    Statistic.PERIOD_MONTHES -> "MM/yy"
                    else -> "yy"
                }
                ChartFactory.getTimeChartIntent(ctx, dataSet, renderer, format, stat.name).setClass(ctx, StatisticActivity::class.java)
            }
        }
        intent.putExtra(StatisticActivity.ACCOUNT_NAME, stat.accountName)
        intent.putExtra(StatisticActivity.FILTER, ctx.getString(stat.getFilterStr()))
        val (start, end) = stat.createTimeRange()
        val time = "${start.formatDateLong()} ${ctx.getString(R.string.rarr)} ${end.formatDateLong()}"
        intent.putExtra(StatisticActivity.TIME_SCALE, time)
        return intent
    }
}
