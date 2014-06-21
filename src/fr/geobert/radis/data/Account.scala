package fr.geobert.radis.data

import java.util.Date
import android.database.Cursor
import fr.geobert.radis.db.AccountTable

object Account {
  def apply(cursor: Cursor): Account = {
    @inline def getIdx(s: String) = {
      cursor.getColumnIndex(s)
    }
    val s = new Account
    s.id = cursor.getLong(0)
    s.name = cursor.getString(getIdx(AccountTable.KEY_ACCOUNT_NAME))
    s.startSum = cursor.getInt(getIdx(AccountTable.KEY_ACCOUNT_START_SUM))
    s.curSum = cursor.getInt(getIdx(AccountTable.KEY_ACCOUNT_CUR_SUM))
    s.curSumDate = new Date(cursor.getLong(getIdx(AccountTable.KEY_ACCOUNT_CUR_SUM_DATE)))
    s.currency = cursor.getString(getIdx(AccountTable.KEY_ACCOUNT_CURRENCY))
    s.projMode = cursor.getInt(getIdx(AccountTable.KEY_ACCOUNT_PROJECTION_MODE))
    s.projDate = new Date(cursor.getLong(getIdx(AccountTable.KEY_ACCOUNT_PROJECTION_DATE)))
    s.opSum = cursor.getInt(getIdx(AccountTable.KEY_ACCOUNT_OP_SUM))
    s.checkedSum = cursor.getInt(getIdx(AccountTable.KEY_ACCOUNT_CHECKED_OP_SUM))
    s.description = cursor.getString(getIdx(AccountTable.KEY_ACCOUNT_DESC))
    s
  }
}

class Account(var id: Long = 0, var name: String = "") {
  var startSum = 0
  var curSum = 0
  var curSumDate: Date = null
  var currency = ""
  var projMode = 0
  var projDate: Date = null
  var opSum = 0
  var checkedSum = 0
  var description = ""

  override def equals(o: scala.Any): Boolean =
    o match {
      case account: Account => id == account.id
      case _ => false
    }

  override def toString: String = name
}
