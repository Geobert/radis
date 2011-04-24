package fr.geobert.radis;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.widget.Toast;
import fr.geobert.radis.db.CommonDbAdapter;

public class ConfigManager extends PreferenceActivity implements
		OnSharedPreferenceChangeListener {
	public final static String KEY_INSERTION_DATE = "insertion_date";
	public final static String KEY_AUTOSTART_ACCOUNT = "autostart_account";
	public final static String KEY_DEFAULT_ACCOUNT = "default_account_choice";
	public final static String SHARED_PREF_NAME = "RadisPrefs";
	private CommonDbAdapter mDbHelper;

	private void displayToast(int msgId) {
		Toast.makeText(this, getString(msgId), Toast.LENGTH_LONG).show();
	}

	private SharedPreferences getSharedPreferences() {
		return super.getSharedPreferences(SHARED_PREF_NAME, MODE_PRIVATE);
	}

	@Override
	public SharedPreferences getSharedPreferences(String name, int mode) {
		return getSharedPreferences();
	}

	private String notEmpty(String s) {
		if (s != null && s.trim().length() == 0) {
			return null;
		}
		return s;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.preferences);

	}

	private void updateLabel(String key) {
		String summary = null;
		String value = notEmpty(getSharedPreferences().getString(key, null));

		if (KEY_INSERTION_DATE.equals(key)) {
			summary = getString(R.string.prefs_insertion_date_text,
					value == null ? "" : value);
		}

		if (summary != null) {
			PreferenceScreen ps = getPreferenceScreen();
			if (ps != null) {
				Preference p = ps.findPreference(key);
				if (p != null) {
					p.setSummary(summary);
					p.setDefaultValue(value);
				}
			}
		}

	}

	@Override
	protected void onResume() {
		super.onResume();
		mDbHelper = CommonDbAdapter.getInstance(this);
		mDbHelper.open();
		SharedPreferences sharedPref = getSharedPreferences();
		SharedPreferences defaultSharedPref = PreferenceManager
				.getDefaultSharedPreferences(this);
		SharedPreferences.Editor edit = sharedPref.edit();
		SharedPreferences.Editor defaultEdit = defaultSharedPref.edit();
		String value = mDbHelper.getPref(KEY_INSERTION_DATE, "20");
		edit.putString(KEY_INSERTION_DATE, value);
		defaultEdit.putString(KEY_INSERTION_DATE, value);
		edit.commit();
		defaultEdit.commit();
		updateLabel(KEY_INSERTION_DATE);
		getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		updateLabel(key);
		String value = notEmpty(getSharedPreferences().getString(key, null));
		mDbHelper.putPref(key, value);
	}

}
