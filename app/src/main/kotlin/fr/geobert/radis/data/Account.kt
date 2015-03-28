package fr.geobert.radis.data

import android.database.Cursor
import fr.geobert.radis.db.AccountTable
import java.util.Date


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
    s.overrideHideQuickAdd = cursor.getInt(getIdx(AccountTable.KEY_ACCOUNT_OVERRIDE_HIDE_QUICK_ADD)) == 1
    s.overrideInsertDate = cursor.getInt(getIdx(AccountTable.KEY_ACCOUNT_OVERRIDE_INSERT_DATE)) == 1
    s.overrideInvertQuickAddComp = cursor.getInt(getIdx(AccountTable.KEY_ACCOUNT_OVERRIDE_INVERT_QUICKADD_COMPLETION)) == 1
    s.overrideUseWeighedInfo = cursor.getInt(getIdx(AccountTable.KEY_ACCOUNT_OVERRIDE_USE_WEIGHTED_INFO)) == 1
    s.hideQuickAdd = cursor.getInt(getIdx(AccountTable.KEY_ACCOUNT_HIDE_QUICK_ADD)) == 1
    s.useWeighedInfo = cursor.getInt(getIdx(AccountTable.KEY_ACCOUNT_USE_WEIGHTED_INFO)) == 1
    s.invertQuickAddComp = cursor.getInt(getIdx(AccountTable.KEY_ACCOUNT_INVERT_QUICKADD_COMPLETION)) == 1
    s.insertDate = cursor.getInt(getIdx(AccountTable.KEY_ACCOUNT_INSERT_DATE))
    s.lastInsertDate = cursor.getLong(getIdx(AccountTable.KEY_ACCOUNT_LAST_INSERTION_DATE))
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
    public var overrideHideQuickAdd: Boolean = false
    public var overrideInvertQuickAddComp: Boolean = false
    public var overrideInsertDate: Boolean = false
    public var overrideUseWeighedInfo: Boolean = false
    public var hideQuickAdd: Boolean = false
    public var invertQuickAddComp: Boolean = true
    public var insertDate: Int = 0
    public var lastInsertDate: Long = 0L
    public var useWeighedInfo: Boolean = true


    override fun equals(other: Any?): Boolean =
            when (other) {
                is Account -> id == other.id
                else -> false
            }

    override fun toString(): String = name
}
