package fr.geobert.radis.ui.editor;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.*;
import com.actionbarsherlock.app.SherlockFragment;
import fr.geobert.radis.R;
import fr.geobert.radis.data.Account;
import fr.geobert.radis.data.AccountManager;
import fr.geobert.radis.data.Operation;
import fr.geobert.radis.db.DbContentProvider;
import fr.geobert.radis.db.InfoTables;
import fr.geobert.radis.tools.*;

import java.text.ParseException;
import java.util.HashMap;

public class OperationEditFragment extends SherlockFragment {
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
    private HashMap<String, InfoManager> mInfoManagersMap;
    private CheckBox mIsTransfertCheck;
    private CommonOpEditor mActivity;
    private OnTransfertCheckedChangeListener mTransfertCheckedListener = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.main_op_edit, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mActivity = (CommonOpEditor) getSherlockActivity();
        mOpThirdPartyText = (MyAutoCompleteTextView) mActivity.findViewById(R.id.edit_op_third_party);
        mOpModeText = (MyAutoCompleteTextView) mActivity.findViewById(R.id.edit_op_mode);
        mOpSumText = (EditText) mActivity.findViewById(R.id.edit_op_sum);
        mOpTagText = (MyAutoCompleteTextView) mActivity.findViewById(R.id.edit_op_tag);
        mSumTextWatcher = new CorrectCommaWatcher(Formater.getSumFormater()
                .getDecimalFormatSymbols().getDecimalSeparator(), mOpSumText);
        mDatePicker = (DatePicker) mActivity.findViewById(R.id.edit_op_date);
        mSrcAccount = (Spinner) mActivity.findViewById(R.id.trans_src_account);
        mDstAccount = (Spinner) mActivity.findViewById(R.id.trans_dst_account);
        mTransfertCont = (LinearLayout) mActivity.findViewById(R.id.transfert_cont);
        mThirdPartyCont = (LinearLayout) mActivity.findViewById(R.id.third_party_cont);
        mNotesText = (EditText) mActivity.findViewById(R.id.edit_op_notes);
        mInfoManagersMap = new HashMap<String, InfoManager>();

        mOpThirdPartyText.setNextFocusDownId(R.id.edit_op_sum);
        mOpSumText.setNextFocusDownId(R.id.edit_op_tag);
        mOpTagText.setNextFocusDownId(R.id.edit_op_mode);
        mOpModeText.setNextFocusDownId(R.id.edit_op_notes);

