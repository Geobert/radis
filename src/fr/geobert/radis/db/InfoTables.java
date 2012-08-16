package fr.geobert.radis.db;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import fr.geobert.radis.tools.AsciiUtils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

// this class manage simple info tables which are _id / name schema
public class InfoTables {
	static final String DATABASE_THIRD_PARTIES_TABLE = "third_parties";
	static final String DATABASE_TAGS_TABLE = "tags";
	static final String DATABASE_MODES_TABLE = "modes";

	public static final String KEY_THIRD_PARTY_ROWID = "_id";
	public static final String KEY_THIRD_PARTY_NAME = "third_party_name";
	public static final String KEY_THIRD_PARTY_NORMALIZED_NAME = "third_party_norm_name";
	public static final String KEY_TAG_ROWID = "_id";
	public static final String KEY_TAG_NAME = "tag_name";
	public static final String KEY_TAG_NORMALIZED_NAME = "tag_norm_name";
	public static final String KEY_MODE_ROWID = "_id";
	public static final String KEY_MODE_NAME = "mode_name";
	public static final String KEY_MODE_NORMALIZED_NAME = "mode_norm_name";

	private static final String DATABASE_THIRD_PARTIES_CREATE = "create table "
			+ DATABASE_THIRD_PARTIES_TABLE + "(" + KEY_THIRD_PARTY_ROWID
			+ " integer primary key autoincrement, " + KEY_THIRD_PARTY_NAME
			+ " text not null, " + KEY_THIRD_PARTY_NORMALIZED_NAME
			+ " text not null);";

	private static final String DATABASE_TAGS_CREATE = "create table "
			+ DATABASE_TAGS_TABLE + "(" + KEY_TAG_ROWID
			+ " integer primary key autoincrement, " + KEY_TAG_NAME
			+ " text not null, " + KEY_TAG_NORMALIZED_NAME + " text not null);";

	private static final String DATABASE_MODES_CREATE = "create table "
			+ DATABASE_MODES_TABLE + "(" + KEY_MODE_ROWID
			+ " integer primary key autoincrement, " + KEY_MODE_NAME
			+ " text not null, " + KEY_MODE_NORMALIZED_NAME
			+ " text not null);";

	protected static final String ADD_NORMALIZED_MODE = "ALTER TABLE "
			+ DATABASE_MODES_TABLE + " ADD COLUMN " + KEY_MODE_NORMALIZED_NAME
			+ " text not null DEFAULT ''";

	protected static final String ADD_NORMALIZED_TAG = "ALTER TABLE "
			+ DATABASE_TAGS_TABLE + " ADD COLUMN " + KEY_TAG_NORMALIZED_NAME
			+ " text not null DEFAULT ''";

	protected static final String ADD_NORMALIZED_THIRD_PARTY = "ALTER TABLE "
			+ DATABASE_THIRD_PARTIES_TABLE + " ADD COLUMN "
			+ KEY_THIRD_PARTY_NORMALIZED_NAME + " text not null DEFAULT ''";

	@SuppressWarnings("serial")
	private static final HashMap<String, String> mColNameNormName = new HashMap<String, String>() {
		{
			put(InfoTables.KEY_THIRD_PARTY_NAME,
					InfoTables.KEY_THIRD_PARTY_NORMALIZED_NAME);
			put(InfoTables.KEY_TAG_NAME, InfoTables.KEY_TAG_NORMALIZED_NAME);
			put(InfoTables.KEY_MODE_NAME, InfoTables.KEY_MODE_NORMALIZED_NAME);
		}
	};

	@SuppressWarnings("serial")
	private static final HashMap<String, String> mInfoColMap = new HashMap<String, String>() {
		{
			put(DATABASE_THIRD_PARTIES_TABLE, KEY_THIRD_PARTY_NAME);
			put(DATABASE_TAGS_TABLE, KEY_TAG_NAME);
			put(DATABASE_MODES_TABLE, KEY_MODE_NAME);
		}
	};

	private static LinkedHashMap<String, Long> mThirdPartiesMap;
	private static LinkedHashMap<String, Long> mTagsMap;
	private static LinkedHashMap<String, Long> mModesMap;
	private static HashMap<String, Cursor> mInfoCursorMap = new HashMap<String, Cursor>();

	static void onCreate(SQLiteDatabase db) {
		db.execSQL(DATABASE_THIRD_PARTIES_CREATE);
		db.execSQL(DATABASE_TAGS_CREATE);
		db.execSQL(DATABASE_MODES_CREATE);
	}

