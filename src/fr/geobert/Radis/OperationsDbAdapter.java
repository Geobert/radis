package fr.geobert.Radis;

import java.util.Map;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.sax.StartElementListener;

public class OperationsDbAdapter extends CommonDbAdapter {
	private static final String TAG = "OperationsDbAdapter";
	private String mAccountName;
	private String mDatabaseTable;

	private Map<String, Long> mModesMap;
	private Map<String, Long> mTagsMap;
	private Map<String, Long> mThirdPartiesMap;

	/**
	 * Constructor - takes the context to allow the database to be
	 * opened/created
	 * 
	 * @param ctx
	 *            the Context within which to work
	 */
	public OperationsDbAdapter(Context ctx, String accountName) {
		super(ctx);
		this.mAccountName = accountName;
		mDatabaseTable = mAccountName + "_ops";
	}

	private void fillCache(String table, String[] cols, Map<String, Long> map) {
		Cursor c = mDb.query(table, cols, null, null, null, null, null);
		if (c.moveToFirst()) {
			do {
				String key = c.getString(1);
				Long value = c.getLong(0);
				mModesMap.put(key, value);
			} while (c.moveToNext());
		}
		c.close();
	}

	private void fillCaches() {
		fillCache(DATABASE_MODES_TABLE, new String[] { KEY_MODE_ROWID,
				KEY_MODE_NAME }, mModesMap);
		fillCache(DATABASE_TAGS_TABLE, new String[] { KEY_TAG_ROWID,
				KEY_TAG_NAME }, mTagsMap);
		fillCache(DATABASE_THIRD_PARTIES_TABLE, new String[] {
				KEY_THIRD_PARTY_ROWID, KEY_THIRD_PARTY_NAME }, mThirdPartiesMap);
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
	public OperationsDbAdapter open() throws SQLException {
		super.open();
		fillCaches();
		return this;
	}

	/**
	 * get the rowId from cache if exists or fill the table with new value
	 * 
	 * @param key
	 * @param map
	 * @param table
	 * @param col
	 * @return
	 * @throws SQLException
	 */
	private long getKeyIdOrCreate(String key, Map<String, Long> map,
			String table, String col) throws SQLException {
		Long i = map.get(key);
		if (null != i) {
			return i.longValue();
		} else {
			ContentValues initialValues = new ContentValues();
			initialValues.put(col, key);
			long id = mDb.insert(table, null, initialValues);
			if (id != -1) {
				map.put(key, id);
			} else {
				throw new SQLException("Database insertion error : " + key
						+ " in " + table);
			}
			return id;
		}
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
	public long createOp(Operation op) {
		ContentValues initialValues = new ContentValues();

		String key = op.getThirdParty();
		long id = getKeyIdOrCreate(key, mThirdPartiesMap,
				DATABASE_THIRD_PARTIES_TABLE, KEY_THIRD_PARTY_NAME);
		initialValues.put(KEY_OP_THIRD_PARTY, id);

		key = op.getTag();
		id = getKeyIdOrCreate(key, mTagsMap, DATABASE_TAGS_TABLE, KEY_TAG_NAME);
		initialValues.put(KEY_OP_TAG, id);

		key = op.getMode();
		id = getKeyIdOrCreate(key, mModesMap, DATABASE_MODES_TABLE,
				KEY_MODE_NAME);
		initialValues.put(KEY_OP_MODE, id);

		initialValues.put(KEY_OP_SUM, op.getSum());
		initialValues.put(KEY_OP_DATE, op.getDate());
		return mDb.insert(mDatabaseTable, null, initialValues);
	}

	/**
	 * Delete the note with the given rowId
	 * 
	 * @param rowId
	 *            id of note to delete
	 * @return true if deleted, false otherwise
	 */
	public boolean deleteOp(long rowId) {
		return mDb.delete(mDatabaseTable, KEY_OP_ROWID + "=" + rowId, null) > 0;
	}

	/**
	 * Return a Cursor over the list of all notes in the database
	 * 
	 * @return Cursor over all notes
	 */
	public Cursor fetchOps(long startRowId, int nbRows) {
		return mDb.query(mDatabaseTable, new String[] { KEY_OP_ROWID,
				KEY_OP_THIRD_PARTY, KEY_OP_TAG, KEY_OP_SUM, KEY_OP_DATE },
				KEY_OP_ROWID + ">=" + startRowId + " AND " + KEY_OP_ROWID + "<"
						+ (startRowId + nbRows), null, null, null, null);
	}

	public Cursor fetchOneOp(long rowId) {
		return mDb.query(true, mDatabaseTable, new String[] { KEY_OP_ROWID,
				KEY_OP_THIRD_PARTY, KEY_OP_TAG, KEY_OP_SUM, KEY_OP_DATE },
				KEY_OP_ROWID + "=" + rowId, null, null, null, null, null);
	}

	public Cursor fechOpsByDate(long startDate, long endDate) {
		return mDb.query(mDatabaseTable, new String[] { KEY_OP_ROWID,
				KEY_OP_THIRD_PARTY, KEY_OP_TAG, KEY_OP_SUM, KEY_OP_DATE },
				KEY_OP_DATE + ">=" + startDate + " AND " + KEY_OP_DATE + "<="
						+ endDate, null, null, null, null);
	}

	public boolean updateOp(long rowId, Operation op) {
		ContentValues args = new ContentValues();
		String key = op.getThirdParty();
		long id = getKeyIdOrCreate(key, mThirdPartiesMap,
				DATABASE_THIRD_PARTIES_TABLE, KEY_THIRD_PARTY_NAME);
		args.put(KEY_OP_THIRD_PARTY, id);

		key = op.getTag();
		id = getKeyIdOrCreate(key, mTagsMap, DATABASE_TAGS_TABLE, KEY_TAG_NAME);
		args.put(KEY_OP_TAG, id);

		key = op.getMode();
		id = getKeyIdOrCreate(key, mModesMap, DATABASE_MODES_TABLE,
				KEY_MODE_NAME);
		args.put(KEY_OP_MODE, id);
		
		args.put(KEY_OP_SUM, op.getSum());
		args.put(KEY_OP_DATE, op.getDate());

		return mDb.update(mDatabaseTable, args, KEY_OP_ROWID + "=" + rowId,
				null) > 0;
	}
}
