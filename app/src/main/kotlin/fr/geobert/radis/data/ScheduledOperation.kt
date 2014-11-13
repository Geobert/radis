package fr.geobert.radis.data

import android.app.Activity
import android.database.Cursor
import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import fr.geobert.radis.R
import fr.geobert.radis.db.ScheduledOperationTable
import fr.geobert.radis.tools.Tools

import java.util.Calendar
import java.util.GregorianCalendar
import kotlin.platform.platformStatic


public fun ScheduledOperation(op: Operation, accountId: Long): ScheduledOperation = ScheduledOperation.new(op, accountId)

public fun ScheduledOperation(op: Cursor, accountId: Long): ScheduledOperation = ScheduledOperation.new(op, accountId)

public fun ScheduledOperation(op: Cursor): ScheduledOperation = ScheduledOperation.new(op)

public fun ScheduledOperation(accountId: Long): ScheduledOperation = ScheduledOperation.new(accountId)

public fun ScheduledOperation(parcel: Parcel): ScheduledOperation = ScheduledOperation.new(parcel)

public class ScheduledOperation : Operation() {
    public var mPeriodicity: Int = 0
    public var mPeriodicityUnit: Int = 1
    public var mEndDate: GregorianCalendar = Tools.createClearedCalendar()

    class object {
        fun new(op: Operation, accountId: Long): ScheduledOperation {
            val __ = ScheduledOperation()
            __.initWithOperation(op)
            __.mAccountId = accountId
            return __
        }

        fun new(op: Cursor, accountId: Long): ScheduledOperation {
            val __ = ScheduledOperation()
            __.initWithCursor(op)
            __.mAccountId = accountId
            return __
        }

        fun new(op: Cursor): ScheduledOperation {
            val __ = ScheduledOperation()
            __.initWithCursor(op)
            __.mAccountId = op.getLong(op.getColumnIndex(ScheduledOperationTable.KEY_SCHEDULED_ACCOUNT_ID))
            __.mEndDate = GregorianCalendar()
            __.mEndDate.setTimeInMillis(op.getLong(op.getColumnIndex(ScheduledOperationTable.KEY_SCHEDULED_END_DATE)))
            Tools.clearTimeOfCalendar(__.mEndDate)
            __.mPeriodicity = op.getInt(op.getColumnIndex(ScheduledOperationTable.KEY_SCHEDULED_PERIODICITY))
            __.mPeriodicityUnit = op.getInt(op.getColumnIndex(ScheduledOperationTable.KEY_SCHEDULED_PERIODICITY_UNIT))
            return __
        }

        fun new(accountId: Long): ScheduledOperation {
            val __ = ScheduledOperation()
            __.mAccountId = accountId
            return __
        }

        fun new(p: Parcel): ScheduledOperation {
            val __ = ScheduledOperation()
            __.readFromParcel(p)
            return __
        }

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

        public val CREATOR: Parcelable.Creator<ScheduledOperation> = object : Parcelable.Creator<ScheduledOperation> {
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
        return mEndDate.get(Calendar.MONTH)
    }

    public fun setEndMonth(month: Int) {
        this.mEndDate.set(Calendar.MONTH, month)
    }

    public fun getEndDay(): Int {
        return mEndDate.get(Calendar.DAY_OF_MONTH)
    }

    public fun setEndDay(day: Int) {
        this.mEndDate.set(Calendar.DAY_OF_MONTH, day)
    }

    public fun getEndYear(): Int {
        return mEndDate.get(Calendar.YEAR)
    }

    public fun setEndYear(year: Int) {
        this.mEndDate.set(Calendar.YEAR, year)
    }

    public fun getEndDate(): Long {
        return mEndDate.getTimeInMillis()
    }

    override fun writeToParcel(dst: Parcel, flags: Int) {
        super.writeToParcel(dst, flags)
        dst.writeInt(mPeriodicity)
        dst.writeInt(mPeriodicityUnit)
        dst.writeLong(mAccountId)
        dst.writeInt(getEndDay())
        dst.writeInt(getEndMonth())
        dst.writeInt(getEndYear())
    }

    override fun readFromParcel(p: Parcel) {
        Log.d("Radis", "schedule operation readFromParcel")
        super.readFromParcel(p)
        mPeriodicity = p.readInt()
        mPeriodicityUnit = p.readInt()
        mAccountId = p.readLong()
        mEndDate = GregorianCalendar()
        mEndDate.clear()
        setEndDay(p.readInt())
        setEndMonth(p.readInt())
        setEndYear(p.readInt())
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
}
