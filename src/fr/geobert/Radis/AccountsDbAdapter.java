package fr.geobert.Radis;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;

public class AccountsDbAdapter extends CommonDbAdapter {
	/**
	 * Constructor - takes the context to allow the database to be
	 * opened/created
	 * 
	 * @param ctx
	 *            the Context within which to work
	 */
	public AccountsDbAdapter(Context ctx) {
		super(ctx);
	}

	/**
	 * Open the notes database. If it cannot be opened, try to create a new
	 * instance of the database. If it cannot be created, throw an exception to
	 * signal the failure
	 * 
	 * @return this (self reference, allowing this to be chained in an
	 *         initialization call)
	 * @throws SQLException
	 *             if the database could be neither opened or created
	 */
	public AccountsDbAdapter open() throws SQLException {
		super.open();
		return this;
	}

	/**
	 * Create a new note using the title and body provided. If the note is
	 * successfully created return the new rowId for that note, otherwise return
	 * a -1 to indicate failure.
	 * 
	 * @param title
	 *            the title of the note
	 * @param body
	 *            the body of the note
	 * @return rowId or -1 if failed
	 */
	public long createAccount(String name, String desc, double start_sum,
			String currency) {
		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_ACCOUNT_NAME, name);
		initialValues.put(KEY_ACCOUNT_DESC, desc);
		initialValues.put(KEY_ACCOUNT_START_SUM, start_sum);
		initialValues.put(KEY_ACCOUNT_OP_SUM, 0);
		initialValues.put(KEY_ACCOUNT_CUR_SUM, start_sum);
		initialValues.put(KEY_ACCOUNT_CURRENCY, currency);
		long rowId = mDb.insert(DATABASE_ACCOUNT_TABLE, null, initialValues);
		try {
			mDb.execSQL(String.format(DATABASE_OP_CREATE, rowId));
		} catch (Exception e) {
			int i = 0;
			i = i + 1;
		}
		return rowId;
	}

	/**
	 * Delete the note with the given rowId
	 * 
	 * @param rowId
	 *            id of note to delete
	 * @return true if deleted, false otherwise
	 */
	public boolean deleteAccount(long rowId) {
		mDb.execSQL(String.format(DATABASE_OP_DROP, rowId));
		return mDb
				.delete(DATABASE_ACCOUNT_TABLE, KEY_ACCOUNT_ROWID + "=" + rowId, null) > 0;
	}

	/**
	 * Return a Cursor over the list of all notes in the database
	 * 
	 * @return Cursor over all notes
	 */
	public Cursor fetchAllAccounts() {
		return mDb.query(DATABASE_ACCOUNT_TABLE, new String[] { KEY_ACCOUNT_ROWID,
				KEY_ACCOUNT_NAME, KEY_ACCOUNT_CUR_SUM, KEY_ACCOUNT_CURRENCY },
				null, null, null, null, null);
	}

	/**
	 * Return a Cursor positioned at the note that matches the given rowId
	 * 
	 * @param rowId
	 *            id of note to retrieve
	 * @return Cursor positioned to matching note, if found
	 * @throws SQLException
	 *             if note could not be found/retrieved
	 */
	public Cursor fetchAccount(long rowId) throws SQLException {
		Cursor mCursor = mDb.query(true, DATABASE_ACCOUNT_TABLE, new String[] {
				KEY_ACCOUNT_ROWID, KEY_ACCOUNT_NAME, KEY_ACCOUNT_DESC,
				KEY_ACCOUNT_START_SUM, KEY_ACCOUNT_CUR_SUM, KEY_ACCOUNT_OP_SUM,
				KEY_ACCOUNT_CURRENCY }, KEY_ACCOUNT_ROWID + "=" + rowId, null,
				null, null, null, null);
		if (mCursor != null) {
			mCursor.moveToFirst();
		}
		return mCursor;

	}

	/**
	 * Update the note using the details provided. The note to be updated is
	 * specified using the rowId, and it is altered to use the title and body
	 * values passed in
	 * 
	 * @param rowId
	 *            id of note to update
	 * @param title
	 *            value to set note title to
	 * @param body
	 *            value to set note body to
	 * @return true if the note was successfully updated, false otherwise
	 */
	public boolean updateAccount(long rowId, String name, String desc,
			double start_sum, String currency) {
		ContentValues args = new ContentValues();
		args.put(KEY_ACCOUNT_NAME, name);
		args.put(KEY_ACCOUNT_DESC, desc);
		args.put(KEY_ACCOUNT_START_SUM, start_sum);
		args.put(KEY_ACCOUNT_CURRENCY, currency);
		return mDb.update(DATABASE_ACCOUNT_TABLE, args,
				KEY_ACCOUNT_ROWID + "=" + rowId, null) > 0;
	}

	public boolean updateCurrentSum(long rowId) {
		Cursor account = mDb.query(true, DATABASE_ACCOUNT_TABLE, new String[] {
				KEY_ACCOUNT_START_SUM, KEY_ACCOUNT_OP_SUM }, KEY_ACCOUNT_ROWID + "=" + rowId, null,
				null, null, null, null);
		if (account != null) {
			account.moveToFirst();
		}
		double start = account
				.getDouble(account
						.getColumnIndexOrThrow(AccountsDbAdapter.KEY_ACCOUNT_START_SUM));
		double opSum = account.getDouble(account
				.getColumnIndexOrThrow(AccountsDbAdapter.KEY_ACCOUNT_OP_SUM));
		ContentValues args = new ContentValues();
		args.put(KEY_ACCOUNT_CUR_SUM, start + opSum);
		return mDb.update(DATABASE_ACCOUNT_TABLE, args,
				KEY_ACCOUNT_ROWID + "=" + rowId, null) > 0;
	}
}
