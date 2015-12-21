package fr.geobert.radis.ui

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import fr.geobert.radis.MainActivity
import fr.geobert.radis.R
import fr.geobert.radis.data.AccountConfig
import fr.geobert.radis.data.Operation
import fr.geobert.radis.db.DbContentProvider
import fr.geobert.radis.db.InfoTables
import fr.geobert.radis.db.OperationTable
import fr.geobert.radis.tools.CorrectCommaWatcher
import fr.geobert.radis.tools.DBPrefsManager
import fr.geobert.radis.tools.MyAutoCompleteTextView
import fr.geobert.radis.tools.QuickAddTextWatcher
import fr.geobert.radis.tools.TIME_ZONE
import fr.geobert.radis.tools.Tools
import fr.geobert.radis.tools.getGroupSeparator
import fr.geobert.radis.tools.getSumSeparator
import fr.geobert.radis.tools.showDatePickerFragment
import fr.geobert.radis.ui.adapter.InfoAdapter
import hirondelle.date4j.DateTime
import kotlin.properties.Delegates

public class QuickAddController(private val mActivity: MainActivity, container: View) {
    private val mQuickAddThirdParty: MyAutoCompleteTextView
    private val mQuickAddAmount: EditText
    private val mQuickAddButton: ImageButton
    private val mQuickAddTextWatcher: QuickAddTextWatcher
    private val mCorrectCommaWatcher: CorrectCommaWatcher
    private val mLayout: LinearLayout
    private var invert: Boolean by Delegates.notNull()

    private fun getCurAccountId() = mActivity.mAccountManager.getCurrentAccountId(mActivity)
    private fun getCurConfig() = mActivity.mAccountManager.mCurAccountConfig

    init {
        mLayout = container.findViewById(R.id.quick_add_layout) as LinearLayout
        mQuickAddThirdParty = container.findViewById(R.id.quickadd_third_party) as MyAutoCompleteTextView
        mQuickAddAmount = container.findViewById(R.id.quickadd_amount) as EditText
        mQuickAddButton = container.findViewById(R.id.quickadd_validate) as ImageButton
        mQuickAddThirdParty.nextFocusDownId = R.id.quickadd_amount
        mCorrectCommaWatcher = CorrectCommaWatcher(getSumSeparator(), getGroupSeparator(), mQuickAddAmount).setAutoNegate(true)

        mQuickAddTextWatcher = QuickAddTextWatcher(mQuickAddThirdParty, mQuickAddAmount, mQuickAddButton)

        setQuickAddButEnabled(mQuickAddButton, false)
        mQuickAddButton.post(object : Runnable {
            private fun adjustImageButton(btn: ImageButton) {
                val params = btn.layoutParams as LinearLayout.LayoutParams
                params.bottomMargin = 3
                params.height = mQuickAddThirdParty.measuredHeight
                btn.layoutParams = params
            }

            override fun run() {
                if (Build.VERSION.SDK_INT < 11) {
                    adjustImageButton(mQuickAddButton)
                }
            }
        })
    }

    //    public fun setAccount(accountId: Long) {
    //        mAccountId = accountId
    //        setEnabled(accountId != 0)
    //    }

