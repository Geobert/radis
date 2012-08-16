package fr.geobert.radis.db;

import java.util.HashMap;
import java.util.Map.Entry;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import fr.geobert.radis.tools.PrefsManager;

public class PreferenceTable {
	public static final String DATABASE_PREFS_TABLE = "preferences";
	public static final String KEY_PREFS_ROWID = "_id";
	public static final String KEY_PREFS_NAME = "pref_name";
	public static final String KEY_PREFS_VALUE = "pref_value";
	protected static final String DATABASE_PREFS_CREATE = "create table "
			+ DATABASE_PREFS_TABLE + "(" + KEY_PREFS_ROWID
			+ " integer primary key autoincrement, " + KEY_PREFS_NAME
			+ " text not null, " + KEY_PREFS_VALUE + " text not null);";
	public static final String[] PREFS_COLS = { KEY_PREFS_NAME, KEY_PREFS_VALUE };

	static void onCreate(SQLiteDatabase db) {
		db.execSQL(DATABASE_PREFS_CREATE);
	}

	static void onUpgrade(Context ctx, SQLiteDatabase db, int oldVersion,
			int newVersion) {
		switch (oldVersion) {
		case 10:
			upgradeFromV10(ctx, db, oldVersion, newVersion);
		}
	}

	static void setPref(SQLiteDatabase db, final String key, final String value) {
		ContentValues values = new ContentValues();
		values.put(KEY_PREFS_VALUE, value);
		if (null == getPref(db, key)) {
			values.put(KEY_PREFS_NAME, key);
			// insert
			db.insert(DATABASE_PREFS_TABLE, null, values);
		} else {
			// update
			db.update(DATABASE_PREFS_TABLE, values, KEY_PREFS_NAME + "='" + key
					+ "'", null);
		}
	}

	static String getPref(SQLiteDatabase db, final String key) {
		String res = null;
		Cursor c = db.query(DATABASE_PREFS_TABLE,
				new String[] { KEY_PREFS_VALUE }, KEY_PREFS_NAME + "='" + key
						+ "'", null, null, null, null);
		if (null != c) {
			if (c.moveToFirst()) {
				res = c.getString(0);
			}
			c.close();
		}
		return res;
	}

	static HashMap<String, String> getAllPrefs(SQLiteDatabase db) {
		HashMap<String, String> res = new HashMap<String, String>();
		Cursor c = db
				.query(DATABASE_PREFS_TABLE, new String[] { KEY_PREFS_NAME,
						KEY_PREFS_VALUE }, null, null, null, null, null);
		if (null != c) {
			if (c.moveToFirst()) {
				do {
					res.put(c.getString(0), c.getString(1));
				} while (c.moveToNext());
			}
			c.close();
		}
		return res;
	}

	public void deletePref(SQLiteDatabase db, final String key) {
		db.delete(DATABASE_PREFS_TABLE, KEY_PREFS_NAME + "='" + key + "'", null);
	}

	// UPGRADE

	private static void upgradeFromV10(Context ctx, SQLiteDatabase db,
			int oldVersion, int newVersion) {
		db.execSQL(DATABASE_PREFS_CREATE);
		PrefsManager prefs = PrefsManager.getInstance(ctx);
		HashMap<String, String> allPrefs = prefs.getRawData();
		if (null != allPrefs) {
			for (Entry<String, String> elt : allPrefs.entrySet()) {
				ContentValues values = new ContentValues();
				values.put(KEY_PREFS_VALUE, elt.getValue());
				values.put(KEY_PREFS_NAME, elt.getKey());
				db.insert(DATABASE_PREFS_TABLE, null, values);
			}
		}
	}
}
