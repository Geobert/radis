package fr.geobert.radis.data

import android.content.Context
import android.database.Cursor
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.support.v4.app.LoaderManager
import android.support.v4.content.CursorLoader
import android.support.v4.content.Loader
import android.support.v4.widget.SimpleCursorAdapter
import android.util.Log
import fr.geobert.radis.db.AccountTable
import fr.geobert.radis.db.PreferenceTable
import fr.geobert.radis.tools.DBPrefsManager
import fr.geobert.radis.tools.map
import fr.geobert.radis.ui.ConfigFragment
import fr.geobert.radis.ui.adapter.AccountAdapter
import java.util.ArrayList
import java.util.Currency

public class AccountManager(val ctx: FragmentActivity) : LoaderManager.LoaderCallbacks<Cursor> {
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

    public var mAccountAdapter: AccountAdapter = AccountAdapter(ctx)
        private set

    public var mCurAccountConfig: AccountConfig? = null
        private set

    private var isFetching = false

    public fun getDefaultAccountId(context: Context): Long? {
        //        Log.d(TAG, "--->getDefaultAccountId, curDefAcc:$mCurDefaultAccount")
        if (this.mCurDefaultAccount == null) {
            this.mCurDefaultAccount = DBPrefsManager.getInstance(context).getLong(ConfigFragment.KEY_DEFAULT_ACCOUNT)
            if (mCurDefaultAccount == null) {
                //                Log.d(TAG, "getDefaultAccountId, no pref for def account")
                // no pref set, take the first account, set it as default
                if (mAccountAdapter.getCount() > 0) {
                    this.mCurDefaultAccount = mAccountAdapter.getAccount(0).id
                    DBPrefsManager.getInstance(context).put(ConfigFragment.KEY_DEFAULT_ACCOUNT, mCurDefaultAccount)
                } else {
                    this.mCurDefaultAccount = 1 // fallback, should not happen
                }
            }
        }
        //        Log.d(TAG, "<---getDefaultAccountId, curDefAcc:$mCurDefaultAccount")
        return this.mCurDefaultAccount
    }

    public fun getCurrentAccountId(context: Context): Long {
        //        Log.d(TAG, "--->getCurrentAccountId, backup:$mCurAccountIdBackup, curAccId:$mCurAccountId, accCursor:${mAccountAdapter?.getCount()}")
        if (mCurAccountIdBackup != null) {
            setCurrentAccountId(mCurAccountIdBackup, context)
            clearBackup()
        } else if (mCurAccountId != null) {
            //            Log.d(TAG, "<---getCurrentAccountId, curAcc:$mCurAccountId")
            return mCurAccountId!!
        } else if (getDefaultAccountId(context) != null) {
            setCurrentAccountId(getDefaultAccountId(context), context)
        } else if (mAccountAdapter.getCount() > 0) {
            setCurrentAccountId(mAccountAdapter.getAccount(0).id, context)
        } else {
            throw RuntimeException("No current account!")
        }
        //        Log.d(TAG, "<---getCurrentAccountId, curAcc:$mCurAccountId")
        return mCurAccountId!!
    }

    public fun getCurrentAccountPosition(ctx: Context): Int {
        //        Log.d(TAG, "--->getCurrentAccountPosition, mCurAccountPos:$mCurAccountPos")
        if (mCurAccountPos == -1) {
            getCurrentAccountId(ctx)
        }
        //        Log.d(TAG, "<---getCurrentAccountPosition, mCurAccountPos:$mCurAccountPos")
        return mCurAccountPos
    }

    public fun getCurrentAccount(ctx: Context): Account {
        return mAccountAdapter.getAccount(getCurrentAccountPosition(ctx))
    }

    public fun setCurrentAccountSum() {
        var pos = 0
        val curId = mCurAccountId
        if (curId != null && curId > 0) {
            for (acc in mAccountAdapter) {
                if (curId == acc.id) {
                    AccountTable.initProjectionDate(acc)
                    currentAccountSum = acc.curSum
                    mCurAccCurrencySymbol = acc.getCurrencySymbol(ctx)
                    currentAccountStartSum = acc.startSum
                    break
                }
                pos++
            }
        }
        mCurAccountPos = pos
    }