    public fun initViewBehavior() {
        mQuickAddThirdParty.setAdapter<InfoAdapter>(InfoAdapter(mActivity, DbContentProvider.THIRD_PARTY_URI,
                InfoTables.KEY_THIRD_PARTY_NAME, true, mActivity.mAccountManager.mCurAccountConfig as AccountConfig))

        mQuickAddAmount.addTextChangedListener(mCorrectCommaWatcher)

        setupListeners()

        setQuickAddButEnabled(mQuickAddButton, false)
        mQuickAddThirdParty.addTextChangedListener(mQuickAddTextWatcher)
        mQuickAddAmount.addTextChangedListener(mQuickAddTextWatcher)
        mQuickAddAmount.setOnEditorActionListener(object : TextView.OnEditorActionListener {
            override fun onEditorAction(v: TextView?, actionId: Int, event: KeyEvent?): Boolean {
                try {
                    if (mQuickAddButton.isEnabled) {
                        if (invert) showDatePicker() else quickAddOp()
                    } else {
                        Tools.popError(mActivity, mActivity.getString(R.string.quickadd_fields_not_filled), null)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                return true
            }
        })
    }

    public fun setupListeners() {
        val addTodayAction = { v: View ->
            try {
                quickAddOp()
            } catch (e: Exception) {
                Tools.popError(mActivity, e.message ?: "internal error", null)
                e.printStackTrace()
            }
        }

        val askDateAction = { v: View ->
            showDatePicker()
        }
        val c = getCurConfig()
        val prefs = DBPrefsManager.getInstance(mActivity).getInt(ConfigFragment.KEY_QUICKADD_ACTION, ConfigFragment.DEFAULT_QUICKADD_LONG_PRESS_ACTION)

        invert = (if (c != null && c.overrideQuickAddAction) c.quickAddAction else prefs) == 1
        mQuickAddButton.setOnClickListener(if (invert) askDateAction else addTodayAction)

        mQuickAddButton.setOnLongClickListener(object : View.OnLongClickListener {
            override fun onLongClick(v: View): Boolean {
                if (invert)
                    addTodayAction(v)
                else
                    askDateAction(v)
                return true
            }
        })
    }


    private fun showDatePicker() {
        showDatePickerFragment(mActivity, { datePicker, date ->
            try {
                quickAddOp(date)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, DateTime.today(TIME_ZONE))
    }

    private fun quickAddOp() {
        quickAddOp(null)
    }

    private fun quickAddOp(date: DateTime?) {
        val op = Operation()
        if (date != null) {
            op.setDate(date.getMilliseconds(TIME_ZONE))
        }
        op.mThirdParty = mQuickAddThirdParty.text.toString()
        op.setSumStr(mQuickAddAmount.text.toString())
        Log.d("QuickAdd", "cur accountId = ${getCurAccountId()}")
        if (OperationTable.createOp(mActivity, op, getCurAccountId()) > -1) {
            val i = Intent()
            i.putExtra("operation", op)
            mActivity.updateDisplay(i)
        }

        mQuickAddAmount.setText("")
        mQuickAddThirdParty.setText("")
        val mgr = mActivity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        mgr.hideSoftInputFromWindow(mQuickAddAmount.windowToken, 0)
        mCorrectCommaWatcher.setAutoNegate(true)
        mQuickAddAmount.clearFocus()
        mQuickAddThirdParty.clearFocus()
    }

    public fun clearFocus() {
        mQuickAddThirdParty.clearFocus()
        val imm = mActivity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(mQuickAddThirdParty.windowToken, 0)
    }

    //    public fun getFocus() {
    //        mQuickAddThirdParty.requestFocus()
    //    }

    public fun setAutoNegate(autoNeg: Boolean) {
        mCorrectCommaWatcher.setAutoNegate(autoNeg)
    }

    //    public fun setEnabled(enabled: Boolean) {
    //        mQuickAddThirdParty.setEnabled(enabled)
    //        mQuickAddAmount.setEnabled(enabled)
    //    }

    public fun onSaveInstanceState(outState: Bundle) {
        outState.putCharSequence("third_party", mQuickAddThirdParty.text)
        outState.putCharSequence("amount", mQuickAddAmount.text)
    }

    public fun onRestoreInstanceState(state: Bundle) {
        mQuickAddThirdParty.setText(state.getCharSequence("third_party"))
        mQuickAddAmount.setText(state.getCharSequence("amount"))
    }

    public fun setVisibility(visibility: Int) {
        mLayout.visibility = visibility
    }

    //    public fun isVisible(): Boolean {
    //        return mLayout.getVisibility() == View.VISIBLE
    //    }

    companion object {
        public fun setQuickAddButEnabled(but: ImageButton, b: Boolean) {
            but.isEnabled = b
        }
    }
}
