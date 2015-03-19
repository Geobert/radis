package fr.geobert.radis.data

import java.util.Date
import android.database.Cursor
import fr.geobert.radis.db.AccountTable


public fun Account(cursor: Cursor): Account {
    fun getIdx(s: String): Int = cursor.getColumnIndex(s)

    val s = Account()
    s.id = cursor.getLong(0)
    s.name = cursor.getString(getIdx(AccountTable.KEY_ACCOUNT_NAME))
    s.startSum = cursor.getLong(getIdx(AccountTable.KEY_ACCOUNT_START_SUM))
    s.curSum = cursor.getLong(getIdx(AccountTable.KEY_ACCOUNT_CUR_SUM))
    s.curSumDate = Date(cursor.getLong(getIdx(AccountTable.KEY_ACCOUNT_CUR_SUM_DATE)))
    s.currency = cursor.getString(getIdx(AccountTable.KEY_ACCOUNT_CURRENCY))
    s.projMode = cursor.getInt(getIdx(AccountTable.KEY_ACCOUNT_PROJECTION_MODE))
    s.projDate = Date(cursor.getLong(getIdx(AccountTable.KEY_ACCOUNT_PROJECTION_DATE)))
    s.opSum = cursor.getLong(getIdx(AccountTable.KEY_ACCOUNT_OP_SUM))
    s.checkedSum = cursor.getLong(getIdx(AccountTable.KEY_ACCOUNT_CHECKED_OP_SUM))
    s.description = cursor.getString(getIdx(AccountTable.KEY_ACCOUNT_DESC))
    return s
}


public class Account(public var id: Long = 0, public var name: String = "") {
    public var startSum: Long = 0
    public var curSum: Long = 0
    public var curSumDate: Date? = null
    public var currency: String = ""
    public var projMode: Int = 0
    public var projDate: Date? = null
    public var opSum: Long = 0
    public var checkedSum: Long = 0
    public var description: String = ""

    override fun equals(other: Any?): Boolean =
            when (other) {
                is Account -> id == other.id
                else -> false
            }

    override fun toString(): String = name
}
