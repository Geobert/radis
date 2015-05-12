package fr.geobert.radis.data

import android.app.Activity
import android.database.Cursor
import android.os.Parcel
import android.os.Parcelable
import fr.geobert.radis.R
import fr.geobert.radis.db.ScheduledOperationTable
import fr.geobert.radis.tools.TIME_ZONE
import fr.geobert.radis.tools.Tools
import hirondelle.date4j.DateTime
import java.util.Calendar
import java.util.GregorianCalendar
import kotlin.platform.platformStatic
import kotlin.properties.Delegates

public class ScheduledOperation() : Operation() {
    public var mPeriodicity: Int by Delegates.mapVar(parcels)
    public var mPeriodicityUnit: Int by Delegates.mapVar(parcels)
    public var mEndDate: DateTime by Delegates.mapVar(parcels)

    init {
        mPeriodicity = 0
        mPeriodicityUnit = 1
        mEndDate = DateTime.today(TIME_ZONE)
    }

    constructor(op: Operation, accountId: Long) : this() {
        initWithOperation(op)
        mAccountId = accountId
    }

    constructor(c: Cursor, accountId: Long) : this() {
        initWithCursor(c)
        mAccountId = accountId
    }

    constructor(c: Cursor) : this() {
        initWithCursor(c)
        mAccountId = c.getLong(c.getColumnIndex(ScheduledOperationTable.KEY_SCHEDULED_ACCOUNT_ID))
        mEndDate = DateTime.forInstant(c.getLong(c.getColumnIndex(ScheduledOperationTable.KEY_SCHEDULED_END_DATE)), TIME_ZONE)
        mPeriodicity = c.getInt(c.getColumnIndex(ScheduledOperationTable.KEY_SCHEDULED_PERIODICITY))
        mPeriodicityUnit = c.getInt(c.getColumnIndex(ScheduledOperationTable.KEY_SCHEDULED_PERIODICITY_UNIT))
    }

    constructor(accountId: Long) : this() {
        mAccountId = accountId
        mEndDate = DateTime.forInstant(0, TIME_ZONE)
    }

    constructor(p: Parcel) : this() {
        readFromParcel(p)
    }

    companion object {
        public val WEEKLY_PERIOD: Int = 0
        public val MONTHLY_PERIOD: Int = 1
        public val YEARLY_PERIOD: Int = 2
        public val CUSTOM_DAILY_PERIOD: Int = 3
        public val CUSTOM_WEEKLY_PERIOD: Int = 4
        public val CUSTOM_MONTHLY_PERIOD: Int = 5
        public val CUSTOM_YEARLY_PERIOD: Int = 6

        public fun getUnitStr(context: Activity, unit: Int, periodicity: Int): String? {
            val s = context.getResources().getStringArray(R.array.periodicity_labels)[unit]
            if (unit < CUSTOM_DAILY_PERIOD) {
                return s
            } else if (unit <= CUSTOM_YEARLY_PERIOD) {
                return s.format(periodicity)
            }
            return null
        }

        platformStatic public val CREATOR: Parcelable.Creator<ScheduledOperation> = object : Parcelable.Creator<ScheduledOperation> {
            override fun createFromParcel(`in`: Parcel): ScheduledOperation {
                return ScheduledOperation(`in`)
            }

            override fun newArray(size: Int): Array<ScheduledOperation?> {
                return arrayOfNulls(size)
            }
        }

        public platformStatic fun addPeriodicityToDate(op: ScheduledOperation) {
            when (op.mPeriodicityUnit) {
                ScheduledOperation.WEEKLY_PERIOD -> op.addDay(7)
                ScheduledOperation.MONTHLY_PERIOD -> op.addMonth(1)
                ScheduledOperation.YEARLY_PERIOD -> op.addYear(1)
                ScheduledOperation.CUSTOM_DAILY_PERIOD -> op.addDay(op.mPeriodicity)
                ScheduledOperation.CUSTOM_WEEKLY_PERIOD -> op.addDay(7 * op.mPeriodicity)
                ScheduledOperation.CUSTOM_MONTHLY_PERIOD -> op.addMonth(op.mPeriodicity)
                ScheduledOperation.CUSTOM_YEARLY_PERIOD -> op.addYear(op.mPeriodicity)
            }
        }
    }

    public fun getEndMonth(): Int {
        return mEndDate.getMonth()
    }

    public fun setEndMonth(month: Int) {
        mEndDate = DateTime.forDateOnly(mEndDate.getYear(), month, mEndDate.getDay())
    }

    public fun getEndDay(): Int {
        return mEndDate.getDay()
    }

    public fun setEndDay(day: Int) {
        this.mEndDate = DateTime.forDateOnly(mEndDate.getYear(), mEndDate.getMonth(), day)
    }

    public fun getEndYear(): Int {
        return mEndDate.getYear()
    }

    public fun setEndYear(year: Int) {
        this.mEndDate = DateTime.forDateOnly(year, mEndDate.getMonth(), mEndDate.getDay())
    }

    public fun getEndDate(): Long {
        return mEndDate.getMilliseconds(TIME_ZONE)
    }

    public fun isObsolete(): Boolean {
        return (getEndDate() > 0) && (getEndDate() < getDate())
    }

    override fun equals(op: Operation?): Boolean {
        if (null == op) {
            return false
        }
        val schOp = op as ScheduledOperation
        return super.equals(op) && mAccountId == schOp.mAccountId && mEndDate == schOp.mEndDate && mPeriodicity == schOp.mPeriodicity && mPeriodicityUnit == schOp.mPeriodicityUnit
    }

    public fun periodicityEquals(schOp: ScheduledOperation): Boolean {
        return mPeriodicity == schOp.mPeriodicity && mPeriodicityUnit == schOp.mPeriodicityUnit
    }

    fun clearEndDate() {
        mEndDate = DateTime.forInstant(0, TIME_ZONE)
    }
}
