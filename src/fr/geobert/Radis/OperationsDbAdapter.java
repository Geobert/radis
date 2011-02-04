package fr.geobert.Radis;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;

@SuppressWarnings("serial")
public class OperationsDbAdapter extends AccountsDbAdapter {
	private long mAccountId;
	private String mDatabaseTable;

	private LinkedHashMap<String, Long> mModesMap;
	private LinkedHashMap<String, Long> mTagsMap;
	private LinkedHashMap<String, Long> mThirdPartiesMap;

	private HashMap<String, Cursor> mInfoCursorMap;
	private static final HashMap<String, String> mInfoColMap = new HashMap<String, String>() {
		{
			put(DATABASE_THIRD_PARTIES_TABLE, KEY_THIRD_PARTY_NAME);
			put(DATABASE_TAGS_TABLE, KEY_TAG_NAME);
			put(DATABASE_MODES_TABLE, KEY_MODE_NAME);
		}
	};

	private static final String OP_ORDERING = "ops." + KEY_OP_DATE
			+ " desc, ops." + KEY_OP_ROWID + " desc";

	/**
	 * Constructor - takes the context to allow the database to be
	 * opened/created
	 * 
	 * @param ctx
	 *            the Context within which to work
	 */
	public OperationsDbAdapter(Context ctx, long accountRowId) {
		super(ctx);
		mAccountId = accountRowId;
		mDatabaseTable = String.format(OPS_ACCOUNT_TABLE, mAccountId);
		mInfoCursorMap = new HashMap<String, Cursor>();
	}

	private void fillCache(String table, String[] cols, Map<String, Long> map) {
		Cursor c = mDb.query(table, cols, null, null, null, null, null);
		if (c.moveToFirst()) {
			do {
				String key = c.getString(1);
				Long value = c.getLong(0);
				map.put(key, value);
			} while (c.moveToNext());
		}
		c.close();
	}

