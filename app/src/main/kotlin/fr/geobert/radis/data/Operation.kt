package fr.geobert.radis.data

import android.database.Cursor
import android.os.Parcel
import android.os.Parcelable
import fr.geobert.radis.db.InfoTables
import fr.geobert.radis.db.OperationTable
import fr.geobert.radis.tools.TIME_ZONE
import fr.geobert.radis.tools.extractSumFromStr
import fr.geobert.radis.tools.formatSum
import fr.geobert.radis.tools.plusMonth
import fr.geobert.radis.ui.adapter.CellState
import hirondelle.date4j.DateTime
import java.util.*
import kotlin.properties.getValue
import kotlin.properties.setValue

public open class Operation() : ImplParcelable, Comparable<Operation> {
    override val parcels = hashMapOf<String, Any?>()
    public var mDate: DateTime by parcels
    public var mThirdParty: String by parcels
    public var mTag: String by parcels
    public var mMode: String by parcels
    public var mNotes: String by parcels
    public var mSum: Long by parcels
    public var mScheduledId: Long by parcels
    public var mRowId: Long by parcels
    public var mTransSrcAccName: String by parcels
    public var mIsChecked: Boolean by parcels

    // if these value are != 0, it is a transfert operation between 2 accounts
    // mTransferAccountId is the other account
    public var mTransferAccountId: Long by parcels
    public var mAccountId: Long by parcels

    // properties used only for ui
    public var isSelected: Boolean = false
    public var state: CellState = CellState.STATE_REGULAR_CELL


    init {
        mDate = DateTime.today(TIME_ZONE)
        mThirdParty = ""
        mTag = ""
        mMode = ""
        mNotes = ""
        mSum = 0L
        mScheduledId = 0
        mRowId = 0
        mTransSrcAccName = ""
        mIsChecked = false
        mTransferAccountId = 0L
        mAccountId = 0L
    }


    constructor(op: Operation) : this() {
        initWithOperation(op)
    }

    constructor(c: Cursor) : this() {
        initWithCursor(c)
    }

    constructor(p: Parcel) : this() {
        readFromParcel(p)
    }

    companion object {
        public val CREATOR: Parcelable.Creator<Operation> = object : Parcelable.Creator<Operation> {
            override fun createFromParcel(p: Parcel): Operation {
                return Operation(p)
            }

            override fun newArray(size: Int): Array<Operation?> {
                return arrayOfNulls(size)
            }
        }
    }

    fun initWithCursor(op: Cursor) {
        mRowId = op.getLong(op.getColumnIndex(OperationTable.KEY_OP_ROWID))
        mThirdParty = op.getString(op.getColumnIndexOrThrow(InfoTables.KEY_THIRD_PARTY_NAME)) ?: ""
        mMode = op.getString(op.getColumnIndexOrThrow(InfoTables.KEY_MODE_NAME)) ?: ""
        mTag = op.getString(op.getColumnIndexOrThrow(InfoTables.KEY_TAG_NAME)) ?: ""
        mSum = op.getLong(op.getColumnIndexOrThrow(OperationTable.KEY_OP_SUM))
        setDate(op.getLong(op.getColumnIndexOrThrow(OperationTable.KEY_OP_DATE)))
        mNotes = op.getString(op.getColumnIndexOrThrow(OperationTable.KEY_OP_NOTES)) ?: ""
        val idx = op.getColumnIndex(OperationTable.KEY_OP_SCHEDULED_ID)
        if (idx >= 0) {
            mScheduledId = op.getLong(idx)
        } else {
            mScheduledId = 0
        }
        val transIdx = op.getColumnIndex(OperationTable.KEY_OP_TRANSFERT_ACC_ID)
        if (transIdx >= 0) {
            mTransferAccountId = op.getLong(transIdx)
            mTransSrcAccName = op.getString(op.getColumnIndex(OperationTable.KEY_OP_TRANSFERT_ACC_NAME)) ?: ""
        }

        val accIdx = op.getColumnIndex(OperationTable.KEY_OP_ACCOUNT_ID)
        if (accIdx >= 0) {
            mAccountId = op.getLong(accIdx)
        }
        val checkedIdx = op.getColumnIndex(OperationTable.KEY_OP_CHECKED)
        if (-1 != checkedIdx) {
            mIsChecked = op.getInt(checkedIdx) == 1
        }
    }

