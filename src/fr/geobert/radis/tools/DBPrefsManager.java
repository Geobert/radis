package fr.geobert.radis.tools;

import java.util.HashMap;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import fr.geobert.radis.RadisConfiguration;
import fr.geobert.radis.db.DbContentProvider;
import fr.geobert.radis.db.PreferenceTable;

public class DBPrefsManager implements LoaderCallbacks<Cursor> {
	public final static String SHARED_PREF_NAME = "radis_prefs";
	private static DBPrefsManager mInstance;
	private Context mCurrentCtx;
	private HashMap<String, String> mCache = null;
	private final int FILL_CACHE = 100;

	private Runnable mCbk;

	public void fillCache(FragmentActivity ctx, Runnable cbk) {
		if (mCache == null) {
			mCbk = cbk;
			ctx.getSupportLoaderManager().initLoader(FILL_CACHE, null, this);
		} else {
			cbk.run();
		}
	}
	
	public void fillCache(Context ctx) {
		if (mCache == null) {
			Cursor data = ctx.getContentResolver().query(DbContentProvider.PREFS_URI, PreferenceTable.PREFS_COLS, null, null, null);
			mCache = new HashMap<String, String>();
			if (data.moveToFirst()) {
				do {
					mCache.put(data.getString(0), data.getString(1));
				} while (data.moveToNext());
			}
		}
	}

	public static DBPrefsManager getInstance(Context ctx) {
		if (null == mInstance) {
			mInstance = new DBPrefsManager();
		}
		mInstance.mCurrentCtx = ctx;
		return mInstance;
	}

	public String getString(final String key) {
		return mCache.get(key);
	}

	public String getString(final String key, final String defValue) {
		String v = mCache.get(key);
		return v != null ? v : defValue;
	}

	public Boolean getBoolean(final String key) {
		String v = mCache.get(key);
		try {
			Boolean b = Boolean.valueOf(v);
			return b;
		} catch (Exception e) {
			return null;
		}
	}

	public Boolean getBoolean(final String key, final boolean defValue) {
		String v = mCache.get(key);
		try {
			Boolean b = Boolean.valueOf(v);
			return b != null ? b : defValue;
		} catch (Exception e) {
			return defValue;
		}
	}

	public Integer getInt(final String key) {
		String v = mCache.get(key);
		try {
			Integer b = Integer.valueOf(v);
			return b;
		} catch (Exception e) {
			return null;
		}
	}

	public Integer getInt(final String key, final int defValue) {
		String v = mCache.get(key);
		try {
			Integer b = Integer.valueOf(v);
			return b != null ? b : defValue;
		} catch (Exception e) {
			return defValue;
		}
	}

	public Long getLong(final String key) {
		String v = mCache.get(key);
		try {
			Long b = Long.valueOf(v);
			return b;
		} catch (Exception e) {
			return null;
		}
	}

	public Long getLong(final String key, final long defValue) {
		String v = mCache.get(key);
		try {
			Long b = Long.valueOf(v);
			return b != null ? b : defValue;
		} catch (Exception e) {
			return defValue;
		}
	}

	public void put(final String key, final Object value) {
		ContentResolver cr = mCurrentCtx.getContentResolver();
		ContentValues values = new ContentValues();
		values.put(PreferenceTable.KEY_PREFS_NAME, key);
		values.put(PreferenceTable.KEY_PREFS_VALUE, value.toString());
		if (mCache.get(key) == null) {
			cr.insert(DbContentProvider.PREFS_URI, values);
		} else {
			cr.update(DbContentProvider.PREFS_URI, values,
					PreferenceTable.KEY_PREFS_NAME + "=?", new String[] { key });
		}
		mCache.put(key, value.toString());
	}

	private void deletePref(String key) {
		mCurrentCtx.getContentResolver().delete(DbContentProvider.PREFS_URI,
				PreferenceTable.KEY_PREFS_NAME + "=?", new String[] { key });
	}

	public void clearAccountRelated() {
		Editor editor = mCurrentCtx.getSharedPreferences(SHARED_PREF_NAME,
				PreferenceActivity.MODE_PRIVATE).edit();
		editor.remove(RadisConfiguration.KEY_DEFAULT_ACCOUNT);
		editor.commit();
		deletePref(RadisConfiguration.KEY_DEFAULT_ACCOUNT);
		if (mCache != null) {
			mCache.remove(RadisConfiguration.KEY_DEFAULT_ACCOUNT);
		}
	}

	public void resetAll() {
		clearAccountRelated();
		deletePref(RadisConfiguration.KEY_INSERTION_DATE);
		deletePref(RadisConfiguration.KEY_LAST_INSERTION_DATE);
		if (mCache != null) {
			mCache.remove(RadisConfiguration.KEY_INSERTION_DATE);
			mCache.remove(RadisConfiguration.KEY_LAST_INSERTION_DATE);
		}
		Editor editor = mCurrentCtx.getSharedPreferences(SHARED_PREF_NAME,
				PreferenceActivity.MODE_PRIVATE).edit();
		editor.clear();
		editor.commit();
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		CursorLoader cursorLoader = null;
		switch (id) {
		case FILL_CACHE:
			cursorLoader = new CursorLoader(mCurrentCtx,
					DbContentProvider.PREFS_URI, PreferenceTable.PREFS_COLS,
					null, null, null);
			break;
		default:
			break;
		}
		return cursorLoader;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		switch (loader.getId()) {
		case FILL_CACHE:
			mCache = new HashMap<String, String>();
			if (data.moveToFirst()) {
				do {
					mCache.put(data.getString(0), data.getString(1));
				} while (data.moveToNext());
			}
			if (mCbk != null) {
				mCbk.run();
			}
			break;
		default:
			break;
		}

	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		// nothing to do
	}
}