    public fun setCurrentAccountId(currentAccountId: Long?, ctx: Context): Long {
        mCurAccountId = currentAccountId
        if (currentAccountId == null) {
            mCurAccountPos = -1
            mCurAccountConfig = null
            return (-1).toLong()
        } else {
            refreshConfig(ctx, currentAccountId)
            setCurrentAccountSum()
            return getCurrentAccountProjDate()
        }
    }

    public fun refreshConfig(ctx: Context, id: Long) {
        Log.d(TAG, "refreshConfig, acc id:$id")
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

    fun refreshCurrentAccount() {
        val cursor = AccountTable.fetchAccount(ctx, getCurrentAccountId(ctx))
        if (cursor.moveToFirst()) {
            updateAccount(Account(cursor))
        }
    }

    public fun fetchAllAccounts(force: Boolean, cbk: () -> Any?) {
        Log.d(TAG, ">>>fetchAllAccounts:$force, fetching:$isFetching, empty:${mAccountAdapter.isEmpty()} ")
        this.mCallbacks.add(cbk)
        if (!isFetching) {
            isFetching = true
            if (force || mAccountAdapter.isEmpty()) {
                if (mAccountLoader == null) {
                    ctx.getSupportLoaderManager().initLoader<Cursor>(GET_ACCOUNTS, Bundle(), this)
                } else {
                    ctx.getSupportLoaderManager().restartLoader<Cursor>(GET_ACCOUNTS, Bundle(), this)
                }
            } else {
                Log.d(TAG, "<<<fetchAllAccounts:$force")
                execCbks()
                isFetching = false
            }
        }
    }

    //    public fun getCurrentAccount(): Account? {
    //        return allAccountsCursor?.getByFilter({ it.getLong(0) == getCurrentAccountId(ctx) }, { Account(it) })
    //    }

    override fun onCreateLoader(i: Int, bundle: Bundle?): Loader<Cursor> {
        val loader = AccountTable.getAllAccountsLoader(ctx)
        mAccountLoader = loader
        return loader
    }

    fun execCbks() {
        val cbks = ArrayList(mCallbacks)
        for (r in cbks) {
            r()
        }
        mCallbacks.clear()
    }

    override fun onLoadFinished(cursorLoader: Loader<Cursor>, cursor: Cursor) {
        when (cursorLoader.getId()) {
            GET_ACCOUNTS -> {
                mAccountAdapter.swapCursor(cursor)
                if (mCurAccountId != null) {
                    setCurrentAccountSum()
                }
                Log.d(TAG, "onLoadFinished: cursor:${cursor.getCount()}, count:${mAccountAdapter.getCount()}")
                execCbks()
                isFetching = false
            }
        }
    }

    private fun updateAccount(a: Account) {
        for (acc in mAccountAdapter) {
            if (acc.id == a.id) {
                acc.curSumDate = a.curSumDate
                acc.curSum = a.curSum
                acc.checkedSum = a.checkedSum
                acc.currency = a.currency
                acc.lastInsertDate = a.lastInsertDate
                acc.opSum = a.opSum
                acc.name = a.name
                acc.description = a.description
                acc.startSum = a.startSum
                acc.projMode = a.projMode
                acc.projDate = a.projDate
                mAccountAdapter.notifyDataSetChanged()
                return
            }
        }
    }

    override fun onLoaderReset(cursorLoader: Loader<Cursor>) {
    }

    public fun getCurrentAccountCheckedSum(): Long {
        return AccountTable.getCheckedSum(ctx, mCurAccountId)
    }

    companion object {
        private val GET_ACCOUNTS = 200
        private val GET_ONE_ACCOUNT = 210
        private val TAG = "AccountManager"
    }


}
