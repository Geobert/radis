package fr.geobert.radis.ui.editor

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.support.v4.app.LoaderManager
import android.support.v4.content.Loader
import android.support.v7.app.ActionBarActivity
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import fr.geobert.radis.BaseActivity
import fr.geobert.radis.R
import fr.geobert.radis.db.AccountTable
import fr.geobert.radis.tools.*
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Arrays
import java.util.Currency
import java.util.Locale

public class AccountEditor : BaseActivity(), LoaderManager.LoaderCallbacks<Cursor>, EditorToolbarTrait {
    private var mAccountNameText: EditText? = null
    private var mAccountStartSumText: EditText? = null
    private var mAccountDescText: EditText? = null
    private var mAccountCurrency: Spinner? = null
    private var mCustomCurrency: EditText? = null
    private var mProjectionController: ProjectionDateController? = null
    private var mRowId: Long = 0
    private var customCurrencyIdx = -1
    private var mOnRestore = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super<BaseActivity>.onCreate(savedInstanceState)

        setContentView(R.layout.account_creation)

        initToolbar(this)
        mRowId = if (savedInstanceState != null) {
            savedInstanceState.getSerializable(PARAM_ACCOUNT_ID) as Long
        } else {
            val extra = getIntent().getExtras()
            if (extra != null) {
                extra.getLong(PARAM_ACCOUNT_ID)
            } else {
                NO_ACCOUNT
            }
        }
        if (NO_ACCOUNT == mRowId) {
            setTitle(R.string.account_creation)
        } else {
            setTitle(R.string.account_edit_title)
        }

        mAccountNameText = findViewById(R.id.edit_account_name) as EditText
        mAccountDescText = findViewById(R.id.edit_account_desc) as EditText
        mAccountStartSumText = findViewById(R.id.edit_account_start_sum) as EditText
        val w = CorrectCommaWatcher(getSumSeparator(), mAccountStartSumText)
        w.setAutoNegate(false)
        mAccountStartSumText!!.addTextChangedListener(w)
        mAccountStartSumText!!.setOnFocusChangeListener(object : View.OnFocusChangeListener {
            override fun onFocusChange(v: View, hasFocus: Boolean) {
                if (hasFocus) {
                    (v as EditText).selectAll()
                }
            }
        })
        mAccountCurrency = findViewById(R.id.currency_spinner) as Spinner
        mCustomCurrency = findViewById(R.id.custom_currency) as EditText

