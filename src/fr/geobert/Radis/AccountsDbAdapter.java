package fr.geobert.Radis;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;

public class AccountsDbAdapter extends CommonDbAdapter {
	protected Cursor mCurAccount;

	public AccountsDbAdapter(Context ctx) {
		super(ctx);
	}

	public AccountsDbAdapter open() throws SQLException {
		super.open();
		return this;
	}

	public long createAccount(String name, String desc, double start_sum,
			String currency) {
		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_ACCOUNT_NAME, name);
		initialValues.put(KEY_ACCOUNT_DESC, desc);
		initialValues.put(KEY_ACCOUNT_START_SUM, start_sum);
		initialValues.put(KEY_ACCOUNT_OP_SUM, 0);
		initialValues.put(KEY_ACCOUNT_CUR_SUM, start_sum);
		initialValues.put(KEY_ACCOUNT_CURRENCY, currency);
		return mDb.insert(DATABASE_ACCOUNT_TABLE, null, initialValues);
	}

	public boolean deleteAccount(long rowId) {
		mDb.execSQL(String.format(DATABASE_OP_DROP, rowId));
		return mDb.delete(DATABASE_ACCOUNT_TABLE, KEY_ACCOUNT_ROWID + "="
				+ rowId, null) > 0;
	}

	public Cursor fetchAllAccounts() {
		return mDb.query(DATABASE_ACCOUNT_TABLE, new String[] {
				KEY_ACCOUNT_ROWID, KEY_ACCOUNT_NAME, KEY_ACCOUNT_CUR_SUM,
				KEY_ACCOUNT_CURRENCY }, null, null, null, null, null);
	}

	public Cursor fetchAccount(long rowId) throws SQLException {
		mCurAccount = mDb.query(true, DATABASE_ACCOUNT_TABLE, new String[] {
				KEY_ACCOUNT_ROWID, KEY_ACCOUNT_NAME, KEY_ACCOUNT_DESC,
				KEY_ACCOUNT_START_SUM, KEY_ACCOUNT_CUR_SUM, KEY_ACCOUNT_OP_SUM,
				KEY_ACCOUNT_CURRENCY }, KEY_ACCOUNT_ROWID + "=" + rowId, null,
				null, null, null, null);
		Cursor c = mCurAccount;
		if (c != null) {
			c.moveToFirst();
		}
		return c;

	}

	public boolean updateAccount(long rowId, String name, String desc,
			double start_sum, String currency) {
		ContentValues args = new ContentValues();
		args.put(KEY_ACCOUNT_NAME, name);
		args.put(KEY_ACCOUNT_DESC, desc);
		args.put(KEY_ACCOUNT_START_SUM, start_sum);
		args.put(KEY_ACCOUNT_CURRENCY, currency);
		return mDb.update(DATABASE_ACCOUNT_TABLE, args, KEY_ACCOUNT_ROWID + "="
				+ rowId, null) > 0;
	}

	public boolean updateOpSum(long rowId, double newSum) {
		ContentValues args = new ContentValues();
		args.put(KEY_ACCOUNT_OP_SUM, newSum);
		return mDb.update(DATABASE_ACCOUNT_TABLE, args, KEY_ACCOUNT_ROWID + "="
				+ rowId, null) > 0;
	}

	public double updateCurrentSum(long rowId) {
		Cursor account = getCurAccountIfDiff(rowId);
		double start = account
				.getDouble(account
						.getColumnIndexOrThrow(AccountsDbAdapter.KEY_ACCOUNT_START_SUM));
		double opSum = account.getDouble(account
				.getColumnIndexOrThrow(AccountsDbAdapter.KEY_ACCOUNT_OP_SUM));
		ContentValues args = new ContentValues();
		double curSum = start + opSum;
		args.put(KEY_ACCOUNT_CUR_SUM, curSum);
		mDb.update(DATABASE_ACCOUNT_TABLE, args, KEY_ACCOUNT_ROWID + "="
				+ rowId, null);
		return curSum;
	}

	private Cursor getCurAccountIfDiff(long rowId) {
		Cursor account = mCurAccount;
		if (null == account
				|| !account.isFirst()
				|| account
						.getLong(account
								.getColumnIndexOrThrow(AccountsDbAdapter.KEY_ACCOUNT_ROWID)) != rowId) {
			account = fetchAccount(rowId);
		} else {
			account.requery();
			account.moveToFirst();
		}
		return account;
	}
}
