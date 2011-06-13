package fr.geobert.radis.tools;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.util.HashMap;

import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceActivity;
import android.util.Log;
import fr.geobert.radis.RadisConfiguration;

public class PrefsManager {
	public final static String SHARED_PREF_NAME = "radis_prefs";
	private static PrefsManager mInstance;
	private HashMap<String, String> mPrefs;
	private Context mCurrentCtx;

	@SuppressWarnings("unchecked")
	private PrefsManager(Context ctx) {
		FileInputStream fis;
		try {
			fis = ctx.openFileInput(SHARED_PREF_NAME);
			ObjectInputStream ois = new ObjectInputStream(fis);
			mPrefs = (HashMap<String, String>) ois.readObject();
		} catch (FileNotFoundException e) {
			mPrefs = new HashMap<String, String>();
		} catch (StreamCorruptedException e) {
			Log.e("RADIS", "StreamCorruptedException: " + e.getMessage());
		} catch (IOException e) {
			Log.e("RADIS", "IOException: " + e.getMessage());
		} catch (ClassNotFoundException e) {
			Log.e("RADIS", "ClassNotFoundException: " + e.getMessage());
		}
	}

	public static PrefsManager getInstance(Context ctx) {
		if (null == mInstance) {
			mInstance = new PrefsManager(ctx);
		}
		mInstance.mCurrentCtx = ctx;
		return mInstance;
	}

	public boolean commit() {
		try {
			FileOutputStream fos = mCurrentCtx.openFileOutput(SHARED_PREF_NAME,
					Context.MODE_PRIVATE);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(mPrefs);
			return true;
		} catch (FileNotFoundException e) {
			return false;
		} catch (IOException e) {
			return false;
		}
	}

	public String getString(final String key) {
		return mPrefs.get(key);
	}

	public String getString(final String key, final String defValue) {
		String v = mPrefs.get(key);
		return v != null ? v : defValue;
	}

	public Boolean getBoolean(final String key) {
		String v = mPrefs.get(key);
		try {
			Boolean b = Boolean.valueOf(v);
			return b;
		} catch (Exception e) {
			return null;
		}
	}

	public Boolean getBoolean(final String key, final boolean defValue) {
		String v = mPrefs.get(key);
		try {
			Boolean b = Boolean.valueOf(v);
			return b != null ? b : defValue;
		} catch (Exception e) {
			return defValue;
		}
	}

	public Integer getInt(final String key) {
		String v = mPrefs.get(key);
		try {
			Integer b = Integer.valueOf(v);
			return b;
		} catch (Exception e) {
			return null;
		}
	}

	public Integer getInt(final String key, final int defValue) {
		String v = mPrefs.get(key);
		try {
			Integer b = Integer.valueOf(v);
			return b != null ? b : defValue;
		} catch (Exception e) {
			return defValue;
		}
	}

	public Long getLong(final String key) {
		String v = mPrefs.get(key);
		try {
			Long b = Long.valueOf(v);
			return b;
		} catch (Exception e) {
			return null;
		}
	}

	public Long getLong(final String key, final long defValue) {
		String v = mPrefs.get(key);
		try {
			Long b = Long.valueOf(v);
			return b != null ? b : defValue;
		} catch (Exception e) {
			return defValue;
		}
	}

	public void put(final String key, final Object value) {
		mPrefs.put(key, value.toString());
	}

	public void clearAccountRelated() {
		Editor editor = mCurrentCtx.getSharedPreferences(SHARED_PREF_NAME,
				PreferenceActivity.MODE_PRIVATE).edit();
		editor.remove(RadisConfiguration.KEY_DEFAULT_ACCOUNT);
		editor.commit();
		mPrefs.remove(RadisConfiguration.KEY_DEFAULT_ACCOUNT);
		commit();
	}

	public void resetAll() {
		clearAccountRelated();
		mPrefs.remove(RadisConfiguration.KEY_INSERTION_DATE);
		commit();
		Editor editor = mCurrentCtx.getSharedPreferences(SHARED_PREF_NAME,
				PreferenceActivity.MODE_PRIVATE).edit();
		editor.clear();
		editor.commit();
	}
}
