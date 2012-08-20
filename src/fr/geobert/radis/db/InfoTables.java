package fr.geobert.radis.db;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import fr.geobert.radis.tools.AsciiUtils;

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
			put(DbContentProvider.THIRD_PARTY_URI.toString(),
					KEY_THIRD_PARTY_NAME);
			put(DbContentProvider.TAGS_URI.toString(), KEY_TAG_NAME);
			put(DbContentProvider.MODES_URI.toString(), KEY_MODE_NAME);
		}
	};

	private static final int GET_TP = 700;
	private static final int GET_MODES = 710;
	private static final int GET_TAGS = 720;

	private static LinkedHashMap<String, Long> mThirdPartiesMap;
	private static LinkedHashMap<String, Long> mTagsMap;
	private static LinkedHashMap<String, Long> mModesMap;
	private static HashMap<String, Cursor> mInfoCursorMap = new HashMap<String, Cursor>();

	private static class InfoCacheFiller implements LoaderCallbacks<Cursor> {
		private Context mCtx;

		public InfoCacheFiller(Context ctx) {
			mCtx = ctx;
		}

		@Override
		public Loader<Cursor> onCreateLoader(int id, Bundle args) {
			Uri u = null;
			String[] cols = null;
			switch (id) {
			case GET_TP:
				cols = new String[] { KEY_THIRD_PARTY_ROWID,
						KEY_THIRD_PARTY_NORMALIZED_NAME };
				u = DbContentProvider.THIRD_PARTY_URI;
				break;
			case GET_MODES:
				cols = new String[] { KEY_MODE_ROWID, KEY_MODE_NORMALIZED_NAME };
				u = DbContentProvider.MODES_URI;
				break;
			case GET_TAGS:
				cols = new String[] { KEY_TAG_ROWID, KEY_TAG_NORMALIZED_NAME };
				u = DbContentProvider.TAGS_URI;
				break;
			default:
				break;
			}
			return new CursorLoader(mCtx, u, cols, null, null, null);
		}

		@Override
		public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
			switch (loader.getId()) {
			case GET_TP:
				fillCache(data, mThirdPartiesMap);
				break;
			case GET_MODES:
				fillCache(data, mModesMap);
				break;
			case GET_TAGS:
				fillCache(data, mTagsMap);
				break;
			default:
				break;
			}
		}

		@Override
		public void onLoaderReset(Loader<Cursor> arg0) {

		}

	}

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

	private static void fillCache(Cursor c, Map<String, Long> map) {
		if (c.moveToFirst()) {
			do {
				String key = c.getString(1).toLowerCase();
				Long value = c.getLong(0);
				map.put(key, value);
			} while (c.moveToNext());
		}
	}

	public static void fillCaches(FragmentActivity ctx) {
		mModesMap = new LinkedHashMap<String, Long>();
		mTagsMap = new LinkedHashMap<String, Long>();
		mThirdPartiesMap = new LinkedHashMap<String, Long>();

		InfoCacheFiller cbk = new InfoCacheFiller(ctx);

		ctx.getSupportLoaderManager().initLoader(GET_TP, null, cbk);
		ctx.getSupportLoaderManager().initLoader(GET_MODES, null, cbk);
		ctx.getSupportLoaderManager().initLoader(GET_TAGS, null, cbk);
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

	public static long getKeyIdIfExists(String key, Uri table) {
		long res = -1;
		if (table.equals(DbContentProvider.THIRD_PARTY_URI)) {
			res = getKeyIdIfExistsFromThirdParties(key);
		} else if (table.equals(DbContentProvider.TAGS_URI)) {
			res = getKeyIdIfExistsFromTags(key);
		} else if (table.equals(DbContentProvider.MODES_URI)) {
			res = getKeyIdIfExistsFromModes(key);
		}
		return res;
	}

	static long getKeyIdIfExistsFromThirdParties(String key) {
		Long res = null;
		res = mThirdPartiesMap.get(key.toLowerCase());
		return res == null ? -1 : res.longValue();
	}

	static long getKeyIdIfExistsFromTags(String key) {
		Long res = null;
		res = mTagsMap.get(key.toLowerCase());
		return res == null ? -1 : res.longValue();
	}

	static long getKeyIdIfExistsFromModes(String key) {
		Long res = null;
		res = mModesMap.get(key.toLowerCase());
		return res == null ? -1 : res.longValue();
	}

	public static boolean updateInfo(Context ctx, Uri table, long rowId,
			String value, String oldValue) {
		ContentValues args = new ContentValues();
		args.put(mInfoColMap.get(table.toString()), value);
		args.put(mColNameNormName.get(mInfoColMap.get(table.toString())), AsciiUtils
				.convertNonAscii(value).trim().toLowerCase());
		int res = ctx.getContentResolver().update(
				Uri.parse(table + "/" + rowId), args, null, null);

		// update cache
		Map<String, Long> m = null;
		if (table.equals(DbContentProvider.THIRD_PARTY_URI)) {
			m = mThirdPartiesMap;
		} else if (table.equals(DbContentProvider.TAGS_URI)) {
			m = mTagsMap;
		} else if (table.equals(DbContentProvider.MODES_URI)) {
			m = mModesMap;
		}
		m.remove(oldValue);
		m.put(value.toLowerCase(), rowId);
		return res > 0;
	}

	public static long createInfo(Context ctx, Uri table, String value) {
		ContentValues args = new ContentValues();
		String k = table.toString();
		args.put(mInfoColMap.get(k), value);
		args.put(mColNameNormName.get(mInfoColMap.get(k)), AsciiUtils
				.convertNonAscii(value).trim().toLowerCase());
		Uri r = ctx.getContentResolver().insert(table, args);
		long res = Long.parseLong(r.getLastPathSegment());
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

	public static boolean deleteInfo(Context ctx, Uri table, long rowId) {
		boolean res = ctx.getContentResolver().delete(
				Uri.parse(table + "/" + rowId), null, null) > 0;
		return res;
	}

	public static Cursor fetchMatchingInfo(Context ctx, Uri table,
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
		Cursor c = ctx.getContentResolver().query(table,
				new String[] { "_id", colName }, where, params,
				colName + " asc");
		if (null != c) {
			c.moveToFirst();
		}
		mInfoCursorMap.put(table.toString(), c);
		return c;
	}

	public static CursorLoader getMatchingInfoLoader(Context ctx, Uri table,
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
		CursorLoader loader = new CursorLoader(ctx, table, new String[] {
				"_id", colName }, where, params, colName + " asc");
		// mInfoCursorMap.put(table.toString(), c);
		return loader;
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