	static void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		switch (oldVersion) {
		case 7:
			upgradeFromV7(db, oldVersion, newVersion);
		case 8:
			upgradeFromV8(db, oldVersion, newVersion);
		default:
		}
	}

	private static void fillCache(SQLiteDatabase db, String table,
			String[] cols, Map<String, Long> map) {
		Cursor c = db.query(table, cols, null, null, null, null, null);
		if (c.moveToFirst()) {
			do {
				String key = c.getString(1).toLowerCase();
				Long value = c.getLong(0);
				map.put(key, value);
			} while (c.moveToNext());
		}
		c.close();
	}

	static void fillCaches(SQLiteDatabase db) {
		mModesMap = new LinkedHashMap<String, Long>();
		mTagsMap = new LinkedHashMap<String, Long>();
		mThirdPartiesMap = new LinkedHashMap<String, Long>();

		fillCache(db, DATABASE_MODES_TABLE, new String[] { KEY_MODE_ROWID,
				KEY_MODE_NORMALIZED_NAME }, mModesMap);
		fillCache(db, DATABASE_TAGS_TABLE, new String[] { KEY_TAG_ROWID,
				KEY_TAG_NORMALIZED_NAME }, mTagsMap);
		fillCache(db, DATABASE_THIRD_PARTIES_TABLE, new String[] {
				KEY_THIRD_PARTY_ROWID, KEY_THIRD_PARTY_NORMALIZED_NAME },
				mThirdPartiesMap);
	}

	static long getKeyIdOrCreateFromThirdParties(Context ctx, String key) {
		return getKeyIdOrCreate(ctx, key, mThirdPartiesMap,
				DbContentProvider.THIRD_PARTY_URI, KEY_THIRD_PARTY_NAME);
	}

	static long getKeyIdOrCreateFromTags(Context ctx, String key) {
		return getKeyIdOrCreate(ctx, key, mTagsMap, DbContentProvider.TAGS_URI,
				KEY_TAG_NAME);
	}

	static long getKeyIdOrCreateFromModes(Context ctx, String key) {
		return getKeyIdOrCreate(ctx, key, mModesMap,
				DbContentProvider.MODES_URI, KEY_MODE_NAME);
	}

	private static long getKeyIdOrCreate(Context ctx, String key,
			LinkedHashMap<String, Long> map, Uri table, String col)
			throws SQLException {
		String origKey = key.toString();
		key = AsciiUtils.convertNonAscii(key).trim().toLowerCase();
		if (key.length() == 0) {
			return -1;
		}
		Long i = map.get(key);
		if (null != i) {
			return i.longValue();
		} else {
			ContentValues initialValues = new ContentValues();
			initialValues.put(col, origKey);
			initialValues.put(mColNameNormName.get(col), key);
			Uri res = ctx.getContentResolver().insert(table, initialValues);
			long id = Long.parseLong(res.getLastPathSegment());
			if (id != -1) {
				map.put(key, id);
			} else {
				throw new SQLException("Database insertion error : " + key
						+ " in " + table);
			}
			return id;
		}
	}

	static long getKeyIdIfExistsFromThirdParties(SQLiteDatabase db, String key) {
		Long res = null;
		res = mThirdPartiesMap.get(key.toLowerCase());
		return res == null ? -1 : res.longValue();
	}

	static long getKeyIdIfExistsFromTags(SQLiteDatabase db, String key) {
		Long res = null;
		res = mTagsMap.get(key.toLowerCase());
		return res == null ? -1 : res.longValue();
	}

	static long getKeyIdIfExistsFromModes(SQLiteDatabase db, String key) {
		Long res = null;
		res = mModesMap.get(key.toLowerCase());
		return res == null ? -1 : res.longValue();
	}

	static boolean updateInfo(SQLiteDatabase db, String table, long rowId,
			String value, String oldValue) {
		ContentValues args = new ContentValues();
		args.put(mInfoColMap.get(table), value);
		args.put(mColNameNormName.get(mInfoColMap.get(table)), AsciiUtils
				.convertNonAscii(value).trim().toLowerCase());
		int res = db.update(table, args, "_id =" + rowId, null);

		// update cache
		Map<String, Long> m = null;
		if (table.equals(DATABASE_THIRD_PARTIES_TABLE)) {
			m = mThirdPartiesMap;
		} else if (table.equals(DATABASE_TAGS_TABLE)) {
			m = mTagsMap;
		} else if (table.equals(DATABASE_MODES_TABLE)) {
			m = mModesMap;
		}
		m.remove(oldValue);
		m.put(value.toLowerCase(), rowId);
		return res > 0;
	}

	static long createInfo(SQLiteDatabase db, String table, String value) {
		ContentValues args = new ContentValues();
		args.put(mInfoColMap.get(table), value);
		args.put(mColNameNormName.get(mInfoColMap.get(table)), AsciiUtils
				.convertNonAscii(value).trim().toLowerCase());
		long res = db.insert(table, null, args);
		if (res > 0) { // update cache
			Map<String, Long> m = null;
			if (table.equals(DATABASE_THIRD_PARTIES_TABLE)) {
				m = mThirdPartiesMap;
			} else if (table.equals(DATABASE_TAGS_TABLE)) {
				m = mTagsMap;
			} else if (table.equals(DATABASE_MODES_TABLE)) {
				m = mModesMap;
			}
			m.put(AsciiUtils.convertNonAscii(value).trim().toLowerCase(), res);
		}
		return res;
	}

	public boolean deleteInfo(SQLiteDatabase db, String table, long rowId) {
		boolean res = db.delete(table, "_id =" + rowId, null) > 0;
		return res;
	}

	public Cursor fetchMatchingInfo(SQLiteDatabase db, String table,
			String colName, String constraint) {
		String where;
		String[] params;
		if (null != constraint) {
			where = mColNameNormName.get(colName) + " LIKE ?";
			params = new String[] { constraint.trim() + "%" };
		} else {
			where = null;
			params = null;
		}
		Cursor c = db.query(table, new String[] { "_id", colName }, where,
				params, null, null, colName + " asc");
		if (null != c) {
			c.moveToFirst();
		}
		mInfoCursorMap.put(table, c);
		return c;
	}

	static void putKeyIdInThirdParties(Context ctx, String key,
			ContentValues initialValues) {
		putKeyId(ctx, key, DbContentProvider.THIRD_PARTY_URI,
				KEY_THIRD_PARTY_NAME, OperationTable.KEY_OP_THIRD_PARTY,
				mThirdPartiesMap, initialValues);
	}

	static void putKeyIdInTags(Context ctx, String key,
			ContentValues initialValues) {
		putKeyId(ctx, key, DbContentProvider.TAGS_URI, KEY_TAG_NAME,
				OperationTable.KEY_OP_TAG, mTagsMap, initialValues);
	}

	static void putKeyIdInModes(Context ctx, String key,
			ContentValues initialValues) {
		putKeyId(ctx, key, DbContentProvider.MODES_URI, KEY_MODE_NAME,
				OperationTable.KEY_OP_MODE, mModesMap, initialValues);
	}

	private static void putKeyId(Context ctx, String key, Uri keyTableName,
			String keyTableCol, String opTableCol,
			LinkedHashMap<String, Long> keyMap, ContentValues initialValues) {
		long id = getKeyIdOrCreate(ctx, key, keyMap, keyTableName, keyTableCol);
		if (id != -1) {
			initialValues.put(opTableCol, id);
		} else {
			initialValues.putNull(opTableCol);
		}
	}

	// UPGRADE FUNCTIONS
	private static void upgradeFromV8(SQLiteDatabase db, int oldVersion,
			int newVersion) {
		Cursor c = db.query(DATABASE_THIRD_PARTIES_TABLE, new String[] {
				KEY_THIRD_PARTY_ROWID, KEY_THIRD_PARTY_NAME }, null, null,
				null, null, null);
		if (c != null && c.moveToFirst()) {
			do {
				ContentValues v = new ContentValues();
				v.put(KEY_THIRD_PARTY_NORMALIZED_NAME,
						AsciiUtils.convertNonAscii(
								c.getString(c
										.getColumnIndex(KEY_THIRD_PARTY_NAME)))
								.toLowerCase());
				db.update(
						DATABASE_THIRD_PARTIES_TABLE,
						v,
						KEY_THIRD_PARTY_ROWID
								+ "="
								+ Long.toString(c.getLong(c
										.getColumnIndex(KEY_THIRD_PARTY_ROWID))),
						null);
			} while (c.moveToNext());
			c.close();
		}
		c = db.query(DATABASE_TAGS_TABLE, new String[] { KEY_TAG_ROWID,
				KEY_TAG_NAME }, null, null, null, null, null);
		if (c != null && c.moveToFirst()) {
			do {
				ContentValues v = new ContentValues();
				v.put(KEY_TAG_NORMALIZED_NAME,
						AsciiUtils.convertNonAscii(
								c.getString(c.getColumnIndex(KEY_TAG_NAME)))
								.toLowerCase());
				db.update(
						DATABASE_TAGS_TABLE,
						v,
						KEY_TAG_ROWID
								+ "="
								+ Long.toString(c.getLong(c
										.getColumnIndex(KEY_TAG_ROWID))), null);
			} while (c.moveToNext());
			c.close();
		}

		c = db.query(DATABASE_MODES_TABLE, new String[] { KEY_MODE_ROWID,
				KEY_MODE_NAME }, null, null, null, null, null);
		if (c != null && c.moveToFirst()) {
			do {
				ContentValues v = new ContentValues();
				v.put(KEY_MODE_NORMALIZED_NAME,
						AsciiUtils.convertNonAscii(
								c.getString(c.getColumnIndex(KEY_MODE_NAME)))
								.toLowerCase());
				db.update(
						DATABASE_MODES_TABLE,
						v,
						KEY_MODE_ROWID
								+ "="
								+ Long.toString(c.getLong(c
										.getColumnIndex(KEY_MODE_ROWID))), null);
			} while (c.moveToNext());
			c.close();
		}
	}

	private static void upgradeFromV7(SQLiteDatabase db, int oldVersion,
			int newVersion) {
		db.execSQL(ADD_NORMALIZED_THIRD_PARTY);
		db.execSQL(ADD_NORMALIZED_TAG);
		db.execSQL(ADD_NORMALIZED_MODE);
	}
}
