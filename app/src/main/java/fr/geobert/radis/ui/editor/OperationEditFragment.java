package fr.geobert.radis.ui.editor;

import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import fr.geobert.radis.R;
import fr.geobert.radis.data.Account;
import fr.geobert.radis.data.DataPackage;
import fr.geobert.radis.data.Operation;
import fr.geobert.radis.db.DbContentProvider;
import fr.geobert.radis.db.InfoTables;
import fr.geobert.radis.tools.CorrectCommaWatcher;
import fr.geobert.radis.tools.InfoAdapter;
import fr.geobert.radis.tools.MyAutoCompleteTextView;
import fr.geobert.radis.tools.Tools;
import fr.geobert.radis.tools.ToolsPackage;

import java.text.ParseException;

public class OperationEditFragment extends Fragment implements TextWatcher {
    private MyAutoCompleteTextView mOpThirdPartyText;
    private MyAutoCompleteTextView mOpModeText;
    private EditText mOpSumText;
    private MyAutoCompleteTextView mOpTagText;
    private CorrectCommaWatcher mSumTextWatcher;
    private DatePicker mDatePicker;
    private Spinner mSrcAccount;
    private Spinner mDstAccount;
    private LinearLayout mTransfertCont;
    private LinearLayout mThirdPartyCont;
    private EditText mNotesText;
    private CheckBox mIsTransfertCheck;
    private CommonOpEditor mActivity;
    private CheckBox mIsChecked;
    private ImageButton mInvertSignBtn;
    private boolean mWasInvertByTransfert;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.main_op_edit, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mActivity = (CommonOpEditor) getActivity();
        mOpThirdPartyText = (MyAutoCompleteTextView) mActivity.findViewById(R.id.edit_op_third_party);
        mOpModeText = (MyAutoCompleteTextView) mActivity.findViewById(R.id.edit_op_mode);
        mOpSumText = (EditText) mActivity.findViewById(R.id.edit_op_sum);
        mOpTagText = (MyAutoCompleteTextView) mActivity.findViewById(R.id.edit_op_tag);
        mSumTextWatcher = new CorrectCommaWatcher(ToolsPackage.getSumSeparator(), mOpSumText, this);
        mDatePicker = (DatePicker) mActivity.findViewById(R.id.edit_op_date);
        mSrcAccount = (Spinner) mActivity.findViewById(R.id.trans_src_account);
        mDstAccount = (Spinner) mActivity.findViewById(R.id.trans_dst_account);
        mTransfertCont = (LinearLayout) mActivity.findViewById(R.id.transfert_cont);
        mThirdPartyCont = (LinearLayout) mActivity.findViewById(R.id.third_party_cont);
        mNotesText = (EditText) mActivity.findViewById(R.id.edit_op_notes);
        mInvertSignBtn = (ImageButton) mActivity.findViewById(R.id.edit_op_sign);
        mOpThirdPartyText.setNextFocusDownId(R.id.edit_op_sum);
        mOpSumText.setNextFocusDownId(R.id.edit_op_tag);
        mOpTagText.setNextFocusDownId(R.id.edit_op_mode);
        mOpModeText.setNextFocusDownId(R.id.edit_op_notes);

        mIsTransfertCheck = (CheckBox) mActivity.findViewById(R.id.is_transfert);
        mIsChecked = (CheckBox) mActivity.findViewById(R.id.is_checked);

