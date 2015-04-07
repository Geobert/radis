package fr.geobert.radis.ui

import android.app.DatePickerDialog
import android.content.Context
import android.content.DialogInterface
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import fr.geobert.radis.MainActivity
import fr.geobert.radis.R
import fr.geobert.radis.data.Account
import fr.geobert.radis.data.Operation
import fr.geobert.radis.db.DbContentProvider
import fr.geobert.radis.db.InfoTables
import fr.geobert.radis.db.OperationTable
import fr.geobert.radis.tools.*
import fr.geobert.radis.ui.adapter.InfoAdapter
import java.util.Calendar
import java.util.GregorianCalendar
import kotlin.platform.platformStatic

public class QuickAddController(private val mActivity: MainActivity, container: View) {
    private val mQuickAddThirdParty: MyAutoCompleteTextView
    private val mQuickAddAmount: EditText
    private val mQuickAddButton: ImageButton
    private val mQuickAddTextWatcher: QuickAddTextWatcher
    private val mCorrectCommaWatcher: CorrectCommaWatcher
    private val mLayout: LinearLayout

    private fun getCurAccountId() = mActivity.mAccountManager.getCurrentAccountId(mActivity)

    init {
        mLayout = container.findViewById(R.id.quick_add_layout) as LinearLayout
        mQuickAddThirdParty = container.findViewById(R.id.quickadd_third_party) as MyAutoCompleteTextView
        mQuickAddAmount = container.findViewById(R.id.quickadd_amount) as EditText
        mQuickAddButton = container.findViewById(R.id.quickadd_validate) as ImageButton
        mQuickAddThirdParty.setNextFocusDownId(R.id.quickadd_amount)
        mCorrectCommaWatcher = CorrectCommaWatcher(getSumSeparator(), mQuickAddAmount).setAutoNegate(true)

        mQuickAddTextWatcher = QuickAddTextWatcher(mQuickAddThirdParty, mQuickAddAmount, mQuickAddButton)

        setQuickAddButEnabled(mQuickAddButton, false)
        mQuickAddButton.post(object : Runnable {
            private fun adjustImageButton(btn: ImageButton) {
                val params = btn.getLayoutParams() as LinearLayout.LayoutParams
                params.bottomMargin = 3
                params.height = mQuickAddThirdParty.getMeasuredHeight()
                btn.setLayoutParams(params)
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
                InfoTables.KEY_THIRD_PARTY_NAME, true, mActivity.mAccountManager.getCurrentAccount() as Account))

        mQuickAddAmount.addTextChangedListener(mCorrectCommaWatcher)

        mQuickAddButton.setOnClickListener(object : View.OnClickListener {
            override fun onClick(view: View) {
                try {
                    quickAddOp()
                } catch (e: Exception) {
                    Tools.popError(mActivity, e.getMessage(), null)
                    e.printStackTrace()
                }

            }
        })
        mQuickAddButton.setOnLongClickListener(object : View.OnLongClickListener {
            override fun onLongClick(view: View): Boolean {
                showDatePicker()
                return true
            }
        })
        setQuickAddButEnabled(mQuickAddButton, false)
        mQuickAddThirdParty.addTextChangedListener(mQuickAddTextWatcher)
        mQuickAddAmount.addTextChangedListener(mQuickAddTextWatcher)
        mQuickAddAmount.setOnEditorActionListener(object : TextView.OnEditorActionListener {
            override fun onEditorAction(v: TextView, actionId: Int, event: KeyEvent): Boolean {
                try {
                    if (mQuickAddButton.isEnabled()) {
                        quickAddOp()
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

    private fun showDatePicker() {
        val today = GregorianCalendar()
        val dialog = DatePickerDialog(mActivity, object : DatePickerDialog.OnDateSetListener {
            private var alreadyFired = 0

            override fun onDateSet(datePicker: DatePicker, y: Int, m: Int, d: Int) {
                Log.d("QuickAdd", "date set : " + y + "/" + m + "/" + d + " ///" + alreadyFired % 2)
                // workaround known android bug
                if (alreadyFired % 2 == 0) {
                    val date = GregorianCalendar(y, m, d)
                    try {
                        quickAddOp(date)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                }
                alreadyFired++
            }
        }, today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH))
        dialog.setTitle(R.string.op_date)
        dialog.setButton(DialogInterface.BUTTON_NEGATIVE, mActivity.getString(android.R.string.cancel),
                null as DialogInterface.OnClickListener)
        dialog.show()
    }

    throws(javaClass<Exception>())
    private fun quickAddOp() {
        quickAddOp(null)
    }

    throws(javaClass<Exception>())
    private fun quickAddOp(date: GregorianCalendar?) {
        val op = Operation()
        if (date != null) {
            Tools.clearTimeOfCalendar(date)
            op.setDate(date.getTimeInMillis())
        }
        op.mThirdParty = mQuickAddThirdParty.getText().toString()
        op.setSumStr(mQuickAddAmount.getText().toString())
        Log.d("QuickAdd", "cur accountId = ${getCurAccountId()}")
        if (OperationTable.createOp(mActivity, op, getCurAccountId()) > -1) {
            mActivity.updateDisplay(null)
        }

        mQuickAddAmount.setText("")
        mQuickAddThirdParty.setText("")
        val mgr = mActivity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        mgr.hideSoftInputFromWindow(mQuickAddAmount.getWindowToken(), 0)
        mCorrectCommaWatcher.setAutoNegate(true)
        mQuickAddAmount.clearFocus()
        mQuickAddThirdParty.clearFocus()
    }

    public fun clearFocus() {
        mQuickAddThirdParty.clearFocus()
        val imm = mActivity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(mQuickAddThirdParty.getWindowToken(), 0)
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
        outState.putCharSequence("third_party", mQuickAddThirdParty.getText())
        outState.putCharSequence("amount", mQuickAddAmount.getText())
    }

    public fun onRestoreInstanceState(state: Bundle) {
        mQuickAddThirdParty.setText(state.getCharSequence("third_party"))
        mQuickAddAmount.setText(state.getCharSequence("amount"))
    }

    public fun setVisibility(visibility: Int) {
        mLayout.setVisibility(visibility)
    }

    //    public fun isVisible(): Boolean {
    //        return mLayout.getVisibility() == View.VISIBLE
    //    }

    companion object {
        platformStatic public fun setQuickAddButEnabled(but: ImageButton, b: Boolean) {
            but.setEnabled(b)
        }
    }
}
