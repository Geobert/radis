package fr.geobert.radis.data;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import fr.geobert.radis.RadisConfiguration;
import fr.geobert.radis.db.AccountTable;
import fr.geobert.radis.tools.DBPrefsManager;

import java.util.ArrayList;

public class AccountManager implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final int GET_ACCOUNTS = 200;
    private Cursor mAllAccountsCursor;
    private SimpleCursorAdapter mSimpleCursorAdapter;
    private Long mCurAccountId = null;
    protected long mCurSum;
    private int mCurAccountPos = -1;
    private Long mCurAccountIdBackup = null;
    public Long mCurDefaultAccount = null;
    private ArrayList<Runnable> mCallbacks;
    private CursorLoader mAccountLoader;
    private FragmentActivity mCtx;

    public AccountManager() {
        mCallbacks = new ArrayList<Runnable>();
    }

    public Cursor getAllAccountsCursor() {
        return mAllAccountsCursor;
    }

    public void setAllAccountsCursor(Cursor cursor) throws IllegalStateException {
        if (this.mAllAccountsCursor != null && this.mAllAccountsCursor != cursor) {
            this.mAllAccountsCursor.close();
        }

        if (cursor != null && cursor.moveToFirst()) {
            this.mAllAccountsCursor = cursor;
            if (mSimpleCursorAdapter != null) {
                this.mSimpleCursorAdapter.changeCursor(cursor);
            }
            if (mCurAccountId != null) {
                setCurrentAccountSum();
            }
        } else {
            this.mAllAccountsCursor = null;
            if (mSimpleCursorAdapter != null) {
                this.mSimpleCursorAdapter.changeCursor(null);
            }
        }
    }

    public SimpleCursorAdapter getSimpleCursorAdapter() {
        return mSimpleCursorAdapter;
    }

    public void setSimpleCursorAdapter(SimpleCursorAdapter adapter) {
        this.mSimpleCursorAdapter = adapter;
    }

    public Long getDefaultAccountId(Context context) {
        if (this.mCurDefaultAccount == null) {
            this.mCurDefaultAccount = DBPrefsManager.getInstance(context).getLong(RadisConfiguration.KEY_DEFAULT_ACCOUNT);
        }
        return DBPrefsManager.getInstance(context).getLong(RadisConfiguration.KEY_DEFAULT_ACCOUNT);
    }

    public Long getCurrentAccountId(Context context) {
        if (mCurAccountIdBackup != null) {
            setCurrentAccountId(mCurAccountIdBackup);
            clearBackup();
        } else if (mCurAccountId != null) {
            return mCurAccountId;
        } else if (getDefaultAccountId(context) != null) {
            setCurrentAccountId(getDefaultAccountId(context));
        } else if (mAllAccountsCursor != null && mAllAccountsCursor.getCount() > 0) {
            setCurrentAccountId(mAllAccountsCursor.getLong(
                    mAllAccountsCursor.getColumnIndex(AccountTable.KEY_ACCOUNT_ROWID)));
        } else {
            return null;
        }
        return mCurAccountId;
    }

    public int getCurrentAccountPosition(Context ctx) {
        if (mCurAccountPos == -1) {
            getCurrentAccountId(ctx);
        }
        return mCurAccountPos;
    }

    private int setCurrentAccountSum() {
        int pos = 0;
        if (mAllAccountsCursor != null) {
            mAllAccountsCursor.moveToFirst();
            final int curSumIdx = mAllAccountsCursor.getColumnIndex(AccountTable.KEY_ACCOUNT_CUR_SUM);
            do {
                if (mCurAccountId == mAllAccountsCursor.getLong(0)) {
                    AccountTable.initProjectionDate(mAllAccountsCursor);
                    mCurSum = mAllAccountsCursor.getLong(curSumIdx);
                    break;
                }
                pos++;
            } while (mAllAccountsCursor.moveToNext());
            mAllAccountsCursor.moveToFirst();
        }
        return pos;
    }

    public void setCurrentAccountId(Long currentAccountId) {
        mCurAccountId = currentAccountId;
        if (currentAccountId == null) {
            mCurAccountPos = -1;
        } else {
            mCurAccountPos = setCurrentAccountSum();
        }
    }

    public long getCurrentAccountSum() {
        return mCurSum;
    }

    public void backupCurAccountId() {
        this.mCurAccountIdBackup = mCurAccountId;
    }

    public void clearBackup() {
        this.mCurAccountIdBackup = null;
    }

    public synchronized void fetchAllAccounts(FragmentActivity activity, final boolean force, Runnable cbk) {
        this.mCtx = activity;
        if (force || mAllAccountsCursor == null || mAllAccountsCursor.getCount() == 0 || mAllAccountsCursor.isClosed()) {
            this.mCallbacks.add(cbk);
            if (mAccountLoader == null) {
                mCtx.getSupportLoaderManager().initLoader(GET_ACCOUNTS, null, this);
            } else {
                mCtx.getSupportLoaderManager().restartLoader(GET_ACCOUNTS, null, this);
            }
        } else {
            cbk.run();
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        mAccountLoader = AccountTable.getAllAccountsLoader(mCtx);
        return mAccountLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (cursorLoader.getId() == GET_ACCOUNTS) {
            setAllAccountsCursor(cursor);
            ArrayList<Runnable> cbks = new ArrayList<Runnable>(mCallbacks);
            for (Runnable r : cbks) {
                r.run();
            }
            mCallbacks.clear();
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
    }
}
