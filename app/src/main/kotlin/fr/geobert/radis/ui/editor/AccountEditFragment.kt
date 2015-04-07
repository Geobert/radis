package fr.geobert.radis.ui.editor

import android.content.Context
import android.database.Cursor
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.LoaderManager
import android.support.v4.content.Loader
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import fr.geobert.radis.BaseActivity
import fr.geobert.radis.R
import fr.geobert.radis.data.Account
import fr.geobert.radis.db.AccountTable
import fr.geobert.radis.tools.*
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Arrays
import java.util.Currency
import java.util.Locale
import kotlin.properties.Delegates

public class AccountEditFragment : Fragment(), LoaderManager.LoaderCallbacks<Cursor> {
    private val mAccountNameText by Delegates.lazy { getActivity().findViewById(R.id.edit_account_name) as EditText }
    private val mAccountStartSumText by Delegates.lazy { getActivity().findViewById(R.id.edit_account_start_sum) as EditText }
    private val mAccountDescText by Delegates.lazy { getActivity().findViewById(R.id.edit_account_desc) as EditText }
    private val mAccountCurrency by Delegates.lazy { getActivity().findViewById(R.id.currency_spinner) as Spinner }
    private val mCustomCurrency by Delegates.lazy { getActivity().findViewById(R.id.custom_currency) as EditText }
    private val mProjectionController by Delegates.lazy { ProjectionDateController(getActivity()) }

    var mAccount: Account by Delegates.notNull()
    private var mRowId: Long = 0
    private var customCurrencyIdx = -1
    private var mOnRestore = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.account_editor, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super<Fragment>.onViewCreated(view, savedInstanceState)
        mProjectionController
        mRowId = if (savedInstanceState != null) {
            savedInstanceState.getSerializable(PARAM_ACCOUNT_ID) as Long
        } else {
            val extra = getActivity().getIntent().getExtras()
            if (extra != null) {
                extra.getLong(PARAM_ACCOUNT_ID)
            } else {
                NO_ACCOUNT
            }
        }
        val act = getActivity() as BaseActivity
        if (NO_ACCOUNT == mRowId) {
            mAccount = Account()
            act.setTitle(R.string.account_creation)
        } else {
            act.setTitle(R.string.account_edit_title)
        }