	private void fillCaches() {
		mModesMap = new LinkedHashMap<String, Long>();
		mTagsMap = new LinkedHashMap<String, Long>();
		mThirdPartiesMap = new LinkedHashMap<String, Long>();

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
		key = key.trim();
		if (key.length() == 0) {
			return -1;
		}
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

	private void putKeyId(String key, String keyTableName, String keyTableCol,
			String opTableCol, Map<String, Long> keyMap,
			ContentValues initialValues) {
		long id = getKeyIdOrCreate(key, keyMap, keyTableName, keyTableCol);
		if (id != -1) {
			initialValues.put(opTableCol, id);
		} else {
			initialValues.putNull(opTableCol);
		}
	}

	public long createOp(Operation op) {
		ContentValues initialValues = new ContentValues();

		String key = op.getThirdParty();
		putKeyId(key, DATABASE_THIRD_PARTIES_TABLE, KEY_THIRD_PARTY_NAME,
				KEY_OP_THIRD_PARTY, mThirdPartiesMap, initialValues);

		key = op.getTag();
		putKeyId(key, DATABASE_TAGS_TABLE, KEY_TAG_NAME, KEY_OP_TAG, mTagsMap,
				initialValues);

		key = op.getMode();
		putKeyId(key, DATABASE_MODES_TABLE, KEY_MODE_NAME, KEY_OP_MODE,
				mModesMap, initialValues);

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

	private final String DATABASE_TABLE_JOINTURE = "%s ops LEFT OUTER JOIN "
			+ DATABASE_THIRD_PARTIES_TABLE + " tp ON ops." + KEY_OP_THIRD_PARTY
			+ " = tp." + KEY_THIRD_PARTY_ROWID + " LEFT OUTER JOIN "
			+ DATABASE_MODES_TABLE + " mode ON ops." + KEY_OP_MODE + " = mode."
			+ KEY_MODE_ROWID + " LEFT OUTER JOIN " + DATABASE_TAGS_TABLE
			+ " tag ON ops." + KEY_OP_TAG + " = tag." + KEY_TAG_ROWID;

	public static final String[] OP_COLS_QUERY = { "ops." + KEY_OP_ROWID,
			"tp." + KEY_THIRD_PARTY_NAME, "tag." + KEY_TAG_NAME,
			"mode." + KEY_MODE_NAME, "ops." + KEY_OP_SUM, "ops." + KEY_OP_DATE };

	public Cursor fetchOps(long startRowId, int nbRows) {
		return mDb.query(String.format(DATABASE_TABLE_JOINTURE, mDatabaseTable,
				mDatabaseTable), OP_COLS_QUERY, KEY_OP_ROWID + ">="
				+ startRowId + " AND " + KEY_OP_ROWID + "<"
				+ (startRowId + nbRows), null, null, null, null);
	}

	public Cursor fetchNLastOps(int nbOps) {
		return mDb.query(
				String.format(DATABASE_TABLE_JOINTURE, mDatabaseTable),
				OP_COLS_QUERY, null, null, null, null, OP_ORDERING, Integer
						.toString(nbOps));
	}

	public Cursor fetchOneOp(long rowId) {
		Cursor c = mDb.query(String.format(DATABASE_TABLE_JOINTURE,
				mDatabaseTable), OP_COLS_QUERY, "ops." + KEY_OP_ROWID + " = "
				+ rowId, null, null, null, null, null);
		if (c != null) {
			c.moveToFirst();
		}
		return c;
	}

	public Cursor fetchOpEarlierThan(long date, int nbOps) {
		Cursor c = null;

		try {
			c = mDb.query(String
					.format(DATABASE_TABLE_JOINTURE, mDatabaseTable),
					OP_COLS_QUERY, "ops." + KEY_OP_DATE + " < " + date, null,
					null, null, OP_ORDERING, Integer.toString(nbOps));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (c != null) {
			c.moveToFirst();
		}
		return c;
	}

	// public Cursor fetchOpsSumsLaterThan(long date) {
	// Cursor c = mDb.query(mDatabaseTable, new String[] { KEY_OP_ROWID,
	// KEY_OP_SUM }, KEY_OP_DATE + " > " + date, null, null, null,
	// null);
	// if (c != null) {
	// c.moveToFirst();
	// }
	// return c;
	// }

	public boolean updateOp(long rowId, Operation op) {
		ContentValues args = new ContentValues();

		String key = op.getThirdParty();
		putKeyId(key, DATABASE_THIRD_PARTIES_TABLE, KEY_THIRD_PARTY_NAME,
				KEY_OP_THIRD_PARTY, mThirdPartiesMap, args);

		key = op.getTag();
		putKeyId(key, DATABASE_TAGS_TABLE, KEY_TAG_NAME, KEY_OP_TAG, mTagsMap,
				args);

		key = op.getMode();
		putKeyId(key, DATABASE_MODES_TABLE, KEY_MODE_NAME, KEY_OP_MODE,
				mModesMap, args);

		args.put(KEY_OP_SUM, op.getSum());
		args.put(KEY_OP_DATE, op.getDate());

		return mDb.update(mDatabaseTable, args, KEY_OP_ROWID + "=" + rowId,
				null) > 0;
	}

	// INFOS
	public boolean updateInfo(String table, long rowId, String value) {
		ContentValues args = new ContentValues();
		args.put(mInfoColMap.get(table), value);
		return mDb.update(table, args, "_id =" + rowId, null) > 0;
	}

	public long createInfo(String table, String value) {
		ContentValues args = new ContentValues();
		args.put(mInfoColMap.get(table), value);
		return mDb.insert(table, null, args);
	}

	public boolean deleteInfo(String table, long rowId) {
		boolean res = mDb.delete(table, "_id =" + rowId, null) > 0;
		mInfoCursorMap.get(table).requery();
		return res;
	}

	public Cursor fetchMatchingInfo(String table, String colName,
			String constraint) {
		String where;
		String[] params;
		if (null != constraint) {
			where = colName + " LIKE ?";
			params = new String[] { constraint.trim() + "%" };
		} else {
			where = null;
			params = null;
		}
		Cursor c = mDb.query(table, new String[] { "_id", colName }, where,
				params, null, null, colName + " asc");
		if (null != c) {
			c.moveToFirst();
		}
		mInfoCursorMap.put(table, c);
		return c;
	}
}
