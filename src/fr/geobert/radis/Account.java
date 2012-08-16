package fr.geobert.radis;

import android.database.Cursor;
import fr.geobert.radis.db.AccountTable;

// used only in schedule op list for the moment, should be completed to be used in editor
public class Account {
	public long mAccountId;
	public String mName;
	
	public Account(Cursor c) {
		mAccountId = c.getLong(c.getColumnIndex(AccountTable.KEY_ACCOUNT_ROWID));
		mName = c.getString(c.getColumnIndex(AccountTable.KEY_ACCOUNT_NAME));
	}
	
	public Account(final long accountId, final String name) {
		mAccountId = accountId;
		mName = name;
	}
	
	@Override
	public String toString() {
		return mName;
	}
}
