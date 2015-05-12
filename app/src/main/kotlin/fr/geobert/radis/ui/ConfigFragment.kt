package fr.geobert.radis.ui

import android.content.SharedPreferences
import android.database.Cursor
import android.os.Bundle
import android.preference.CheckBoxPreference
import android.preference.EditTextPreference
import android.preference.ListPreference
import android.preference.PreferenceManager
import android.support.v4.app.LoaderManager
import android.support.v4.content.Loader
import android.support.v4.preference.PreferenceFragment
import android.util.Log
import android.view.View
import fr.geobert.radis.R
import fr.geobert.radis.data.AccountConfig
import fr.geobert.radis.db.AccountTable
import fr.geobert.radis.db.DbContentProvider
import fr.geobert.radis.db.PreferenceTable
import fr.geobert.radis.tools.DBPrefsManager
import fr.geobert.radis.tools.map
import fr.geobert.radis.ui.editor.AccountEditor
import kotlin.properties.Delegates

public class ConfigFragment : PreferenceFragment(), SharedPreferences.OnSharedPreferenceChangeListener, LoaderManager.LoaderCallbacks<Cursor> {
    // only in global prefs, it is lazy so no crash in AccountEditor
    private val mAccountsChoice by Delegates.lazy { findPreference(KEY_DEFAULT_ACCOUNT) as ListPreference }

    //TODO lazy access to prefs in AccountEditor mode
    private val mOverInsertDate by Delegates.lazy { findPreference(KEY_OVERRIDE_INSERT_DATE) as CheckBoxPreference }

    private val isAccountEditor by Delegates.lazy { getActivity() is AccountEditor }
    private var mOnRestore: Boolean = false

