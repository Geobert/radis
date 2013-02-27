package fr.geobert.radis.ui.editor;

import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import com.actionbarsherlock.app.SherlockFragment;
import fr.geobert.radis.R;
import fr.geobert.radis.data.Account;
import fr.geobert.radis.data.Operation;
import fr.geobert.radis.data.ScheduledOperation;

public class ScheduleEditorFragment extends SherlockFragment implements OnTransfertCheckedChangeListener {
    ScheduledOperation mCurrentSchOp;
    //    private GestureOverlayView mGestureOverlay;
//    private GestureLibrary mGesturelib = null;
    private DatePicker mEndDatePicker;
    private Spinner mAccountSpinner;
    private Spinner mPeriodicitySpinner;
    private EditText mCustomPeriodicityVal;
    private Spinner mCustomPeriodicityUnit;
    private View mCustomPeriodicityCont;
    private CheckBox mEndDateCheck;
    private ScrollView mFlipperScroll;
    private ScheduledOperationEditor mActivity;
    private OpEditFragmentAccessor mOpEditFragment;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        mActivity = (ScheduledOperationEditor) getSherlockActivity();
        mOpEditFragment = (OpEditFragmentAccessor) mActivity;
        initViewReferences();
        initViewBehavior();
    }

    private void initViewReferences() {
        mAccountSpinner = (Spinner) mActivity.findViewById(R.id.account_choice);
        mPeriodicitySpinner = (Spinner) mActivity.findViewById(R.id.periodicity_choice);
        mCustomPeriodicityCont = mActivity.findViewById(R.id.custom_periodicity);
        mCustomPeriodicityVal = (EditText) mActivity.findViewById(R.id.custom_periodicity_value);
        mCustomPeriodicityUnit = (Spinner) mActivity.findViewById(R.id.custom_periodicity_choice);
        mEndDatePicker = (DatePicker) mActivity.findViewById(R.id.edit_end_date);
        mEndDateCheck = (CheckBox) mActivity.findViewById(R.id.end_date_check);
        mFlipperScroll = (ScrollView) mActivity.findViewById(R.id.flipper_scrollview);
    }

    void populateFields() {
        ScheduledOperation op = mCurrentSchOp;
        mActivity.mCurrentOp = op;
        mCustomPeriodicityVal.setText(mCurrentSchOp.mPeriodicity == 0 ? ""
                : Integer.toString(mCurrentSchOp.mPeriodicity));
        poputatePeriodicitySpinner();
        populateCustomPeriodicitySpinner();
//		populateAccountSpinner(AccountList.getAllAccounts(this));
        if (mCurrentSchOp.getEndDate() > 0) {
            mEndDateCheck.setChecked(true);
            mEndDatePicker.setEnabled(true);
            mEndDatePicker.updateDate(op.getEndYear(), op.getEndMonth(),
                    op.getEndDay());
        } else {
            mEndDateCheck.setChecked(false);
            mEndDatePicker.setEnabled(false);
        }
    }

    private void initViewBehavior() {
        mCustomPeriodicityVal
                .setOnFocusChangeListener(new View.OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View v, boolean hasFocus) {
                        if (hasFocus) {
                            ((EditText) v).selectAll();
                        }
                    }
                });

        mEndDatePicker.setEnabled(false);

        mEndDateCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {
                mEndDatePicker.setEnabled(isChecked);
                mEndDatePicker.clearFocus();
            }
        });
        mEndDateCheck.setChecked(false);
    }

    private void populateCustomPeriodicitySpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                mActivity, R.array.periodicity_custom_choices,
                android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mCustomPeriodicityUnit.setAdapter(adapter);

        int unit = mCurrentSchOp.mPeriodicityUnit;
        if (unit >= ScheduledOperation.CUSTOM_DAILY_PERIOD) {
            mCustomPeriodicityUnit.setSelection(unit
                    - ScheduledOperation.CUSTOM_DAILY_PERIOD);
        }
    }

    private void setCustomPeriodicityVisibility(final boolean isVisible) {
        mCustomPeriodicityCont.setVisibility(isVisible ? View.VISIBLE
                : View.GONE);
    }

    @Override
    public void onTransfertCheckedChanged(boolean isChecked) {
        mAccountSpinner.setEnabled(!isChecked);
        Account acc0 = (Account) mAccountSpinner.getItemAtPosition(0);
        if (acc0 != null) {
            if (isChecked) {
                acc0.mName = getString(R.string.defined_by_transfert);
                mAccountSpinner.setSelection(0);
            } else {
                acc0.mName = getString(R.string.choose_account);
            }
        }
    }

    private void poputatePeriodicitySpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                mActivity, R.array.periodicity_choices,
                android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mPeriodicitySpinner.setAdapter(adapter);
        mPeriodicitySpinner
                .setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent,
                                               View view, int pos, long id) {
                        setCustomPeriodicityVisibility(pos == (parent
                                .getAdapter().getCount() - 1));
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> arg0) {
                    }
                });
        int unit = mCurrentSchOp.mPeriodicityUnit;
        if (unit < ScheduledOperation.CUSTOM_DAILY_PERIOD) {
            mPeriodicitySpinner.setSelection(unit);
        }
    }

    private void populateAccountSpinner(Cursor c) {
        if (c.moveToFirst()) {
            ArrayAdapter<Account> adapter = new ArrayAdapter<Account>(mActivity,
                    android.R.layout.simple_spinner_item);
            adapter.add(new Account(0, getString(R.string.choose_account)));
            do {
                adapter.add(new Account(c));
            } while (c.moveToNext());
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mAccountSpinner.setAdapter(adapter);
            if (mCurrentSchOp.mAccountId != 0 || mActivity.mCurAccountId != 0) {
                int pos = 1;
                while (pos < adapter.getCount()) {
                    long id = adapter.getItemId(pos);
                    if (id == mCurrentSchOp.mAccountId || id == mActivity.mCurAccountId) {
                        mAccountSpinner.setSelection(pos);
                        break;
                    } else {
                        pos++;
                    }
                }
            }
            final boolean isTransfert = mOpEditFragment.isTransfertChecked();
            mAccountSpinner.setEnabled(!isTransfert);
            if (isTransfert) {
                mAccountSpinner.setSelection(mOpEditFragment.getSrcAccountSpinnerIdx());
            }
        }
    }

    protected void fillOperationWithInputs(Operation operation) {
        ScheduledOperation op = (ScheduledOperation) operation;
        if (!mActivity.isTransfertChecked()) {
            Account a = (Account) mAccountSpinner.getSelectedItem();
            op.mAccountId = a.mAccountId;
        }
        final boolean isCustom = mPeriodicitySpinner.getSelectedItemPosition() == (mPeriodicitySpinner
                .getAdapter().getCount() - 1);
        if (!isCustom) {
            op.mPeriodicity = 1;
            op.mPeriodicityUnit = mPeriodicitySpinner.getSelectedItemPosition();
        } else {
            op.mPeriodicity = Integer.parseInt(mCustomPeriodicityVal.getText()
                    .toString());
            op.mPeriodicityUnit = mCustomPeriodicityUnit
                    .getSelectedItemPosition()
                    + ScheduledOperation.CUSTOM_DAILY_PERIOD;
        }
        if (mEndDatePicker.isEnabled()) {
            DatePicker dp = mEndDatePicker;
            dp.clearChildFocus(mActivity.getCurrentFocus());
            op.setEndDay(dp.getDayOfMonth());
            op.setEndMonth(dp.getMonth());
            op.setEndYear(dp.getYear());

        } else {
            op.mEndDate.clear();
        }
    }

    boolean isFormValid(StringBuilder errMsg) {
        ScheduledOperation op = mCurrentSchOp;
        boolean res = true;
        if (mPeriodicitySpinner.getSelectedItemPosition() == (mPeriodicitySpinner
                .getAdapter().getCount() - 1)) {
            try {
                Integer.parseInt(mCustomPeriodicityVal.getText().toString());
            } catch (NumberFormatException e) {
                if (errMsg.length() > 0) {
                    errMsg.append("\n");
                }
                errMsg.append(getString(R.string.periodicity_must_be_num));
                res = false;
            }
        }
        if (res) {
            fillOperationWithInputs(op);
            final boolean hasEnd = op.getEndDate() > 0;
            if (hasEnd && (op.getDate() > op.getEndDate())) {
                if (errMsg.length() > 0) {
                    errMsg.append("\n");
                }
                errMsg.append(getString(R.string.end_date_incorrect));
                res = false;
            }
            if ((op.mPeriodicityUnit >= ScheduledOperation.CUSTOM_DAILY_PERIOD)
                    && op.mPeriodicity <= 0) {
                if (errMsg.length() > 0) {
                    errMsg.append("\n");
                }
                errMsg.append(getString(R.string.periodicity_must_be_greater_0));
                res = false;
            }
        }
        if (!mActivity.isTransfertChecked()) {
            Account a = (Account) mAccountSpinner.getSelectedItem();
            if (a.mAccountId == 0) {
                if (errMsg.length() > 0) {
                    errMsg.append("\n");
                }
                errMsg.append(getString(R.string.sch_no_choosen_account));
                res = false;
            }
        }
        return res;
    }
}
