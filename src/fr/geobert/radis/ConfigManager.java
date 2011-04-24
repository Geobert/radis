package fr.geobert.radis;

import java.util.ArrayList;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import fr.geobert.radis.db.CommonDbAdapter;

public class ConfigManager extends PreferenceActivity implements
		OnSharedPreferenceChangeListener {
	public final static String KEY_INSERTION_DATE = "insertion_date";
	public final static String KEY_AUTOSTART_ACCOUNT = "autostart_account";
	public final static String KEY_DEFAULT_ACCOUNT = "default_account_choice";
	public final static String SHARED_PREF_NAME = "RadisPrefs";
	private CommonDbAdapter mDbHelper;

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
		ListPreference accountsChoice = (ListPreference) findPreference(KEY_DEFAULT_ACCOUNT);
		mDbHelper = CommonDbAdapter.getInstance(this);
		mDbHelper.open();
		Cursor accounts = mDbHelper.fetchAllAccounts();
		if (accounts.isFirst()) {
			ArrayList<CharSequence> entries = new ArrayList<CharSequence>();
			ArrayList<CharSequence> values = new ArrayList<CharSequence>();
			do {
				entries.add(accounts.getString(accounts
						.getColumnIndex(CommonDbAdapter.KEY_ACCOUNT_NAME)));
				values.add(String.valueOf(accounts.getLong(accounts
						.getColumnIndex(CommonDbAdapter.KEY_ACCOUNT_ROWID))));

			} while (accounts.moveToNext());

			accountsChoice.setEntries(entries.toArray(new CharSequence[entries
					.size()]));
			accountsChoice.setEntryValues(values
					.toArray(new CharSequence[entries.size()]));
		}
		accounts.close();
	}

	private void updateLabel(String key) {
		String summary = null;
		String value;

		if (KEY_INSERTION_DATE.equals(key)) {
			value = notEmpty(getSharedPreferences().getString(key, null));
			summary = getString(R.string.prefs_insertion_date_text,
					value == null ? "" : value);
		} else if (KEY_DEFAULT_ACCOUNT.equals(key)) {
			ListPreference l = (ListPreference) findPreference(key);
			value = l.getEntry().toString();
			summary = getString(R.string.pref_start_account_label, value);
		}

		if (summary != null) {
			PreferenceScreen ps = getPreferenceScreen();
			if (ps != null) {
				Preference p = ps.findPreference(key);
				if (p != null) {
					p.setSummary(summary);
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
		
		value = mDbHelper.getPref(KEY_AUTOSTART_ACCOUNT, "false");
		final boolean b = Boolean.parseBoolean(value);
		edit.putBoolean(KEY_AUTOSTART_ACCOUNT, b);
		defaultEdit.putBoolean(KEY_AUTOSTART_ACCOUNT, b);
		CheckBoxPreference chk = (CheckBoxPreference) findPreference(KEY_AUTOSTART_ACCOUNT);
		chk.setChecked(b);
		
		value = mDbHelper.getPref(KEY_DEFAULT_ACCOUNT, "");
		
		
		edit.commit();
		defaultEdit.commit();
		updateLabel(KEY_INSERTION_DATE);
		updateLabel(KEY_AUTOSTART_ACCOUNT);
		updateLabel(KEY_DEFAULT_ACCOUNT);
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
		String value = "";
		if (KEY_INSERTION_DATE.equals(key)) {
			value = notEmpty(getSharedPreferences().getString(key, null));
		} else if (KEY_AUTOSTART_ACCOUNT.equals(key)) {
			value = String
					.valueOf(getSharedPreferences().getBoolean(key, true));
		} else if (KEY_DEFAULT_ACCOUNT.equals(key)) {
			ListPreference l = (ListPreference) findPreference(key);
			value = l.getValue();
		}
		mDbHelper.putPref(key, value);
	}

}
