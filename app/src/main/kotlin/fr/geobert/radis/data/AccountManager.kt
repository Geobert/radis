package fr.geobert.radis.data

import android.content.Context
import android.database.Cursor
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.support.v4.app.LoaderManager
import android.support.v4.content.CursorLoader
import android.support.v4.content.Loader
import android.support.v4.widget.SimpleCursorAdapter
import fr.geobert.radis.db.AccountTable
import fr.geobert.radis.db.PreferenceTable
import fr.geobert.radis.tools.DBPrefsManager
import fr.geobert.radis.tools.getByFilter
import fr.geobert.radis.ui.ConfigFragment
import java.util.ArrayList
import java.util.Currency
import kotlin.properties.Delegates

public class AccountManager(val ctx: FragmentActivity) : LoaderManager.LoaderCallbacks<Cursor> {
    private var _allAccountCursor: Cursor? = null
    public var allAccountsCursor: Cursor?
        public set(value) {
            setAllAccCursor(value)
        }
        get() = _allAccountCursor

    private var mSimpleCursorAdapter: SimpleCursorAdapter? = null
    private var mCurAccountId: Long? = null
    public var currentAccountSum: Long = 0
        protected set
    private var mCurAccountPos = -1
    private var mCurAccountIdBackup: Long? = null
    public var mCurDefaultAccount: Long? = null
    private val mCallbacks: ArrayList<() -> Any?> = ArrayList()
    private var mAccountLoader: CursorLoader? = null
    private var mCurAccCurrencySymbol: String? = null
    public var currentAccountStartSum: Long = 0
        private set

    public var mCurAccountConfig: AccountConfig? = null
        private set

    throws(javaClass<IllegalStateException>())
    private fun setAllAccCursor(cursor: Cursor?) {
        val c = this.allAccountsCursor
        if (c != null && c != cursor) {
            c.close()
        }

        if (cursor != null && cursor.moveToFirst()) {
            _allAccountCursor = cursor
            mSimpleCursorAdapter?.changeCursor(cursor)
            if (mCurAccountId != null) {
                setCurrentAccountSum()
            }
        } else {
            _allAccountCursor = null
            this.mSimpleCursorAdapter?.changeCursor(null)
        }
    }

    public fun setSimpleCursorAdapter(adapter: SimpleCursorAdapter) {
        this.mSimpleCursorAdapter = adapter
    }

    public fun getDefaultAccountId(context: Context): Long? {
        if (this.mCurDefaultAccount == null) {
            this.mCurDefaultAccount = DBPrefsManager.getInstance(context).getLong(ConfigFragment.KEY_DEFAULT_ACCOUNT)
            if (mCurDefaultAccount == null) {
                // no pref set, take the first account, set it as default
                if (allAccountsCursor?.moveToFirst() ?: false) {
                    this.mCurDefaultAccount = this.allAccountsCursor?.getLong(0)
                    DBPrefsManager.getInstance(context).put(ConfigFragment.KEY_DEFAULT_ACCOUNT, mCurDefaultAccount)
                }
            }
        }
        return this.mCurDefaultAccount
    }

    public fun getCurrentAccountId(context: Context): Long {
        if (mCurAccountIdBackup != null) {
            setCurrentAccountId(mCurAccountIdBackup, context)
            clearBackup()
        } else if (mCurAccountId != null) {
            return mCurAccountId!!
        } else if (getDefaultAccountId(context) != null) {
            setCurrentAccountId(getDefaultAccountId(context), context)
        } else if (allAccountsCursor != null && allAccountsCursor?.getCount() ?: 0 > 0) {
            setCurrentAccountId(allAccountsCursor!!.getLong(allAccountsCursor!!.getColumnIndex(AccountTable.KEY_ACCOUNT_ROWID)), context)
        } else {
            throw RuntimeException("No current account!")
        }
        return mCurAccountId!!
    }

    public fun getCurrentAccountPosition(ctx: Context): Int {
        if (mCurAccountPos == -1) {
            getCurrentAccountId(ctx)
        }
        return mCurAccountPos
    }