    var mConfig: AccountConfig by Delegates.notNull()
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super<PreferenceFragment>.onCreate(savedInstanceState)
        addPreferencesFromResource(if (isAccountEditor) R.xml.account_prefs else R.xml.preferences)
        if (!isAccountEditor) // do not call account spinner init, it does not exist
            initAccountChoices()
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super<PreferenceFragment>.onViewCreated(view, savedInstanceState)
        if (isAccountEditor) {
            // change only in account editor, because de listView is not match_parent
            getListView().setBackgroundColor(getResources().getColor(R.color.normal_bg))

            val act = getActivity() as AccountEditor
            if (act.isNewAccount()) {
                mConfig = AccountConfig()
            }
        }
        if (savedInstanceState != null)
            onRestoreInstanceState(savedInstanceState)
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
                AccountTable.ACCOUNT_ID_AND_NAME_COLS, null, null, null)
        if (accounts.moveToFirst()) {
            val entries = accounts.map { it.getString(it.getColumnIndex(AccountTable.KEY_ACCOUNT_NAME)) }.copyToArray()
            val values = accounts.map { it.getString(it.getColumnIndex(AccountTable.KEY_ACCOUNT_ROWID)) }.copyToArray()
            mAccountsChoice.setEntries(entries)
            mAccountsChoice.setEntryValues(values)
        }
        accounts.close()
    }

    fun updateLabel(key: String, account: AccountConfig? = null) {
        val summary = when (key) {
            getKey(KEY_INSERTION_DATE) -> {
                val value = if (account != null) account.insertDate.toString() else getPrefs().getString(key, DEFAULT_INSERTION_DATE)
                val s = getString(R.string.prefs_insertion_date_text)
                s.format(value)
            }
            getKey(KEY_NB_MONTH_AHEAD) -> {
                val value = if (account != null) account.nbMonthsAhead else getPrefs().getInt(key, DEFAULT_NB_MONTH_AHEAD)
                getString(R.string.prefs_nb_month_ahead_text).format(value)
            }
            getKey(KEY_QUICKADD_ACTION) -> {
                val valueIdx = if (account != null) account.quickAddAction else getPrefs().getInt(key, DEFAULT_QUICKADD_LONG_PRESS_ACTION)
                val value = getActivity().getResources().getStringArray(R.array.quickadd_actions)[valueIdx]
                getString(R.string.quick_add_long_press_action_text).format(value)
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

        val act = getActivity()
        PreferenceManager.getDefaultSharedPreferences(act).registerOnSharedPreferenceChangeListener(this)

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
            updateLabel(KEY_INSERTION_DATE)
            updateLabel(KEY_NB_MONTH_AHEAD)
            updateLabel(KEY_QUICKADD_ACTION)
        } else if (act is AccountEditor) {
            if (!mOnRestore && !act.isNewAccount()) {
                act.getSupportLoaderManager().initLoader<Cursor>(AccountEditor.GET_ACCOUNT_CONFIG, Bundle(), this)
            } else {
                mOnRestore = false
            }

            if (act.isNewAccount()) {
                updateLabel(KEY_INSERTION_DATE)
                updateLabel(KEY_NB_MONTH_AHEAD)
                updateLabel(KEY_QUICKADD_ACTION)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super<PreferenceFragment>.onSaveInstanceState(outState)
        if (isAccountEditor)
            outState.putParcelable("mConfig", mConfig)
        mOnRestore = true
    }

    private fun onRestoreInstanceState(state: Bundle) {
        mOnRestore = true
        if (isAccountEditor)
            mConfig = state.getParcelable<AccountConfig>("mConfig")
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

    private fun setListPrefValue(key: String, value: Int) {
        val l = findPreference(key) as ListPreference
        l.setValueIndex(value)
        PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().putInt(key, value)
    }

    public fun populateFields(account: AccountConfig) {
        setCheckBoxPrefState(KEY_OVERRIDE_INSERT_DATE, account.overrideInsertDate)
        setCheckBoxPrefState(KEY_OVERRIDE_HIDE_QUICK_ADD, account.overrideHideQuickAdd)
        setCheckBoxPrefState(KEY_OVERRIDE_USE_WEIGHTED_INFO, account.overrideUseWeighedInfo)
        setCheckBoxPrefState(KEY_OVERRIDE_INVERT_QUICKADD_COMPLETION, account.overrideInvertQuickAddComp)
        setCheckBoxPrefState(KEY_OVERRIDE_NB_MONTH_AHEAD, account.overrideNbMonthsAhead)
        setEditTextPrefValue(getKey(KEY_INSERTION_DATE), account.insertDate.toString())
        setEditTextPrefValue(getKey(KEY_NB_MONTH_AHEAD), account.nbMonthsAhead.toString())
        setListPrefValue(getKey(KEY_QUICKADD_ACTION), account.quickAddAction)
        updateLabel(getKey(KEY_INSERTION_DATE), account)
        updateLabel(getKey(KEY_NB_MONTH_AHEAD), account)
        updateLabel(getKey(KEY_QUICKADD_ACTION), account)
        setCheckBoxPrefState(getKey(KEY_HIDE_OPS_QUICK_ADD), account.hideQuickAdd)
        setCheckBoxPrefState(getKey(KEY_USE_WEIGHTED_INFOS), account.useWeighedInfo)
        setCheckBoxPrefState(getKey(KEY_INVERT_COMPLETION_IN_QUICK_ADD), account.invertQuickAddComp)
        PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().commit()
    }

    private fun getCheckBoxPrefValue(key: String): Boolean {
        val chk = findPreference(key) as CheckBoxPreference
        val r = chk.isChecked()
        return r
    }

    private fun getEdtPrefValue(key: String): String {
        val edt = findPreference(key) as EditTextPreference
        val r = edt.getEditText().getText().toString()
        return r
    }

    private fun getListPrefValue(key: String): Int {
        val l = findPreference(key) as ListPreference
        return l.getValue().toInt()
    }

    // called by AccountEditFragment.saveState
    fun fillConfig(): AccountConfig {
        mConfig.overrideInsertDate = getCheckBoxPrefValue(KEY_OVERRIDE_INSERT_DATE)
        val d = getEdtPrefValue(getKey(KEY_INSERTION_DATE))
        mConfig.insertDate = if (d.trim().length() > 0) d.toInt() else DEFAULT_INSERTION_DATE.toInt()
        mConfig.overrideHideQuickAdd = getCheckBoxPrefValue(KEY_OVERRIDE_HIDE_QUICK_ADD)
        mConfig.hideQuickAdd = getCheckBoxPrefValue(getKey(KEY_HIDE_OPS_QUICK_ADD))
        mConfig.overrideUseWeighedInfo = getCheckBoxPrefValue(KEY_OVERRIDE_USE_WEIGHTED_INFO)
        mConfig.useWeighedInfo = getCheckBoxPrefValue(getKey(KEY_USE_WEIGHTED_INFOS))
        mConfig.overrideInvertQuickAddComp = getCheckBoxPrefValue(KEY_OVERRIDE_INVERT_QUICKADD_COMPLETION)
        mConfig.invertQuickAddComp = getCheckBoxPrefValue(getKey(KEY_INVERT_COMPLETION_IN_QUICK_ADD))
        mConfig.overrideNbMonthsAhead = getCheckBoxPrefValue(KEY_OVERRIDE_NB_MONTH_AHEAD)
        val m = getEdtPrefValue(getKey(KEY_NB_MONTH_AHEAD))
        mConfig.nbMonthsAhead = if (m.trim().length() > 0) m.toInt() else DEFAULT_NB_MONTH_AHEAD
        mConfig.overrideQuickAddAction = getCheckBoxPrefValue(KEY_OVERRIDE_QUICKADD_ACTION)
        mConfig.quickAddAction = getListPrefValue(getKey(KEY_QUICKADD_ACTION))
        return mConfig
    }

    override fun onPause() {
        super<PreferenceFragment>.onPause()
        val act = getActivity()
        PreferenceManager.getDefaultSharedPreferences(act).unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
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

    override fun onCreateLoader(id: Int, args: Bundle): Loader<Cursor> {
        val act = getActivity() as AccountEditor
        return PreferenceTable.getAccountConfigLoader(act, act.mRowId)
    }


    override fun onLoadFinished(arg0: Loader<Cursor>, data: Cursor) {
        if (data.moveToFirst()) {
            mConfig = AccountConfig(data)
            populateFields(mConfig)
        } else {
            mConfig = AccountConfig()
        }
    }

    override fun onLoaderReset(arg0: Loader<Cursor>) {
    }


    companion object {
        public val KEY_OVERRIDE_INSERT_DATE: String = "override_insertion_date"
        public val KEY_OVERRIDE_HIDE_QUICK_ADD: String = "override_hide_quickadd"
        public val KEY_OVERRIDE_USE_WEIGHTED_INFO: String = "override_use_weighted_info"
        public val KEY_OVERRIDE_INVERT_QUICKADD_COMPLETION: String = "override_invert_quickadd_completion"
        public val KEY_OVERRIDE_QUICKADD_ACTION: String = "override_quick_add_long_press_action"
        public val KEY_INSERTION_DATE: String = "insertion_date"
        public val KEY_LAST_INSERTION_DATE: String = "LAST_INSERT_DATE"
        public val KEY_DEFAULT_ACCOUNT: String = "quickadd_account"
        public val KEY_HIDE_OPS_QUICK_ADD: String = "hide_ops_quick_add"
        public val KEY_USE_WEIGHTED_INFOS: String = "use_weighted_infos"
        public val KEY_INVERT_COMPLETION_IN_QUICK_ADD: String = "invert_completion_in_quick_add"
        public val KEY_OVERRIDE_NB_MONTH_AHEAD: String = "override_nb_month_ahead"
        public val KEY_NB_MONTH_AHEAD: String = "nb_month_ahead"
        public val KEY_QUICKADD_ACTION: String = "quick_add_long_press_action"
        public val DEFAULT_INSERTION_DATE: String = "25"
        public val DEFAULT_NB_MONTH_AHEAD: Int = 1
        public val DEFAULT_QUICKADD_LONG_PRESS_ACTION: Int = 0
    }

}
