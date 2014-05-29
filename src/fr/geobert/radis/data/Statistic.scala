package fr.geobert.radis.data

import android.database.Cursor
import java.util.Date
import fr.geobert.radis.db.StatisticTable

object Statistic {
  val PERIOD_DAYS = 0
  val PERIOD_MONTHES = 1
  val PERIOD_YEARS = 2
  val PERIOD_ABSOLUTE = 3

  val CHART_PIE = 0
  val CHART_BAR = 1
  val CHART_LINE = 2

  def apply(cursor: Cursor): Statistic = {
    val s = new Statistic
    s.id = cursor.getLong(0)
    s.name = cursor.getString(StatisticTable.STAT_COLS.indexOf(StatisticTable.KEY_STAT_NAME))
    s.accountId = cursor.getLong(StatisticTable.STAT_COLS.indexOf(StatisticTable.KEY_STAT_ACCOUNT))
    s.chartType = cursor.getInt(StatisticTable.STAT_COLS.indexOf(StatisticTable.KEY_STAT_TYPE))
    s.filterType = cursor.getInt(StatisticTable.STAT_COLS.indexOf(StatisticTable.KEY_STAT_FILTER))
    s.periodType = cursor.getInt(StatisticTable.STAT_COLS.indexOf(StatisticTable.KEY_STAT_PERIOD_TYPE))
    s.periodType match {
      case PERIOD_ABSOLUTE =>
        s.startDate = new Date(cursor.getLong(StatisticTable.STAT_COLS.indexOf(StatisticTable.KEY_STAT_START_DATE)))
        s.endDate = new Date(cursor.getLong(StatisticTable.STAT_COLS.indexOf(StatisticTable.KEY_STAT_END_DATE)))
      case _ =>
        s.xLast = cursor.getInt(StatisticTable.STAT_COLS.indexOf(StatisticTable.KEY_STAT_X_LAST))
    }
    s
  }
}

class Statistic {
  var id = 0L
  var name = ""
  var accountId = 0L
  var xLast = 1
  var startDate: Date = null
  var endDate: Date = null
  var periodType = 0
  var chartType = 0
  var filterType = 0

  def getValue(value: String) = {
    value match {
      case StatisticTable.KEY_STAT_NAME => name
      case StatisticTable.KEY_STAT_ACCOUNT => accountId
      case StatisticTable.KEY_STAT_TYPE => chartType
      case StatisticTable.KEY_STAT_FILTER => filterType
      case StatisticTable.KEY_STAT_PERIOD_TYPE => periodType
      case StatisticTable.KEY_STAT_X_LAST => xLast
      case StatisticTable.KEY_STAT_START_DATE => startDate.getTime
      case StatisticTable.KEY_STAT_END_DATE => endDate.getTime
    }
  }
}
