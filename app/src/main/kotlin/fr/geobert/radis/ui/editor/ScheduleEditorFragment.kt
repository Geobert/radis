package fr.geobert.radis.ui.editor

import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.DatePicker
import android.widget.EditText
import android.widget.Spinner
import fr.geobert.radis.R
import fr.geobert.radis.data.Account
import fr.geobert.radis.data.Operation
import fr.geobert.radis.data.ScheduledOperation
import fr.geobert.radis.ui.adapter.AccountAdapter
import kotlin.properties.Delegates

public class ScheduleEditorFragment : Fragment(), OnTransfertCheckedChangeListener {
    private val mActivity by lazy(LazyThreadSafetyMode.NONE) { activity as ScheduledOperationEditor }
    var mCurrentSchOp: ScheduledOperation? = null
    private var mEndDatePicker: DatePicker by Delegates.notNull()
    private var mAccountSpinner: Spinner by Delegates.notNull()
    private var mPeriodicitySpinner: Spinner by Delegates.notNull()
    private var mCustomPeriodicityVal: EditText by Delegates.notNull()
    private var mCustomPeriodicityUnit: Spinner by Delegates.notNull()
    private var mCustomPeriodicityCont: View by Delegates.notNull()
    private var mEndDateCheck: CheckBox by Delegates.notNull()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val l = inflater.inflate(R.layout.scheduling_edit, container, false)
        mEndDatePicker = l.findViewById(R.id.edit_end_date) as DatePicker
        mAccountSpinner = l.findViewById(R.id.account_choice) as Spinner
        mPeriodicitySpinner = l.findViewById(R.id.periodicity_choice) as Spinner
        mCustomPeriodicityVal = l.findViewById(R.id.custom_periodicity_value) as EditText
        mCustomPeriodicityUnit = l.findViewById(R.id.custom_periodicity_choice) as Spinner
        mCustomPeriodicityCont = l.findViewById(R.id.custom_periodicity)
        mEndDateCheck = l.findViewById(R.id.end_date_check) as CheckBox
        return l
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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
        populateAccountSpinner((activity as CommonOpEditor).mAccountManager.mAccountAdapter)
        if (op.getEndDate() > 0) {
            mEndDateCheck.isChecked = true
            mEndDatePicker.isEnabled = true
            mEndDatePicker.updateDate(op.getEndYear(), op.getEndMonth() - 1, op.getEndDay())
        } else {
            mEndDateCheck.isChecked = false
            mEndDatePicker.isEnabled = false
        }
    }

    override fun onResume() {
        super.onResume()
        mActivity.mAccountManager.fetchAllAccounts(false, {
            mActivity.onAllAccountsFetched()
            mActivity.getOpThenPopulate { op ->
                mCurrentSchOp = op as ScheduledOperation
                populateFields()
            }
        })
    }

    private fun initViewBehavior() {
        mCustomPeriodicityVal.onFocusChangeListener = object : View.OnFocusChangeListener {
            override fun onFocusChange(v: View, hasFocus: Boolean) {
                if (hasFocus) {
                    (v as EditText).selectAll()
                }
            }
        }

        mEndDatePicker.isEnabled = false

        mEndDateCheck.setOnCheckedChangeListener(object : CompoundButton.OnCheckedChangeListener {
            override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
                mEndDatePicker.isEnabled = isChecked
                mEndDatePicker.clearFocus()
            }
        })
        mEndDateCheck.isChecked = false


    }

    private fun populateCustomPeriodicitySpinner() {
        val adapter = ArrayAdapter.createFromResource(mActivity, R.array.periodicity_custom_choices, android.R.layout.simple_spinner_item)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        mCustomPeriodicityUnit.adapter = adapter

        val schOp = mCurrentSchOp
        if (schOp != null) {
            val unit = schOp.mPeriodicityUnit
            if (unit >= ScheduledOperation.CUSTOM_DAILY_PERIOD) {
                mCustomPeriodicityUnit.setSelection(unit - ScheduledOperation.CUSTOM_DAILY_PERIOD)
            }
        }
    }

    private fun setCustomPeriodicityVisibility(isVisible: Boolean) {
        mCustomPeriodicityCont.visibility = if (isVisible)
            View.VISIBLE
        else
            View.GONE
    }

    override fun onTransfertCheckedChanged(isChecked: Boolean) {
        mAccountSpinner.isEnabled = !isChecked
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
        mPeriodicitySpinner.adapter = adapter
        mPeriodicitySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, pos: Int, id: Long) {
                setCustomPeriodicityVisibility(pos == (parent.adapter.count - 1))
            }

            override fun onNothingSelected(arg0: AdapterView<*>) {
            }
        }
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
        if (c != null && c.count > 0) {
            val adapter = ArrayAdapter(mActivity, android.R.layout.simple_spinner_item, android.R.id.text1, c.toArrayList())
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            mAccountSpinner.adapter = adapter

            val schOp = mCurrentSchOp
            if (schOp != null && (schOp.mAccountId != 0L || mActivity.mCurAccountId != 0L)) {
                var pos = 0
                while (pos < adapter.count) {
                    val id = adapter.getItem(pos).id
                    if (id == schOp.mAccountId || id == mActivity.mCurAccountId) {
                        mAccountSpinner.setSelection(pos)
                        break
                    } else {
                        pos++
                    }
                }
            }
            val isTransfert = mActivity.isTransfertChecked
            mAccountSpinner.isEnabled = !isTransfert
            if (isTransfert) {
                mAccountSpinner.setSelection(mActivity.srcAccountSpinnerIdx)
            }
        }
    }

    fun fillOperationWithInputs(operation: Operation) {
        val op = operation as ScheduledOperation
        if (!mActivity.isTransfertChecked) {
            //val c = mAccountSpinner.getSelectedItem() as Cursor
            val o = mAccountSpinner.selectedItem
            if (o != null) {
                val c = o as Account
                op.mAccountId = c.id
            }
        }
        Log.d("ScheduleEditorFragment", "selected accountId = ${op.mAccountId}")
        val isCustom = mPeriodicitySpinner.selectedItemPosition == (mPeriodicitySpinner.adapter?.count ?: 0 - 1)
        if (!isCustom) {
            op.mPeriodicity = 1
            op.mPeriodicityUnit = mPeriodicitySpinner.selectedItemPosition
        } else {
            val periodicity = mCustomPeriodicityVal.text.toString()
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

            op.mPeriodicityUnit = mCustomPeriodicityUnit.selectedItemPosition + ScheduledOperation.CUSTOM_DAILY_PERIOD
        }
        if (mEndDatePicker.isEnabled) {
            val dp = mEndDatePicker
            dp.clearChildFocus(mActivity.currentFocus)
            op.setEndDay(dp.dayOfMonth)
            op.setEndMonth(dp.month + 1)
            op.setEndYear(dp.year)
        } else {
            op.clearEndDate()
        }
    }

    fun isFormValid(errMsg: StringBuilder): Boolean {
        val op = mCurrentSchOp
        var res = true
        if (mPeriodicitySpinner.selectedItemPosition == (mPeriodicitySpinner.adapter.count - 1)) {
            try {
                Integer.parseInt(mCustomPeriodicityVal.text.toString())
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
        if (!mActivity.isTransfertChecked) {
            val account = mAccountSpinner.selectedItem as Account
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
        super.onPause()
        fillOperationWithInputs(mActivity.mCurrentOp!!)
    }
}
