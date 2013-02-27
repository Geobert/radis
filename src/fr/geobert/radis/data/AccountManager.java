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
        if (this.mAllAccountsCursor != null) {
            this.mAllAccountsCursor.close();
        }
        this.mAllAccountsCursor = cursor;
        this.mSimpleCursorAdapter.changeCursor(cursor);
        if (cursor != null) {
            cursor.moveToFirst();
        }
    }

    public void setSimpleCursorAdapter(SimpleCursorAdapter adapter) {
        this.mSimpleCursorAdapter = adapter;
    }

    public Long getDefaultAccountId(Context context) {
        return DBPrefsManager.getInstance(context).getLong(RadisConfiguration.KEY_DEFAULT_ACCOUNT);
    }

    public Long getCurrentAccountId(Context context) {
        if (mCurAccountId != null) {
            return mCurAccountId;
        } else if (getDefaultAccountId(context) != null) {
            setCurrentAccountId(getCurrentAccountId(context));
        } else if (mAllAccountsCursor != null && mAllAccountsCursor.getCount() > 0) {
            setCurrentAccountId(mAllAccountsCursor.getLong(
                    mAllAccountsCursor.getColumnIndex(AccountTable.KEY_ACCOUNT_ROWID)));
        } else {
            return null;
        }
        return mCurAccountId;
    }

    public void setCurrentAccountId(Long currentAccountId) {
        this.mCurAccountId = currentAccountId;
    }
}
