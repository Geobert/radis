package fr.geobert.radis.ui.editor

import android.database.Cursor
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.widget.SimpleCursorAdapter
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import fr.geobert.radis.R
import fr.geobert.radis.data.Account
import fr.geobert.radis.data.Operation
import fr.geobert.radis.data.ScheduledOperation
import fr.geobert.radis.db.AccountTable
import fr.geobert.radis.ui.adapter.AccountAdapter
import kotlin.properties.Delegates

public class ScheduleEditorFragment : Fragment(), OnTransfertCheckedChangeListener {
    private val mActivity by Delegates.lazy { getActivity() as ScheduledOperationEditor }
    var mCurrentSchOp: ScheduledOperation? = null
    private val mEndDatePicker by Delegates.lazy { mActivity.findViewById(R.id.edit_end_date) as DatePicker }
    private val mAccountSpinner  by Delegates.lazy { mActivity.findViewById(R.id.account_choice) as Spinner }
    private val mPeriodicitySpinner by Delegates.lazy { mActivity.findViewById(R.id.periodicity_choice) as Spinner }
    private val mCustomPeriodicityVal by Delegates.lazy { mActivity.findViewById(R.id.custom_periodicity_value) as EditText }
    private val mCustomPeriodicityUnit by Delegates.lazy { mActivity.findViewById(R.id.custom_periodicity_choice) as Spinner }
    private val mCustomPeriodicityCont by Delegates.lazy { mActivity.findViewById(R.id.custom_periodicity) }
    private val mEndDateCheck by Delegates.lazy { mActivity.findViewById(R.id.end_date_check) as CheckBox }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.scheduling_edit, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super<Fragment>.onViewCreated(view, savedInstanceState)
        initViewBehavior()
        // done by ScheduledOperationEditor
        //populateFields();
    }

    public fun populateFields() {
        val op = mCurrentSchOp
        mActivity.mCurrentOp = op as Operation
        mCustomPeriodicityVal.setText(if (mCurrentSchOp?.mPeriodicity == 0) ""
        else mCurrentSchOp?.mPeriodicity.toString())
        populatePeriodicitySpinner()
        populateCustomPeriodicitySpinner()
        populateAccountSpinner((getActivity() as CommonOpEditor).mAccountManager.mAccountAdapter)
        if (op.getEndDate() > 0) {
            mEndDateCheck.setChecked(true)
            mEndDatePicker.setEnabled(true)
            mEndDatePicker.updateDate(op.getEndYear(), op.getEndMonth() - 1, op.getEndDay())
        } else {
            mEndDateCheck.setChecked(false)
            mEndDatePicker.setEnabled(false)
        }
    }

    override fun onResume() {
        super<Fragment>.onResume()
        mActivity.mAccountManager.fetchAllAccounts(false, {
            mActivity.onAllAccountsFetched()
            mActivity.getOpThenPopulate { op ->
                mCurrentSchOp = op as ScheduledOperation
                populateFields()
            }
        })
    }

    private fun initViewBehavior() {
        mCustomPeriodicityVal.setOnFocusChangeListener(object : View.OnFocusChangeListener {
            override fun onFocusChange(v: View, hasFocus: Boolean) {
                if (hasFocus) {
                    (v as EditText).selectAll()
                }
            }
        })

        mEndDatePicker.setEnabled(false)

        mEndDateCheck.setOnCheckedChangeListener(object : CompoundButton.OnCheckedChangeListener {
            override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
                mEndDatePicker.setEnabled(isChecked)
                mEndDatePicker.clearFocus()
            }
        })
        mEndDateCheck.setChecked(false)
    }

    private fun populateCustomPeriodicitySpinner() {
        val adapter = ArrayAdapter.createFromResource(mActivity, R.array.periodicity_custom_choices, android.R.layout.simple_spinner_item)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        mCustomPeriodicityUnit.setAdapter(adapter)

        val schOp = mCurrentSchOp
        if (schOp != null) {
            val unit = schOp.mPeriodicityUnit
            if (unit >= ScheduledOperation.CUSTOM_DAILY_PERIOD) {
                mCustomPeriodicityUnit.setSelection(unit - ScheduledOperation.CUSTOM_DAILY_PERIOD)
            }
        }
    }

    private fun setCustomPeriodicityVisibility(isVisible: Boolean) {
        mCustomPeriodicityCont!!.setVisibility(if (isVisible)
            View.VISIBLE
        else
            View.GONE)
    }

    override fun onTransfertCheckedChanged(isChecked: Boolean) {
        mAccountSpinner.setEnabled(!isChecked)
        val acc0 = mAccountSpinner.getItemAtPosition(0) as Account
        if (isChecked) {
            acc0.name = getString(R.string.defined_by_transfert)
            mAccountSpinner.setSelection(0)
        } else {
            acc0.name = getString(R.string.choose_account)
        }
    }

    private fun populatePeriodicitySpinner() {
        val adapter = ArrayAdapter.createFromResource(mActivity, R.array.periodicity_choices,
                android.R.layout.simple_spinner_item)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        mPeriodicitySpinner.setAdapter(adapter)
        mPeriodicitySpinner.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, pos: Int, id: Long) {
                setCustomPeriodicityVisibility(pos == (parent.getAdapter().getCount() - 1))
            }

            override fun onNothingSelected(arg0: AdapterView<*>) {
            }
        })
        val schOp = mCurrentSchOp
        if (schOp != null) {
            val unit = schOp.mPeriodicityUnit
            if (unit < ScheduledOperation.CUSTOM_DAILY_PERIOD) {
                mPeriodicitySpinner.setSelection(unit)
            } else {
                mPeriodicitySpinner.setSelection(ScheduledOperation.CUSTOM_DAILY_PERIOD)
            }
        }
    }

    private fun populateAccountSpinner(c: AccountAdapter?) {
        if (c != null && c.getCount() > 0) {
            val adapter = ArrayAdapter(mActivity, android.R.layout.simple_spinner_item, android.R.id.text1, c.toArrayList())
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            mAccountSpinner.setAdapter(adapter)

            val schOp = mCurrentSchOp
            if (schOp != null && (schOp.mAccountId != 0L || mActivity.mCurAccountId != 0L)) {
                var pos = 1
                while (pos < adapter.getCount()) {
                    val id = adapter.getItemId(pos)
                    if (id == schOp.mAccountId || id == mActivity.mCurAccountId) {
                        mAccountSpinner.setSelection(pos)
                        break
                    } else {
                        pos++
                    }
                }
            }
            val isTransfert = mActivity.isTransfertChecked()
            mAccountSpinner.setEnabled(!isTransfert)
            if (isTransfert) {
                mAccountSpinner.setSelection(mActivity.getSrcAccountSpinnerIdx())
            }
        }
    }

    fun fillOperationWithInputs(operation: Operation) {
        val op = operation as ScheduledOperation
        if (!mActivity.isTransfertChecked()) {
            //val c = mAccountSpinner.getSelectedItem() as Cursor
            val o = mAccountSpinner.getSelectedItem()
            if (o != null) {
                val c = o as Account
                op.mAccountId = c.id
            }
        }
        Log.d("ScheduleEditorFragment", "selected accountId = ${op.mAccountId}")
        val isCustom = mPeriodicitySpinner.getSelectedItemPosition() == (mPeriodicitySpinner.getAdapter()?.getCount() ?: 0 - 1)
        if (!isCustom) {
            op.mPeriodicity = 1
            op.mPeriodicityUnit = mPeriodicitySpinner.getSelectedItemPosition()
        } else {
            val periodicity = mCustomPeriodicityVal.getText().toString()
            try {
                op.mPeriodicity = Integer.parseInt(periodicity)
            } catch (e: NumberFormatException) {
                val b = StringBuffer()
                for (c in periodicity.toCharArray()) {
                    if ("0123456789".contains(c.toString())) {
                        b.append(c)
                    }
                }
                try {
                    op.mPeriodicity = Integer.parseInt(b.toString())
                    mCustomPeriodicityVal.setText(b)
                } catch (e2: NumberFormatException) {
                    op.mPeriodicity = 0
                }

            }

            op.mPeriodicityUnit = mCustomPeriodicityUnit.getSelectedItemPosition() + ScheduledOperation.CUSTOM_DAILY_PERIOD
        }
        if (mEndDatePicker.isEnabled()) {
            val dp = mEndDatePicker
            dp.clearChildFocus(mActivity.getCurrentFocus())
            op.setEndDay(dp.getDayOfMonth())
            op.setEndMonth(dp.getMonth() + 1)
            op.setEndYear(dp.getYear())
        } else {
            op.clearEndDate()
        }
    }

    fun isFormValid(errMsg: StringBuilder): Boolean {
        val op = mCurrentSchOp
        var res = true
        if (mPeriodicitySpinner.getSelectedItemPosition() == (mPeriodicitySpinner.getAdapter().getCount() - 1)) {
            try {
                Integer.parseInt(mCustomPeriodicityVal.getText().toString())
            } catch (e: NumberFormatException) {
                if (errMsg.length() > 0) {
                    errMsg.append("\n")
                }
                errMsg.append(getString(R.string.periodicity_must_be_num))
                res = false
            }

        }
        if (res && op != null) {
            fillOperationWithInputs(op)
            val hasEnd = op.getEndDate() > 0
            if (hasEnd && (op.getDate() > op.getEndDate())) {
                if (errMsg.length() > 0) {
                    errMsg.append("\n")
                }
                errMsg.append(getString(R.string.end_date_incorrect))
                res = false
            }
            if ((op.mPeriodicityUnit >= ScheduledOperation.CUSTOM_DAILY_PERIOD) && op.mPeriodicity <= 0) {
                if (errMsg.length() > 0) {
                    errMsg.append("\n")
                }
                errMsg.append(getString(R.string.periodicity_must_be_greater_0))
                res = false
            }
        }
        if (!mActivity.isTransfertChecked()) {
            val account = mAccountSpinner.getSelectedItem() as Account
            val accountId = account.id
            if (accountId == 0L) {
                if (errMsg.length() > 0) {
                    errMsg.append("\n")
                }
                errMsg.append(getString(R.string.sch_no_choosen_account))
                res = false
            }
        }
        return res
    }

    override fun onPause() {
        super<Fragment>.onPause()
        fillOperationWithInputs(mActivity.mCurrentOp!!)
    }
}
