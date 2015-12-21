package fr.geobert.radis.ui.editor

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.Spinner
import fr.geobert.radis.R
import fr.geobert.radis.data.Operation
import fr.geobert.radis.data.ScheduledOperation
import fr.geobert.radis.tools.TIME_ZONE
import fr.geobert.radis.tools.showDatePickerFragment
import hirondelle.date4j.DateTime
import kotlin.properties.Delegates

public class ScheduleEditorFragment : OperationEditFragment() {
    private val mActivity by lazy { activity as ScheduledOperationEditor }

    private var mPeriodicitySpinner: Spinner by Delegates.notNull()
    private var mCustomPeriodicityVal: EditText by Delegates.notNull()
    private var mCustomPeriodicityUnit: Spinner by Delegates.notNull()
    private var mCustomPeriodicityCont: View by Delegates.notNull()
    private var mEndDateCheck: CheckBox by Delegates.notNull()
    private var mEndDateButton: Button by Delegates.notNull()
    private var mEndDate: DateTime = DateTime.today(TIME_ZONE)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val l = super.onCreateView(inflater, container, savedInstanceState)
        mPeriodicitySpinner = l.findViewById(R.id.periodicity_choice) as Spinner
        mCustomPeriodicityVal = l.findViewById(R.id.custom_periodicity_value) as EditText
        mCustomPeriodicityUnit = l.findViewById(R.id.custom_periodicity_choice) as Spinner
        mCustomPeriodicityCont = l.findViewById(R.id.custom_periodicity)
        mEndDateCheck = l.findViewById(R.id.end_date_check) as CheckBox
        mEndDateButton = l.findViewById(R.id.sch_op_date_btn) as Button
        mEndDateButton.setOnClickListener {
            showDatePickerFragment(mActivity, { picker, date ->
                mEndDate = date
                updateDateBtn(mEndDateButton, mEndDate)
            }, opDate, opDate)
        }

        l.findViewById(R.id.sched_op_section).visibility = View.VISIBLE
        return l
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViewBehavior()
        // done by ScheduledOperationEditor
        //populateFields();
    }

    fun populateFields(op: ScheduledOperation) {
        populateCommonFields(op)
        mActivity.mCurrentOp = op
        mCustomPeriodicityVal.setText(if (op.mPeriodicity == 0) "" else op.mPeriodicity.toString())
        populatePeriodicitySpinner()
        populateCustomPeriodicitySpinner()
        if (op.getEndDate() > 0) {
            mEndDateCheck.isChecked = true
            mEndDateButton.isEnabled = true
            mEndDate = op.mEndDate
        } else {
            mEndDateCheck.isChecked = false
            mEndDateButton.isEnabled = false
        }
        updateDateBtn(mEndDateButton, mEndDate)
    }

    override fun onResume() {
        super.onResume()
        mActivity.mAccountManager.fetchAllAccounts(false, {
            mActivity.onAllAccountsFetched()
            mActivity.getOpThenPopulate { op ->
                populateFields(op as ScheduledOperation)
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

        mEndDateButton.isEnabled = false

        mEndDateCheck.setOnCheckedChangeListener(object : CompoundButton.OnCheckedChangeListener {
            override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
                mEndDateButton.isEnabled = isChecked
            }
        })
        mEndDateCheck.isChecked = false


    }

    private fun populateCustomPeriodicitySpinner() {
        val adapter = ArrayAdapter.createFromResource(mActivity, R.array.periodicity_custom_choices,
                android.R.layout.simple_spinner_item)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        mCustomPeriodicityUnit.adapter = adapter

        val schOp = mActivity.mCurrentSchOp
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
        val schOp = mActivity.mCurrentSchOp
        if (schOp != null) {
            val unit = schOp.mPeriodicityUnit
            if (unit < ScheduledOperation.CUSTOM_DAILY_PERIOD) {
                mPeriodicitySpinner.setSelection(unit)
            } else {
                mPeriodicitySpinner.setSelection(ScheduledOperation.CUSTOM_DAILY_PERIOD)
            }
        }
    }

    override fun fillOperationWithInputs(op: Operation) {
        super.fillOperationWithInputs(op)
        val o = op as ScheduledOperation
        Log.d("ScheduleEditorFragment", "selected accountId = ${o.mAccountId}")
        val isCustom = mPeriodicitySpinner.selectedItemPosition == (mPeriodicitySpinner.adapter?.count ?: 0 - 1)
        if (!isCustom) {
            o.mPeriodicity = 1
            o.mPeriodicityUnit = mPeriodicitySpinner.selectedItemPosition
        } else {
            val periodicity = mCustomPeriodicityVal.text.toString()
            try {
                o.mPeriodicity = Integer.parseInt(periodicity)
            } catch (e: NumberFormatException) {
                val b = StringBuffer()
                for (c in periodicity.toCharArray()) {
                    if ("0123456789".contains(c.toString())) {
                        b.append(c)
                    }
                }
                try {
                    o.mPeriodicity = Integer.parseInt(b.toString())
                    mCustomPeriodicityVal.setText(b)
                } catch (e2: NumberFormatException) {
                    o.mPeriodicity = 0
                }

            }

            o.mPeriodicityUnit = mCustomPeriodicityUnit.selectedItemPosition + ScheduledOperation.CUSTOM_DAILY_PERIOD
        }
        if (mEndDateButton.isEnabled) {
            o.setEndDay(mEndDate.day)
            o.setEndMonth(mEndDate.month)
            o.setEndYear(mEndDate.year)
        } else {
            o.clearEndDate()
        }
    }

    override fun isFormValid(errMsg: StringBuilder): Boolean {
        val op = mActivity.mCurrentSchOp
        var res = true
        if (mPeriodicitySpinner.selectedItemPosition == (mPeriodicitySpinner.adapter.count - 1)) {
            try {
                Integer.parseInt(mCustomPeriodicityVal.text.toString())
            } catch (e: NumberFormatException) {
                if (errMsg.length > 0) {
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
                if (errMsg.length > 0) {
                    errMsg.append("\n")
                }
                errMsg.append(getString(R.string.end_date_incorrect))
                res = false
            }
            if ((op.mPeriodicityUnit >= ScheduledOperation.CUSTOM_DAILY_PERIOD) && op.mPeriodicity <= 0) {
                if (errMsg.length > 0) {
                    errMsg.append("\n")
                }
                errMsg.append(getString(R.string.periodicity_must_be_greater_0))
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
