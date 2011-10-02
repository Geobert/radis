package fr.geobert.radis.tools;

import java.util.HashMap;

import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceActivity;
import fr.geobert.radis.RadisConfiguration;
import fr.geobert.radis.db.CommonDbAdapter;

public class DBPrefsManager {
	public final static String SHARED_PREF_NAME = "radis_prefs";
	private static DBPrefsManager mInstance;
	private Context mCurrentCtx;
	private CommonDbAdapter mDb;
	private HashMap<String, String> mCache;

	private DBPrefsManager(Context ctx) {
		mDb = CommonDbAdapter.getInstance(ctx);
		fillCache();
	}

	private void fillCache() {
		mCache = mDb.getAllPrefs();
	}

	public static DBPrefsManager getInstance(Context ctx) {
		if (null == mInstance) {
			mInstance = new DBPrefsManager(ctx);
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
		mCache.put(key, value.toString());
		mDb.setPref(key, value.toString());
	}

	public void clearAccountRelated() {
		Editor editor = mCurrentCtx.getSharedPreferences(SHARED_PREF_NAME,
				PreferenceActivity.MODE_PRIVATE).edit();
		editor.remove(RadisConfiguration.KEY_DEFAULT_ACCOUNT);
		editor.commit();
		mDb.deletePref(RadisConfiguration.KEY_DEFAULT_ACCOUNT);
		mCache.remove(RadisConfiguration.KEY_DEFAULT_ACCOUNT);
	}

	public void resetAll() {
		clearAccountRelated();
		mDb.deletePref(RadisConfiguration.KEY_INSERTION_DATE);
		mCache.remove(RadisConfiguration.KEY_INSERTION_DATE);
		Editor editor = mCurrentCtx.getSharedPreferences(SHARED_PREF_NAME,
				PreferenceActivity.MODE_PRIVATE).edit();
		editor.clear();
		editor.commit();	
	}
}
