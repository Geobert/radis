package fr.geobert.radis;

import java.util.ArrayList;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import fr.geobert.radis.db.CommonDbAdapter;
import fr.geobert.radis.tools.PrefsManager;

public class RadisConfiguration extends PreferenceActivity implements
		OnSharedPreferenceChangeListener {
	public final static String KEY_INSERTION_DATE = "insertion_date";
	public final static String KEY_AUTOSTART_ACCOUNT = "autostart_account";
	public final static String KEY_DEFAULT_ACCOUNT = "default_account_choice";

	private ListPreference mAccountsChoice;

	private PrefsManager getPrefs() {
		return PrefsManager.getInstance(this);
	}

	private SharedPreferences getSharedPreferences() {
		return super.getSharedPreferences(PrefsManager.SHARED_PREF_NAME,
				MODE_PRIVATE);
	}

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
		//mAccountsChoice = (ListPreference) findPreference(KEY_DEFAULT_ACCOUNT);
		//initAccountChoices();
	}

	private void initAccountChoices() {
		CommonDbAdapter dbHelper = CommonDbAdapter.getInstance(this);
		dbHelper.open();
		Cursor accounts = dbHelper.fetchAllAccounts();
		if (accounts.isFirst()) {
			ArrayList<CharSequence> entries = new ArrayList<CharSequence>();
			ArrayList<CharSequence> values = new ArrayList<CharSequence>();
			do {
				entries.add(accounts.getString(accounts
						.getColumnIndex(CommonDbAdapter.KEY_ACCOUNT_NAME)));
				values.add(String.valueOf(accounts.getLong(accounts
						.getColumnIndex(CommonDbAdapter.KEY_ACCOUNT_ROWID))));

			} while (accounts.moveToNext());

			mAccountsChoice.setEntries(entries.toArray(new CharSequence[entries
					.size()]));
			mAccountsChoice.setEntryValues(values
					.toArray(new CharSequence[entries.size()]));
		}
		accounts.close();
	}
	
	private void updateLabel(String key) {
		String summary = null;
		String value;

		if (KEY_INSERTION_DATE.equals(key)) {
			value = getPrefs().getString(key, "25");
			summary = getString(R.string.prefs_insertion_date_text,
					value == null ? "" : value);
		} else if (KEY_DEFAULT_ACCOUNT.equals(key)) {
			ListPreference l = (ListPreference) findPreference(key);
			CharSequence s = l.getEntry();
			if (null != s) {
				value = s.toString();
				summary = getString(R.string.pref_start_account_label, value);
			}
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
		String value = getPrefs().getString(KEY_INSERTION_DATE, "20");
		EditTextPreference ep = (EditTextPreference) findPreference(KEY_INSERTION_DATE);
		ep.getEditText().setText(value);

		// final boolean b = getPrefs().getBoolean(KEY_AUTOSTART_ACCOUNT, false)
		// .booleanValue();
		// CheckBoxPreference chk = (CheckBoxPreference)
		// findPreference(KEY_AUTOSTART_ACCOUNT);
		// chk.setChecked(b);
		//
		// value = getPrefs().getString(KEY_DEFAULT_ACCOUNT);
		// if (value != null) {
		// for (CharSequence s : mAccountsChoice.getEntryValues()) {
		// if (value.equals(s)) {
		// mAccountsChoice.setValue(s.toString());
		// }
		// }
		// }

		updateLabel(KEY_INSERTION_DATE);
		// updateLabel(KEY_AUTOSTART_ACCOUNT);
		// updateLabel(KEY_DEFAULT_ACCOUNT);
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
		String value = "";
		Preference p = findPreference(key);
		if (p instanceof EditTextPreference) {
			value = notEmpty(sharedPreferences.getString(key, null));
		} else if (p instanceof CheckBoxPreference) {
			value = String.valueOf(sharedPreferences.getBoolean(key, false));
		} else if (p instanceof ListPreference) {
			ListPreference l = (ListPreference) p;
			value = l.getValue();
		}

		getPrefs().put(key, value);
		getPrefs().commit();
		updateLabel(key);
	}

}
