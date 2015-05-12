package fr.geobert.radis.data

import android.database.Cursor
import android.os.Parcel
import android.os.Parcelable
import fr.geobert.radis.R
import fr.geobert.radis.db.StatisticTable
import fr.geobert.radis.tools.TIME_ZONE
import fr.geobert.radis.tools.Tools
import fr.geobert.radis.tools.minusMonth
import fr.geobert.radis.tools.minusYear
import hirondelle.date4j.DateTime
import java.util.Calendar
import java.util.Date
import kotlin.platform.platformStatic
import kotlin.properties.Delegates

public class Statistic() : ImplParcelable {
    override val parcels = hashMapOf<String, Any?>()

    companion object {
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

        platformStatic public val CREATOR: Parcelable.Creator<Statistic> = object : Parcelable.Creator<Statistic> {
            override fun createFromParcel(p: Parcel): Statistic {
                return Statistic(p)
            }

            override fun newArray(size: Int): Array<Statistic?> {
                return arrayOfNulls(size)
            }
        }
    }

    var id: Long by Delegates.mapVar(parcels)
    var name: String by Delegates.mapVar(parcels)
    var accountId: Long by Delegates.mapVar(parcels)
    var accountName: String by Delegates.mapVar(parcels)
    var xLast: Int by Delegates.mapVar(parcels)
    var endDate: DateTime by Delegates.mapVar(parcels)
    var date: DateTime by Delegates.mapVar(parcels)
    var startDate: DateTime by Delegates.mapVar(parcels)
    var timeScaleType: Int by Delegates.mapVar(parcels)
    var chartType: Int by Delegates.mapVar(parcels)
    var filterType: Int by Delegates.mapVar(parcels)

    init {
        id = 0L
        name = ""
        accountId = 0L
        accountName = ""
        xLast = 1
        timeScaleType = Statistic.PERIOD_DAYS
        chartType = Statistic.CHART_PIE
        filterType = 0
        val today = DateTime.today(TIME_ZONE)
        date = today.minusMonth(1)
        startDate = today.minusMonth(1)
        endDate = today
    }

    constructor(cursor: Cursor) : this() {
        id = cursor.getLong(0)
        name = cursor.getString(StatisticTable.STAT_COLS.indexOf(StatisticTable.KEY_STAT_NAME))
        accountName = cursor.getString(StatisticTable.STAT_COLS.indexOf(StatisticTable.KEY_STAT_ACCOUNT_NAME))
        accountId = cursor.getLong(StatisticTable.STAT_COLS.indexOf(StatisticTable.KEY_STAT_ACCOUNT))
        chartType = cursor.getInt(StatisticTable.STAT_COLS.indexOf(StatisticTable.KEY_STAT_TYPE))
        filterType = cursor.getInt(StatisticTable.STAT_COLS.indexOf(StatisticTable.KEY_STAT_FILTER))
        timeScaleType = cursor.getInt(StatisticTable.STAT_COLS.indexOf(StatisticTable.KEY_STAT_PERIOD_TYPE))
        if (isPeriodAbsolute()) {
            startDate = DateTime.forInstant(cursor.getLong(StatisticTable.STAT_COLS.indexOf(StatisticTable.KEY_STAT_START_DATE)), TIME_ZONE)
            endDate = DateTime.forInstant(cursor.getLong(StatisticTable.STAT_COLS.indexOf(StatisticTable.KEY_STAT_END_DATE)), TIME_ZONE)
        } else {
            xLast = cursor.getInt(StatisticTable.STAT_COLS.indexOf(StatisticTable.KEY_STAT_X_LAST))
        }
    }

    constructor(p: Parcel) : this() {
        readFromParcel(p)
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
                StatisticTable.KEY_STAT_START_DATE -> startDate.getMilliseconds(TIME_ZONE)
                StatisticTable.KEY_STAT_END_DATE -> endDate.getMilliseconds(TIME_ZONE)
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
     * @return (startDate, endDate)
     */
    fun createTimeRange(): Pair<DateTime, DateTime> {
        fun createXLastRange(): Pair<DateTime, DateTime> {
            val today = DateTime.today(TIME_ZONE)
            val startDate = when (this.timeScaleType) {
                Statistic.PERIOD_DAYS -> today.minusDays(this.xLast)
                Statistic.PERIOD_MONTHES -> today.minusMonth(this.xLast)
                Statistic.PERIOD_YEARS -> today.minusYear(this.xLast)
                else -> throw NoWhenBranchMatchedException()
            }
            return Pair(startDate, today)
        }

        return when (this.timeScaleType) {
            Statistic.PERIOD_ABSOLUTE -> Pair(startDate, endDate)
            else -> createXLastRange()
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
