package fr.geobert.radis.data

import java.io.Serializable

import android.database.Cursor
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.Date
import fr.geobert.radis.db.StatisticTable
import kotlin.properties.Delegates
import fr.geobert.radis.tools.Tools
import fr.geobert.radis.R

public fun Statistic(cursor: Cursor): Statistic {
    val s = Statistic()
    s.id = cursor.getLong(0)
    s.name = cursor.getString(StatisticTable.STAT_COLS.indexOf(StatisticTable.KEY_STAT_NAME)) as String
    s.accountName = cursor.getString(StatisticTable.STAT_COLS.indexOf(StatisticTable.KEY_STAT_ACCOUNT_NAME)) as String
    s.accountId = cursor.getLong(StatisticTable.STAT_COLS.indexOf(StatisticTable.KEY_STAT_ACCOUNT))
    s.chartType = cursor.getInt(StatisticTable.STAT_COLS.indexOf(StatisticTable.KEY_STAT_TYPE))
    s.filterType = cursor.getInt(StatisticTable.STAT_COLS.indexOf(StatisticTable.KEY_STAT_FILTER))
    s.timeScaleType = cursor.getInt(StatisticTable.STAT_COLS.indexOf(StatisticTable.KEY_STAT_PERIOD_TYPE))
    if (s.isPeriodAbsolute()) {
        s.startDate = Date(cursor.getLong(StatisticTable.STAT_COLS.indexOf(StatisticTable.KEY_STAT_START_DATE)))
        s.endDate = Date(cursor.getLong(StatisticTable.STAT_COLS.indexOf(StatisticTable.KEY_STAT_END_DATE)))
    } else {
        s.xLast = cursor.getInt(StatisticTable.STAT_COLS.indexOf(StatisticTable.KEY_STAT_X_LAST))
    }
    return s
}

public class Statistic : Serializable {
    class object {
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
    }
    var id = 0L
    var name = ""
    var accountId = 0L
    var accountName = ""
    var xLast = 1
    var endDate: Date = GregorianCalendar().getTime()
    val date = GregorianCalendar()
    var startDate: Date by Delegates.notNull()
    var timeScaleType = Statistic.PERIOD_DAYS
    var chartType = Statistic.CHART_PIE
    var filterType: Int = 0

    {
        date.add(Calendar.MONTH, -1)
        startDate = date.getTime()
    }

    fun getValue(value: String) =
            when (value) {
                StatisticTable.KEY_STAT_NAME -> name
                StatisticTable.KEY_STAT_ACCOUNT_NAME -> accountName
                StatisticTable.KEY_STAT_ACCOUNT -> accountId
                StatisticTable.KEY_STAT_TYPE -> chartType
                StatisticTable.KEY_STAT_FILTER -> filterType
                StatisticTable.KEY_STAT_PERIOD_TYPE -> timeScaleType
                StatisticTable.KEY_STAT_X_LAST -> xLast
                StatisticTable.KEY_STAT_START_DATE -> startDate.getTime()
                StatisticTable.KEY_STAT_END_DATE -> endDate.getTime()
                else -> {
                    throw NoSuchFieldException()
                }
            }


    fun isPeriodAbsolute() = timeScaleType == Statistic.PERIOD_ABSOLUTE

    override fun equals(other: Any?): Boolean =
            if (other is Statistic) {
                this.id == other.id && this.name == other.name && this.accountId == other.accountId &&
                        this.chartType == other.chartType && this.filterType == other.filterType &&
                        this.timeScaleType == other.timeScaleType &&
                        this.isPeriodAbsolute() == other.isPeriodAbsolute() &&
                        if (this.isPeriodAbsolute()) {
                            this.startDate == other.startDate && this.endDate == other.endDate
                        } else {
                            this.xLast == other.xLast
                        }

            } else {
                false
            }
    /**
     * create a time range according to statistic's configuration
     * @param stat
     * @return (startDate, endDate)
     */
    fun createTimeRange(): Pair<Date, Date> {
        fun createXLastRange(field: Int): Pair<Date, Date> {
            val endDate = Tools.createClearedCalendar()
            val startDate = Tools.createClearedCalendar()
            startDate.add(field, -this.xLast)
            return Pair(startDate.getTime(), endDate.getTime())
        }

        return when (this.timeScaleType) {
            Statistic.PERIOD_DAYS -> createXLastRange(Calendar.DAY_OF_MONTH)
            Statistic.PERIOD_MONTHES -> createXLastRange(Calendar.MONTH)
            Statistic.PERIOD_YEARS -> createXLastRange(Calendar.YEAR)
            Statistic.PERIOD_ABSOLUTE -> Pair(this.startDate, this.endDate)
            else -> {
                throw NoWhenBranchMatchedException()
            }
        }
    }

    fun getFilterStr(): Int =
            when (filterType) {
                Statistic.THIRD_PARTY -> R.string.third_party
                Statistic.TAGS -> R.string.tag
                Statistic.MODE -> R.string.mode
                else -> R.string.no_filter
            }

}
