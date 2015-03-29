package fr.geobert.radis.ui

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.CheckBoxPreference
import android.preference.EditTextPreference
import android.preference.ListPreference
import android.support.v4.preference.PreferenceFragment
import android.view.View
import android.view.ViewGroup
import fr.geobert.radis.R
import fr.geobert.radis.db.AccountTable
import fr.geobert.radis.db.DbContentProvider
import fr.geobert.radis.tools.DBPrefsManager
import fr.geobert.radis.tools.map
import fr.geobert.radis.ui.editor.AccountEditor
import kotlin.properties.Delegates

public class ConfigFragment : PreferenceFragment(), SharedPreferences.OnSharedPreferenceChangeListener {
    private val mAccountsChoice: ListPreference by Delegates.lazy { findPreference(KEY_DEFAULT_ACCOUNT) as ListPreference }
    private val isAccountEditor by Delegates.lazy { getActivity() is AccountEditor }

    override fun onCreate(savedInstanceState: Bundle?) {
        super<PreferenceFragment>.onCreate(savedInstanceState)
        addPreferencesFromResource(if (isAccountEditor) R.xml.account_prefs else R.xml.preferences)
        if (!isAccountEditor)
            initAccountChoices()
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super<PreferenceFragment>.onViewCreated(view, savedInstanceState)
        if (isAccountEditor)
            getListView().setBackgroundColor(getResources().getColor(R.color.normal_bg))
    }

    private fun getPrefs(): DBPrefsManager {
        return DBPrefsManager.getInstance(getActivity())
    }

    fun getSharedPreferences(): SharedPreferences {
        return getActivity().getSharedPreferences(DBPrefsManager.SHARED_PREF_NAME, Context.MODE_PRIVATE)
    }

    private fun notEmpty(s: String?): String? {
        if (s != null && s.trim().length() == 0) {
            return null
        }
        return s
    }

    private fun initAccountChoices() {
        val accounts = getActivity().getContentResolver().query(DbContentProvider.ACCOUNT_URI,
                AccountTable.ACCOUNT_COLS, null, null, null)
        //        val entries = Array<CharSequence>(accounts.getCount())
        //        val values = Array<CharSequence>()
        if (accounts.moveToFirst()) {
            val entries = accounts.map { it.getString(it.getColumnIndex(AccountTable.KEY_ACCOUNT_NAME)) }.copyToArray()
            val values = accounts.map { it.getString(it.getColumnIndex(AccountTable.KEY_ACCOUNT_ROWID)) }.copyToArray()
            mAccountsChoice.setEntries(entries)
            mAccountsChoice.setEntryValues(values)

            //            do {
            //                entries.add(accounts.getString(accounts.getColumnIndex(AccountTable.KEY_ACCOUNT_NAME)))
            //                values.add(String.valueOf(accounts.getLong(accounts.getColumnIndex(AccountTable.KEY_ACCOUNT_ROWID))))
            //
            //            } while (accounts.moveToNext())
            //            mAccountsChoice!!.setEntries(entries.toArray<CharSequence>(arrayOfNulls<CharSequence>(entries.size())))
            //            mAccountsChoice!!.setEntryValues(values.toArray<CharSequence>(arrayOfNulls<CharSequence>(entries.size())))
        }
        accounts.close()
    }

    private fun updateLabel(key: String) {
        var summary: String? = null

        if (KEY_INSERTION_DATE == key) {
            val value = getPrefs().getString(getKey(key), DEFAULT_INSERTION_DATE)
            val s = getString(R.string.prefs_insertion_date_text)
            summary = s.format(value)
        } else if (KEY_DEFAULT_ACCOUNT == key) {
            // !isAccountEditor only
            val l = findPreference(key) as ListPreference
            val s = l.getEntry()
            if (null != s) {
                val value = s.toString()
                summary = getString(R.string.default_account_desc, value)
            }
        }
        if (summary != null) {
            val ps = getPreferenceScreen()
            if (ps != null) {
                ps.findPreference(key)?.setSummary(summary)
            }
        }

    }

    fun getKey(k: String) = if (isAccountEditor) "${k}_for_account" else k

    override fun onResume() {
        super<PreferenceFragment>.onResume()

        if (!isAccountEditor) {
            var value: String? = getPrefs().getString(KEY_INSERTION_DATE, DEFAULT_INSERTION_DATE)
            val ep = findPreference(KEY_INSERTION_DATE) as EditTextPreference
            ep.getEditText().setText(value)

            value = getPrefs().getString(KEY_DEFAULT_ACCOUNT)
            if (value != null) {
                for (s in mAccountsChoice.getEntryValues()) {
                    if (value == s) {
                        mAccountsChoice.setValue(s.toString())
                    }
                }
            }
            updateLabel(KEY_DEFAULT_ACCOUNT)
        }

        updateLabel(KEY_INSERTION_DATE)
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super<PreferenceFragment>.onPause()
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        var value: String? = ""
        val p = findPreference(key)
        if (p is EditTextPreference) {
            value = notEmpty(sharedPreferences.getString(key, null))
        } else if (p is ListPreference) {
            val l = p
            value = l.getValue()
        } else if (p is CheckBoxPreference) {
            val c = p
            value = java.lang.Boolean.toString(c.isChecked())
        }

        getPrefs().put(key, value)
        updateLabel(key)
    }

    companion object {
        public val KEY_INSERTION_DATE: String = "insertion_date"
        public val KEY_LAST_INSERTION_DATE: String = "LAST_INSERT_DATE"
        public val KEY_DEFAULT_ACCOUNT: String = "quickadd_account"
        public val KEY_HIDE_OPS_QUICK_ADD: String = "hide_ops_quick_add"
        public val KEY_USE_WEIGHTED_INFOS: String = "use_weighted_infos"
        public val KEY_INVERT_COMPLETION_IN_QUICK_ADD: String = "invert_completion_in_quick_add"
        public val DEFAULT_INSERTION_DATE: String = "25"
    }
}
