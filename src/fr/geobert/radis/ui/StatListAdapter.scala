package fr.geobert.radis.ui

import java.text.{DecimalFormat, ParsePosition, FieldPosition, NumberFormat}
import java.util.{Currency, Calendar, Date}

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.view.{View, ViewGroup}
import android.widget.{ArrayAdapter, ImageButton, LinearLayout, TextView}
import fr.geobert.radis.R
import fr.geobert.radis.data.{Account, Operation, Statistic}
import fr.geobert.radis.db.{AccountTable, OperationTable}
import fr.geobert.radis.tools.Tools
import org.achartengine.chart.BarChart
import org.achartengine.chart.BarChart.Type
import org.achartengine.model.{XYSeries, CategorySeries, XYMultipleSeriesDataset}
import org.achartengine.renderer.{SimpleSeriesRenderer, XYMultipleSeriesRenderer, DefaultRenderer}
import org.achartengine.{ChartFactory, GraphicalView}
import org.scaloid.common.Implicits

import scala.collection.JavaConversions._

class StatRowHolder(v: View) extends Implicits {
  val nameLbl: TextView = v.find(R.id.chart_name)
  val trashBtn: ImageButton = v.find(R.id.chart_delete)
  val editBtn: ImageButton = v.find(R.id.edit_chart)
  val graph: LinearLayout = v.find(R.id.chart)
}

object StatListAdapter {
  def apply(resource: Int)(implicit ctx: Context) = {
    new ArrayAdapter[Statistic](ctx, resource) with StatListAdapter
  }
}

trait StatListAdapter extends ArrayAdapter[Statistic] with Implicits {
  addAll(stubStatList().toIterable)

  def ctx = getContext.asInstanceOf[Activity]

  def stubStatList() = {
    val s1 = new Statistic
    s1.name = "s1"
    s1.accountId = 1
    s1.chartType = Statistic.CHART_PIE
    s1.filterType = Statistic.THIRD_PARTY
    s1.timeScaleType = Statistic.PERIOD_DAYS
    s1.xLast = 5
    val s2 = new Statistic
    s2.name = "s2"
    s2.accountId = 1
    s2.chartType = Statistic.CHART_BAR
    s2.filterType = Statistic.THIRD_PARTY
    s2.timeScaleType = Statistic.PERIOD_MONTHES
    s2.xLast = 5
    val s3 = new Statistic
    s3.name = "s3"
    s3.accountId = 1
    s3.chartType = Statistic.CHART_LINE
    s3.filterType = Statistic.TAGS
    s3.timeScaleType = Statistic.PERIOD_DAYS
    s3.xLast = 5
    s1 :: List()
  }


  private def createTimeRange(stat: Statistic): (Date, Date) = {
    def createXLastRange(field: Int) = {
      val endDate = Tools.createClearedCalendar()
      val startDate = Tools.createClearedCalendar()
      startDate.add(field, -stat.xLast)
      (startDate.getTime, endDate.getTime)
    }

    stat.timeScaleType match {
      case Statistic.PERIOD_DAYS =>
        createXLastRange(Calendar.DAY_OF_MONTH)
      case Statistic.PERIOD_MONTHES =>
        createXLastRange(Calendar.MONTH)
      case Statistic.PERIOD_YEARS =>
        createXLastRange(Calendar.YEAR)
      case Statistic.PERIOD_ABSOLUTE =>
        (stat.startDate, stat.endDate)
    }
  }

  private def sumPerFilter(stat: Statistic) = {
    val (startDate, endDate) = createTimeRange(stat)
    val ops = OperationTable.getOpsBetweenDate(ctx, startDate, endDate, stat.accountId).orm(c => new Operation(c))

    // partition the list according to filterType
    def partAndSum[K](f: (Operation) => K) = {
      ops.groupBy(f).mapValues(_.foldLeft(0l)((s: Long, o: Operation) => s + o.mSum))
    }
    stat.filterType match {
      case Statistic.THIRD_PARTY =>
        partAndSum(_.mThirdParty)
      case Statistic.TAGS =>
        partAndSum(_.mTag)
      case Statistic.MODE =>
        partAndSum(_.mMode)
    }
  }

  private def createCategorySeries(stat: Statistic): (CategorySeries, DefaultRenderer) = {
    /** Colors to be used for the pie slices. */
    val cursor = AccountTable.fetchAccount(ctx, stat.accountId)
    cursor.moveToFirst()
    val account = Account(cursor)
    val colors = List(Color.GREEN, Color.BLUE, Color.MAGENTA, Color.CYAN)
    val numFormat = NumberFormat.getCurrencyInstance
    numFormat.setCurrency(Currency.getInstance(account.currency))
    numFormat.setMinimumFractionDigits(2)

    val data = new CategorySeries("")
    val renderer = new DefaultRenderer
    renderer.setStartAngle(180)
    renderer.setDisplayValues(true)
    renderer.setLegendTextSize(16)
    renderer.setLabelsTextSize(16)
    renderer.setShowLegend(false)

    // fill the data
    sumPerFilter(stat).foreach((t: (String, Long)) => {
      data.add(t._1, t._2 / 100.0d)
      val r = new SimpleSeriesRenderer
      r.setColor(colors((data.getItemCount - 1) % colors.length))
      r.setChartValuesFormat(numFormat )
      renderer.addSeriesRenderer(r)
    })

    data -> renderer
  }

  private def createXYDataSet(stat: Statistic): (XYMultipleSeriesDataset, XYMultipleSeriesRenderer) = {
    val data = new XYMultipleSeriesDataset
    val renderer = new XYMultipleSeriesRenderer()
    sumPerFilter(stat).foreach((t: (String, Long)) => {
      val (catData, _) = createCategorySeries(stat)
      data.addSeries(catData.toXYSeries)
      // TODO renderer
    })

    data -> renderer
  }

  private def createChartView(stat: Statistic): GraphicalView = {
    stat.chartType match {
      case Statistic.CHART_PIE =>
        val (dataSet, renderer) = createCategorySeries(stat)
        ChartFactory.getPieChartView(ctx, dataSet, renderer)
      case Statistic.CHART_BAR =>
        val (dataSet, renderer) = createXYDataSet(stat)
        ChartFactory.getBarChartView(ctx, dataSet, renderer, Type.DEFAULT)
      case Statistic.CHART_LINE =>
        val (dataSet, renderer) = createXYDataSet(stat)
        ChartFactory.getLineChartView(ctx, dataSet, renderer)
    }
  }

  override def getView(position: Int, convertView: View, parent: ViewGroup): View = {
    var row = convertView

    if (row == null) {
      row = getContext.asInstanceOf[Activity].getLayoutInflater.inflate(R.layout.statistic_row, null)
      val h = new StatRowHolder(row)
      row.setTag(h)
    }

    val holder = row.getTag.asInstanceOf[StatRowHolder]
    val stat = getItem(position)
    holder.nameLbl.text = stat.name
    holder.trashBtn.onClick((_: View) => {
      // TODO
    })
    holder.editBtn.onClick((_: View) => {
      //StatisticEditor.callMeForResult(stat.id)
    })
    holder.graph.removeAllViews()
    val chart = createChartView(stat)
    holder.graph.addView(chart)
    row
  }

}
