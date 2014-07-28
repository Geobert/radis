package fr.geobert.radis.ui

import java.text._
import java.util.{Calendar, Currency, Date, GregorianCalendar, Locale}

import android.app.Activity
import android.content.Context
import android.database.Cursor
import android.graphics.{Color, Paint}
import android.os.Build
import android.support.v4.widget.{SimpleCursorAdapter, CursorAdapter}
import android.view.{View, ViewGroup}
import android.widget._
import fr.geobert.radis.R
import fr.geobert.radis.data.{Account, Operation, Statistic}
import fr.geobert.radis.db.{StatisticTable, AccountTable, OperationTable}
import fr.geobert.radis.tools.{Formater, Tools}
import org.achartengine.chart.BarChart.Type
import org.achartengine.chart.PointStyle
import org.achartengine.model.{CategorySeries, TimeSeries, XYMultipleSeriesDataset, XYSeries}
import org.achartengine.renderer.{DefaultRenderer, SimpleSeriesRenderer, XYMultipleSeriesRenderer, XYSeriesRenderer}
import org.achartengine.{ChartFactory, GraphicalView}
import org.scaloid.common.Implicits

import scala.collection.JavaConversions._

class StatRowHolder(v: View) extends Implicits {
  val nameLbl: TextView = v.find(R.id.chart_name)
  val accountNameLbl: TextView = v.find(R.id.chart_account_name)
  val trashBtn: ImageButton = v.find(R.id.chart_delete)
  val editBtn: ImageButton = v.find(R.id.edit_chart)
  val graph: LinearLayout = v.find(R.id.chart)
}

object StatListAdapter {
  def apply(cursor: Cursor)(implicit ctx: Context) = {
    val a = new SimpleCursorAdapter(ctx, R.layout.statistic_row, cursor,
      Array(StatisticTable.KEY_STAT_NAME, StatisticTable.KEY_STAT_ACCOUNT_NAME),
      Array(R.id.chart_name, R.id.chart_account_name),
      CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER) with StatListAdapter
    a.ctx = ctx
    a
  }
}

trait StatListAdapter extends SimpleCursorAdapter with Implicits {
  private def getColorsArray(id: Int) = ctx.getResources.getStringArray(id).map((s) => Color.parseColor(s))

  private lazy val pos_colors: Array[Int] = getColorsArray(R.array.positive_colors)
  private lazy val neg_colors: Array[Int] = getColorsArray(R.array.negative_colors)

  var ctx: Context = _

  class StatViewBinder extends SimpleCursorAdapter.ViewBinder {
    override def setViewValue(v: View, c: Cursor, colIdx: Int): Boolean = {
      val stat = Statistic(c)
      c.getColumnName(colIdx) match {
        case StatisticTable.KEY_STAT_NAME =>
          v.asInstanceOf[TextView].text = stat.name
        case StatisticTable.KEY_STAT_ACCOUNT_NAME =>
          v.asInstanceOf[TextView].text = stat.accountName
          val holder = v.getParent.asInstanceOf[View].getTag.asInstanceOf[StatRowHolder]
          holder.trashBtn.onClick((_: View) => {
            // TODO
          })
          holder.editBtn.onClick((_: View) => {
            //StatisticEditor.callMeForResult(stat.id)
          })
          holder.graph.removeAllViews()
          val chart = createChartView(stat)
          chart.setBackgroundColor(ctx.getResources.getColor(android.R.color.transparent))
          holder.graph.addView(chart)
      }
      true
    }
  }

  setViewBinder(new StatViewBinder)

  override def newView(p1: Context, p2: Cursor, p3: ViewGroup): View = {
    val row = super.newView(p1, p2, p3)
    val h = new StatRowHolder(row)
    row.setTag(h)
    row
  }

  /**
   * create a time range according to statistic's configuration
   * @param stat
   * @return (startDate, endDate)
   */
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

  /**
   * get the ops list according to the time range and group by partFunc
   * @param stat the stat to analyse
   * @return a map with the group key and List(Operation)
   */
  private def partOps(stat: Statistic): (Map[String, List[Operation]], Map[String, List[Operation]]) = {
    val (startDate, endDate) = createTimeRange(stat)
    val ops = OperationTable.getOpsBetweenDate(ctx, startDate, endDate, stat.accountId).orm(c => new Operation(c))
    val (pos, neg) = ops.partition(o => o.mSum > 0)
    (pos.groupBy(partFunc(stat)), neg.groupBy(partFunc(stat)))
  }

