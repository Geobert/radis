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
import fr.geobert.radis.R
import fr.geobert.radis.data.Account
import fr.geobert.radis.db.AccountTable
import fr.geobert.radis.tools.CorrectCommaWatcher
import fr.geobert.radis.tools.ProjectionDateController
import fr.geobert.radis.tools.extractSumFromStr
import fr.geobert.radis.tools.formatSum
import fr.geobert.radis.tools.getGroupSeparator
import fr.geobert.radis.tools.getSumSeparator
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.properties.Delegates

public class AccountEditFragment : Fragment(), LoaderManager.LoaderCallbacks<Cursor> {
    private var edit_account_name: EditText by Delegates.notNull()
    private var edit_account_start_sum: EditText by Delegates.notNull()
    private var edit_account_desc: EditText by Delegates.notNull()
    private var currency_spinner: Spinner by Delegates.notNull()
    private var custom_currency: EditText by Delegates.notNull()
    private val mProjectionController by lazy(LazyThreadSafetyMode.NONE) { ProjectionDateController(activity) }

    private var customCurrencyIdx = -1
    private var mOnRestore: Boolean = false

    var mAccount: Account by Delegates.notNull()
        private set

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val c = inflater.inflate(R.layout.account_editor, container, false)
        edit_account_name = c.findViewById(R.id.edit_account_name) as EditText
        edit_account_start_sum = c.findViewById(R.id.edit_account_start_sum) as EditText
        edit_account_desc = c.findViewById(R.id.edit_account_desc) as EditText
        currency_spinner = c.findViewById(R.id.currency_spinner) as Spinner
        custom_currency = c.findViewById(R.id.custom_currency) as EditText
        return c
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mProjectionController // trigger lazy access
        val act = activity as AccountEditor
        if (act.isNewAccount()) {
            mAccount = Account()
        }

