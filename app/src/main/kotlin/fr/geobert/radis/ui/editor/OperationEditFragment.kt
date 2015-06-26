package fr.geobert.radis.ui.editor

import android.os.Build
import android.os.Bundle
import android.support.v4.app.Fragment
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.*
import fr.geobert.radis.R
import fr.geobert.radis.data.Account
import fr.geobert.radis.data.Operation
import fr.geobert.radis.data.ScheduledOperation
import fr.geobert.radis.db.DbContentProvider
import fr.geobert.radis.db.InfoTables
import fr.geobert.radis.tools.*
import fr.geobert.radis.ui.adapter.AccountAdapter
import fr.geobert.radis.ui.adapter.InfoAdapter
import java.text.ParseException
import kotlin.properties.Delegates

public class OperationEditFragment() : Fragment(), TextWatcher {
    private var edit_op_sum: EditText by Delegates.notNull()
    private var edit_op_third_party: MyAutoCompleteTextView by Delegates.notNull()
    private var edit_op_tag: MyAutoCompleteTextView by Delegates.notNull()
    private var edit_op_mode: MyAutoCompleteTextView by Delegates.notNull()
    private var edit_op_notes: EditText by Delegates.notNull()
    private var edit_op_date: DatePicker by Delegates.notNull()
    private var third_party_cont: LinearLayout by Delegates.notNull()
    private var transfert_cont: LinearLayout by Delegates.notNull()
    private var is_transfert: CheckBox by Delegates.notNull()
    private var is_checked: CheckBox by Delegates.notNull()
    private var edit_op_sign: ImageButton by Delegates.notNull()
    private var trans_src_account: Spinner by Delegates.notNull()
    private var trans_dst_account: Spinner by Delegates.notNull()
    private var mSumTextWatcher: CorrectCommaWatcher by Delegates.notNull()
    private var mActivity: CommonOpEditor by Delegates.notNull()
    private var mWasInvertByTransfert: Boolean = false
    private var isOkClicked: Boolean = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val l = inflater.inflate(R.layout.main_op_edit, container, false)
        edit_op_sum = l.findViewById(R.id.edit_op_sum) as EditText
        edit_op_third_party = l.findViewById(R.id.edit_op_third_party) as MyAutoCompleteTextView
        edit_op_tag = l.findViewById(R.id.edit_op_tag) as MyAutoCompleteTextView
        edit_op_mode = l.findViewById(R.id.edit_op_mode) as MyAutoCompleteTextView
        edit_op_notes = l.findViewById(R.id.edit_op_notes) as EditText
        edit_op_date = l.findViewById(R.id.edit_op_date) as DatePicker
        third_party_cont = l.findViewById(R.id.third_party_cont) as LinearLayout
        transfert_cont = l.findViewById(R.id.transfert_cont) as LinearLayout
        is_transfert = l.findViewById(R.id.is_transfert) as CheckBox
        is_checked = l.findViewById(R.id.is_checked) as CheckBox
        edit_op_sign = l.findViewById(R.id.edit_op_sign) as ImageButton
        trans_src_account = l.findViewById(R.id.trans_src_account) as Spinner
        trans_dst_account = l.findViewById(R.id.trans_dst_account) as Spinner
        mSumTextWatcher = CorrectCommaWatcher(getSumSeparator(), edit_op_sum, this)
        return l
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super<Fragment>.onActivityCreated(savedInstanceState)
        mActivity = getActivity() as CommonOpEditor
        edit_op_third_party.setNextFocusDownId(R.id.edit_op_sum)
        edit_op_sum.setNextFocusDownId(R.id.edit_op_tag)
        edit_op_tag.setNextFocusDownId(R.id.edit_op_mode)
        edit_op_mode.setNextFocusDownId(R.id.edit_op_notes)