  /**
   * get the partitioning function according to filterType
   * @param stat the stat to analyse
   * @return the partitioning function
   */
  private def partFunc(stat: Statistic): (Operation) => String = {
    stat.filterType match {
      case Statistic.THIRD_PARTY => {
        _.mThirdParty
      }
      case Statistic.TAGS => {
        _.mTag
      }
      case Statistic.MODE => {
        _.mMode
      }
      case Statistic.NO_FILTER =>
        (o: Operation) => {
          val g = new GregorianCalendar()
          g.setTimeInMillis(o.getDate)
          stat.timeScaleType match {
            case Statistic.PERIOD_DAYS | Statistic.PERIOD_ABSOLUTE =>
              Formater.getFullDateFormater.format(g.getTime)
            case Statistic.PERIOD_MONTHES =>
              if (Build.VERSION.SDK_INT >= 9) {
                g.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault)
              } else {
                new DateFormatSymbols().getShortMonths()(g.get(Calendar.MONTH))
              }
            case Statistic.PERIOD_YEARS =>
              g.get(Calendar.YEAR).toString
          }
        }
    }
  }

  private def sumPerFilter(stat: Statistic): (Map[String, Long], Map[String, Long]) = {
    // partition the list according to filterType
    //partOps(stat).mapValues(_.foldLeft(0l)((s: Long, o: Operation) => s + o.mSum))
    def sumMap(m: Map[String, List[Operation]]) = m.mapValues(_.foldLeft(0l)((s: Long, o: Operation) => s + o.mSum))
    val (pos, neg) = partOps(stat)
    (sumMap(pos), sumMap(neg))
  }

  private def initNumFormat(stat: Statistic): NumberFormat = {
    val cursor = AccountTable.fetchAccount(ctx, stat.accountId)
    cursor.moveToFirst()
    val account = Account(cursor)
    val numFormat = NumberFormat.getCurrencyInstance
    numFormat.setCurrency(Currency.getInstance(account.currency))
    numFormat.setMinimumFractionDigits(2)
    numFormat
  }

  // for pie chart
  private def createCategorySeries(stat: Statistic): (CategorySeries, DefaultRenderer) = {
    val data = new CategorySeries("")
    val renderer = new DefaultRenderer
    renderer.setStartAngle(180)
    renderer.setDisplayValues(true)
    renderer.setLegendTextSize(16)
    renderer.setLabelsTextSize(16)
    renderer.setLabelsColor(Color.BLACK)
    renderer.setShowLegend(true)

    // fill the data
    val (pos, neg) = sumPerFilter(stat)
    def construct(m: Map[String, Long], colors: Array[Int]): Unit = {
      m.foreach((t: (String, Long)) => {
        data.add(t._1, t._2 / 100.0d)
        val r = new SimpleSeriesRenderer
        r.setColor(colors((data.getItemCount - 1) % colors.length))
        r.setChartValuesFormat(initNumFormat(stat))
        renderer.addSeriesRenderer(r)
      })
    }
    construct(pos, pos_colors)
    construct(neg, neg_colors)
    data -> renderer
  }

  // for lines chart
  private def createLineXYDataSet(stat: Statistic): (XYMultipleSeriesDataset, XYMultipleSeriesRenderer) = {
    val data = new XYMultipleSeriesDataset
    val renderer = createXYMultipleSeriesRenderer(stat)
    def createXYSeriesRenderer(colors: Array[Int]): XYSeriesRenderer = {
      val r = new XYSeriesRenderer
      r.setColor(colors((data.getSeriesCount - 1) % colors.length))
      r.setChartValuesFormat(initNumFormat(stat))
      r.setDisplayChartValues(false) // otherwise, crash in achartengine
      r.setPointStyle(PointStyle.CIRCLE)
      r.setFillPoints(true)
      r.setLineWidth(2)
      r
    }

    stat.filterType match {
      case Statistic.NO_FILTER =>
        val s = new TimeSeries("") // TODO account name ?
      val (pos, neg) = partOps(stat)
        def construct(m: Map[String, List[Operation]], colors: Array[Int]): Unit = {
          m.foreach((t: (String, List[Operation])) => {
            val v = math.abs(t._2.foldLeft(0l)((i: Long, op: Operation) => {
              i + op.mSum
            }))
            renderer.setYAxisMax(math.max(v + 1, renderer.getYAxisMax))
            renderer.addYTextLabel(v, Formater.getSumFormater.format(v))
            s.add(t._2(0).getDateObj, v)
          })
          data.addSeries(s)
          renderer.addSeriesRenderer(createXYSeriesRenderer(colors))
        }
        construct(pos, pos_colors)
        construct(neg, neg_colors)
      case _ =>
        val (pos, neg) = partOps(stat)
        def construct(m: Map[String, List[Operation]], colors: Array[Int]): Unit = {
          m.foreach((t: (String, List[Operation])) => {
            val s = new TimeSeries(t._1)
            t._2.foreach((op: Operation) => {
              val v = math.abs(op.mSum / 100.0d)
              renderer.setYAxisMax(math.max(v + 1, renderer.getYAxisMax))
              renderer.addYTextLabel(v, op.getSumStr)
              s.add(op.getDateObj, v)
            })
            data.addSeries(s)
            renderer.addSeriesRenderer(createXYSeriesRenderer(colors))
          })
        }
        construct(pos, pos_colors)
        construct(neg, neg_colors)
    }
    data -> renderer
  }

  private def filterName(stat: Statistic): String = {
    val stId = stat.filterType match {
      case Statistic.THIRD_PARTY =>
        R.string.third_parties
      case Statistic.TAGS =>
        R.string.tags
      case Statistic.MODE =>
        R.string.modes
      case Statistic.NO_FILTER =>
        R.string.time
    }
    ctx.getString(stId)
  }

  // for bar and lines chart
  private def createXYMultipleSeriesRenderer(stat: Statistic): XYMultipleSeriesRenderer = {
    val renderer = new XYMultipleSeriesRenderer()
    renderer.setBackgroundColor(ctx.getResources.getColor(android.R.color.transparent))
    renderer.setApplyBackgroundColor(true)
    renderer.setLabelsTextSize(18)
    renderer.setShowLegend(false)
    renderer.setMarginsColor(ctx.getResources.getColor(android.R.color.transparent))
    renderer.setYAxisMin(0)
    renderer.setMargins(Array[Int](0, 0, 0, 0)) // top, left, bottom, right
    stat.chartType match {
      case Statistic.CHART_BAR =>
        renderer.setYLabelsAlign(Paint.Align.RIGHT)
        renderer.setYTitle(ctx.getString(R.string.sum))
        renderer.setXLabels(0)
        renderer.setYLabels(0)
        renderer.setXAxisMin(0)
        renderer.setXAxisMax(0)
        renderer.setBarWidth(50)
        renderer.setBarSpacing(0.5)
        renderer.setXTitle(filterName(stat))
      case Statistic.CHART_LINE =>
        renderer.setPointSize(5)
        renderer.setYLabelsAlign(Paint.Align.LEFT)
    }
    renderer
  }

  // for bar chart
  private def createBarXYDataSet(stat: Statistic): (XYMultipleSeriesDataset, XYMultipleSeriesRenderer) = {
    val data = new XYMultipleSeriesDataset
    val renderer = createXYMultipleSeriesRenderer(stat)
    var (pos, neg) = sumPerFilter(stat)
    pos.keys.foreach(s1 => {
      if (!neg.keys.exists(s2 => s1.equals(s2))) neg += (s1 -> 0l)
    })
    neg.keys.foreach(s1 => {
      if (!pos.keys.exists(s2 => s1.equals(s2))) pos += (s1 -> 0l)
    })
    var xLabels: List[String] = List()
    def construct(m: Map[String, Long], isPos: Boolean): Unit = {
      val s = new CategorySeries(if (isPos) "pos" else "neg")
      m.foreach((t: (String, Long)) => {
        val v = math.abs(t._2 / 100.0d)
        renderer.setYAxisMax(math.max(v + 1, renderer.getYAxisMax))

        val existingSeries = xLabels.find(ser => ser.equals(t._1))
        if (!existingSeries.isDefined) {
          renderer.setXAxisMax(renderer.getXAxisMax + 1)
          renderer.addXTextLabel(renderer.getXAxisMax, t._1)
          xLabels = t._1 :: xLabels
        }
        s.add(v)
        data.addSeries(s.toXYSeries)

        val r = new XYSeriesRenderer
        r.setColor(if (isPos) pos_colors(0) else neg_colors(0))
        r.setChartValuesFormat(initNumFormat(stat))
        r.setDisplayChartValues(true)
        r.setChartValuesTextSize(16)
        renderer.addSeriesRenderer(r)
      })

      renderer.setXAxisMax(renderer.getXAxisMax + 1)
    }
    construct(pos, isPos = true)
    construct(neg, isPos = false)
    data -> renderer
  }

  private def createChartView(stat: Statistic): GraphicalView = {
    stat.chartType = Statistic.CHART_BAR
    stat.timeScaleType = Statistic.PERIOD_DAYS
    stat.xLast = 100
    stat.chartType match {
      case Statistic.CHART_PIE =>
        val (dataSet, renderer) = createCategorySeries(stat)
        ChartFactory.getPieChartView(ctx, dataSet, renderer)
      case Statistic.CHART_BAR =>
        val (dataSet, renderer) = createBarXYDataSet(stat)
        ChartFactory.getBarChartView(ctx, dataSet, renderer, Type.STACKED)
      case Statistic.CHART_LINE =>
        val (dataSet, renderer) = createLineXYDataSet(stat)
        val format = stat.timeScaleType match {
          case Statistic.PERIOD_DAYS | Statistic.PERIOD_ABSOLUTE =>
            "dd/MM/yy"
          case Statistic.PERIOD_MONTHES =>
            "MM/yy"
          case Statistic.PERIOD_YEARS =>
            "yy"
        }
        ChartFactory.getTimeChartView(ctx, dataSet, renderer, format)
    }
  }

}