        val w = CorrectCommaWatcher(getSumSeparator(), getGroupSeparator(), edit_account_start_sum)
        w.setAutoNegate(false)
        edit_account_start_sum.addTextChangedListener(w)
        edit_account_start_sum.onFocusChangeListener = object : View.OnFocusChangeListener {
            override fun onFocusChange(v: View, hasFocus: Boolean) {
                if (hasFocus) {
                    (v as EditText).selectAll()
                }
            }
        }
        fillCurrencySpinner()
        if (savedInstanceState != null)
            onRestoreInstanceState(savedInstanceState)
    }

    private fun fillCurrencySpinner() {
        val mCurrAdapter = ArrayAdapter.createFromResource(activity, R.array.all_currencies, android.R.layout.simple_spinner_item)
        mCurrAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        currency_spinner.adapter = mCurrAdapter
        currency_spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

            override fun onItemSelected(arg0: AdapterView<*>?, arg1: View?, pos: Int, id: Long) {
                custom_currency.visibility = if (pos == getCustomCurrencyIdx(activity)) View.VISIBLE else View.GONE
            }

            override fun onNothingSelected(arg0: AdapterView<*>?) {
            }

        }
    }

    // check the form and fill mAccount
    fun isFormValid(errMsg: StringBuilder): Boolean {
        val name = edit_account_name.text.toString()
        var res = true
        if (name.length == 0) {
            errMsg.append(R.string.empty_account_name)
            res = false
        }
        if (edit_account_start_sum.text.length == 0) {
            edit_account_start_sum.setText("0")
        }


        // check if currency is correct
        if (currency_spinner.selectedItemPosition == getCustomCurrencyIdx(activity)) {
            val currency = custom_currency.text.toString().trim().toUpperCase()
            try {
                Currency.getInstance(currency)
            } catch (e: IllegalArgumentException) {
                if (errMsg.length > 0)
                    errMsg.append("\n")
                errMsg.append(getString(R.string.bad_format_for_currency))
                res = false
            }

        }
        // check projection date format
        if (mProjectionController.mProjectionDate.visibility == View.VISIBLE) {
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
                if (errMsg.length > 0)
                    errMsg.append("\n")
                errMsg.append(getString(R.string.bad_format_for_date))
                res = false
            }

        }
        return res
    }

    fun populateFields(account: Account) {
        edit_account_name.setText(account.name)
        edit_account_desc.setText(account.description)
        edit_account_start_sum.setText((account.startSum / 100.0).formatSum())
        var currencyStr = account.currency
        if (currencyStr.length == 0) {
            currencyStr = Currency.getInstance(Locale.getDefault()).currencyCode
        }
        initCurrencySpinner(currencyStr)
        mProjectionController.populateFields(account)
    }

    private fun getCustomCurrencyIdx(ctx: Context): Int {
        if (customCurrencyIdx == -1) {
            val res = ctx.resources
            val allCurrencies = res.getStringArray(R.array.all_currencies)
            customCurrencyIdx = allCurrencies.size - 1
        }
        return customCurrencyIdx
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("name", edit_account_name.text.toString())
        outState.putString("startSum", edit_account_start_sum.text.toString())
        outState.putInt("currency", currency_spinner.selectedItemPosition)
        val customCurIdx = getCustomCurrencyIdx(activity)
        outState.putInt("customCurrencyIdx", customCurIdx)
        if (currency_spinner.selectedItemPosition == customCurIdx) {
            outState.putString("customCurrency", custom_currency.text.toString())
        }
        outState.putString("desc", edit_account_desc.text.toString())
        mProjectionController.onSaveInstanceState(outState)
        outState.putParcelable("mAccount", mAccount)
        mOnRestore = true
    }

    private fun onRestoreInstanceState(state: Bundle) {
        edit_account_name.setText(state.getString("name"))
        edit_account_start_sum.setText(state.getString("startSum"))
        currency_spinner.setSelection(state.getInt("currency"))
        customCurrencyIdx = state.getInt("customCurrencyIdx")
        if (currency_spinner.selectedItemPosition == getCustomCurrencyIdx(activity)) {
            custom_currency.setText(state.getString("customCurrency"))
            custom_currency.isEnabled = true
        } else {
            custom_currency.isEnabled = false
        }
        edit_account_desc.setText(state.getString("desc"))
        mProjectionController.onRestoreInstanceState(state)
        mAccount = state.getParcelable("mAccount")
        mOnRestore = true
    }

    override fun onResume() {
        super.onResume()
        val act = activity as AccountEditor
        if (!mOnRestore && !act.isNewAccount()) {
            act.supportLoaderManager.initLoader<Cursor>(AccountEditor.GET_ACCOUNT, Bundle(), this)
        } else {
            mOnRestore = false
        }
        if ((activity as AccountEditor).isNewAccount()) {
            initCurrencySpinner()
        }
    }

    fun initCurrencySpinner() {
        try {
            initCurrencySpinner(Currency.getInstance(Locale.getDefault()).currencyCode)
        } catch (ex: IllegalArgumentException) {
            initCurrencySpinner(Currency.getInstance(Locale("fr", "FR")).currencyCode)
        }
    }

    protected fun initCurrencySpinner(currencyStr: String) {
        val allCurrencies = resources.getStringArray(R.array.all_currencies)
        val pos = Arrays.binarySearch(allCurrencies, currencyStr)
        if (pos >= 0) {
            currency_spinner.setSelection(pos)
            custom_currency.isEnabled = false
        } else {
            currency_spinner.setSelection(getCustomCurrencyIdx(activity))
            custom_currency.isEnabled = true
        }
    }

    fun fillAccount(): Account {
        mAccount.name = edit_account_name.text.toString().trim()
        mAccount.description = edit_account_desc.text.toString().trim()
        try {
            mAccount.startSum = edit_account_start_sum.text.toString().extractSumFromStr()
            mAccount.currency = if (currency_spinner.selectedItemPosition == getCustomCurrencyIdx(activity)) {
                custom_currency.text.toString().trim().toUpperCase()
            } else {
                currency_spinner.selectedItem.toString()
            }
            mAccount.projMode = mProjectionController.getMode()
            mAccount.projDate = mProjectionController.getDate()
        } catch (e: ParseException) {
            e.printStackTrace()
        }
        return mAccount
    }

    override fun onCreateLoader(id: Int, args: Bundle): Loader<Cursor> {
        val act = activity as AccountEditor
        return AccountTable.getAccountLoader(act, act.mRowId)
    }

    override fun onLoadFinished(arg0: Loader<Cursor>, data: Cursor) {
        if (data.moveToFirst()) {
            AccountTable.initProjectionDate(data)
            mAccount = Account(data)
            populateFields(mAccount)
        }
    }

    override fun onLoaderReset(arg0: Loader<Cursor>) {
    }


}
