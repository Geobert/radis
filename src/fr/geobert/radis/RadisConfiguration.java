package fr.geobert.radis;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import fr.geobert.radis.db.AccountTable;
import fr.geobert.radis.db.DbContentProvider;
import fr.geobert.radis.tools.DBPrefsManager;

import java.util.ArrayList;

public class RadisConfiguration extends PreferenceActivity implements
        OnSharedPreferenceChangeListener {
    public final static String KEY_INSERTION_DATE = "insertion_date";
    public final static String KEY_LAST_INSERTION_DATE = "LAST_INSERT_DATE";
    public final static String KEY_DEFAULT_ACCOUNT = "quickadd_account";
    public final static String KEY_HIDE_ACCOUNT_QUICK_ADD = "hide_account_quick_add";
    public final static String KEY_HIDE_OPS_QUICK_ADD = "hide_ops_quick_add";
    public final static String KEY_USE_WEIGHTED_INFOS = "use_weighted_infos";
    public final static String DEFAULT_INSERTION_DATE = "25";

    private ListPreference mAccountsChoice;

    private DBPrefsManager getPrefs() {
        return DBPrefsManager.getInstance(this);
    }

    private SharedPreferences getSharedPreferences() {
        return super.getSharedPreferences(DBPrefsManager.SHARED_PREF_NAME,
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
        mAccountsChoice = (ListPreference) findPreference(KEY_DEFAULT_ACCOUNT);
        initAccountChoices();
    }

    private void initAccountChoices() {
        Cursor accounts = getContentResolver().query(
                DbContentProvider.ACCOUNT_URI, AccountTable.ACCOUNT_COLS, null,
                null, null);
        ArrayList<CharSequence> entries = new ArrayList<CharSequence>();
        ArrayList<CharSequence> values = new ArrayList<CharSequence>();
        if (accounts.moveToFirst()) {
            do {
                entries.add(accounts.getString(accounts
                        .getColumnIndex(AccountTable.KEY_ACCOUNT_NAME)));
                values.add(String.valueOf(accounts.getLong(accounts
                        .getColumnIndex(AccountTable.KEY_ACCOUNT_ROWID))));

            } while (accounts.moveToNext());
        }
        mAccountsChoice.setEntries(entries.toArray(new CharSequence[entries
                .size()]));
        mAccountsChoice.setEntryValues(values.toArray(new CharSequence[entries
                .size()]));
        accounts.close();
    }

    private void updateLabel(String key) {
        String summary = null;

        if (KEY_INSERTION_DATE.equals(key)) {
            String value = getPrefs().getString(key, DEFAULT_INSERTION_DATE);
            String s = getString(R.string.prefs_insertion_date_text);
            summary = String.format(s, value);
        } else if (KEY_DEFAULT_ACCOUNT.equals(key)) {
            ListPreference l = (ListPreference) findPreference(key);
            CharSequence s = l.getEntry();
            if (null != s) {
                String value = s.toString();
                summary = getString(R.string.quickadd_account_desc, value);
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
        String value = getPrefs().getString(KEY_INSERTION_DATE,
                DEFAULT_INSERTION_DATE);
        EditTextPreference ep = (EditTextPreference) findPreference(KEY_INSERTION_DATE);
        ep.getEditText().setText(value);

        value = getPrefs().getString(KEY_DEFAULT_ACCOUNT);
        if (value != null) {
            for (CharSequence s : mAccountsChoice.getEntryValues()) {
                if (value.equals(s)) {
                    mAccountsChoice.setValue(s.toString());
                }
            }
        }

        updateLabel(KEY_INSERTION_DATE);
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
        String value = "";
        Preference p = findPreference(key);
        if (p instanceof EditTextPreference) {
            value = notEmpty(sharedPreferences.getString(key, null));
        } else if (p instanceof ListPreference) {
            ListPreference l = (ListPreference) p;
            value = l.getValue();
        } else if (p instanceof CheckBoxPreference) {
            CheckBoxPreference c = (CheckBoxPreference) p;
            value = Boolean.toString(c.isChecked());
        }

        getPrefs().put(key, value);
        updateLabel(key);
    }

}
