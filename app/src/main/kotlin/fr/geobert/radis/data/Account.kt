package fr.geobert.radis.data

import android.content.Context
import android.database.Cursor
import android.os.Parcel
import android.os.Parcelable
import fr.geobert.radis.db.AccountTable
import java.util.*
import kotlin.properties.get
import kotlin.properties.set

public class Account(accountId: Long = 0, accountName: String = "") : ImplParcelable {
    override val parcels = hashMapOf<String, Any?>()
    public var id: Long by parcels
    public var name: String by parcels
    public var startSum: Long by parcels
    public var curSum: Long by parcels
    public var currency: String by parcels
    public var projMode: Int by parcels
    public var projDate: String by parcels // TODO better type for this, it is use as MInt for day of month projection or as jj/mm/yyyy for absolute projection
    public var opSum: Long by parcels
    public var checkedSum: Long by parcels
    public var description: String by parcels
    public var lastInsertDate: Long by parcels

    init {
        id = accountId
        name = accountName
        startSum = 0
        curSum = 0
        currency = ""
        projMode = 0
        projDate = ""
        opSum = 0
        checkedSum = 0
        description = ""
        lastInsertDate = 0
    }

    public var curSumDate: Date? = null

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

    constructor(p: Parcel) : this() {
        readFromParcel(p)
    }

    override fun equals(other: Any?): Boolean =
            when (other) {
                is Account -> id == other.id
                else -> false
            }

    override fun toString(): String = name

    fun getCurrencySymbol(ctx: Context): String {
        fun cleanupSymbols(str: String, symb: Array<String>): String {
            return symb.fold(str, { str, symb -> if (str.contains(symb)) symb else str })
        }

        return try {
            val c = Currency.getInstance(currency).symbol
            cleanupSymbols(c, arrayOf("£", "$"))
        } catch (ex: IllegalArgumentException) {
            Currency.getInstance(ctx.resources.configuration.locale).symbol
        }
    }

    companion object {
        @JvmStatic public val CREATOR: Parcelable.Creator<Account> = object : Parcelable.Creator<Account> {
            override fun createFromParcel(p: Parcel): Account {
                return Account(p)
            }

            override fun newArray(size: Int): Array<Account?> {
                return arrayOfNulls(size)
            }
        }
    }

}
