package fr.geobert.radis.data

import android.database.Cursor
import android.os.Parcel
import android.os.Parcelable
import fr.geobert.radis.db.AccountTable
import java.util.Date

public class Account(public var id: Long = 0, public var name: String = "") : ImplParcelable {
    private var _startSum: MLong = MLong(0)
    private var _curSum: MLong = MLong(0)
    private var _currency: MString = MString("")
    private var _projMode: MInt = MInt(0)
    private var _projDate: MString = MString("") // TODO better type for this, it is use as MInt for day of month projection or as jj/mm/yyyy for absolute projection
    private var _opSum: MLong = MLong(0)
    private var _checkedSum: MLong = MLong(0)
    private var _description: MString = MString("")
    private var _lastInsertDate: MLong = MLong(0)

    public var curSumDate: Date? = null

    override val parcels = listOf(_startSum, _curSum, _currency, _projMode, _projDate, _opSum, _checkedSum, _description, _lastInsertDate)

    constructor(cursor: Cursor) : this() {
        fun getIdx(s: String): Int = cursor.getColumnIndex(s)

        id = cursor.getLong(0)
        name = cursor.getString(getIdx(AccountTable.KEY_ACCOUNT_NAME))
        startSum = cursor.getLong(getIdx(AccountTable.KEY_ACCOUNT_START_SUM))
        curSum = cursor.getLong(getIdx(AccountTable.KEY_ACCOUNT_CUR_SUM))
        curSumDate = Date(cursor.getLong(getIdx(AccountTable.KEY_ACCOUNT_CUR_SUM_DATE)))
        currency = cursor.getString(getIdx(AccountTable.KEY_ACCOUNT_CURRENCY))
        projMode = cursor.getInt(getIdx(AccountTable.KEY_ACCOUNT_PROJECTION_MODE))
        projDate = cursor.getString(getIdx(AccountTable.KEY_ACCOUNT_PROJECTION_DATE))
        opSum = cursor.getLong(getIdx(AccountTable.KEY_ACCOUNT_OP_SUM))
        checkedSum = cursor.getLong(getIdx(AccountTable.KEY_ACCOUNT_CHECKED_OP_SUM))
        description = cursor.getString(getIdx(AccountTable.KEY_ACCOUNT_DESC))
        lastInsertDate = cursor.getLong(getIdx(AccountTable.KEY_ACCOUNT_LAST_INSERTION_DATE))
    }

    override fun equals(other: Any?): Boolean =
            when (other) {
                is Account -> id == other.id
                else -> false
            }

    override fun toString(): String = name


    public var startSum: Long
        get() = _startSum.get()
        set(v) = _startSum.set(v)
    public var curSum: Long
        get() = _curSum.get()
        set(v) = _curSum.set(v)
    public var currency: String
        get() = _currency.get()
        set(v) = _currency.set(v)
    public var projMode: Int
        get() = _projMode.get()
        set(v) = _projMode.set(v)
    public var projDate: String
        get() = _projDate.get()
        set(v) = _projDate.set(v)
    public var opSum: Long
        get() = _opSum.get()
        set(v) = _opSum.set(v)
    public var checkedSum: Long
        get() = _checkedSum.get()
        set(v) = _checkedSum.set(v)
    public var description: String
        get() = _description.get()
        set(v) = _description.set(v)
    public var lastInsertDate: Long
        get() = _lastInsertDate.get()
        set(v) = _lastInsertDate.set(v)
}
