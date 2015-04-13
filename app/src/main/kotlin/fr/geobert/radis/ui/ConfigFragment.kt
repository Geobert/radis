package fr.geobert.radis.ui

import android.content.SharedPreferences
import android.os.Bundle
import android.preference.CheckBoxPreference
import android.preference.EditTextPreference
import android.preference.ListPreference
import android.preference.PreferenceManager
import android.support.v4.preference.PreferenceFragment
import android.util.Log
import android.view.View
import fr.geobert.radis.R
import fr.geobert.radis.data.Account
import fr.geobert.radis.db.AccountTable
import fr.geobert.radis.db.DbContentProvider
import fr.geobert.radis.tools.DBPrefsManager
import fr.geobert.radis.tools.map
import fr.geobert.radis.ui.editor.AccountEditor
import kotlin.properties.Delegates

public class ConfigFragment() : PreferenceFragment() {
    // only in global prefs, it is lazy so no crash in AccountEditor
    private val mAccountsChoice by Delegates.lazy { findPreference(KEY_DEFAULT_ACCOUNT) as ListPreference }

    //TODO lazy access to prefs in AccountEditor mode
    private val mOverInsertDate by Delegates.lazy { findPreference(KEY_OVERRIDE_INSERT_DATE) as CheckBoxPreference }

    private val isAccountEditor by Delegates.lazy { getActivity() is AccountEditor }

    override fun onCreate(savedInstanceState: Bundle?) {
        super<PreferenceFragment>.onCreate(savedInstanceState)
        addPreferencesFromResource(if (isAccountEditor) R.xml.account_prefs else R.xml.preferences)
        if (!isAccountEditor) // do not call account spinner init, it does not exist
            initAccountChoices()
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super<PreferenceFragment>.onViewCreated(view, savedInstanceState)
        if (isAccountEditor) // change only in account editor, because de listView is not match_parent
            getListView().setBackgroundColor(getResources().getColor(R.color.normal_bg))
    }

    private fun getPrefs(): DBPrefsManager {
        return DBPrefsManager.getInstance(getActivity())
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
        if (accounts.moveToFirst()) {
            val entries = accounts.map { it.getString(it.getColumnIndex(AccountTable.KEY_ACCOUNT_NAME)) }.copyToArray()
            val values = accounts.map { it.getString(it.getColumnIndex(AccountTable.KEY_ACCOUNT_ROWID)) }.copyToArray()
            mAccountsChoice.setEntries(entries)
            mAccountsChoice.setEntryValues(values)
        }
        accounts.close()
    }

