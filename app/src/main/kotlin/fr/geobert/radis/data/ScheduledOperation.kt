package fr.geobert.radis.data

import android.app.Activity
import android.database.Cursor
import android.os.*
import fr.geobert.radis.R
import fr.geobert.radis.db.ScheduledOperationTable
import fr.geobert.radis.tools.TIME_ZONE
import hirondelle.date4j.DateTime

class ScheduledOperation() : Operation() {
    var mPeriodicity: Int by parcels
    var mPeriodicityUnit: Int by parcels
    var mEndDate: DateTime by parcels

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
        val WEEKLY_PERIOD: Int = 0
        val MONTHLY_PERIOD: Int = 1
        val YEARLY_PERIOD: Int = 2
        val CUSTOM_DAILY_PERIOD: Int = 3
        val CUSTOM_WEEKLY_PERIOD: Int = 4
        val CUSTOM_MONTHLY_PERIOD: Int = 5
        val CUSTOM_YEARLY_PERIOD: Int = 6

        fun getUnitStr(context: Activity, unit: Int, periodicity: Int): String? {
            val s = context.resources.getStringArray(R.array.periodicity_labels)[unit]
            if (unit < CUSTOM_DAILY_PERIOD) {
                return s
            } else if (unit <= CUSTOM_YEARLY_PERIOD) {
                return s.format(periodicity)
            }
            return null
        }

        val CREATOR: Parcelable.Creator<ScheduledOperation> = object : Parcelable.Creator<ScheduledOperation> {
            override fun createFromParcel(`in`: Parcel): ScheduledOperation {
                return ScheduledOperation(`in`)
            }

            override fun newArray(size: Int): Array<ScheduledOperation?> {
                return arrayOfNulls(size)
            }
        }

        fun addPeriodicityToDate(op: ScheduledOperation) {
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

    fun getEndMonth(): Int {
        return mEndDate.month
    }

    fun setEndMonth(month: Int) {
        val d = if (month == 2 && mEndDate.day >= 29) {
            Math.min(mEndDate.day, if (mEndDate.isLeapYear) 29 else 28)
        } else mEndDate.day
        mEndDate = DateTime.forDateOnly(mEndDate.year, month, d)
    }

    fun getEndDay(): Int {
        return mEndDate.day
    }

    fun setEndDay(day: Int) {
        val d = if (day >= 29 && mEndDate.month == 2) {
            Math.min(day, if (mEndDate.isLeapYear) 29 else 28)
        } else day
        this.mEndDate = DateTime.forDateOnly(mEndDate.year, mEndDate.month, d)
    }

    fun getEndYear(): Int {
        return mEndDate.year
    }

    fun setEndYear(year: Int) {
        val t = DateTime.forDateOnly(year, 1, 1)
        val d = if (mEndDate.day >= 29 && mEndDate.month == 2) {
            Math.min(mEndDate.day, if (t.isLeapYear) 29 else 28)
        } else mEndDate.day
        this.mEndDate = DateTime.forDateOnly(year, mEndDate.month, d)
    }

    fun getEndDate(): Long {
        return mEndDate.getMilliseconds(TIME_ZONE)
    }

    fun isObsolete(): Boolean {
        return (getEndDate() > 0) && (getEndDate() < getDate())
    }

    override fun equals(op: Operation?): Boolean {
        if (null == op) {
            return false
        }
        val schOp = op as ScheduledOperation
        return super.equals(op) && mAccountId == schOp.mAccountId && mEndDate == schOp.mEndDate &&
                mPeriodicity == schOp.mPeriodicity && mPeriodicityUnit == schOp.mPeriodicityUnit
    }

    fun periodicityEquals(schOp: ScheduledOperation): Boolean {
        return mPeriodicity == schOp.mPeriodicity && mPeriodicityUnit == schOp.mPeriodicityUnit
    }

    fun clearEndDate() {
        mEndDate = DateTime.forInstant(0, TIME_ZONE)
    }
}