        mIsTransfertCheck = (CheckBox) mActivity.findViewById(R.id.is_transfert);
        mIsTransfertCheck
                .setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(CompoundButton arg0,
                                                 boolean arg1) {
                        onTransfertCheckedChanged(arg1);
                    }
                });
        mTransfertCont.setVisibility(View.GONE);
        mThirdPartyCont.post(new Runnable() {
            @Override
            public void run() {
                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) mThirdPartyCont.getLayoutParams();
                params.height = mThirdPartyCont.getMeasuredHeight();
                mTransfertCont.setLayoutParams(params);
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void onResume() {
        super.onResume();
        initViewAdapters();
        initListeners();
    }

    void setTransfertCheckedChangeListener(OnTransfertCheckedChangeListener listener) {
        mTransfertCheckedListener = listener;
    }

    final protected void initListeners() {
        mOpSumText.addTextChangedListener(mSumTextWatcher);
        mActivity.findViewById(R.id.edit_op_third_parties_list).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //showDialog(THIRD_PARTIES_DIALOG_ID);
                    }
                });

        mActivity.findViewById(R.id.edit_op_tags_list).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // showDialog(TAGS_DIALOG_ID);
                    }
                });

        mActivity.findViewById(R.id.edit_op_modes_list).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // showDialog(MODES_DIALOG_ID);
                    }
                });

        mActivity.findViewById(R.id.edit_op_sign).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            invertSign();
                        } catch (ParseException e) {
                            // nothing to do
                        }
                    }
                });
    }

    final protected void onTransfertCheckedChanged(boolean isChecked) {
        Animation in = AnimationUtils.loadAnimation(mActivity,
                android.R.anim.fade_in);
        Animation out = AnimationUtils.makeOutAnimation(mActivity, true);
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
        if (mTransfertCheckedListener != null) {
            mTransfertCheckedListener.onTransfertCheckedChanged(isChecked);
        }
    }

    boolean isTransfertChecked() {
        return mIsTransfertCheck.isChecked();
    }

    int getSrcAccountIdx() {
        return mSrcAccount.getSelectedItemPosition();
    }

    protected void initViewAdapters() {
        mOpThirdPartyText.setAdapter(new InfoAdapter(mActivity,
                DbContentProvider.THIRD_PARTY_URI,
                InfoTables.KEY_THIRD_PARTY_NAME));
        mOpModeText.setAdapter(new InfoAdapter(mActivity,
                DbContentProvider.MODES_URI, InfoTables.KEY_MODE_NAME));
        mOpTagText.setAdapter(new InfoAdapter(mActivity, DbContentProvider.TAGS_URI,
                InfoTables.KEY_TAG_NAME));
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
                adapter.add(new Account(c));
                adapter2.add(new Account(c));
            } while (c.moveToNext());

            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mSrcAccount.setAdapter(adapter);
            mDstAccount.setAdapter(adapter2);
        }

        final boolean isTransfert = mActivity.mCurrentOp.mTransferAccountId > 0;
        mIsTransfertCheck.setChecked(isTransfert);
        if (isTransfert) {
            initAccountSpinner(mSrcAccount, mActivity.mCurrentOp.mAccountId);
            initAccountSpinner(mDstAccount, mActivity.mCurrentOp.mTransferAccountId);
        } else {
            if (mActivity.mCurAccountId != 0) {
                initAccountSpinner(mSrcAccount, mActivity.mCurAccountId);
            }
        }

    }

    private void initAccountSpinner(Spinner spin, long accountId) {
        int pos = 0;
        SpinnerAdapter adapter = spin.getAdapter();
        while (pos < adapter.getCount()) {
            long id = adapter.getItemId(pos);
            if (id == accountId) {
                spin.setSelection(pos);
                break;
            } else {
                pos++;
            }
        }
    }

    final protected void populateCommonFields(Operation op) {
        Tools.setTextWithoutComplete(mOpThirdPartyText, op.mThirdParty);
        Tools.setTextWithoutComplete(mOpModeText, op.mMode);
        Tools.setTextWithoutComplete(mOpTagText, op.mTag);
        mDatePicker.updateDate(op.getYear(), op.getMonth(), op.getDay());
        mActivity.mPreviousSum = op.mSum;
        mNotesText.setText(op.mNotes);
        Tools.setSumTextGravity(mOpSumText);
        if (mActivity.mCurrentOp.mSum == 0.0) {
            mOpSumText.setText("");
            mSumTextWatcher.setAutoNegate(true);
        } else {
            mOpSumText.setText(mActivity.mCurrentOp.getSumStr());
        }
        populateTransfertSpinner(AccountManager.getInstance().getAllAccountsCursor());
    }

    private void invertSign() throws ParseException {
        mSumTextWatcher.setAutoNegate(false);
        Double sum = Formater.getSumFormater()
                .parse(mOpSumText.getText().toString()).doubleValue();
        if (sum != null) {
            sum = -sum;
        }
        mOpSumText.setText(Formater.getSumFormater().format(sum));
    }

    protected boolean isFormValid(StringBuilder errMsg) {
        boolean res = true;
        String str;
        if (mIsTransfertCheck.isChecked()) {
            final Account srcAccount = (Account) mSrcAccount.getSelectedItem();
            final Account dstAccount = (Account) mDstAccount.getSelectedItem();
            if (srcAccount.mAccountId == 0) {
                errMsg.append(getString(R.string.err_transfert_no_src));
                res = false;
            } else if (dstAccount.mAccountId == 0) {
                errMsg.append(getString(R.string.err_transfert_no_dst));
                res = false;
            } else if (srcAccount.mAccountId > 0 && dstAccount.mAccountId > 0 && srcAccount.mAccountId == dstAccount.mAccountId) {
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
                Formater.getSumFormater().parse(str).doubleValue();
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

    private InfoManager createInfoManagerIfNeeded(Uri table, String colName,
                                                  String title, int editId, int deletiId) {
        InfoManager i = mInfoManagersMap.get(table.toString());
        if (null == i) {
            i = new InfoManager(mActivity, title, table, colName, editId, deletiId);
            mInfoManagersMap.put(table.toString(), i);
        }
        return i;
    }

    protected void fillOperationWithInputs(Operation op) {
        op.mMode = mOpModeText.getText().toString().trim();
        op.mTag = mOpTagText.getText().toString().trim();
        op.setSumStr(mOpSumText.getText().toString());
        op.mNotes = mNotesText.getText().toString().trim();

        DatePicker dp = mDatePicker;
        dp.clearChildFocus(mActivity.getCurrentFocus());
        op.setDay(dp.getDayOfMonth());
        op.setMonth(dp.getMonth());
        op.setYear(dp.getYear());

        if (mIsTransfertCheck.isChecked()) {
            final Account srcAccount = (Account) mSrcAccount.getSelectedItem();
            final Account dstAccount = (Account) mDstAccount.getSelectedItem();
            if (srcAccount.mAccountId > 0 && dstAccount.mAccountId > 0
                    && srcAccount.mAccountId != dstAccount.mAccountId) {
                // a valid transfert has been setup
                op.mTransferAccountId = dstAccount.mAccountId;
                op.mAccountId = srcAccount.mAccountId;
                op.mThirdParty = dstAccount.mName.trim();
                op.mTransSrcAccName = srcAccount.mName;
            } else {
                op.mThirdParty = mOpThirdPartyText.getText().toString().trim();
            }
        } else {
            op.mTransferAccountId = 0;
            op.mTransSrcAccName = "";
            op.mThirdParty = mOpThirdPartyText.getText().toString().trim();
        }
    }
}
