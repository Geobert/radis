package fr.geobert.radis.data

import android.database.Cursor
import android.support.v4.widget.SimpleCursorAdapter
import android.support.v4.content.{Loader, CursorLoader}
import android.support.v4.app.{LoaderManager, FragmentActivity}
import android.os.Bundle

object SAccountManager extends LoaderManager.LoaderCallbacks[Cursor] {
  private final val GET_ACCOUNTS: Int = 200
  private var mAllAccounts: List[Account] = List()
  private var mSimpleCursorAdapter: SimpleCursorAdapter = null
  private var mCurAccountId: Long = 0L
  protected var mCurSum: Long = 0L
  private var mCurAccountPos: Int = -1
  private var mCurAccountIdBackup: Long = 0L
  var mCurDefaultAccount: Long = 0L
  private var mCallbacks: List[Runnable] = List()
  private var mAccountLoader: CursorLoader = null
  private var mCtx: FragmentActivity = null
  private var mCurAccCurrencySymbol: String = null
  private var mStartSum: Long = 0L

  override def onCreateLoader(p1: Int, p2: Bundle): Loader[Cursor] = ???

  override def onLoaderReset(p1: Loader[Cursor]): Unit = ???

  override def onLoadFinished(p1: Loader[Cursor], p2: Cursor): Unit = ???
}