        third_party_cont.post {
            fun adjustImageButton(btn: ImageButton) {
                val params = btn.getLayoutParams() as LinearLayout.LayoutParams
                params.bottomMargin = 3
                params.height = third_party_cont.getMeasuredHeight()
                btn.setLayoutParams(params)
            }

            val params = third_party_cont.getLayoutParams() as LinearLayout.LayoutParams
            params.height = third_party_cont.getMeasuredHeight()
            transfert_cont.setLayoutParams(params)
            if (Build.VERSION.SDK_INT < 11) {
                adjustImageButton(mActivity.findViewById(R.id.edit_op_third_parties_list) as ImageButton)
                adjustImageButton(mActivity.findViewById(R.id.edit_op_tags_list) as ImageButton)
                adjustImageButton(mActivity.findViewById(R.id.edit_op_modes_list) as ImageButton)
                adjustImageButton(mActivity.findViewById(R.id.edit_op_sign) as ImageButton)
            }
        }
    }

    fun onAllAccountFetched() {
        if (isResumed()) {
            mSumTextWatcher.setAutoNegate(edit_op_sum.getText().toString().trim().length() == 0)
            populateTransfertSpinner(mActivity.mAccountManager.mAccountAdapter)
            initViewAdapters()
            initListeners()
            is_transfert.setOnCheckedChangeListener { arg0: CompoundButton, arg1: Boolean ->
                onTransfertCheckedChanged(arg1)
            }

            configThirdPartyTransfertCont(is_transfert.isChecked())
            edit_op_third_party.clearFocus()
            edit_op_sum.clearFocus()
            edit_op_mode.clearFocus()
            edit_op_tag.clearFocus()
            edit_op_notes.clearFocus()

            if (mActivity.getCurrentFocus() != null) {
                Tools.hideKeyboard(mActivity)
            }
        }
    }

    protected fun initListeners() {
        edit_op_sum.addTextChangedListener(mSumTextWatcher)
        mActivity.findViewById(R.id.edit_op_third_parties_list).setOnClickListener {
            synchronized (this@OperationEditFragment) {
                val d = InfoManagerDialog.createThirdPartiesListDialog(mActivity)
                if (!d.isAdded()) {
                    d.show(getFragmentManager(), "thirdPartiesDialog")
                }
            }
        }


        mActivity.findViewById(R.id.edit_op_tags_list).setOnClickListener {
            synchronized (this@OperationEditFragment) {
                val d = InfoManagerDialog.createTagsListDialog(mActivity)
                if (!d.isAdded()) {
                    d.show(getFragmentManager(), "tagsDialog")
                }
            }
        }


        mActivity.findViewById(R.id.edit_op_modes_list).setOnClickListener {
            synchronized (this@OperationEditFragment) {
                val d = InfoManagerDialog.createModesListDialog(mActivity)
                if (!d.isAdded()) {
                    d.show(getFragmentManager(), "modesDialog")
                }
            }
        }

        edit_op_sign.setOnClickListener {
            try {
                invertSign()
            } catch (e: ParseException) {
                // nothing to do
            }

        }

    }

    protected fun onTransfertCheckedChanged(isChecked: Boolean) {
        val `in` = AnimationUtils.loadAnimation(mActivity, android.R.anim.fade_in)
        val out = AnimationUtils.makeOutAnimation(mActivity, true)
        mSumTextWatcher.setAllowNegativeSum(!isChecked)
        edit_op_sign.setEnabled(!isChecked)
        val sum = edit_op_sum.getText().toString().extractSumFromStr()
        try {
            if (sum < 0) {
                invertSign()
                this.mWasInvertByTransfert = true
            } else {
                if (mWasInvertByTransfert) {
                    invertSign()
                }
            }
        } catch (e: ParseException) {
            e.printStackTrace()
        }


        if (isChecked == true) {
            transfert_cont.startAnimation(`in`)
            third_party_cont.startAnimation(out)
            transfert_cont.setVisibility(View.VISIBLE)
            third_party_cont.setVisibility(View.GONE)
        } else {
            transfert_cont.startAnimation(out)
            third_party_cont.startAnimation(`in`)
            transfert_cont.setVisibility(View.GONE)
            third_party_cont.setVisibility(View.VISIBLE)
        }
    }

    private fun configThirdPartyTransfertCont(isChecked: Boolean) {
        // HACK, ui bug where neither Transfert nor ThirdParty is draw
        transfert_cont.post {
            onTransfertCheckedChanged(isChecked)
        }
    }

    fun isTransfertChecked(): Boolean {
        return is_transfert.isChecked()
    }

    fun getSrcAccountIdx(): Int {
        return trans_src_account.getSelectedItemPosition()
    }

    protected fun initViewAdapters() {
        val account = mActivity.mAccountManager.mCurAccountConfig
        if (account != null) {
            edit_op_third_party.setAdapter<InfoAdapter>(InfoAdapter(mActivity, DbContentProvider.THIRD_PARTY_URI, InfoTables.KEY_THIRD_PARTY_NAME, false, account))
            edit_op_mode.setAdapter<InfoAdapter>(InfoAdapter(mActivity, DbContentProvider.MODES_URI, InfoTables.KEY_MODE_NAME, false, account))
            edit_op_tag.setAdapter<InfoAdapter>(InfoAdapter(mActivity, DbContentProvider.TAGS_URI, InfoTables.KEY_TAG_NAME, false, account))
        }
    }

    protected fun populateTransfertSpinner(c: AccountAdapter?) {
        if (c != null && c.getCount() > 0) {
            val adapter = ArrayAdapter<Account>(mActivity, android.R.layout.simple_spinner_item)
            val adapter2 = ArrayAdapter<Account>(mActivity, android.R.layout.simple_spinner_item)
            adapter.add(Account(0, getString(R.string.no_transfert)))
            adapter2.add(Account(0, getString(R.string.no_transfert)))
            c.forEach {
                adapter.add(it)
                adapter2.add(it)
            }

            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            trans_src_account.setAdapter(adapter)
            trans_dst_account.setAdapter(adapter2)
            val curOp = mActivity.mCurrentOp
            if (curOp != null) {
                val isTransfert = curOp.mTransferAccountId > 0
                is_transfert.setChecked(isTransfert)
                if (isTransfert) {
                    initAccountSpinner(trans_src_account, curOp.mAccountId)
                    initAccountSpinner(trans_dst_account, curOp.mTransferAccountId)

                } else {
                    if (mActivity.mCurAccountId != 0L) {
                        initAccountSpinner(trans_src_account, mActivity.mCurAccountId)
                    }
                }
            }
        }
    }

    private fun initAccountSpinner(spin: Spinner, accountId: Long) {
        val adapter = spin.getAdapter() as ArrayAdapter<Account> //TODO how to remove this warning
        val pos = adapter.getPosition(Account(accountId, ""))
        if (pos > -1) {
            spin.setSelection(pos)
        }
    }

    fun populateCommonFields(op: Operation) {
        Tools.setTextWithoutComplete(edit_op_third_party, op.mThirdParty)
        Tools.setTextWithoutComplete(edit_op_mode, op.mMode)
        Tools.setTextWithoutComplete(edit_op_tag, op.mTag)
        edit_op_date.updateDate(op.getYear(), op.getMonth() - 1, op.getDay())
        mActivity.mPreviousSum = op.mSum
        edit_op_notes.setText(op.mNotes)
        Tools.setSumTextGravity(edit_op_sum)
        mSumTextWatcher.setAutoNegate(false)
        val curOp = mActivity.mCurrentOp
        if (curOp != null) {
            if (curOp.mSum == 0L) {
                edit_op_sum.setText("")
                mSumTextWatcher.setAutoNegate(true)
            } else {
                edit_op_sum.setText(curOp.getSumStr())
            }
        }
        is_checked.setChecked(op.mIsChecked)
        populateTransfertSpinner((getActivity() as CommonOpEditor).mAccountManager.mAccountAdapter)
    }

    throws(ParseException::class)
    private fun invertSign() {
        mSumTextWatcher.setAutoNegate(false)
        val sum: Double? = edit_op_sum.getText().toString().parseSum()
        val s = if (sum != null) {
            -sum
        } else {
            sum
        }
        edit_op_sum.setText(s?.formatSum())
    }

    fun isFormValid(errMsg: StringBuilder): Boolean {
        var res = true
        val str: String
        if (is_transfert.isChecked()) {
            val srcAccount = trans_src_account.getSelectedItem() as Account
            val dstAccount = trans_dst_account.getSelectedItem() as Account
            if (srcAccount.id == 0L) {
                errMsg.append(getString(R.string.err_transfert_no_src))
                res = false
            } else if (dstAccount.id == 0L) {
                errMsg.append(getString(R.string.err_transfert_no_dst))
                res = false
            } else if (srcAccount.id > 0 && dstAccount.id > 0 && srcAccount.id == dstAccount.id) {
                errMsg.append(getString(R.string.err_transfert_same_acc))
                res = false
            }
        } else {
            str = edit_op_third_party.getText().toString().trim()
            if (str.length() == 0) {
                if (errMsg.length() > 0) {
                    errMsg.append("\n")
                }
                errMsg.append(getString(R.string.empty_third_party))
                res = false
            }
        }
        str = edit_op_sum.getText().toString().replace('+', ' ').trim()
        if (str.length() == 0) {
            if (errMsg.length() > 0) {
                errMsg.append("\n")
            }
            errMsg.append(getString(R.string.empty_amount))
            res = false
        } else {
            try {
                str.parseSum()
            } catch (e: ParseException) {
                if (errMsg.length() > 0) {
                    errMsg.append("\n")
                }
                errMsg.append(getString(R.string.invalid_amount))
                res = false
            }

        }

        return res
    }

    fun fillOperationWithInputs(op: Operation) {
        op.mMode = edit_op_mode.getText().toString().trim()
        op.mTag = edit_op_tag.getText().toString().trim()
        op.mNotes = edit_op_notes.getText().toString().trim()
        mActivity.getCurrentFocus()?.clearFocus()
        op.setSumStr(edit_op_sum.getText().toString())
        op.setDay(edit_op_date.getDayOfMonth())
        op.setMonth(edit_op_date.getMonth() + 1)
        op.setYear(edit_op_date.getYear())
        op.mIsChecked = is_checked.isChecked()

        if (is_transfert.isChecked()) {
            val srcAccount = trans_src_account.getSelectedItem() as Account
            val dstAccount = trans_dst_account.getSelectedItem() as Account
            if (srcAccount.id > 0 && dstAccount.id > 0 && srcAccount.id != dstAccount.id) {
                // a valid transfert has been setup
                op.mTransferAccountId = dstAccount.id
                op.mAccountId = srcAccount.id
                op.mThirdParty = dstAccount.name.trim()
                op.mTransSrcAccName = srcAccount.name
                // invert sum because with sum > 0 (and I forced it), A->B means -sum in A and +sum in B
                op.mSum = -op.mSum
            } else {
                op.mThirdParty = edit_op_third_party.getText().toString().trim()
            }
        } else {
            op.mTransferAccountId = 0
            op.mTransSrcAccName = ""
            op.mThirdParty = edit_op_third_party.getText().toString().trim()
        }
    }

    override fun onPause() {
        super<Fragment>.onPause()
        if (mActivity.mCurrentOp == null) {
            if (mActivity is OperationEditor) {
                mActivity.mCurrentOp = Operation()
            } else if (mActivity is ScheduledOperationEditor) {
                mActivity.mCurrentOp = ScheduledOperation(mActivity.mCurAccountId)
            }
        }
        if (!isOkClicked)
            fillOperationWithInputs(mActivity.mCurrentOp as Operation)
    }

    public fun setCheckedEditVisibility(visibility: Int) {
        //        mActivity.findViewById(R.id.checked_title).setVisibility(visibility);
        is_checked.setVisibility(visibility)
    }

    override fun beforeTextChanged(charSequence: CharSequence, i: Int, i2: Int, i3: Int) {
        // nothing
    }

    override fun onTextChanged(charSequence: CharSequence, i: Int, i2: Int, i3: Int) {
        // nothing
    }

    override fun afterTextChanged(editable: Editable) {
        mWasInvertByTransfert = false
    }
}