    fun initWithOperation(op: Operation) {
        setDate(op.getDate())
        mThirdParty = op.mThirdParty
        mTag = op.mTag
        mMode = op.mMode
        mNotes = op.mNotes
        mSum = op.mSum
        mScheduledId = op.mScheduledId
        mTransferAccountId = op.mTransferAccountId
        mTransSrcAccName = op.mTransSrcAccName
        mAccountId = op.mAccountId
        mIsChecked = op.mIsChecked
    }

    public fun getMonth(): Int {
        return mDate.month
    }

    public fun setMonth(month: Int) {
        val d = Math.min(mDate.day, DateTime.forDateOnly(mDate.year, month, 1).endOfMonth.day)
        mDate = DateTime.forDateOnly(mDate.year, month, d)
    }

    public fun getDay(): Int {
        return mDate.day
    }

    public fun setDay(day: Int) {
        val d = Math.min(day, mDate.endOfMonth.day)
        mDate = DateTime.forDateOnly(mDate.year, mDate.month, d)
    }

    public fun getYear(): Int {
        return mDate.year
    }

    public fun setYear(year: Int) {
        // will throw exception if month = feb and day = 29 and year is not leap, adjust the day
        val d = if (mDate.month == 2 && mDate.isLeapYear && !DateTime.forDateOnly(year, 1, 1).isLeapYear)
            Math.min(mDate.day, 28)
        else
            mDate.day
        mDate = DateTime.forDateOnly(year, mDate.month, d)
    }

    public fun getDate(): Long {
        return mDate.getMilliseconds(TIME_ZONE)
    }

    public fun setDate(date: Long) {
        val d = DateTime.forInstant(date, TIME_ZONE)
        mDate = DateTime.forDateOnly(d.year, d.month, d.day)
    }

    public fun getDateObj(): Date {
        return Date(mDate.getMilliseconds(TIME_ZONE))
    }

    public fun addDay(nbDays: Int) {
        mDate = mDate.plusDays(nbDays)
    }

    public fun addMonth(nbMonths: Int) {
        mDate = mDate.plusMonth(nbMonths)
    }

    public fun addYear(nbYears: Int) {
        mDate.plus(nbYears, 0, 0, 0, 0, 0, 0, DateTime.DayOverflow.LastDay)
    }

    public fun getSumStr(): String {
        return (mSum.toDouble() / 100.0).formatSum()
    }

    public fun setSumStr(sumStr: String) {
        mSum = sumStr.extractSumFromStr()
    }

    //    public fun setDateStr(dateStr: String) {
    //        mDate.setTime(dateStr.parseDate())
    //    }

    override fun describeContents(): Int {
        return 0
    }

    public open fun equals(op: Operation?): Boolean {
        if (null == op) {
            return false
        }
        return mDate == op.mDate && equalsButDate(op)
    }

    public fun equalsButDate(op: Operation?): Boolean {
        if (null == op) {
            return false
        }
        return mThirdParty == op.mThirdParty && mTag == op.mTag && mMode == op.mMode && mNotes == op.mNotes &&
                mSum == op.mSum && mScheduledId == op.mScheduledId && mTransferAccountId == op.mTransferAccountId &&
                mIsChecked == op.mIsChecked
    }

    // comparator for sorting purpose
    override fun compareTo(other: Operation): Int {
        return if (mDate.gt(other.mDate)) {
            1
        } else if (mDate.equals(other.mDate)) {
            if (mRowId >= other.mRowId) {
                1
            } else {
                -1
            }
        } else {
            -1
        }
    }

}