        mThirdPartyCont.post(new Runnable() {
            private void adjustImageButton(ImageButton btn) {
                if (btn != null) {
                    LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) btn.getLayoutParams();
                    params.bottomMargin = 3;
                    params.height = mThirdPartyCont.getMeasuredHeight();
                    btn.setLayoutParams(params);
                }
            }

            @Override
            public void run() {
                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) mThirdPartyCont.getLayoutParams();
                params.height = mThirdPartyCont.getMeasuredHeight();
                mTransfertCont.setLayoutParams(params);
                if (Build.VERSION.SDK_INT < 11) {
                    ImageButton btn = (ImageButton) mActivity.findViewById(R.id.edit_op_third_parties_list);
                    adjustImageButton(btn);
                    btn = (ImageButton) mActivity.findViewById(R.id.edit_op_tags_list);
                    adjustImageButton(btn);
                    btn = (ImageButton) mActivity.findViewById(R.id.edit_op_modes_list);
                    adjustImageButton(btn);
                    btn = (ImageButton) mActivity.findViewById(R.id.edit_op_sign);
                    adjustImageButton(btn);
                }
            }
        });

    }

    @Override
    public void onResume() {
        super.onResume();
        mSumTextWatcher.setAutoNegate(mOpSumText.getText().toString().trim().length() == 0);
        if (mActivity.mCurrentOp != null) {
            populateTransfertSpinner(((CommonOpEditor) getActivity()).getAccountManager().getAllAccountsCursor());
        }
        initViewAdapters();
        initListeners();
        mIsTransfertCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton arg0,
                                         boolean arg1) {
                onTransfertCheckedChanged(arg1);
            }
        });
        configThirdPartyTransfertCont(mIsTransfertCheck.isChecked());
        mOpThirdPartyText.clearFocus();
        mOpSumText.clearFocus();
        mOpModeText.clearFocus();
        mOpTagText.clearFocus();
        mNotesText.clearFocus();

        if (mActivity.getCurrentFocus() != null) {
            Tools.hideKeyboard(mActivity);
        }
    }

    final protected void initListeners() {
        mOpSumText.addTextChangedListener(mSumTextWatcher);
        mActivity.findViewById(R.id.edit_op_third_parties_list).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        synchronized (OperationEditFragment.this) {
                            InfoManagerDialog d = InfoManagerDialog.createThirdPartiesListDialog(mActivity);
                            if (!d.isAdded()) {
                                d.show(getFragmentManager(), "thirdPartiesDialog");
                            }
                        }
                    }
                }
        );

        mActivity.findViewById(R.id.edit_op_tags_list).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        synchronized (OperationEditFragment.this) {
                            InfoManagerDialog d = InfoManagerDialog.createTagsListDialog(mActivity);
                            if (!d.isAdded()) {
                                d.show(getFragmentManager(), "tagsDialog");
                            }
                        }
                    }
                }
        );

        mActivity.findViewById(R.id.edit_op_modes_list).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        synchronized (OperationEditFragment.this) {
                            InfoManagerDialog d = InfoManagerDialog.createModesListDialog(mActivity);
                            if (!d.isAdded()) {
                                d.show(getFragmentManager(), "modesDialog");
                            }
                        }
                    }
                }
        );
        mInvertSignBtn.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            invertSign();
                        } catch (ParseException e) {
                            // nothing to do
                        }
                    }
                }
        );
    }

    final protected void onTransfertCheckedChanged(boolean isChecked) {
        Animation in = AnimationUtils.loadAnimation(mActivity,
                android.R.anim.fade_in);
        Animation out = AnimationUtils.makeOutAnimation(mActivity, true);
        mSumTextWatcher.setAllowNegativeSum(!isChecked);
        mInvertSignBtn.setEnabled(!isChecked);
        long sum = Tools.extractSumFromStr(mOpSumText.getText().toString());
        try {
            if (sum < 0) {
                invertSign();
                this.mWasInvertByTransfert = true;
            } else {
                if (mWasInvertByTransfert) {
                    invertSign();
                }
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        if (isChecked == true) {
            mTransfertCont.startAnimation(in);
            mThirdPartyCont.startAnimation(out);
            mTransfertCont.setVisibility(View.VISIBLE);
            mThirdPartyCont.setVisibility(View.GONE);
        } else {
            mTransfertCont.startAnimation(out);
            mThirdPartyCont.startAnimation(in);
            mTransfertCont.setVisibility(View.GONE);
            mThirdPartyCont.setVisibility(View.VISIBLE);
        }
    }

    private void configThirdPartyTransfertCont(final boolean isChecked) {
        // HACK, ui bug where neither Transfert nor ThirdParty is draw
        mTransfertCont.post(new Runnable() {
            @Override
            public void run() {
                onTransfertCheckedChanged(isChecked);
            }
        });
    }

    boolean isTransfertChecked() {
        return mIsTransfertCheck.isChecked();
    }

    int getSrcAccountIdx() {
        return mSrcAccount.getSelectedItemPosition();
    }

    protected void initViewAdapters() {
        mOpThirdPartyText.setAdapter(new InfoAdapter(mActivity, DbContentProvider.THIRD_PARTY_URI,
                InfoTables.KEY_THIRD_PARTY_NAME, false));
        mOpModeText.setAdapter(new InfoAdapter(mActivity, DbContentProvider.MODES_URI,
                InfoTables.KEY_MODE_NAME, false));
        mOpTagText.setAdapter(new InfoAdapter(mActivity, DbContentProvider.TAGS_URI,
                InfoTables.KEY_TAG_NAME, false));
    }

    final protected void populateTransfertSpinner(Cursor c) {
        if (c != null && c.moveToFirst()) {
            ArrayAdapter<Account> adapter = new ArrayAdapter<Account>(mActivity,
                    android.R.layout.simple_spinner_item);
            ArrayAdapter<Account> adapter2 = new ArrayAdapter<Account>(mActivity,
                    android.R.layout.simple_spinner_item);
            adapter.add(new Account(0, getString(R.string.no_transfert)));
            adapter2.add(new Account(0, getString(R.string.no_transfert)));
            do {
                adapter.add(DataPackage.Account(c));
                adapter2.add(DataPackage.Account(c));
            } while (c.moveToNext());

            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mSrcAccount.setAdapter(adapter);
            mDstAccount.setAdapter(adapter2);

            final boolean isTransfert = mActivity.mCurrentOp.getmTransferAccountId() > 0;
            mIsTransfertCheck.setChecked(isTransfert);
            if (isTransfert) {
                initAccountSpinner(mSrcAccount, mActivity.mCurrentOp.getmAccountId());
                initAccountSpinner(mDstAccount, mActivity.mCurrentOp.getmTransferAccountId());
            } else {
                if (mActivity.mCurAccountId != 0) {
                    initAccountSpinner(mSrcAccount, mActivity.mCurAccountId);
                }
            }
        }
    }

    private void initAccountSpinner(Spinner spin, long accountId) {
        ArrayAdapter<Account> adapter = (ArrayAdapter<Account>) spin.getAdapter();
        int pos = adapter.getPosition(new Account(accountId, ""));
        if (pos > -1) {
            spin.setSelection(pos);
        }
    }

    final protected void populateCommonFields(Operation op) {
        Tools.setTextWithoutComplete(mOpThirdPartyText, op.getmThirdParty());
        Tools.setTextWithoutComplete(mOpModeText, op.getmMode());
        Tools.setTextWithoutComplete(mOpTagText, op.getmTag());
        mDatePicker.updateDate(op.getYear(), op.getMonth(), op.getDay());
        mActivity.mPreviousSum = op.getmSum();
        mNotesText.setText(op.getmNotes());
        Tools.setSumTextGravity(mOpSumText);
        mSumTextWatcher.setAutoNegate(false);
        if (mActivity.mCurrentOp.getmSum() == 0.0) {
            mOpSumText.setText("");
            mSumTextWatcher.setAutoNegate(true);
        } else {
            mOpSumText.setText(mActivity.mCurrentOp.getSumStr());
        }
        mIsChecked.setChecked(op.getmIsChecked());
        populateTransfertSpinner(((CommonOpEditor) getActivity()).getAccountManager().getAllAccountsCursor());
    }

    private void invertSign() throws ParseException {
        mSumTextWatcher.setAutoNegate(false);
        Double sum = ToolsPackage.parseSum(mOpSumText.getText().toString());
        if (sum != null) {
            sum = -sum;
        }
        mOpSumText.setText(ToolsPackage.formatSum(sum));
    }

    protected boolean isFormValid(StringBuilder errMsg) {
        boolean res = true;
        String str;
        if (mIsTransfertCheck.isChecked()) {
            final Account srcAccount = (Account) mSrcAccount.getSelectedItem();
            final Account dstAccount = (Account) mDstAccount.getSelectedItem();
            if (srcAccount.getId() == 0) {
                errMsg.append(getString(R.string.err_transfert_no_src));
                res = false;
            } else if (dstAccount.getId() == 0) {
                errMsg.append(getString(R.string.err_transfert_no_dst));
                res = false;
            } else if (srcAccount.getId() > 0 && dstAccount.getId() > 0 &&
                    srcAccount.getId() == dstAccount.getId()) {
                errMsg.append(getString(R.string.err_transfert_same_acc));
                res = false;
            }
        } else {
            str = mOpThirdPartyText.getText().toString().trim();
            if (str.length() == 0) {
                if (errMsg.length() > 0) {
                    errMsg.append("\n");
                }
                errMsg.append(getString(R.string.empty_third_party));
                res = false;
            }
        }
        str = mOpSumText.getText().toString().replace('+', ' ').trim();
        if (str.length() == 0) {
            if (errMsg.length() > 0) {
                errMsg.append("\n");
            }
            errMsg.append(getString(R.string.empty_amount));
            res = false;
        } else {
            try {
                ToolsPackage.parseSum(str);
            } catch (ParseException e) {
                if (errMsg.length() > 0) {
                    errMsg.append("\n");
                }
                errMsg.append(getString(R.string.invalid_amount));
                res = false;
            }
        }

        return res;
    }

    protected void fillOperationWithInputs(Operation op) {
        op.setmMode(mOpModeText.getText().toString().trim());
        op.setmTag(mOpTagText.getText().toString().trim());
        op.setmNotes(mNotesText.getText().toString().trim());
        op.setSumStr(mOpSumText.getText().toString());
        DatePicker dp = mDatePicker;
        dp.clearChildFocus(mActivity.getCurrentFocus());
        op.setDay(dp.getDayOfMonth());
        op.setMonth(dp.getMonth());
        op.setYear(dp.getYear());
        op.setmIsChecked(mIsChecked.isChecked());

        if (mIsTransfertCheck.isChecked()) {
            final Account srcAccount = (Account) mSrcAccount.getSelectedItem();
            final Account dstAccount = (Account) mDstAccount.getSelectedItem();
            if (srcAccount.getId() > 0 && dstAccount.getId() > 0
                    && srcAccount.getId() != dstAccount.getId()) {
                // a valid transfert has been setup
                op.setmTransferAccountId(dstAccount.getId());
                op.setmAccountId(srcAccount.getId());
                op.setmThirdParty(dstAccount.getName().trim());
                op.setmTransSrcAccName(srcAccount.getName());
                // invert sum because with sum > 0 (and I forced it), A->B means -sum in A and +sum in B
                op.setmSum(-op.getmSum());
            } else {
                op.setmThirdParty(mOpThirdPartyText.getText().toString().trim());
            }
        } else {
            op.setmTransferAccountId(0);
            op.setmTransSrcAccName("");
            op.setmThirdParty(mOpThirdPartyText.getText().toString().trim());
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mActivity.mCurrentOp == null) {
            if (mActivity instanceof OperationEditor) {
                mActivity.mCurrentOp = new Operation();
            } else if (mActivity instanceof ScheduledOperationEditor) {
                mActivity.mCurrentOp = fr.geobert.radis.data.DataPackage.ScheduledOperation(mActivity.mCurAccountId);
            }
        }
        fillOperationWithInputs(mActivity.mCurrentOp);
    }

    public void setCheckedEditVisibility(int visibility) {
//        mActivity.findViewById(R.id.checked_title).setVisibility(visibility);
        mIsChecked.setVisibility(visibility);
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        // nothing
    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        // nothing
    }

    @Override
    public void afterTextChanged(Editable editable) {
        mWasInvertByTransfert = false;
    }
}