        val w = CorrectCommaWatcher(getSumSeparator(), mAccountStartSumText)
        w.setAutoNegate(false)
        mAccountStartSumText.addTextChangedListener(w)
        mAccountStartSumText.setOnFocusChangeListener(object : View.OnFocusChangeListener {
            override fun onFocusChange(v: View, hasFocus: Boolean) {
                if (hasFocus) {
                    (v as EditText).selectAll()
                }
            }
        })
        fillCurrencySpinner()
    }

    private fun fillCurrencySpinner() {
        val mCurrAdapter = ArrayAdapter.createFromResource(getActivity(), R.array.all_currencies, android.R.layout.simple_spinner_item)
        mCurrAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        mAccountCurrency.setAdapter(mCurrAdapter)
        mAccountCurrency.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {

            override fun onItemSelected(arg0: AdapterView<*>, arg1: View, pos: Int, id: Long) {
                mCustomCurrency.setEnabled(pos == getCustomCurrencyIdx(getActivity()))
            }

            override fun onNothingSelected(arg0: AdapterView<*>) {
            }

        })
    }

    // check the form and fill mAccount
    fun isFormValid(errMsg: StringBuilder): Boolean {
        val name = mAccountNameText.getText().toString()
        var res = true
        if (name.length() == 0) {
            errMsg.append(R.string.empty_account_name)
            res = false
        }
        if (mAccountStartSumText.getText().length() == 0) {
            mAccountStartSumText.setText("0")
        }


        // check if currency is correct
        if (mAccountCurrency.getSelectedItemPosition() == getCustomCurrencyIdx(getActivity())) {
            val currency = mCustomCurrency.getText().toString().trim().toUpperCase()
            try {
                Currency.getInstance(currency)
            } catch (e: IllegalArgumentException) {
                if (errMsg.length() > 0)
                    errMsg.append("\n")
                errMsg.append(getString(R.string.bad_format_for_currency))
                res = false
            }

        }
        // check projection date format
        if (mProjectionController.mProjectionDate.getVisibility() == View.VISIBLE) {
            //                && mProjectionController.getDate().trim().length() == 0) {
            try {
                val format: SimpleDateFormat =
                        if (mProjectionController.getMode() == AccountTable.PROJECTION_DAY_OF_NEXT_MONTH) {
                            SimpleDateFormat("dd")
                        } else {
                            SimpleDateFormat("dd/MM/yyyy")
                        }
                val d = format.parse(mProjectionController.getDate().trim())
                mProjectionController.mProjectionDate.setText(format.format(d))
            } catch (e: ParseException) {
                e.printStackTrace()
                if (errMsg.length() > 0)
                    errMsg.append("\n")
                errMsg.append(getString(R.string.bad_format_for_date))
                res = false
            }

        }
        // done in saveState
        //        if (res) {
        //            mAccount.name = name
        //            mAccount.startSum = mAccountStartSumText.getText().toString().extractSumFromStr()
        //            mAccount.currency = if (mAccountCurrency.getSelectedItemPosition() == getCustomCurrencyIdx(getActivity())) {
        //                mCustomCurrency.getText().toString().trim().toUpperCase()
        //            } else {
        //                mAccountCurrency.getSelectedItem().toString()
        //            }
        //            mAccount.description = mAccountDescText.getText().toString()
        //            mAccount.projMode = mProjectionController.getMode()
        //            mAccount.projDate = mProjectionController.getDate()
        //            // TODO prefs
        //        }
        return res
    }

    private fun populateFields(account: Account) {
        mAccountNameText.setText(account.name)
        mAccountDescText.setText(account.description)
        mAccountStartSumText.setText((account.startSum / 100.0).formatSum())
        var currencyStr = account.currency
        if (currencyStr.length() == 0) {
            currencyStr = Currency.getInstance(Locale.getDefault()).getCurrencyCode()
        }
        initCurrencySpinner(currencyStr)
        mProjectionController.populateFields(account)
    }

    private fun getCustomCurrencyIdx(ctx: Context): Int {
        if (customCurrencyIdx == -1) {
            val res = ctx.getResources()
            val allCurrencies = res.getStringArray(R.array.all_currencies)
            customCurrencyIdx = allCurrencies.size() - 1
        }
        return customCurrencyIdx
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super<Fragment>.onSaveInstanceState(outState)
        outState.putString("name", mAccountNameText.getText().toString())
        outState.putString("startSum", mAccountStartSumText.getText().toString())
        outState.putInt("currency", mAccountCurrency.getSelectedItemPosition())
        val customCurIdx = getCustomCurrencyIdx(getActivity())
        outState.putInt("customCurrencyIdx", customCurIdx)
        if (mAccountCurrency.getSelectedItemPosition() == customCurIdx) {
            outState.putString("customCurrency", mCustomCurrency.getText().toString())
        }
        outState.putString("desc", mAccountDescText.getText().toString())
        mProjectionController.onSaveInstanceState(outState)
        mOnRestore = true
    }

    fun onRestoreInstanceState(state: Bundle) {
        mAccountNameText.setText(state.getString("name"))
        mAccountStartSumText.setText(state.getString("startSum"))
        mAccountCurrency.setSelection(state.getInt("currency"))
        customCurrencyIdx = state.getInt("customCurrencyIdx")
        if (mAccountCurrency.getSelectedItemPosition() == getCustomCurrencyIdx(getActivity())) {
            mCustomCurrency.setText(state.getString("customCurrency"))
            mCustomCurrency.setEnabled(true)
        } else {
            mCustomCurrency.setEnabled(false)
        }
        mAccountDescText.setText(state.getString("desc"))
        mProjectionController.onRestoreInstanceState(state)
        mOnRestore = true
    }

    private fun initCurrencySpinner(currencyStr: String) {
        val allCurrencies = getResources().getStringArray(R.array.all_currencies)
        val pos = Arrays.binarySearch(allCurrencies, currencyStr)
        if (pos >= 0) {
            mAccountCurrency.setSelection(pos)
            mCustomCurrency.setEnabled(false)
        } else {
            mAccountCurrency.setSelection(getCustomCurrencyIdx(getActivity()))
            mCustomCurrency.setEnabled(true)
        }
    }

    override fun onResume() {
        super<Fragment>.onResume()
        if (!mOnRestore && mRowId != NO_ACCOUNT) {
            getActivity().getSupportLoaderManager().initLoader<Cursor>(GET_ACCOUNT, Bundle(), this)
        } else {
            mOnRestore = false
            if (mRowId == NO_ACCOUNT) {
                try {
                    initCurrencySpinner(Currency.getInstance(Locale.getDefault()).getCurrencyCode())
                } catch (ex: IllegalArgumentException) {
                    initCurrencySpinner(Currency.getInstance(Locale("fr", "FR")).getCurrencyCode())
                }

            }
        }
    }

    fun saveState() {
        mAccount.name = mAccountNameText.getText().toString().trim()
        mAccount.description = mAccountDescText.getText().toString().trim()
        try {
            mAccount.startSum = mAccountStartSumText.getText().toString().extractSumFromStr()
            mAccount.currency = if (mAccountCurrency.getSelectedItemPosition() == getCustomCurrencyIdx(getActivity())) {
                mCustomCurrency.getText().toString().trim().toUpperCase()
            } else {
                mAccountCurrency.getSelectedItem().toString()
            }
            mAccount.projMode = mProjectionController.getMode()
            mAccount.projDate = mProjectionController.getDate()

            // TODO prefs

            if (mRowId == NO_ACCOUNT) {
                AccountTable.createAccount(getActivity(), mAccount)
            } else {
                AccountTable.updateAccount(getActivity(), mAccount)
            }
        } catch (e: ParseException) {
            e.printStackTrace()
        }

    }

    override fun onCreateLoader(id: Int, args: Bundle): Loader<Cursor> {
        val loader = AccountTable.getAccountLoader(getActivity(), mRowId)
        return loader
    }

    override fun onLoadFinished(arg0: Loader<Cursor>, data: Cursor) {
        if (data.moveToFirst()) {
            AccountTable.initProjectionDate(data)
            mAccount = Account(data)
            populateFields(mAccount)
            val act = getActivity() as AccountEditor
            act.getConfigFrag().populateFields(mAccount) // TODO cleanup by moving cursor loader imp in AccountEditor
        }
    }

    override fun onLoaderReset(arg0: Loader<Cursor>) {
    }

    companion object {
        public val NO_ACCOUNT: Long = 0
        public val ACCOUNT_EDITOR: Int = 1000
        public val PARAM_ACCOUNT_ID: String = "account_id"
        private val GET_ACCOUNT = 400
    }
}
