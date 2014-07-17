package fr.geobert.radis.data

import java.io.Serializable

import android.database.Cursor
import java.util.{Calendar, GregorianCalendar, Date}
import android.os.Parcelable.Creator
import android.os.{Parcel, Parcelable}
import fr.geobert.radis.db.StatisticTable

object Statistic {
  val PERIOD_DAYS = 0
  val PERIOD_MONTHES = 1
  val PERIOD_YEARS = 2
  val PERIOD_ABSOLUTE = 3

  val CHART_PIE = 0
  val CHART_BAR = 1
  val CHART_LINE = 2

  // filter spinner idx
  val THIRD_PARTY = 0
  val TAGS = 1
  val MODE = 2
  val NO_FILTER = 3

  def apply(cursor: Cursor): Statistic = {
    val s = new Statistic
    s.id = cursor.getLong(0)
    s.name = cursor.getString(StatisticTable.STAT_COLS.indexOf(StatisticTable.KEY_STAT_NAME))
    s.accountId = cursor.getLong(StatisticTable.STAT_COLS.indexOf(StatisticTable.KEY_STAT_ACCOUNT))
    s.chartType = cursor.getInt(StatisticTable.STAT_COLS.indexOf(StatisticTable.KEY_STAT_TYPE))
    s.filterType = cursor.getInt(StatisticTable.STAT_COLS.indexOf(StatisticTable.KEY_STAT_FILTER))
    s.timeScaleType = cursor.getInt(StatisticTable.STAT_COLS.indexOf(StatisticTable.KEY_STAT_PERIOD_TYPE))
    if (s.isPeriodAbsolute) {
      s.startDate = new Date(cursor.getLong(StatisticTable.STAT_COLS.indexOf(StatisticTable.KEY_STAT_START_DATE)))
      s.endDate = new Date(cursor.getLong(StatisticTable.STAT_COLS.indexOf(StatisticTable.KEY_STAT_END_DATE)))
    } else {
      s.xLast = cursor.getInt(StatisticTable.STAT_COLS.indexOf(StatisticTable.KEY_STAT_X_LAST))
    }
    s
  }
}

class Statistic extends Serializable {
  var id = 0L
  var name = ""
  var accountId = 0L
  var accountName = ""
  var xLast = 1
  var endDate: Date = new GregorianCalendar().getTime
  val date = new GregorianCalendar()
  date.add(Calendar.MONTH, -1)
  var startDate: Date = date.getTime
  var timeScaleType = Statistic.PERIOD_DAYS
  var chartType = Statistic.CHART_PIE
  var filterType = 0

  def getValue(value: String) = {
    value match {
      case StatisticTable.KEY_STAT_NAME => name
      case StatisticTable.KEY_STAT_ACCOUNT_NAME => accountName
      case StatisticTable.KEY_STAT_ACCOUNT => accountId
      case StatisticTable.KEY_STAT_TYPE => chartType
      case StatisticTable.KEY_STAT_FILTER => filterType
      case StatisticTable.KEY_STAT_PERIOD_TYPE => timeScaleType
      case StatisticTable.KEY_STAT_X_LAST => xLast
      case StatisticTable.KEY_STAT_START_DATE => startDate.getTime
      case StatisticTable.KEY_STAT_END_DATE => endDate.getTime
    }
  }

  def isPeriodAbsolute = timeScaleType == Statistic.PERIOD_ABSOLUTE

  def ==(that: Statistic): Boolean = {
    this.id == that.id && this.name == that.name && this.accountId == that.accountId &&
      this.chartType == that.chartType && this.filterType == that.filterType && this.timeScaleType == that.timeScaleType &&
      this.isPeriodAbsolute == that.isPeriodAbsolute && {
      if (this.isPeriodAbsolute) {
        this.startDate == that.startDate && this.endDate == that.endDate
      } else {
        this.xLast == that.xLast
      }
    }
  }

  def !=(that: Statistic): Boolean = {
    !(this == that)
  }
}
