package fr.geobert.radis.data;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.SimpleCursorAdapter;
import fr.geobert.radis.RadisConfiguration;
import fr.geobert.radis.db.AccountTable;
import fr.geobert.radis.tools.DBPrefsManager;

public class AccountManager {
    private static AccountManager ourInstance = new AccountManager();
    private Cursor mAllAccountsCursor;
    private SimpleCursorAdapter mSimpleCursorAdapter;
    private Long mCurAccountId = null;
    protected long mCurSum;
    private int mCurAccountPos = -1;
    private Long mCurAccountIdBackup = null;
    public Long mCurDefaultAccount = null;

    private AccountManager() {

    }

    public static AccountManager getInstance() {
        return ourInstance;
    }

    public Cursor getAllAccountsCursor() {
        return mAllAccountsCursor;
    }

    public void setAllAccountsCursor(Cursor cursor) throws IllegalStateException {
        if (mSimpleCursorAdapter == null) {
            throw new IllegalStateException("Must call setSimpleCursorAdapter first");
        }
        if (this.mAllAccountsCursor != null && this.mAllAccountsCursor != cursor) {
            this.mAllAccountsCursor.close();
        }
        if (cursor != null && cursor.moveToFirst()) {
            this.mAllAccountsCursor = cursor;
            this.mSimpleCursorAdapter.changeCursor(cursor);
            if (mCurAccountId != null) {
                setCurrentAccountSum();
            }
        } else {
            this.mAllAccountsCursor = null;
            this.mSimpleCursorAdapter.changeCursor(null);
        }
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
                if (mCurAccountId.longValue() == mAllAccountsCursor.getLong(0)) {
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
}