    fun updateLabel(key: String, account: Account? = null) {
        val summary = when (key) {
            getKey(KEY_INSERTION_DATE) -> {
                val value = if (account != null) account.insertDate.toString() else getPrefs().getString(key, DEFAULT_INSERTION_DATE)
                val s = getString(R.string.prefs_insertion_date_text)
                s.format(value)
            }
            KEY_DEFAULT_ACCOUNT -> {
                // !isAccountEditor only
                val l = findPreference(key) as ListPreference
                val s = l.getEntry()
                if (null != s) {
                    val value = s.toString()
                    getString(R.string.default_account_desc, value)
                } else {
                    null
                }
            }
            else -> null
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

        Log.d("PrefBug", "onResume set listener")
        val act = getActivity()
        PreferenceManager.getDefaultSharedPreferences(act).registerOnSharedPreferenceChangeListener(act as SharedPreferences.OnSharedPreferenceChangeListener)

        if (!isAccountEditor) {
            var value: String? = getPrefs().getString(KEY_INSERTION_DATE, DEFAULT_INSERTION_DATE)
            Log.d("PrefBug", "onResume $KEY_INSERTION_DATE = $value")
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
            updateLabel(KEY_INSERTION_DATE)
        } else if (act is AccountEditor) {
            if (act.getAccountFrag().isNewAccount()) {
                updateLabel(KEY_INSERTION_DATE)
            }
        }
    }

    private fun setCheckBoxPrefState(key: String, value: Boolean) {
        val checkbox = findPreference(key) as CheckBoxPreference
        checkbox.setChecked(value)
        PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().putBoolean(key, value)
    }

    private fun setEditTextPrefValue(key: String, value: String) {
        val edt = findPreference(key) as EditTextPreference
        edt.getEditText().setText(value)
        PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().putString(key, value)
    }

    public fun populateFields(account: Account) {
        setCheckBoxPrefState(KEY_OVERRIDE_INSERT_DATE, account.overrideInsertDate)
        setCheckBoxPrefState(KEY_OVERRIDE_HIDE_QUICK_ADD, account.overrideHideQuickAdd)
        setCheckBoxPrefState(KEY_OVERRIDE_USE_WEIGHTED_INFO, account.overrideUseWeighedInfo)
        setCheckBoxPrefState(KEY_OVERRIDE_INVERT_QUICKADD_COMPLETION, account.overrideInvertQuickAddComp)
        setEditTextPrefValue(getKey(KEY_INSERTION_DATE), account.insertDate.toString())
        updateLabel(getKey(KEY_INSERTION_DATE), account)
        setCheckBoxPrefState(getKey(KEY_HIDE_OPS_QUICK_ADD), account.hideQuickAdd)
        setCheckBoxPrefState(getKey(KEY_USE_WEIGHTED_INFOS), account.useWeighedInfo)
        setCheckBoxPrefState(getKey(KEY_INVERT_COMPLETION_IN_QUICK_ADD), account.invertQuickAddComp)
        PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().commit()
    }

    private fun getCheckBoxPrefValue(key: String): Boolean {
        val chk = findPreference(key) as CheckBoxPreference
        val r = chk.isChecked()
        Log.d("Bug", "getCheckBoxPrefValue $key = $r")
        return r
    }

    private fun getEdtPrefValue(key: String): String {
        val edt = findPreference(key) as EditTextPreference
        val r = edt.getEditText().getText().toString()
        Log.d("Bug", "getEdtPrefValue $key = $r")
        return r
    }

    // called by AccountEditFragment.saveState
    fun saveState(account: Account) {
        account.overrideInsertDate = getCheckBoxPrefValue(KEY_OVERRIDE_INSERT_DATE)
        val d = getEdtPrefValue(getKey(KEY_INSERTION_DATE))
        account.insertDate = if (d.trim().length() > 0) d.toInt() else DEFAULT_INSERTION_DATE.toInt()
        account.overrideHideQuickAdd = getCheckBoxPrefValue(KEY_OVERRIDE_HIDE_QUICK_ADD)
        account.hideQuickAdd = getCheckBoxPrefValue(getKey(KEY_HIDE_OPS_QUICK_ADD))
        account.overrideUseWeighedInfo = getCheckBoxPrefValue(KEY_OVERRIDE_USE_WEIGHTED_INFO)
        account.useWeighedInfo = getCheckBoxPrefValue(getKey(KEY_USE_WEIGHTED_INFOS))
        account.overrideInvertQuickAddComp = getCheckBoxPrefValue(KEY_OVERRIDE_INVERT_QUICKADD_COMPLETION)
        account.invertQuickAddComp = getCheckBoxPrefValue(getKey(KEY_INVERT_COMPLETION_IN_QUICK_ADD))
    }

    override fun onPause() {
        super<PreferenceFragment>.onPause()
        val act = getActivity()
        PreferenceManager.getDefaultSharedPreferences(act).unregisterOnSharedPreferenceChangeListener(act as SharedPreferences.OnSharedPreferenceChangeListener)
    }

    fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        val p = findPreference(key)
        val value = if (p is EditTextPreference) {
            notEmpty(sharedPreferences.getString(key, null))
        } else if (p is ListPreference) {
            p.getValue()
        } else if (p is CheckBoxPreference) {
            java.lang.Boolean.toString(p.isChecked())
        } else {
            ""
        }
        getPrefs().put(key, value) // put "_for_account" keys in cache for updateLabel
        updateLabel(key)
    }

    companion object {
        public val KEY_OVERRIDE_INSERT_DATE: String = "override_insertion_date"
        public val KEY_OVERRIDE_HIDE_QUICK_ADD: String = "override_hide_quickadd"
        public val KEY_OVERRIDE_USE_WEIGHTED_INFO: String = "override_use_weighted_info"
        public val KEY_OVERRIDE_INVERT_QUICKADD_COMPLETION: String = "override_invert_quickadd_completion"
        public val KEY_INSERTION_DATE: String = "insertion_date"
        public val KEY_LAST_INSERTION_DATE: String = "LAST_INSERT_DATE"
        public val KEY_DEFAULT_ACCOUNT: String = "quickadd_account"
        public val KEY_HIDE_OPS_QUICK_ADD: String = "hide_ops_quick_add"
        public val KEY_USE_WEIGHTED_INFOS: String = "use_weighted_infos"
        public val KEY_INVERT_COMPLETION_IN_QUICK_ADD: String = "invert_completion_in_quick_add"
        public val DEFAULT_INSERTION_DATE: String = "25"
    }

}
