package fr.geobert.radis.data

import android.database.Cursor
import android.os.Parcel
import android.os.Parcelable
import fr.geobert.radis.db.InfoTables
import fr.geobert.radis.db.OperationTable
import fr.geobert.radis.tools.*
import hirondelle.date4j.DateTime
import java.util.Date
import kotlin.platform.platformStatic

public open class Operation() : Parcelable {
    protected var mDate: DateTime = DateTime.today(TIME_ZONE)
    public var mThirdParty: String = ""
    public var mTag: String = ""
    public var mMode: String = ""
    public var mNotes: String = ""
    public var mSum: Long = 0
    public var mScheduledId: Long = 0
    public var mRowId: Long = 0
    public var mTransSrcAccName: String = ""
    public var mIsChecked: Boolean = false

    // if these value are != 0, it is a transfert operation between 2 accounts
    // mTransferAccountId is the other account
    public var mTransferAccountId: Long = 0
    public var mAccountId: Long = 0

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
        platformStatic public val CREATOR: Parcelable.Creator<Operation> = object : Parcelable.Creator<Operation> {
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
        return mDate.getMonth()
    }

    public fun setMonth(month: Int) {
        val d = Math.min(mDate.getDay(), DateTime.forDateOnly(mDate.getYear(), month, 1).getEndOfMonth().getDay())
        mDate = DateTime.forDateOnly(mDate.getYear(), month, d)
    }

    public fun getDay(): Int {
        return mDate.getDay()
    }

    public fun setDay(day: Int) {
        val d = Math.min(day, mDate.getEndOfMonth().getDay())
        mDate = DateTime.forDateOnly(mDate.getYear(), mDate.getMonth(), d)
    }

    public fun getYear(): Int {
        return mDate.getYear()
    }

    public fun setYear(year: Int) {
        // will throw exception if month = feb and day = 29 and year is not leap, adjust the day
        val d = if (mDate.getMonth() == 2 && mDate.isLeapYear() && !DateTime.forDateOnly(year, 1, 1).isLeapYear())
            Math.min(mDate.getDay(), 28)
        else
            mDate.getDay()
        mDate = DateTime.forDateOnly(year, mDate.getMonth(), d)
    }

    public fun getDate(): Long {
        return mDate.getMilliseconds(TIME_ZONE)
    }

    public fun setDate(date: Long) {
        val d = DateTime.forInstant(date, TIME_ZONE)
        mDate = DateTime.forDateOnly(d.getYear(), d.getMonth(), d.getDay())
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

    override fun writeToParcel(dst: Parcel, flags: Int) {
        dst.writeInt(getDay())
        dst.writeInt(getMonth())
        dst.writeInt(getYear())
        dst.writeString(mThirdParty)
        dst.writeString(mTag)
        dst.writeString(mMode)
        dst.writeString(mNotes)
        dst.writeLong(mSum)
        dst.writeLong(mScheduledId)
        dst.writeLong(mTransferAccountId)
        dst.writeBoolean(mIsChecked)
    }

    protected open fun readFromParcel(p: Parcel) {
        setDay(p.readInt())
        setMonth(p.readInt())
        setYear(p.readInt())

        mThirdParty = p.readString()
        mTag = p.readString()
        mMode = p.readString()
        mNotes = p.readString()
        mSum = p.readLong()
        mScheduledId = p.readLong()
        mTransferAccountId = p.readLong()
        mIsChecked = p.readBoolean()
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
}
