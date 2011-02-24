package fr.geobert.radis;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;

@SuppressWarnings("serial")
public class OperationsDbAdapter extends CommonDbAdapter {
	private long mAccountId;

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

	private final String DATABASE_TABLE_JOINTURE = DATABASE_OPERATIONS_TABLE
			+ " ops LEFT OUTER JOIN " + DATABASE_THIRD_PARTIES_TABLE
			+ " tp ON ops." + KEY_OP_THIRD_PARTY + " = tp."
			+ KEY_THIRD_PARTY_ROWID + " LEFT OUTER JOIN "
			+ DATABASE_MODES_TABLE + " mode ON ops." + KEY_OP_MODE + " = mode."
			+ KEY_MODE_ROWID + " LEFT OUTER JOIN " + DATABASE_TAGS_TABLE
			+ " tag ON ops." + KEY_OP_TAG + " = tag." + KEY_TAG_ROWID;

	public static final String[] OP_COLS_QUERY = { "ops." + KEY_OP_ROWID,
			"tp." + KEY_THIRD_PARTY_NAME, "tag." + KEY_TAG_NAME,
			"mode." + KEY_MODE_NAME, "ops." + KEY_OP_SUM, "ops." + KEY_OP_DATE,
			"ops." + KEY_OP_ACCOUNT_ID, "ops." + KEY_OP_NOTES };

	private static final String RESTRICT_TO_ACCOUNT = "ops."
			+ KEY_OP_ACCOUNT_ID + " = %d";

	public OperationsDbAdapter(Context ctx, long accountRowId) {
		super(ctx);
		mAccountId = accountRowId;
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

	public OperationsDbAdapter open() throws SQLException {
		super.open();
		fillCaches();
		return this;
	}

	public long getKeyIdOrCreate(String key, String table) throws SQLException {
		if (table.equals(DATABASE_THIRD_PARTIES_TABLE)) {
			return getKeyIdOrCreate(key, mThirdPartiesMap, table,
					KEY_THIRD_PARTY_NAME);
		} else if (table.equals(DATABASE_TAGS_TABLE)) {
			return getKeyIdOrCreate(key, mTagsMap, table, KEY_TAG_NAME);
		} else if (table.equals(DATABASE_MODES_TABLE)) {
			return getKeyIdOrCreate(key, mModesMap, table, KEY_MODE_NAME);
		}
		return 0;
	}

	public long getKeyIdIfExists(String key, String table) {
		Long res = null;
		if (table.equals(DATABASE_THIRD_PARTIES_TABLE)) {
			res = mThirdPartiesMap.get(key);
		} else if (table.equals(DATABASE_TAGS_TABLE)) {
			res = mTagsMap.get(key);
		} else if (table.equals(DATABASE_MODES_TABLE)) {
			res = mModesMap.get(key);
		}
		if (null != res) {
			return res.longValue();
		}
		return -1;
	}

	private long getKeyIdOrCreate(String key, LinkedHashMap<String, Long> map,
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
			String opTableCol, LinkedHashMap<String, Long> keyMap,
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

		String key = op.mThirdParty;
		putKeyId(key, DATABASE_THIRD_PARTIES_TABLE, KEY_THIRD_PARTY_NAME,
				KEY_OP_THIRD_PARTY, mThirdPartiesMap, initialValues);

		key = op.mTag;
		putKeyId(key, DATABASE_TAGS_TABLE, KEY_TAG_NAME, KEY_OP_TAG, mTagsMap,
				initialValues);

		key = op.mMode;
		putKeyId(key, DATABASE_MODES_TABLE, KEY_MODE_NAME, KEY_OP_MODE,
				mModesMap, initialValues);

		initialValues.put(KEY_OP_SUM, op.mSum);
		initialValues.put(KEY_OP_DATE, op.getDate());
		initialValues.put(KEY_OP_ACCOUNT_ID, mAccountId);
		initialValues.put(KEY_OP_NOTES, op.mNotes);
		return mDb.insert(DATABASE_OPERATIONS_TABLE, null, initialValues);
	}

	public boolean deleteOp(long rowId) {
		return mDb.delete(DATABASE_OPERATIONS_TABLE,
				KEY_OP_ROWID + "=" + rowId, null) > 0;
	}

	public Cursor fetchNLastOps(int nbOps) {
		return mDb.query(DATABASE_TABLE_JOINTURE, OP_COLS_QUERY,
				String.format(RESTRICT_TO_ACCOUNT, mAccountId), null, null,
				null, OP_ORDERING, Integer.toString(nbOps));
	}

	public Cursor fetchOneOp(long rowId) {
		Cursor c = mDb.query(DATABASE_TABLE_JOINTURE, OP_COLS_QUERY,
				String.format(RESTRICT_TO_ACCOUNT, mAccountId) + " AND ops."
						+ KEY_OP_ROWID + " = " + rowId, null, null, null, null,
				null);
		if (c != null) {
			c.moveToFirst();
		}
		return c;
	}

	public Cursor fetchOpEarlierThan(long date, int nbOps) {
		Cursor c = null;

		try {
			c = mDb.query(DATABASE_TABLE_JOINTURE, OP_COLS_QUERY,
					String.format(RESTRICT_TO_ACCOUNT, mAccountId)
							+ " AND ops." + KEY_OP_DATE + " < " + date, null,
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

	public boolean updateOp(long rowId, Operation op) {
		ContentValues args = new ContentValues();

		String key = op.mThirdParty;
		putKeyId(key, DATABASE_THIRD_PARTIES_TABLE, KEY_THIRD_PARTY_NAME,
				KEY_OP_THIRD_PARTY, mThirdPartiesMap, args);

		key = op.mTag;
		putKeyId(key, DATABASE_TAGS_TABLE, KEY_TAG_NAME, KEY_OP_TAG, mTagsMap,
				args);

		key = op.mMode;
		putKeyId(key, DATABASE_MODES_TABLE, KEY_MODE_NAME, KEY_OP_MODE,
				mModesMap, args);

		args.put(KEY_OP_SUM, op.mSum);
		args.put(KEY_OP_DATE, op.getDate());
		args.put(KEY_OP_NOTES, op.mNotes);
		return mDb.update(DATABASE_OPERATIONS_TABLE, args, KEY_OP_ROWID + "="
				+ rowId, null) > 0;
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