        fillCurrencySpinner()
        mProjectionController = ProjectionDateController(this)
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        when (item.getItemId()) {
            R.id.confirm -> {
                onOkClicked()
                return true
            }
            else -> return super<BaseActivity>.onOptionsItemSelected(item)
        }
    }

    private fun onOkClicked() {
        val errMsg = StringBuilder()
        if (isFormValid(errMsg)) {
            setResult(Activity.RESULT_OK)
            saveState()
            finish()
            this.overridePendingTransition(R.anim.enter_from_right, 0)
        } else {
            Tools.popError(this, errMsg.toString(), null)
        }
    }

    private fun fillCurrencySpinner() {
        val mCurrAdapter = ArrayAdapter.createFromResource(this, R.array.all_currencies, android.R.layout.simple_spinner_item)
        mCurrAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        mAccountCurrency!!.setAdapter(mCurrAdapter)
        mAccountCurrency!!.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {

            override fun onItemSelected(arg0: AdapterView<*>, arg1: View, pos: Int, id: Long) {
                mCustomCurrency!!.setEnabled(pos == getCustomCurrencyIdx(this@AccountEditor))
            }

            override fun onNothingSelected(arg0: AdapterView<*>) {
            }

        })
    }

    private fun isFormValid(errMsg: StringBuilder): Boolean {
        val name = mAccountNameText!!.getText().toString()
        val startSumStr = mAccountStartSumText!!.getText().toString()
        var res = true
        if (name.length() == 0) {
            errMsg.append(R.string.empty_account_name)
            res = false
        }
        if (startSumStr.length() == 0) {
            mAccountStartSumText!!.setText("0")
        }
        // check if currency is correct
        if (mAccountCurrency!!.getSelectedItemPosition() == getCustomCurrencyIdx(this)) {
            val currency = mCustomCurrency!!.getText().toString().trim().toUpperCase()
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
        if (mProjectionController!!.mProjectionDate.isEnabled()) {
            //                && mProjectionController.getDate().trim().length() == 0) {
            try {
                val format: SimpleDateFormat
                if (mProjectionController!!.getMode() == AccountTable.PROJECTION_DAY_OF_NEXT_MONTH) {
                    format = SimpleDateFormat("dd")
                } else {
                    format = SimpleDateFormat("dd/MM/yyyy")
                }
                val d = format.parse(mProjectionController!!.getDate().trim())
                mProjectionController!!.mProjectionDate.setText(format.format(d))
            } catch (e: ParseException) {
                e.printStackTrace()
                if (errMsg.length() > 0)
                    errMsg.append("\n")
                errMsg.append(getString(R.string.bad_format_for_date))
                res = false
            }

        }
        return res
    }

    private fun populateFields(account: Cursor) {
        mAccountNameText!!.setText(account.getString(account.getColumnIndexOrThrow(AccountTable.KEY_ACCOUNT_NAME)))
        mAccountDescText!!.setText(account.getString(account.getColumnIndexOrThrow(AccountTable.KEY_ACCOUNT_DESC)))
        mAccountStartSumText!!.setText((account.getLong(account.getColumnIndexOrThrow(AccountTable.KEY_ACCOUNT_START_SUM)).toDouble() / 100.0).formatSum())
        var currencyStr = account.getString(account.getColumnIndexOrThrow(AccountTable.KEY_ACCOUNT_CURRENCY))
        if (currencyStr.length() == 0) {
            currencyStr = Currency.getInstance(Locale.getDefault()).getCurrencyCode()
        }
        initCurrencySpinner(currencyStr)
        mProjectionController!!.populateFields(account)
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
        super<BaseActivity>.onSaveInstanceState(outState)
        outState.putString("name", mAccountNameText!!.getText().toString())
        outState.putString("startSum", mAccountStartSumText!!.getText().toString())
        outState.putInt("currency", mAccountCurrency!!.getSelectedItemPosition())
        val customCurIdx = getCustomCurrencyIdx(this)
        outState.putInt("customCurrencyIdx", customCurIdx)
        if (mAccountCurrency!!.getSelectedItemPosition() == customCurIdx) {
            outState.putString("customCurrency", mCustomCurrency!!.getText().toString())
        }
        outState.putString("desc", mAccountDescText!!.getText().toString())
        mProjectionController!!.onSaveInstanceState(outState)
        mOnRestore = true
    }

    override fun onRestoreInstanceState(state: Bundle) {
        super<BaseActivity>.onRestoreInstanceState(state)
        mAccountNameText!!.setText(state.getString("name"))
        mAccountStartSumText!!.setText(state.getString("startSum"))
        mAccountCurrency!!.setSelection(state.getInt("currency"))
        customCurrencyIdx = state.getInt("customCurrencyIdx")
        if (mAccountCurrency!!.getSelectedItemPosition() == getCustomCurrencyIdx(this)) {
            mCustomCurrency!!.setText(state.getString("customCurrency"))
            mCustomCurrency!!.setEnabled(true)
        } else {
            mCustomCurrency!!.setEnabled(false)
        }
        mAccountDescText!!.setText(state.getString("desc"))
        mProjectionController!!.onRestoreInstanceState(state)
        mOnRestore = true
    }

    private fun initCurrencySpinner(currencyStr: String) {
        val allCurrencies = getResources().getStringArray(R.array.all_currencies)
        val pos = Arrays.binarySearch(allCurrencies, currencyStr)
        if (pos >= 0) {
            mAccountCurrency!!.setSelection(pos)
            mCustomCurrency!!.setEnabled(false)
        } else {
            mAccountCurrency!!.setSelection(getCustomCurrencyIdx(this))
            mCustomCurrency!!.setEnabled(true)
        }
    }

    override fun onResume() {
        super<BaseActivity>.onResume()
        if (!mOnRestore && mRowId != NO_ACCOUNT) {
            getSupportLoaderManager().initLoader<Cursor>(GET_ACCOUNT, Bundle(), this)
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

    private fun saveState() {
        val name = mAccountNameText!!.getText().toString().trim()
        val desc = mAccountDescText!!.getText().toString().trim()
        try {
            val startSum = Tools.extractSumFromStr(mAccountStartSumText!!.getText().toString())
            var currency: String? = null
            if (mAccountCurrency!!.getSelectedItemPosition() == getCustomCurrencyIdx(this)) {
                currency = mCustomCurrency!!.getText().toString().trim().toUpperCase()
            } else {
                currency = mAccountCurrency!!.getSelectedItem().toString()
            }
            if (mRowId == NO_ACCOUNT) {
                AccountTable.createAccount(this, name, desc, startSum, currency, mProjectionController!!.getMode(), mProjectionController!!.getDate())
            } else {
                AccountTable.updateAccount(this, mRowId, name, desc, startSum, currency, mProjectionController)
            }
            //            AccountList.refreshDisplay(this);
        } catch (e: ParseException) {
            e.printStackTrace()
        }

    }

    override fun onCreateLoader(id: Int, args: Bundle): Loader<Cursor> {
        val loader = AccountTable.getAccountLoader(this, mRowId)
        return loader
    }

    override fun onLoadFinished(arg0: Loader<Cursor>, data: Cursor) {
        if (data.moveToFirst()) {
            AccountTable.initProjectionDate(data)
            populateFields(data)
        }
    }

    override fun onLoaderReset(arg0: Loader<Cursor>) {
    }

    companion object {
        public val NO_ACCOUNT: Long = 0
        public val ACCOUNT_EDITOR: Int = 1000
        public val PARAM_ACCOUNT_ID: String = "account_id"
        private val GET_ACCOUNT = 400

        public fun callMeForResult(context: ActionBarActivity, accountId: Long) {
            val intent = Intent(context, javaClass<AccountEditor>())
            intent.putExtra(PARAM_ACCOUNT_ID, accountId)
            context.startActivityForResult(intent, ACCOUNT_EDITOR)
        }
    }
}
