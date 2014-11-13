package fr.geobert.radis.data

import android.database.Cursor
import android.os.Parcel
import android.os.Parcelable
import fr.geobert.radis.db.InfoTables
import fr.geobert.radis.db.OperationTable
import fr.geobert.radis.tools.Formater
import fr.geobert.radis.tools.Tools

import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar


public fun Operation(op: Operation): Operation {
    val __ = Operation()
    __.initWithOperation(op)
    return __
}

public fun Operation(op: Cursor): Operation {
    val __ = Operation()
    __.initWithCursor(op)
    return __
}

public fun Operation(p: Parcel): Operation {
    return Operation.new(p)
}

public open class Operation : Parcelable {
    protected var mDate: GregorianCalendar = Tools.createClearedCalendar()
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

    class object {
        fun new(p: Parcel): Operation {
            val __ = Operation()
            __.readFromParcel(p)
            return __
        }

        fun new(c: Cursor): Operation {
            return Operation(c)
        }

        fun new(op: Operation): Operation {
            return Operation(op)
        }

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
        return mDate.get(Calendar.MONTH)
    }

    public fun setMonth(month: Int) {
        this.mDate.set(Calendar.MONTH, month)
    }

    public fun getDay(): Int {
        return mDate.get(Calendar.DAY_OF_MONTH)
    }

    public fun setDay(day: Int) {
        this.mDate.set(Calendar.DAY_OF_MONTH, day)
    }

    public fun getYear(): Int {
        return mDate.get(Calendar.YEAR)
    }

    public fun setYear(year: Int) {
        this.mDate.set(Calendar.YEAR, year)
    }

    public fun getDateStr(): String {
        return Formater.getFullDateFormater().format(mDate.getTime())
    }

    public fun getShortDateStr(): String {
        return Formater.getShortDateFormater(null).format(mDate.getTime())
    }

    public fun getDate(): Long {
        return mDate.getTimeInMillis()
    }

    public fun setDate(date: Long) {
        mDate.setTimeInMillis(date)
        Tools.clearTimeOfCalendar(mDate)
    }

    public fun getDateObj(): Date {
        return mDate.getTime()
    }

    public fun addDay(nbDays: Int) {
        mDate.add(Calendar.DAY_OF_MONTH, nbDays)
    }

    public fun addMonth(nbMonths: Int) {
        mDate.add(Calendar.MONTH, nbMonths)
    }

    public fun addYear(nbYears: Int) {
        mDate.add(Calendar.YEAR, nbYears)
    }

    public fun getSumStr(): String {
        return Formater.getSumFormater().format(mSum.toDouble() / 100.0)
    }

    public fun setSumStr(sumStr: String) {
        mSum = Tools.extractSumFromStr(sumStr)
    }

    public fun setDateStr(dateStr: String) {
        mDate.setTime(Formater.getFullDateFormater().parse(dateStr))
    }

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
        dst.writeInt(if (mIsChecked) 1 else 0)
    }

    protected open fun readFromParcel(p: Parcel) {
        setDay(p.readInt())
        setMonth(p.readInt())
        setYear(p.readInt())

        mThirdParty = p.readString() ?: ""
        mTag = p.readString() ?: ""
        mMode = p.readString() ?: ""
        mNotes = p.readString() ?: ""
        mSum = p.readLong()
        mScheduledId = p.readLong()
        mTransferAccountId = p.readLong()
        mIsChecked = p.readInt() == 1
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