    private fun setCurrentAccountSum(): Int {
        var pos = 0
        if (allAccountsCursor != null) {
            allAccountsCursor!!.moveToFirst()
            do {
                if (mCurAccountId == allAccountsCursor!!.getLong(0)) {
                    val curSumIdx = allAccountsCursor!!.getColumnIndex(AccountTable.KEY_ACCOUNT_CUR_SUM)
                    val currencyIdx = allAccountsCursor!!.getColumnIndex(AccountTable.KEY_ACCOUNT_CURRENCY)
                    AccountTable.initProjectionDate(allAccountsCursor)
                    currentAccountSum = allAccountsCursor!!.getLong(curSumIdx)
                    val curStr = allAccountsCursor!!.getString(currencyIdx)
                    try {
                        mCurAccCurrencySymbol = Currency.getInstance(curStr).getSymbol()
                    } catch (e: IllegalArgumentException) {
                        mCurAccCurrencySymbol = Currency.getInstance(ctx.getResources().getConfiguration().locale).getSymbol()
                    }

                    currentAccountStartSum = allAccountsCursor!!.getLong(allAccountsCursor!!.getColumnIndex(AccountTable.KEY_ACCOUNT_START_SUM))
                    break
                }
                pos++
            } while (allAccountsCursor!!.moveToNext())
            allAccountsCursor!!.moveToFirst()
        }
        return pos
    }


    public fun setCurrentAccountId(currentAccountId: Long?, ctx: Context): Long {
        mCurAccountId = currentAccountId
        if (currentAccountId == null) {
            mCurAccountPos = -1
            mCurAccountConfig = null
            return (-1).toLong()
        } else {
            refreshConfig(ctx, currentAccountId)
            mCurAccountPos = setCurrentAccountSum()
            return getCurrentAccountProjDate()
        }
    }

    public fun refreshConfig(ctx: Context, id: Long) {
        mCurAccountConfig = AccountConfig(PreferenceTable.fetchPrefForAccount(ctx, id))
    }

    private fun getCurrentAccountProjDate(): Long {
        return AccountTable.getProjectionDate()
    }

    public fun backupCurAccountId() {
        this.mCurAccountIdBackup = mCurAccountId
    }

    public fun clearBackup() {
        this.mCurAccountIdBackup = null
    }

    synchronized public fun fetchAllAccounts(force: Boolean, cbk: () -> Any?) {
        if (force || allAccountsCursor == null || allAccountsCursor?.getCount() == 0 || allAccountsCursor?.isClosed() ?: false) {
            this.mCallbacks.add(cbk)
            if (mAccountLoader == null) {
                ctx.getSupportLoaderManager().initLoader<Cursor>(GET_ACCOUNTS, Bundle(), this)
            } else {
                ctx.getSupportLoaderManager().restartLoader<Cursor>(GET_ACCOUNTS, Bundle(), this)
            }
        } else {
            cbk()
        }
    }

    public fun getCurrentAccount(): Account? {
        return allAccountsCursor?.getByFilter({ it.getLong(0) == getCurrentAccountId(ctx) }, { Account(it) })
    }

    override fun onCreateLoader(i: Int, bundle: Bundle?): Loader<Cursor> {
        val loader = AccountTable.getAllAccountsLoader(ctx)
        mAccountLoader = loader
        return loader
    }

    override fun onLoadFinished(cursorLoader: Loader<Cursor>, cursor: Cursor) {
        if (cursorLoader.getId() == GET_ACCOUNTS) {
            allAccountsCursor = cursor
            val cbks = ArrayList(mCallbacks)
            for (r in cbks) {
                r()
            }
            mCallbacks.clear()
        }
    }

    override fun onLoaderReset(cursorLoader: Loader<Cursor>) {
    }

    public fun getCurrentAccountCheckedSum(): Long {
        return AccountTable.getCheckedSum(ctx, mCurAccountId)
    }

    companion object {
        private val GET_ACCOUNTS = 200
    }

}
